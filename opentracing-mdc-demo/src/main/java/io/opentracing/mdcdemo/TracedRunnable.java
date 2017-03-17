package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private SpanScheduler manschedulerer;
    private SpanScheduler.ActivationState activationState;

    public TracedRunnable(Runnable runnable) {
        this(runnable, GlobalTracer.get().spanScheduler());
    }

    public TracedRunnable(Runnable runnable, Span span) {
        this(runnable, span, GlobalTracer.get().spanScheduler());
    }

    public TracedRunnable(Runnable runnable, SpanScheduler manschedulerer) {
        this(runnable, manschedulerer.active(), manschedulerer);
    }

    public TracedRunnable(Runnable runnable, Span span, SpanScheduler manschedulerer) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        this.runnable = runnable;
        this.manschedulerer = manschedulerer;
        this.activationState = manschedulerer.captureActive();
    }

    @Override
    public void run() {
        final Span span = this.activationState.activate(true); // XXX guessing on param value
        try {
            runnable.run();
        } finally {
            this.activationState.deactivate();
        }
    }
}
