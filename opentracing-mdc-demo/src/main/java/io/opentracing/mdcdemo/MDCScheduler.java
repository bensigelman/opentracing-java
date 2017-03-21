package io.opentracing.mdcdemo;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Scheduler;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDCScheduler illustrates the core Scheduler concepts and capabilities to a first approximation. Not
 * production-quality code.
 */
public class MDCScheduler implements Scheduler {
<<<<<<< Updated upstream
    private final ThreadLocal<MDCSnapshot> tlsSnapshot = new ThreadLocal<MDCSnapshot>();

    class MDCSnapshot implements Continuation {
        private final Map<String, String> mdcContext;
        private final Span span;
        private boolean finishOnDeactivate;
        private MDCSnapshot toRestore = null;

        MDCSnapshot(Span span) {
=======
    private final ThreadLocal<MDCContinuation> tlsSnapshot = new ThreadLocal<MDCContinuation>();

    class MDCContinuation implements Continuation {
        private final Map<String, String> mdcContext;
        private final Span span;
        private boolean finishOnDeactivate;
        private MDCContinuation toRestore = null;

        MDCContinuation(Span span) {
>>>>>>> Stashed changes
            this.mdcContext = MDC.getCopyOfContextMap();
            this.span = span;
        }

        @Override
        public Span activate(boolean finishOnDeactivate) {
            this.finishOnDeactivate = finishOnDeactivate;
            toRestore = tlsSnapshot.get();
            tlsSnapshot.set(this);
            return span;
        }

        @Override
        public void close() {
            this.doDeactivate(this.finishOnDeactivate);
        }

        @Override
        public void deactivate() {
            doDeactivate(this.finishOnDeactivate);
        }

        private void doDeactivate(boolean finishSpan) {
            if (span != null && finishSpan) {
                span.finish();
            }

            if (tlsSnapshot.get() != this) {
                // This shouldn't happen if users call methods in the expected order. Bail out.
                return;
            }
            tlsSnapshot.set(toRestore);
        }
    }

    @Override
<<<<<<< Updated upstream
    public MDCSnapshot captureActive() {
        return new MDCSnapshot(active());
    }

    @Override
    public MDCSnapshot capture(Span span) {
        return new MDCSnapshot(span);
=======
    public MDCContinuation captureActive() {
        return new MDCContinuation(active());
    }

    @Override
    public MDCContinuation capture(Span span) {
        return new MDCContinuation(span);
>>>>>>> Stashed changes
    }

    @Override
    public Span active() {
<<<<<<< Updated upstream
        MDCSnapshot snapshot = tlsSnapshot.get();
=======
        MDCContinuation snapshot = tlsSnapshot.get();
>>>>>>> Stashed changes
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
