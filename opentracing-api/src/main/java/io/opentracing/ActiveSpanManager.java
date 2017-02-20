package io.opentracing;

/**
 * XXX(bhs): comment
 */
public interface ActiveSpanManager {
    // XXX(bhs): good comment. just a marker, obviously.
    public interface Snapshot {
        Span span();
    }

    /**
     * Returns the span associated with the current execution context.
     *
     */
    Span active();

    /**
     * Retrieve the associated SpanSnapshot.
     * @return the SpanSnapshot that encapsulates Span state that should propagate across in-process concurrency boundaries.
     */
    Snapshot snapshot(Span span);

    /**
     * Activates a given Snapshot (per snapshot()).
     */
    Span activate(Snapshot snapshot);

    /**
     * XXX: comment
     */
    void deactivate(Snapshot snapshot);

}
