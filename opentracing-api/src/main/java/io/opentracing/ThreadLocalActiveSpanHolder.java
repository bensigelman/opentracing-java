package io.opentracing;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadLocalActiveSpanHolder is a trivial ActiveSpanHolder implementation that relies on Java's thread-local storage primitives.
 *
 * @see ActiveSpanHolder
 * @see Tracer#holder()
 */
public class ThreadLocalActiveSpanHolder implements ActiveSpanHolder {
    @Override
    public ActiveSpan active() {
        return null;
    }

    @Override
    public Continuation wrapForActivation(Span span) {
        return null;
    }
}
