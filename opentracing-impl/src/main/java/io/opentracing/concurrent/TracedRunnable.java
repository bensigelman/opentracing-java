package io.opentracing.concurrent;

import io.opentracing.ActiveSpanManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private ActiveSpanManager manager;
    private ActiveSpanManager.Snapshot snapshot;

    public TracedRunnable(Runnable runnable) {
        this(runnable, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, Span span) {
        this(runnable, span, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, ActiveSpanManager manager) {
        this(runnable, manager.active(), manager);
    }

    public TracedRunnable(Runnable runnable, Span span, ActiveSpanManager manager) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        this.runnable = runnable;
        this.manager = manager;
        this.snapshot = manager.snapshot(span);
        span.incRef();
    }

    @Override
    public void run() {
        final Span span = this.snapshot.activate();
        try {
            runnable.run();
        } finally {
            this.snapshot.deactivate();
        }
    }
}
