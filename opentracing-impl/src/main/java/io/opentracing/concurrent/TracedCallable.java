package io.opentracing.concurrent;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.impl.GlobalTracer;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private SpanSnapshot snapshot;
    private Tracer tracer;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable) {
        this(callable, GlobalTracer.get());
    }

    public TracedCallable(Callable<T> callable, Tracer tracer) {
        this(callable, tracer.active(), tracer);
    }

    public TracedCallable(Callable<T> callable, Span span, Tracer tracer) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        if (tracer == null) throw new NullPointerException("Tracer is <null>.");
        this.callable= callable;
        this.snapshot = tracer.snapshot(span);
        this.tracer = tracer;
    }

    public T call() throws Exception {
        final Span span = tracer.resume(snapshot);
        try {
            return callable.call();
        } finally {
            tracer.deactivate(span);
        }
    }
}
