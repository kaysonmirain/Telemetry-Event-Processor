package com.kaysonmirain.telemetry;

import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String input = "data/sample_telemetry.csv";
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        int port = 0;
        for (int index = 0; index < args.length; index++) {
            if (args[index].equals("--input") && index + 1 < args.length) input = args[++index];
            else if (args[index].equals("--threads") && index + 1 < args.length) threads = Integer.parseInt(args[++index]);
            else if (args[index].equals("--serve") && index + 1 < args.length) port = Integer.parseInt(args[++index]);
        }

        String jdbcUrl = System.getenv("JDBC_URL");
        EventRepository repository = jdbcUrl == null || jdbcUrl.isBlank()
                ? new InMemoryEventRepository()
                : new JdbcEventRepository(jdbcUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

        try (repository) {
            List<TelemetryEvent> events = CsvEventReader.read(Path.of(input));
            EventProcessor processor = new EventProcessor(repository);
            EventProcessor.ProcessingReport report = processor.processAll(events, threads);
            System.out.println("accepted=" + report.accepted() + " duplicates=" + report.duplicates()
                    + " invalid=" + report.invalid() + " stored=" + report.stored());
            System.out.println(processor.metricsJson());
            if (port > 0) {
                ApiServer server = new ApiServer(port, processor);
                server.start();
                System.out.println("api=http://127.0.0.1:" + port + "/metrics");
                Thread.currentThread().join();
            }
        }
    }
}
