package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private SpanScheduler.Continuation continuation;
    private SpanScheduler scheduler;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable, SpanScheduler scheduler) {
        this(callable, scheduler.active(), scheduler);
    }

    public TracedCallable(Callable<T> callable, Span span, SpanScheduler scheduler) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.scheduler = scheduler;
        this.continuation = scheduler.captureActive();
    }

    public T call() throws Exception {
        final Span span = continuation.activate(true);
        try {
            return callable.call();
        } finally {
            continuation.deactivate();
        }
    }
}
