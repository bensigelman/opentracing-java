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

        @Override
        public Span activate() {
            toRestore = tlsSnapshot.get();
            tlsSnapshot.set(this);
            return span;
        }

        @Override
        public void deactivate() {
            if (span != null) {
                span.decRef();
            }

            if (tlsSnapshot.get() != this) {
                // do nothing
                return;
            }
            MDCSnapshot nextActiveSnapshot = toRestore;
            while (nextActiveSnapshot != null) {
                if (!nextActiveSnapshot.span.isFinished()) {
                    break;
                }
                nextActiveSnapshot = nextActiveSnapshot.toRestore;
            }
            tlsSnapshot.set(nextActiveSnapshot);
        }
    }

    @Override
    public MDCSnapshot snapshot(Span span) {
        return new MDCSnapshot(span);
    }

    @Override
    public Span active() {
        MDCSnapshot snapshot = tlsSnapshot.get();
        if (snapshot == null) {
            return null;
        }
        return snapshot.span;
    }
}
