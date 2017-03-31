package io.opentracing.impl;

import io.opentracing.ActiveSpanHolder;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bhs on 3/30/17.
 */
public abstract class AbstractActiveSpanHolder implements ActiveSpanHolder {

    public abstract class AbstractActiveSpan implements ActiveSpanHolder.ActiveSpan {
        private final AtomicInteger refCount;

        protected AbstractActiveSpan(AtomicInteger refCount) {
            this.refCount = refCount;
        }

        @Override
        public final void deactivate() {
            doDeactivate();
            decRef();
        }

        /**
         * Per the {@link java.io.Closeable} API.
         */
        @Override
        public final void close() {
            this.deactivate();
        }

        /**
         * Implementations must clean up any state (including thread-locals, etc) associated with the previosly active
         * {@link Span}.
         */
        protected abstract void doDeactivate();

        /**
         * Return the {@link ActiveSpanHolder} associated wih this {@link Continuation}.
         */
        protected abstract ActiveSpanHolder holder();

        /**
         * Decrement the {@link Continuation}'s reference count, calling {@link Span#finish()} if no more references
         * remain.
         */
        final void decRef() {
            if (0 == refCount.decrementAndGet()) {
                Span span = this.span();
                if (span != null) {
                    this.span().finish();
                }
            }
        }

        /**
         * Fork and take a reference to the {@link Span} associated with this {@link Continuation} and any 3rd-party
         * execution context of interest.
         *
         * @return a new {@link Continuation} to {@link Continuation#activate()} at the appropriate time.
         */
        @Override
        public final Continuation fork() {
            refCount.incrementAndGet();
            return ((AbstractActiveSpanHolder)holder()).doMakeContinuation(span(), refCount);
        }
    }

    public abstract class AbstractContinuation implements ActiveSpanHolder.Continuation {
        protected final AtomicInteger refCount;

        protected AbstractContinuation(AtomicInteger refCount) {
            this.refCount = refCount;
        }

    }

    @Override
    public final Continuation wrapForActivation(Span span) {
        Continuation rval = doMakeContinuation(span, new AtomicInteger(1));
        return rval;
    }

    protected abstract Continuation doMakeContinuation(Span span, AtomicInteger refCount);
}
