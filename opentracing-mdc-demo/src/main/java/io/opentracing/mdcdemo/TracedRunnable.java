package io.opentracing.mdcdemo;

import io.opentracing.Scheduler;
import io.opentracing.Span;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private Scheduler schedulerer;
    private Scheduler.Continuation continuation;

    public TracedRunnable(Runnable runnable, Scheduler schedulerer) {
        this(runnable, schedulerer.active(), schedulerer);
    }

    public TracedRunnable(Runnable runnable, Span span, Scheduler schedulerer) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        this.runnable = runnable;
        this.schedulerer = schedulerer;
        this.continuation = schedulerer.captureActive();
    }

    @Override
    public void run() {
        // NOTE: There's no way to be sure about the finishOnDeactivate parameter to activate(), so we play it safe.
        final Span span = this.continuation.activate(false);
        try {
            runnable.run();
        } finally {
            this.continuation.deactivate();
        }
    }
}
