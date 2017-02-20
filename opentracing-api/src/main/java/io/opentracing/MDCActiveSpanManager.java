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
    public Span activate(Snapshot snapshot) {
        if (!(snapshot instanceof MDCSnapshot)) {
            throw new IllegalArgumentException("activate() expected MDCSnapshot");
        }
        ((MDCSnapshot) snapshot).setToRestore(tlsSnapshot.get());
        tlsSnapshot.set((MDCSnapshot)snapshot);
        return snapshot.span();
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
