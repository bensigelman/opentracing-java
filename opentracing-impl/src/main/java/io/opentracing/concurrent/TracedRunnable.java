package io.opentracing.concurrent;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private SpanSnapshot snapshot;
    private Tracer tracer;

    public TracedRunnable(Runnable runnable) {
        this(runnable, GlobalTracer.get());
    }

    public TracedRunnable(Runnable runnable, Span span) {
        this(runnable, span, GlobalTracer.get());
    }

    public TracedRunnable(Runnable runnable, Tracer tracer) {
        this(runnable, tracer.active(), tracer);
    }

    public TracedRunnable(Runnable runnable, Span span, Tracer tracer) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        if (tracer == null) throw new NullPointerException("Tracer is <null>.");
        this.runnable = runnable;
        this.snapshot = tracer.snapshot(span);
        this.tracer = tracer;
    }

    @Override
    public void run() {
        final Span span = tracer.resume(snapshot);
        try {
            runnable.run();
        } finally {
            tracer.deactivate(span);
        }
    }
}
