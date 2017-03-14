package io.opentracing;

/**
 * SpanScheduler allows an existing (possibly thread-local-aware) execution context provider to also manage the current
 * active OpenTracing span.
 */
public interface SpanScheduler {

    /**
     * A SpanClosure can be used *once* to make a Span active within a SpanScheduler, then deactivate it once the
     * "closure" (or period of Span activity) has finished.
     *
     * Most users do not directly interact with SpanClosure, activate(), or deactivate(), but rather use
     * SpanScheduler-aware Runnables/Callables/Executors. Those higher-level primitives need not be defined within the
     * OpenTracing core API.
     *
     * @see SpanScheduler#captureActive()
     */
    interface SpanClosure extends AutoCloseable {

        /**
         * Make the Span encapsulated by this SpanClosure active and return it.
         *
         * NOTE: It is an error to call activate() more than once on a single SpanClosure instance.
         *
         * @see SpanScheduler#captureActive(boolean)
         * @return the newly-activated Span
         */
        Span activate();

        /**
         * @return the encapsulated Span, or null if there isn't one.
         */
        Span span();

        /**
         * End this active period for the Span previously returned by activate().
         *
         * NOTE: It is an error to call deactivate() more than once on a single SpanClosure instance.
         */
        void deactivate();

    }

    /**
     * @return the currently active Span for this SpanScheduler, or null if no such Span could be found
     */
    Span active();

    /**
     * Capture any SpanScheduler-specific context (e.g., MDC context) along with the active Span (even if null) and
     * encapsulate it in a SpanClosure for activation in the future, perhaps in a different thread or on a different
     * executor.
     *
     * If the active Span is null, the implementation must still return a valid SpanClosure; when the closure activates,
     * it will clear any active Span.
     *
     * @param autoFinish true if.f. the span should be finish()ed when the SpanClosure is deactivated
     *
     * @return a SpanClosure that represents the active Span and any other SpanScheduler-specific context, even if the
     *     active Span is null.
     *
     * @see SpanScheduler.SpanClosure
     */
    SpanClosure captureActive(boolean autoFinish);

    /**
     * Explicitly capture the newly-started Span along with any active state (e.g., MDC state) from the current
     * execution context.
     *
     * @param span the Span just started
     * @param autoFinish true if.f. the span should be finish()ed when the SpanClosure is deactivated
     * @return a SpanClosure that represents the active Span and any other SpanScheduler-specific context, even if the
     *     active Span is null.
     *
     * @see SpanClosure#onFinish(Span)
     */
    SpanClosure onStart(Span span, boolean autoFinish);

    /**
     * Tell the SpanScheduler that a particular Span has finished (and update any structures accordingly).
     */
    void onFinish(Span span);
}
