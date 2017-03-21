package io.opentracing.mdcdemo;

import io.opentracing.Scheduler;
import io.opentracing.Span;

import java.util.concurrent.Callable;

public class TracedCallable<T> implements Callable<T> {
    private Scheduler.Continuation continuation;
    private Scheduler scheduler;
    private Callable<T> callable;

    public TracedCallable(Callable<T> callable, Scheduler scheduler) {
        this(callable, scheduler.active(), scheduler);
    }

    public TracedCallable(Callable<T> callable, Span span, Scheduler scheduler) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        this.callable = callable;
        this.scheduler = scheduler;
        this.continuation = scheduler.captureActive();
    }

    public T call() throws Exception {
<<<<<<< Updated upstream
        final Span span = continuation.activate(true);
=======
        // NOTE: There's no way to be sure about the finishOnDeactivate parameter to activate(), so we play it safe.
        final Span span = continuation.activate(false);
>>>>>>> Stashed changes
        try {
            return callable.call();
        } finally {
            continuation.deactivate();
        }
    }
}
