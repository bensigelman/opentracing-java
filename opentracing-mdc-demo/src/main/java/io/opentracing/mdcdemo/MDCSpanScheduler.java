package io.opentracing.mdcdemo;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.SpanScheduler;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDCSpanScheduler illustrates the core SpanScheduler concepts and capabilities to a first approximation. Not
 * production-quality code.
 */
public class MDCSpanScheduler implements SpanScheduler {
    private final ThreadLocal<MDCSnapshot> tlsSnapshot = new ThreadLocal<MDCSnapshot>();

    class MDCSnapshot implements Continuation {
        private final Map<String, String> mdcContext;
        private final Span span;
        private boolean autoFinish;
        private MDCSnapshot toRestore = null;

        MDCSnapshot(Span span) {
            this.mdcContext = MDC.getCopyOfContextMap();
            this.span = span;
        }

        @Override
        public Span activate(boolean autoFinish) {
            this.autoFinish = autoFinish;
            toRestore = tlsSnapshot.get();
            tlsSnapshot.set(this);
            return span;
        }

        @Override
        public void close() {
            this.doDeactivate(this.autoFinish);
        }

        @Override
        public void deactivate() {
            doDeactivate(this.autoFinish);
        }

        private void doDeactivate(boolean finishSpan) {
            if (span != null && finishSpan) {
                span.finish();
            }

            if (tlsSnapshot.get() != this) {
                // This probably shouldn't happen.
                //
                // XXX: log or throw something here?
                return;
            }
            tlsSnapshot.set(toRestore);
        }
    }

    @Override
    public MDCSnapshot captureActive() {
        return new MDCSnapshot(active());
    }

    @Override
    public MDCSnapshot capture(Span span) {
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

    @Override
    public SpanContext activeContext() {
        Span active = this.active();
        if (active == null) return null;
        return active.context();
    }
}
