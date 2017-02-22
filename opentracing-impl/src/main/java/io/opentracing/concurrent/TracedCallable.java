package io.opentracing.concurrent;

import io.opentracing.ActiveSpanManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.impl.GlobalTracer;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private ActiveSpanManager.Snapshot snapshot;
    private ActiveSpanManager manager;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable) {
        this(callable, GlobalTracer.get().activeSpanManager());
    }

    public TracedCallable(Callable<T> callable, ActiveSpanManager manager) {
        this(callable, manager.active(), manager);
    }

    public TracedCallable(Callable<T> callable, Span span, ActiveSpanManager manager) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable= callable;
        this.manager = manager;
        this.snapshot = manager.snapshot(span);
        span.incRef();
    }

    public T call() throws Exception {
        final Span span = manager.activate(snapshot);
        try {
            return callable.call();
        } finally {
            manager.deactivate(snapshot);
        }
    }
}
