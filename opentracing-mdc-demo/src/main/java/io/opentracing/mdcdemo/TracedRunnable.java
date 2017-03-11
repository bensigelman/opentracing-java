package io.opentracing.mdcdemo;

import io.opentracing.SpanManager;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private SpanManager manager;
    private SpanManager.SpanClosure spanClosure;

    public TracedRunnable(Runnable runnable) {
        this(runnable, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, Span span) {
        this(runnable, span, GlobalTracer.get().activeSpanManager());
    }

    public TracedRunnable(Runnable runnable, SpanManager manager) {
        this(runnable, manager.active(), manager);
    }

    public TracedRunnable(Runnable runnable, Span span, SpanManager manager) {
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
