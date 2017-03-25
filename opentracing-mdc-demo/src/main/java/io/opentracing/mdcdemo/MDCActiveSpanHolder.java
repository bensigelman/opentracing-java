package io.opentracing.mdcdemo;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDCActiveSpanHolder illustrates the core ActiveSpanHolder concepts and capabilities to a first approximation. Not
 * production-quality code.
 */
public class MDCActiveSpanHolder implements ActiveSpanHolder {
    private final ThreadLocal<MDCContinuation> tlsSnapshot = new ThreadLocal<MDCContinuation>();

    class MDCContinuation implements Continuation {
        private final Map<String, String> mdcContext;
        private final Span span;
        private MDCContinuation toRestore = null;

        MDCContinuation(Span span) {
            this.mdcContext = MDC.getCopyOfContextMap();
            this.span = span;
        }

        @Override
        public void activate() {
            toRestore = tlsSnapshot.get();
            tlsSnapshot.set(this);
        }

        @Override
        public Span span() {
            return span;
        }

        @Override
        public MDCContinuation capture() {
            return new MDCContinuation(span);
        }

        @Override
        public void close() {
            this.deactivate();
        }

        @Override
        public void deactivate() {
            if (tlsSnapshot.get() != this) {
                // This shouldn't happen if users call methods in the expected order. Bail out.
                return;
            }
            tlsSnapshot.set(toRestore);
        }
    }

    @Override
    public MDCContinuation capture(Span span) {
        return new MDCContinuation(span);
    }

    @Override
    public Continuation active() {
        return tlsSnapshot.get();
    }

    @Override
    public Span activeSpan() {
        final MDCContinuation snapshot = tlsSnapshot.get();
        if (snapshot == null) {
            return null;
        }
        return snapshot.span;
    }

    @Override
    public SpanContext activeContext() {
        final Span active = this.activeSpan();
        if (active == null) return null;
        return active.context();
    }

}
