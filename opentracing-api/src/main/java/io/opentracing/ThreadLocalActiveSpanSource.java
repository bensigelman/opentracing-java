package io.opentracing;

/**
 * ThreadLocalActiveSpanSource is a trivial ActiveSpanSource implementation that relies on Java's thread-local storage primitives.
 *
 * @see ActiveSpanSource
 * @see Tracer#spanSource()
 */
public class ThreadLocalActiveSpanSource implements ActiveSpanSource {
    @Override
    public Handle active() {
        return null;
    }

    @Override
    public Handle adopt(Span span) {
        return null;
    }
}
