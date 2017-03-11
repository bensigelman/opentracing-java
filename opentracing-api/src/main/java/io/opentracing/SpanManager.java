package io.opentracing;

/**
 * SpanManager allows an existing (possibly thread-local-aware) execution context provider to also manage the current
 * active OpenTracing span.
 */
public interface SpanManager {

    /**
     * A SpanClosure can be used *once* to make a Span active within a SpanManager, then deactivate it once the
     * "closure" (or period of Span activity) has finished.
     *
     * Most users do not directly interact with SpanClosure, activate(), or deactivate(), but rather use
     * SpanManager-aware Runnables/Callables/Executors. Those higher-level primitives need not be defined within the
     * OpenTracing core API.
     *
     * @see SpanManager#captureActive()
     */
    interface SpanClosure extends AutoCloseable {

        /**
         * Make the Span encapsulated by this SpanClosure active and return it.
         *
         * NOTE: It is an error to call activate() more than once on a single SpanClosure instance.
         *
         * @see SpanManager#captureActive()
         * @return the newly-activated Span
         */
        Span activate();

        /**
         * @return the encapsulated Span, or null if there isn't one.
         */
        Span span();

        /**
         * End this active period for the Span previously returned by activate(). Finish the span iff finish=true.
         *
         * NOTE: It is an error to call deactivate() more than once on a single SpanClosure instance.
         */
        void deactivate(boolean finishSpan);

    }

    /**
     * @return the currently active Span for this SpanManager, or null if no such Span could be found
     */
    Span active();

     /**
     * Capture any SpanManager-specific context (e.g., MDC context) along with the active Span (even if null) and
     * encapsulate it in a SpanClosure for activation in the future, perhaps in a different thread or on a different
     * executor.
     *
     * If the active Span is null, the implementation must still return a valid SpanClosure; when the closure activates,
     * it will clear any active Span.
     *
     * @see SpanManager.SpanClosure
     *
     * @return a SpanClosure that represents the active Span and any other SpanManager-specific context, even if the
     *     active Span is null.
     */
    SpanClosure captureActive();

    /**
     * Explicitly capture the given Span and any active state (e.g., MDC state) about the current execution context.
     *
     * @param span
     * @return a SpanClosure that represents the active Span and any other SpanManager-specific context, even if the
     *     active Span is null.
     */
    SpanClosure capture(Span span);

    /**
     * Tell the SpanManager that a particular Span has finished (and update any structures accordingly).
     */
    void onFinish(Span span);
}
