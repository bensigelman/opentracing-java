package io.opentracing;

import java.io.Closeable;

/**
 * In any execution context (or any thread, etc), there is at most one "active" {@link Span} primarily responsible for
 * the work accomplished by the surrounding application code. That active Span may be accessed via the
 * {@link Source#active()} method. If the application needs to defer work that should be part of the same Span, the
 * Source provides a {@link ActiveSpan#defer} method that returns a {@link Continuation}; this continuation may be used
 * to re-activate and continue the {@link Span} in that other asynchronous executor and/or thread.
 *
 * <p>
 * {@link ActiveSpan}s are created via {@link Tracer.SpanBuilder#startAndActivate()} or {@link Source#adopt}. They can
 * be {@link ActiveSpan#defer()}ed as {@link ActiveSpan.Continuation}s, then re-{@link Continuation#activate()}d later.
 */
public interface ActiveSpan extends Closeable, Span {
    /**
     * Mark the end of the active period for the {@link Span} pinned by this {@link ActiveSpan}. When the last
     * {@link ActiveSpan} is deactivated for a given {@link Span}, it is automatically {@link Span#finish()}ed.
     * <p>
     * <p>
     * NOTE: It is an error to call deactivate() more than once on a single {@link ActiveSpan}.
     *
     * @see Closeable#close() {@link ActiveSpan}s are auto-closeable and may be used in try-with-resources blocks
     */
    void deactivate();

    /**
     * "Fork" a new {@link Continuation} associated with this {@link ActiveSpan} and {@link Span}, as well as any
     * 3rd-party execution context of interest.
     * <p>
     * <p>
     * The associated {@link Span} will not {@link Span#finish()} while a {@link Continuation} is outstanding; in
     * this way, it provides a reference/pin just like an active @{Handle} does.
     *
     * @return a new {@link Continuation} to {@link Continuation#activate()} at the appropriate time.
     */
    Continuation defer();

    /**
     * A {@link Continuation} can be used *once* to activate a Span along with any non-OpenTracing execution context
     * (e.g., MDC), then deactivate when processing activity moves on to another Span. (In practice, this active period
     * typically extends for the length of a deferred async closure invocation.)
     *
     * <p>
     * Most users do not directly interact with {@link Continuation}, {@link Continuation#activate()} or
     * {@link ActiveSpan#deactivate()}, but rather use {@link Source}-aware Runnables/Callables/Executors.
     * Those higher-level primitives need not be defined within the OpenTracing core API, and so they are not.
     *
     * <p>
     * NOTE: {@link Continuation} extends {@link Closeable} rather than AutoCloseable in order to keep support
     * for JDK1.6.
     *
     * @see Source#adopt(Span)
     */
    interface Continuation {
        /**
         * Make the Span (and other execution context) encapsulated by this Continuation active and return it.
         *
         * <p>
         * NOTE: It is an error to call activate() more than once on a single Continuation instance.
         *
         * @see Source#adopt(Span)
         * @return a handle to the newly-activated Span
         */
        ActiveSpan activate();
    }

    /**
     * {@link Source} allows an existing (possibly thread-local-aware) execution context provider to act as a
     * source for an actively-scheduled OpenTracing Span.
     */
    interface Source {

        /**
         * @return the active {@link ActiveSpan}, or null if none could be found. This does not affect the reference count for
         * the {@link ActiveSpan}.
         */
        ActiveSpan active();

        /**
         * Wrap and "adopt" a @{link Span} by encapsulating it – and any active state (e.g., MDC state) in the execution
         * context – in a new @{link Handle}.
         *
         * @param span the Span just started
         * @return a @{link Handle} that encapsulates the given Span and any other Source-specific context (e.g.,
         * MDC data)
         */
        ActiveSpan adopt(Span span);
    }
}
