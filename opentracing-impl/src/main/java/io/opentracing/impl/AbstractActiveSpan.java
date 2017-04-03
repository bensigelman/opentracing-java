package io.opentracing.impl;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractActiveSpan} deals with {@link Source}/{@link ActiveSpan}/{@link Continuation}
 * reference counting while allowing subclasses to extend with bindings to ThreadLocals, etc.
 */
public abstract class AbstractActiveSpan implements ActiveSpan {
    private final AtomicInteger refCount;
    protected final Span wrapped;

    protected AbstractActiveSpan(Span wrapped, AtomicInteger refCount) {
        this.wrapped = wrapped;
        this.refCount = refCount;
    }

    @Override
    public final void deactivate() {
        doDeactivate();
        decRef();
    }

    /**
     * Per the {@link java.io.Closeable} API.
     */
    @Override
    public final void close() {
        this.deactivate();
    }

    /**
     * Implementations must clean up any state (including thread-locals, etc) associated with the previosly active
     * {@link Span}.
     */
    protected abstract void doDeactivate();

    /**
     * Return the {@link Source} associated wih this {@link Continuation}.
     */
    protected abstract Source spanSource();

    /**
     * Decrement the {@link Continuation}'s reference count, calling {@link Span#finish()} if no more references
     * remain.
     */
    final void decRef() {
        if (0 == refCount.decrementAndGet()) {
            this.finish();
        }
    }

    /**
     * Fork and take a reference to the {@link Span} associated with this {@link Continuation} and any 3rd-party
     * execution context of interest.
     *
     * @return a new {@link Continuation} to {@link Continuation#activate()} at the appropriate time.
     */
    @Override
    public final Continuation defer() {
        refCount.incrementAndGet();
        return ((AbstractSource) spanSource()).makeContinuation(this.wrapped, refCount);
    }

    @Override
    public SpanContext context() {
        return wrapped.context();
    }

    @Override
    public void finish() {
        wrapped.finish();
    }

    @Override
    public void finish(long finishMicros) {
        wrapped.finish(finishMicros);
    }

    @Override
    public Span setTag(String key, String value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span setTag(String key, boolean value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span setTag(String key, Number value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span log(Map<String, ?> fields) {
        return wrapped.log(fields);
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        return wrapped.log(timestampMicroseconds, fields);
    }

    @Override
    public Span log(String event) {
        return wrapped.log(event);
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        return wrapped.log(timestampMicroseconds, event);
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        return wrapped.setBaggageItem(key, value);
    }

    @Override
    public String getBaggageItem(String key) {
        return wrapped.getBaggageItem(key);
    }

    @Override
    public Span setOperationName(String operationName) {
        return wrapped.setOperationName(operationName);
    }

    @Override
    public Span log(String eventName, Object payload) {
        return wrapped.log(eventName, payload);
    }

    @Override
    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        return wrapped.log(timestampMicroseconds, eventName, payload);
    }

    /**
     * A helper implementation that does the grunt work of the {@link Continuation} API.
     */
    public abstract static class AbstractContinuation implements Continuation {
        protected final AtomicInteger refCount;

        protected AbstractContinuation(AtomicInteger refCount) {
            this.refCount = refCount;
        }

    }

    public abstract static class AbstractSource implements Source {

        @Override
        public final ActiveSpan adopt(Span span) {
            return makeContinuation(span, new AtomicInteger(1)).activate();
        }

        protected abstract Continuation makeContinuation(Span wrapped, AtomicInteger refCount);
    }
}
