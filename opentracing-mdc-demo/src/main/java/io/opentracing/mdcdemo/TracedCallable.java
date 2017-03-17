package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private SpanScheduler.ActivationState activationState;
    private SpanScheduler manager;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable) {
        this(callable, GlobalTracer.get().spanScheduler());
    }

    public TracedCallable(Callable<T> callable, SpanScheduler manager) {
        this(callable, manager.active(), manager);
    }

    public TracedCallable(Callable<T> callable, Span span, SpanScheduler manager) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.manager = manager;
        this.activationState = manager.captureActive();
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
