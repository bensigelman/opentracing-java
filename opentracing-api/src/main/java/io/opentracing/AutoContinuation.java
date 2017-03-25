package io.opentracing;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bhs on 3/23/17.
 */
public class AutoContinuation implements ActiveSpanHolder.Continuation {
    private ActiveSpanHolder.Continuation wrapped;
    private final AtomicInteger refCount;

    public static AutoContinuation wrap(ActiveSpanHolder.Continuation wrapped) {
        AutoContinuation cont = new AutoContinuation(wrapped, new AtomicInteger(1));
        // cont.activate();
        return cont;
    }

    private AutoContinuation(ActiveSpanHolder.Continuation wrapped, AtomicInteger refCount) {
        this.wrapped = wrapped;
        this.refCount = refCount;
    }

    @Override
    public void activate() {
        wrapped.activate();
    }

    @Override
    public Span span() {
        return wrapped.span();
    }

    @Override
    public ActiveSpanHolder.Continuation capture() {
        System.out.println("INC: " + refCount.get());
        refCount.incrementAndGet();
        return new AutoContinuation(wrapped.capture(), refCount);
    }

    @Override
    public void deactivate() {
        System.out.println("DEC: " + refCount.get());
        if (refCount.decrementAndGet() == 0) {
            if (wrapped.span() != null) {
                wrapped.span().finish();
            }
        }
        wrapped.deactivate();
    }

    @Override
    public void close() throws IOException {
        this.deactivate();
    }
}
