package io.opentracing;

public interface SpanManager {

    /**
     * A SpanClosure can be used *once* to make a Span active within a SpanManager, then deactivate it once the
     * "closure" (or period of Span activity) has finished.
     *
     * Most users do not directly interact with SpanClosure, activate(), or deactivate(), but rather use
     * SpanManager-aware Runnables/Callables/Executors.
     *
     * @see Span#incRef()
     * @see Span#decRef()
     */
    interface SpanClosure {

        /**
         * Make the Span encapsulated by this SpanClosure active and return it.
         *
         * NOTE: activate() should not affect a Span's reference count (though deactivate() and
         * SpanManager.captureActive() do).
         *
         * @see SpanManager#captureActive()
         * @return the newly-activated Span
         */
        Span activate();

        /**
         * End this active period for the Span previously returned by activate() and decrement its reference count.
         *
         * @see Span#decRef()
         */
        void deactivate();
    }

    /**
     * @return the currently active Span for this SpanManager, or null if no such Span could be found
     */
    Span active();

    /**
     * Capture the active() Span (even if null) via captureWithSpan().
     *
     * @see SpanManager#active()
     * @see SpanManager#captureWithSpan(Span)
     */
    SpanClosure captureActive();
    /**
     * Capture any SpanManager-specific context (e.g., MDC context) along with the given Span (even if null) and
     * encapsulate it in a SpanClosure for activation in the future, perhaps in a different thread or on a different
     * executor.
     *
     * If the provided Span is not null, the implementation must increment the reference count on the encapsulated
     * (active) Span.
     *
     * If the active Span is null, the implementation must still return a valid SpanClosure; when the closure activates,
     * it will clear any active Span.
     *
     * @see SpanManager.SpanClosure
     * @see Span#incRef()
     *
     * @param span the Span to associate with any SpanManager-specific active context
     * @return a SpanClosure that represents the active Span and any other SpanManager-specific context, even if the
     *     active Span is null.
     */
    SpanClosure captureWithSpan(Span span);
}
