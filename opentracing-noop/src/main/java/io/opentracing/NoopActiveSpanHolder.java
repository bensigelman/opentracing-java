package io.opentracing;

import java.io.IOException;

/**
 * A noop (i.e., cheap-as-possible) implementation of a ActiveSpanHolder.
 */
public class NoopActiveSpanHolder implements ActiveSpanHolder {
    public static final ActiveSpan NOOP_ACTIVE_SPAN = new NoopActiveSpan();
    public static final Continuation NOOP_CONTINUATION = new NoopContinuation();

    @Override
    public ActiveSpanHolder.Continuation wrapForActivation(Span span) {
        return NOOP_CONTINUATION;
    }

    @Override
    public ActiveSpan active() { return NOOP_ACTIVE_SPAN; }

    public static class NoopActiveSpan implements ActiveSpanHolder.ActiveSpan {
        @Override
        public Span span() {
            return null;
        }

        @Override
        public void deactivate() {}

        @Override
        public ActiveSpanHolder.Continuation fork() {
            return NOOP_CONTINUATION;
        }

        @Override
        public void close() throws IOException {}
    }
    public static class NoopContinuation implements ActiveSpanHolder.Continuation {
        @Override
        public ActiveSpan activate() {
            return NOOP_ACTIVE_SPAN;
        }
    }
}
