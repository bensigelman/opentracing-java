package io.opentracing.mdcdemo;

import io.opentracing.Span;
import io.opentracing.SpanScheduler;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDCSpanScheduler illustrates the core SpanScheduler concepts and capabilities to a first approximation. Not
 * production-quality code.
 */
public class MDCSpanScheduler implements SpanScheduler {
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
        public void close() {
            this.deactivate(true);
        }

        @Override
        public Span span() {
            return span;
        }

        @Override
        public void deactivate(boolean finishSpan) {
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
    public SpanClosure onStart(Span span) {
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
    public void onFinish(Span span) {
        MDCSnapshot snapshot = tlsSnapshot.get();
        MDCSnapshot prevSnapshot = null;
        while (snapshot != null) {
            if (snapshot.span == span) {
               if (prevSnapshot == null) {
                   tlsSnapshot.set(snapshot.toRestore);
               } else {
                   prevSnapshot.toRestore = snapshot.toRestore;
               }
            }
            prevSnapshot = snapshot;
            snapshot = snapshot.toRestore;
        }
    }
}
