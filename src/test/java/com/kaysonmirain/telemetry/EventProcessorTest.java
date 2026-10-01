package com.kaysonmirain.telemetry;

import java.util.List;

public final class EventProcessorTest {
    public static void main(String[] args) throws Exception {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        EventProcessor processor = new EventProcessor(repository);
        TelemetryEvent event = new TelemetryEvent(1.0, 1, 0, 8, 0.5, false);
        TelemetryEvent completed = new TelemetryEvent(2.0, 1, 8, 8, 1.0, true);
        TelemetryEvent invalid = new TelemetryEvent(3.0, 2, 0, 8, 1.4, false);
        EventProcessor.ProcessingReport report = processor.processAll(List.of(event, event, completed, invalid), 4);
        assert report.accepted() == 2 : report;
        assert report.duplicates() == 1 : report;
        assert report.invalid() == 1 : report;
        assert report.stored() == 2 : report;
        assert processor.regionMetrics().size() == 1;
        assert processor.regionMetrics().getFirst().completions() == 1;
        assert processor.metricsJson().contains("\"accepted\":2");

        EventRepository failingRepository = new EventRepository() {
            @Override public void save(TelemetryEvent ignored) throws Exception {
                throw new Exception("simulated persistence failure");
            }
            @Override public long size() { return 0; }
        };
        boolean failurePropagated = false;
        try {
            new EventProcessor(failingRepository).processAll(List.of(event), 1);
        } catch (IllegalStateException expected) {
            failurePropagated = expected.getMessage().contains("worker failed");
        }
        assert failurePropagated : "worker failures must reach the caller";
        System.out.println("telemetry event processor tests passed");
    }
}
