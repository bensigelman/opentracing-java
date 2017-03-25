package io.opentracing;

/**
 * ThreadLocalActiveSpanHolder is a trivial ActiveSpanHolder implementation that relies on Java's thread-local storage primitives.
 *
 * @see ActiveSpanHolder
 * @see Tracer#scheduler()
 */
public class ThreadLocalActiveSpanHolder implements ActiveSpanHolder {
    private final ThreadLocal<Continuation> threadLocalActive = new ThreadLocal<Continuation>();

    @Override
    public Continuation active() {
        return threadLocalActive.get();
    }

    @Override
    public Span activeSpan() {
        Continuation state = threadLocalActive.get();
        return (state == null) ? null : state.span;
    }

    @Override
    public Continuation capture(Span span) {
        return new Continuation(span);
    }

    @Override
    public SpanContext activeContext() {
        Span active = this.activeSpan();
        if (active == null) return null;
        return active.context();
    }

    class Continuation implements ActiveSpanHolder.Continuation {
        private final Span span;
        private Continuation toRestore = null;

        private Continuation(Span span) { this.span = span; }

        @Override
        public void activate() {
            toRestore = threadLocalActive.get();
            threadLocalActive.set(this);
        }

        @Override
        public void close() {
            this.deactivate();
        }


        @Override
        public Span span() {
            return span;
        }

        @Override
        public ActiveSpanHolder.Continuation capture() {
            return null;
        }

        @Override
        public void deactivate() {
            if (threadLocalActive.get() != this) {
                // This should not happen; bail out.
                return;
            }
            threadLocalActive.set(toRestore);
        }
    }
}
