package io.opentracing;

import java.io.Closeable;

/**
 * {@link ActiveSpanSource} allows an existing (possibly thread-local-aware) execution context provider to also manage an
 * actively-scheduled OpenTracing Span.
 *
 * <p>
 * In any execution context (or any thread, etc), there is at most one "active" Span primarily responsible for the
 * work accomplished by the surrounding application code. That active Span may be accessed via the
 * {@link ActiveSpanSource#active()} method. If the application needs to defer work that should be part of the same
 * Span, the ActiveSpanSource provides a {@link ActiveSpanSource#capture(Span)} method that returns a
 * {@link Continuation}; this continuation / activation context may be used to re-activate and deactivate the
 * captured Span in that other asynchronous executor and/or thread.
 *
 * <p>
 * There are two important use cases for {@link ActiveSpanSource} and {@link ActiveSpanSource.Continuation}:
 * <ul>
 *
 *     <li>Accessing the active {@link ActiveSpanSource.Continuation}/{@link Span}/{@link SpanContext}: first, call
 *     {@link Tracer#holder()}, then use {@link ActiveSpanSource#active()} / {@link ActiveSpanSource#activeSpan()} /
 *     {@link ActiveSpanSource#activeContext()}
 *
 *     <li>Propagating the active {@link ActiveSpanSource.Continuation}/{@link Span} to another (async) executor. First,
 *     call {@link Continuation#fork()} to defer a reference to the active Span, then pass that to the async
 *     method (even via a final local variable that's used within a closure). Within that closure, code should call
 *     {@link Continuation#activate()} to install the captured {@link ActiveSpanSource.Continuation}/{@link Span} for
 *     subsequent calls to {@linkplain ActiveSpanSource#active active*()}. (Helper libraries can abstract away much
 *     of the above behind {@link java.util.concurrent.ExecutorService} wrappers)
 *
 * </ul>
 */
public interface ActiveSpanSource {

    interface Handle extends Closeable {
        /**
         * @return the {@link Span} associated with this {@link Continuation}, or null if there is no such {@link Span}.
         */
        Span span();

        /**
         * Mark the end of this active period for the Span previously returned by activate().
         *
         * <p>
         * NOTE: It is an error to call deactivate() more than once on a single Continuation instance.
         *
         * @see Closeable#close()
         */
        void deactivate();

        /**
         * Fork and take a reference to the {@link Span} associated with this {@link Continuation} and any 3rd-party
         * execution context of interest.
         *
         * @return a new {@link Continuation} to {@link Continuation#activate()} at the appropriate time.
         */
        Continuation defer();
    }

    /**
     * A {@link Continuation} can be used *once* to activate a Span and some non-OpenTracing execution context (e.g.,
     * MDC), then deactivate when processing activity moves on to another Span. (In practice, this active period
     * typically extends for the length of a deferred async closure invocation.)
     *
     * <p>
     * Most users do not directly interact with {@link Continuation}, {@link Continuation#activate()} or
     * {@link Handle#deactivate()}, but rather use {@link ActiveSpanSource}-aware Runnables/Callables/Executors.
     * Those higher-level primitives need not be defined within the OpenTracing core API, and so they are not.
     *
     * <p>
     * NOTE: {@link Continuation} extends {@link java.io.Closeable} rather than AutoCloseable in order to keep support
     * for JDK1.6.
     *
     * @see ActiveSpanSource#adopt(Span)
     */
    interface Continuation {
        /**
         * Make the Span (and other execution context) encapsulated by this Continuation active and return it.
         *
         * <p>
         * NOTE: It is an error to call activate() more than once on a single Continuation instance.
         *
         * @see ActiveSpanSource#adopt(Span)
         * @return a handle to the newly-activated Span
         */
        Handle activate();
    }

    /**
     * @return the active {@link Continuation}, or null if none could be found.
     */
    Handle active();

    /**
     * Explicitly defer the newly-started Span along with any active state (e.g., MDC state) from the current
     * execution context.
     *
     * @param span the Span just started
     * @return a Continuation that represents the active Span and any other ActiveSpanSource-specific context, even if the
     *     active Span is null.
     */
    Handle adopt(Span span);
}
