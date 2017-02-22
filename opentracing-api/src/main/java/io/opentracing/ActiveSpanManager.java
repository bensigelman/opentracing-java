package io.opentracing;

public interface ActiveSpanManager {
    // Basically a marker interface
    public interface Snapshot {
        Span activate();
        void deactivate();
    }

    // Get the currently active Span (perhaps for this thread, etc)
    Span active();

    // Create a Snapshot encapsulating both the given Span and any state needed to activate/deactivate (see below)
    Snapshot snapshot(Span span);
}
