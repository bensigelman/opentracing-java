package io.opentracing.mdcdemo;

import io.opentracing.SpanManager;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private SpanManager.SpanClosure spanClosure;
    private SpanManager manager;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable) {
        this(callable, GlobalTracer.get().activeSpanManager());
    }

    public TracedCallable(Callable<T> callable, SpanManager manager) {
        this(callable, manager.active(), manager);
    }

    public TracedCallable(Callable<T> callable, Span span, SpanManager manager) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.manager = manager;
        this.spanClosure = manager.captureActive();
    }

    public T call() throws Exception {
        final Span span = spanClosure.activate();
        try {
            return callable.call();
        } finally {
            spanClosure.deactivate(false);
        }
    }
}
