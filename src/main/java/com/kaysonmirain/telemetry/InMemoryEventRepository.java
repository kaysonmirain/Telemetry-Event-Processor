package com.kaysonmirain.telemetry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEventRepository implements EventRepository {
    private final Set<String> eventKeys = ConcurrentHashMap.newKeySet();

    @Override
    public void save(TelemetryEvent event) {
        eventKeys.add(event.eventKey());
    }

    @Override
    public long size() {
        return eventKeys.size();
    }
}
