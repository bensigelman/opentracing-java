package io.opentracing.mdcdemo;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private ActiveSpanHolder.Continuation continuation;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable, ActiveSpanHolder activeSpanHolder) {
        this(callable, activeSpanHolder.active());
    }

    public TracedCallable(Callable<T> callable, ActiveSpanHolder.ActiveSpan activeSpan) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.continuation = activeSpan.fork();
    }

    public T call() throws Exception {
        try (ActiveSpanHolder.ActiveSpan activeSpan = continuation.activate()) {
            return callable.call();
        }
    }
}
