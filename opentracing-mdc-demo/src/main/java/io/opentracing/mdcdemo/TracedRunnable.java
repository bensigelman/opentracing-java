package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private SpanScheduler manager;
    private SpanScheduler.SpanClosure spanClosure;

    public TracedRunnable(Runnable runnable) {
        this(runnable, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, Span span) {
        this(runnable, span, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, SpanScheduler manager) {
        this(runnable, manager.active(), manager);
    }

    public TracedRunnable(Runnable runnable, Span span, SpanScheduler manager) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        this.runnable = runnable;
        this.manager = manager;
        this.spanClosure = manager.captureActive();
    }

    @Override
    public void run() {
        final Span span = this.spanClosure.activate();
        try {
            runnable.run();
        } finally {
            this.spanClosure.deactivate(false);
        }
    }
}
