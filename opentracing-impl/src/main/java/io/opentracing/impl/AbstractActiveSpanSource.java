package io.opentracing.impl;

import io.opentracing.ActiveSpanSource;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbstractActiveSpanSource deals with {@link ActiveSpanSource}/{@link Handle}/{@link Continuation}
 * reference counting while allowing subclasses to extend with bindings to ThreadLocals, etc.
 */
public abstract class AbstractActiveSpanSource implements ActiveSpanSource {

    public abstract class AbstractHandle implements Handle {
        private final AtomicInteger refCount;

        protected AbstractHandle(AtomicInteger refCount) {
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
         * Return the {@link ActiveSpanSource} associated wih this {@link Continuation}.
         */
        protected abstract ActiveSpanSource spanSource();

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
        public final Continuation defer() {
            refCount.incrementAndGet();
            return ((AbstractActiveSpanSource) spanSource()).makeContinuation(span(), refCount);
        }
    }

    public abstract class AbstractContinuation implements ActiveSpanSource.Continuation {
        protected final AtomicInteger refCount;

        protected AbstractContinuation(AtomicInteger refCount) {
            this.refCount = refCount;
        }

    }

    @Override
    public final Handle adopt(Span span) {
        return makeContinuation(span, new AtomicInteger(1)).activate();
    }

    protected abstract Continuation makeContinuation(Span span, AtomicInteger refCount);
}
