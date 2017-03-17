package io.opentracing;

/**
 * ThreadLocalScheduler is a trivial SpanScheduler implementation that relies on Java's thread-local storage primitives.
 *
 * @see SpanScheduler
 * @see Tracer#spanScheduler()
 */
public class ThreadLocalScheduler implements SpanScheduler {
    ThreadLocal<ActivationState> threadLocalActive;

    @Override
    public Span active() {
        ActivationState state = threadLocalActive.get();
        return (state == null) ? null : state.span;
    }

    @Override
    public ActivationState capture(Span span) {
        return new ThreadLocalScheduler.ActivationState(span);
    }

    @Override
    public SpanContext activeContext() {
        Span active = this.active();
        if (active == null) return null;
        return active.context();
    }

    @Override
    public ActivationState captureActive() {
        return capture(active());
    }

    class ActivationState implements SpanScheduler.ActivationState {
        private final Span span;
        private boolean autoFinish;
        private ActivationState toRestore = null;

        @Override
        public Span activate(boolean finishOnDeactivate) {
            toRestore = threadLocalActive.get();
            threadLocalActive.set(this);
            return null;
        }

        @Override
        public void close() {
            this.deactivate();
        }

        @Override
        public void deactivate() {
            if (span != null && this.autoFinish) {
                span.finish();
            }

            if (threadLocalActive.get() != this) {
                // This should not happen; bail out.
                return;
            }
            threadLocalActive.set(toRestore);
        }

        private ActivationState(Span span) {
            this.span = span;
        }
    }
}
