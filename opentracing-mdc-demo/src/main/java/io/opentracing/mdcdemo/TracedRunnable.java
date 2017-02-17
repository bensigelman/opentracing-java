package io.opentracing.mdcdemo;

import io.opentracing.SpanScheduler;
import io.opentracing.Span;
import io.opentracing.impl.GlobalTracer;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private SpanScheduler manschedulerer;
    private SpanScheduler.Continuation continuation;

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
        this.continuation = manschedulerer.captureActive();
    }

    @Override
    public void run() {
        final Span span = this.continuation.activate(true); // XXX guessing on param value
        try {
            runnable.run();
        } finally {
            this.continuation.deactivate();
        }
    }
}
