package com.kaysonmirain.telemetry;

public interface EventRepository extends AutoCloseable {
    void save(TelemetryEvent event) throws Exception;
    long size();
    @Override default void close() throws Exception {}
}
