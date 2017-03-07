package io.opentracing;

public interface SpanManager {

    /**
     * A SpanClosure can be used *once* to make a Span active within a SpanManager, then deactivate it once the
     * "closure" (or period of Span activity) has finished.
     *
     * Most users do not directly interact with SpanClosure, activate(), or deactivate(), but rather use
     * SpanManager-aware Runnables/Callables/Executors.
     */
    interface SpanClosure {

        /**
         * Make the Span encapsulated by this SpanClosure active and return it.
         *
         * XXX: handle the case where the Span is null.
         *
         * @see SpanManager#captureActive()
         * @return the newly-activated Span
         */
        Span activate();

        /**
         * End this active period for the Span previously returned by activate(). Finish the span iff finish=true.
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

    // XXX: comment
    SpanClosure capture(Span span);
}
