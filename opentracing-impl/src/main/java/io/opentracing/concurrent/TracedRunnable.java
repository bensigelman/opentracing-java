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
    }

    @Override
    public void run() {
        final Span span = manager.activate(this.snapshot);
        try {
            runnable.run();
        } finally {
            manager.deactivate(this.snapshot);
        }
    }
}
