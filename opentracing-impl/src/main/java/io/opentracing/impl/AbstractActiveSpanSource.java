package io.opentracing.impl;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpanSource;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractActiveSpanSource} implements {@link #adopt} for {@link AbstractActiveSpan}.
 */
public abstract class AbstractActiveSpanSource implements ActiveSpanSource {

    @Override
    public final ActiveSpan adopt(Span span) {
        return makeContinuation(span, new AtomicInteger(1)).activate();
    }

    protected abstract AbstractActiveSpan.Continuation makeContinuation(Span wrapped, AtomicInteger refCount);
}
