package io.opentracing;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActiveSpanHolder allows an existing (possibly thread-local-aware) execution context provider to also manage an
 * actively-scheduled OpenTracing Span.
 *
 * <p>
 * In any execution context (or any thread, etc), there is at most one "active" Span primarily responsible for the
 * work accomplished by the surrounding application code. That active Span may be accessed via the
 * {@link ActiveSpanHolder#active()} method. If the application needs to defer work that should be part of the same
 * Span, the ActiveSpanHolder provides a {@link ActiveSpanHolder#capture(Span)} method that returns a
 * {@link Continuation}; this activation context may be used to re-activate and deactivate the
 * captured Span in that other asynchronous executor and/or thread.
 *
 * XXX: need to do something where activate/deactivate is bare, but there's an opt-in refcounting scheme which can wrap
 * a Span, ideally at the OT level and ideally at buildSpan-time. Oh, maybe for the Continuation-building start variant.
 * Actually, this would require changes to capture() (as well as refcounting on Span activation and deactivation).
 */
public abstract class ActiveSpanHolder {
    // XXX START HERE: create an Observer API that gets invoked for capture, activate, and deactivate. Ideally one
    // observer per Span instance, though maybe Span is provided as an arg?

    /**
     * A Continuation can be used *once* to activate a Span and other execution context, then deactivate once the
     * active period has concluded. (In practice, this active period typically extends for the length of a deferred
     * async closure invocation.)
     *
     * <p>
     * Most users do not directly interact with Continuation, activate(), or deactivate(), but rather use
     * ActiveSpanHolder-aware Runnables/Callables/Executors. Those higher-level primitives need not be defined within the
     * OpenTracing core API.
     *
     * <p>
     * NOTE: We extend Closeable rather than AutoCloseable in order to keep support for JDK1.6.
     *
     * @see ActiveSpanHolder#capture(Span)
     */
    public static abstract class Continuation implements Closeable {
        private final AtomicInteger refCount;

        protected Continuation(AtomicInteger refCount) {
            this.refCount = refCount;
        }

        /**
         * Make the Span (and other execution context) encapsulated by this Continuation active and return it.
         *
         * <p>
         * NOTE: It is an error to call activate() more than once on a single Continuation instance.
         *
         * @see ActiveSpanHolder#capture(Span)
         * @return the newly-activated Span
         */
        public abstract void activate();

        public abstract Span span();

        /**
         * Mark the end of this active period for the Span previously returned by activate().
         *
         * <p>
         * NOTE: It is an error to call deactivate() more than once on a single Continuation instance.
         *
         * @see Closeable#close()
         */
        public final void deactivate() {
            doDeactivate();
            decRef();
        }

        @Override
        public final void close() {
            this.deactivate();
        }

        public final Continuation capture() {
            refCount.incrementAndGet();
            return holder().doCapture(span(), refCount);
        }

        protected abstract void doDeactivate();
        protected abstract ActiveSpanHolder holder();

        final void decRef() {
            if (0 == refCount.decrementAndGet()) {
                Span span = this.span();
                if (span != null) {
                    this.span().finish();
                }
            }
        }
    }

    public abstract Continuation active();

    /**
     * Explicitly capture the newly-started Span along with any active state (e.g., MDC state) from the current
     * execution context.
     *
     * @param span the Span just started
     * @return a Continuation that represents the active Span and any other ActiveSpanHolder-specific context, even if the
     *     active Span is null.
     */
    public final Continuation capture(Span span) {
        Continuation rval = doCapture(span, new AtomicInteger(1));
        return rval;
    }

    protected abstract Continuation doCapture(Span span, AtomicInteger refCount);

    // (Convenience methods follow)

    /**
     * @return the currently active Span for this ActiveSpanHolder, or null if there is no such Span
     */
    public final Span activeSpan() {
        Continuation continuation = active();
        if (continuation == null) {
            return null;
        }
        return continuation.span();
    }

    /**
     * @return the currently active SpanContext, or null if there is no active Span
     */
    public final SpanContext activeContext() {
        Span span = activeSpan();
        if (span == null) {
            return null;
        }
        return span.context();
    }
}
