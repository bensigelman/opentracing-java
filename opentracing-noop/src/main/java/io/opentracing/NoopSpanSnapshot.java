package io.opentracing;

public interface NoopSpanSnapshot extends SpanSnapshot {
    static final NoopSpanSnapshotImpl INSTANCE = new NoopSpanSnapshotImpl();
}

final class NoopSpanSnapshotImpl implements NoopSpanSnapshot {

}