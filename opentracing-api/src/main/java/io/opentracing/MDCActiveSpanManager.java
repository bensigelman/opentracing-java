package io.opentracing;

import org.slf4j.MDC;
import sun.awt.image.ImageWatched;

import java.util.Map;

/**
 * XXX: comment
 */
public class MDCActiveSpanManager implements io.opentracing.ActiveSpanManager {
    private final ThreadLocal<MDCSnapshot> tlsSnapshot = new ThreadLocal<MDCSnapshot>();

    class MDCSnapshot implements ActiveSpanManager.Snapshot {
        private final Map<String, String> mdcContext;
        private final Span span;
        private MDCSnapshot toRestore = null;

        MDCSnapshot(Span span) {
            this.mdcContext = MDC.getCopyOfContextMap();
            this.span = span;
        }

        public Span span() {
            return span;
        }

        void setToRestore(MDCSnapshot toRestore) {
            this.toRestore = toRestore;
        }

        MDCSnapshot toRestore() {
            return toRestore;
        }
    }

    @Override
    public Span active() {
        MDCSnapshot snapshot = tlsSnapshot.get();
        if (snapshot == null) {
            return null;
        }
        return snapshot.span();
    }

    @Override
    public MDCSnapshot snapshot(Span span) {
        return new MDCSnapshot(span);
    }

    @Override
    public void activate(Snapshot snapshot) {
        if (!(snapshot instanceof MDCSnapshot)) {
            throw new IllegalArgumentException("activate() expected MDCSnapshot");
        }
        ((MDCSnapshot) snapshot).setToRestore(tlsSnapshot.get());
        tlsSnapshot.set((MDCSnapshot)snapshot);
    }

    @Override
    public void deactivate(Snapshot snapshot) {
        if (!(snapshot instanceof MDCSnapshot)) {
            throw new IllegalArgumentException("deactivate() expected MDCSnapshot");
        }

        if (tlsSnapshot.get() != snapshot) {
            // do nothing
            return;
        }
        MDCSnapshot nextActiveSnapshot = ((MDCSnapshot)snapshot).toRestore();
        while (nextActiveSnapshot != null) {
            if (!nextActiveSnapshot.span().isFinished()) {
                break;
            }
            nextActiveSnapshot = nextActiveSnapshot.toRestore();
        }
        tlsSnapshot.set(nextActiveSnapshot);
    }
}

/*

STREAM OF CONSCIOUSNESS:

When the user starts a new Span:
 - Span goes into TLS immediately
 - Existing TLS Span must become a parent pointer or similar

When a Span comes into the foreground:
 - must adopt the context from the last time it was paused or created
 - must retain a pointer to whatever it replaced

When a Span goes into the background or finishes:
 - restore whatever retained pointer there was
   - ... and if that pointer has already been finished?

I'm troubled by the case where multiple children are created and then the parent finishes before the children.

[ Span P        ]
  [ Span A                 ]
  [ Span B            ]
  [ Span C                      ]

(Where P is the parent of A, B, and C)

When A-C finish in that scenario, what should happen to the threadlocal?

What if all "managed" activity had to happen in the context of a Closure/Runnable? Return value could indicate whether
the Span should finish or not.

 */
