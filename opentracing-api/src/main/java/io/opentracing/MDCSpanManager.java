package io.opentracing;

import org.slf4j.MDC;

import java.util.Map;

/**
 * XXX: comment
 */
public class MDCSpanManager implements SpanManager {
    private final ThreadLocal<MDCSnapshot> tlsSnapshot = new ThreadLocal<MDCSnapshot>();

    class MDCSnapshot implements SpanClosure {
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
    public MDCSnapshot captureActive() {
        return new MDCSnapshot(active());
    }
    @Override
    public MDCSnapshot captureWithSpan(Span span) {
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
