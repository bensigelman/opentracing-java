package io.opentracing.mdcdemo;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;

import java.io.IOException;


public class TracedRunnable implements Runnable {
    private Runnable runnable;
    private ActiveSpanHolder.Continuation continuation;

    public TracedRunnable(Runnable runnable, ActiveSpanHolder holder) {
        this(runnable, holder.active());
    }

    public TracedRunnable(Runnable runnable, ActiveSpanHolder.ActiveSpan activeSpan) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        this.runnable = runnable;
        this.continuation = activeSpan.fork();
    }

    @Override
    public void run() {
        // NOTE: There's no way to be sure about the finishOnDeactivate parameter to activate(), so we play it safe.
        try (ActiveSpanHolder.ActiveSpan activeSpan = this.continuation.activate()) {
            runnable.run();
        } catch (IOException e) {
            // Do nothing?
        }
    }
}
