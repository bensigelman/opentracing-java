package io.opentracing;

/**
 * ThreadLocalScheduler is a trivial SpanScheduler implementation that relies on Java's thread-local storage primitives.
 *
 * @see SpanScheduler
 * @see Tracer#spanScheduler()
 */
public class ThreadLocalScheduler implements SpanScheduler {
    ThreadLocal<Continuation> threadLocalActive = new ThreadLocal<>();

    @Override
    public Span active() {
        Continuation state = threadLocalActive.get();
        return (state == null) ? null : state.span;
    }

    @Override
    public Continuation capture(Span span) {
        return new Continuation(span);
    }

    @Override
    public SpanContext activeContext() {
        Span active = this.active();
        if (active == null) return null;
        return active.context();
    }

    @Override
    public Continuation captureActive() {
        return capture(active());
    }

    class Continuation implements SpanScheduler.Continuation {
        private final Span span;
        private boolean autoFinish;
        private Continuation toRestore = null;

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

        private Continuation(Span span) {
            this.span = span;
        }
    }
}
