package io.opentracing;

public interface ActiveSpanManager {
    // Basically a marker interface
    public interface Snapshot {
        Span span();
    }

    // Get the currently active Span (perhaps for this thread, etc)
    Span active();

    // Create a Snapshot encapsulating both the given Span and any state needed to activate/deactivate (see below)
    Snapshot snapshot(Span span);

    // Make the Snapshot and the Span it contains "active" per active().
    //
    // *Must* be paired with a subsequent call to deactivate().
    Span activate(Snapshot snapshot);

    // See activate() above.
    void deactivate(Snapshot snapshot);

}
