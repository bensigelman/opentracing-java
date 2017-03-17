package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private SpanScheduler.ActivationState activationState;
    private SpanScheduler scheduler;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable) {
        this(callable, GlobalTracer.get().spanScheduler());
    }

    public TracedCallable(Callable<T> callable, SpanScheduler scheduler) {
        this(callable, scheduler.active(), scheduler);
    }

    public TracedCallable(Callable<T> callable, Span span, SpanScheduler scheduler) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.scheduler = scheduler;
        this.activationState = scheduler.captureActive();
    }

    public T call() throws Exception {
        final Span span = activationState.activate(true);
        try {
            return callable.call();
        } finally {
            activationState.deactivate();
        }
    }
}
