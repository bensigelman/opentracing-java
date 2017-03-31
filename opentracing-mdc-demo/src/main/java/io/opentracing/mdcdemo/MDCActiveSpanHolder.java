package io.opentracing.mdcdemo;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;
import io.opentracing.impl.AbstractActiveSpanHolder;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MDCActiveSpanHolder illustrates the core ActiveSpanHolder concepts and capabilities to a first approximation. Not
 * production-quality code.
 */
public class MDCActiveSpanHolder extends AbstractActiveSpanHolder {
    private final ThreadLocal<MDCActiveSpan> tlsSnapshot = new ThreadLocal<MDCActiveSpan>();

    class MDCActiveSpan extends AbstractActiveSpan {
        private final Span span;
        private MDCActiveSpan toRestore = null;

        MDCActiveSpan(Span span, Map<String, String> mdcContext, AtomicInteger refCount) {
            super(refCount);
            this.span = span;
            this.toRestore = tlsSnapshot.get();
            tlsSnapshot.set(this);
            MDC.setContextMap(mdcContext);
        }

        @Override
        public Span span() {
            return span;
        }

        @Override
        protected void doDeactivate() {
            if (tlsSnapshot.get() != this) {
                // This shouldn't happen if users call methods in the expected order. Bail out.
                return;
            }
            tlsSnapshot.set(toRestore);
        }

        @Override
        protected ActiveSpanHolder holder() {
            return MDCActiveSpanHolder.this;
        }

    }
    class MDCContinuation extends AbstractContinuation {
        private final Map<String, String> mdcContext;
        private final Span span;

        MDCContinuation(Span span, AtomicInteger refCount) {
            super(refCount);
            this.mdcContext = MDC.getCopyOfContextMap();
            this.span = span;
        }

        @Override
        public MDCActiveSpan activate() {
            return new MDCActiveSpan(span, mdcContext, refCount);
        }
    }

    @Override
    protected MDCContinuation doMakeContinuation(Span span, AtomicInteger refCount) {
        return new MDCContinuation(span, refCount);
    }

    @Override
    public MDCActiveSpan active() {
        return tlsSnapshot.get();
    }

}
