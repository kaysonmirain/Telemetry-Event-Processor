package com.kaysonmirain.telemetry;

public record TelemetryEvent(
        double timeSeconds,
        int agentId,
        int fromNode,
        int toNode,
        double progress,
        boolean completed) {

    public String eventKey() {
        return timeSeconds + ":" + agentId + ":" + fromNode + ":" + toNode;
    }

    public int regionId() {
        return Math.max(0, toNode / 8);
    }
}
