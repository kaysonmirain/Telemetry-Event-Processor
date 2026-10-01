package com.kaysonmirain.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

public final class EventProcessor {
    public record RegionMetrics(int regionId, long events, long completions, double averageProgress) {}
    public record ProcessingReport(long accepted, long duplicates, long invalid, long stored) {}

    private static final class MutableRegionMetrics {
        final LongAdder events = new LongAdder();
        final LongAdder completions = new LongAdder();
        final DoubleAdder progress = new DoubleAdder();
    }

    private final EventRepository repository;
    private final Map<Integer, MutableRegionMetrics> regions = new ConcurrentHashMap<>();
    private final java.util.Set<String> seen = ConcurrentHashMap.newKeySet();
    private final LongAdder accepted = new LongAdder();
    private final LongAdder duplicates = new LongAdder();
    private final LongAdder invalid = new LongAdder();

    public EventProcessor(EventRepository repository) {
        this.repository = repository;
    }

    public void process(TelemetryEvent event) {
        if (event.progress() < 0.0 || event.progress() > 1.0 || event.fromNode() < 0 || event.toNode() < 0) {
            invalid.increment();
            return;
        }
        if (!seen.add(event.eventKey())) {
            duplicates.increment();
            return;
        }
        try {
            repository.save(event);
        } catch (Exception error) {
            throw new IllegalStateException("event persistence failed", error);
        }
        MutableRegionMetrics region = regions.computeIfAbsent(event.regionId(), ignored -> new MutableRegionMetrics());
        region.events.increment();
        region.progress.add(event.progress());
        if (event.completed()) region.completions.increment();
        accepted.increment();
    }

    public ProcessingReport processAll(List<TelemetryEvent> events, int threadCount) throws InterruptedException {
        try (ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threadCount))) {
            List<Future<?>> tasks = new ArrayList<>();
            for (TelemetryEvent event : events) tasks.add(pool.submit(() -> process(event)));
            pool.shutdown();
            for (Future<?> task : tasks) {
                try {
                    task.get();
                } catch (ExecutionException error) {
                    throw new IllegalStateException("telemetry worker failed", error.getCause());
                }
            }
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) throw new IllegalStateException("processing timed out");
        }
        return report();
    }

    public ProcessingReport report() {
        return new ProcessingReport(accepted.sum(), duplicates.sum(), invalid.sum(), repository.size());
    }

    public List<RegionMetrics> regionMetrics() {
        List<RegionMetrics> output = new ArrayList<>();
        regions.forEach((regionId, value) -> {
            long count = value.events.sum();
            output.add(new RegionMetrics(regionId, count, value.completions.sum(),
                    count == 0 ? 0.0 : value.progress.sum() / count));
        });
        output.sort(Comparator.comparingInt(RegionMetrics::regionId));
        return output;
    }

    public String metricsJson() {
        ProcessingReport report = report();
        StringBuilder json = new StringBuilder("{\"accepted\":").append(report.accepted())
                .append(",\"duplicates\":").append(report.duplicates())
                .append(",\"invalid\":").append(report.invalid())
                .append(",\"stored\":").append(report.stored()).append(",\"regions\":[");
        List<RegionMetrics> metrics = regionMetrics();
        for (int index = 0; index < metrics.size(); index++) {
            RegionMetrics region = metrics.get(index);
            if (index > 0) json.append(',');
            json.append("{\"regionId\":").append(region.regionId())
                    .append(",\"events\":").append(region.events())
                    .append(",\"completions\":").append(region.completions())
                    .append(",\"averageProgress\":").append(String.format(java.util.Locale.ROOT, "%.4f", region.averageProgress()))
                    .append('}');
        }
        return json.append("]}").toString();
    }
}
