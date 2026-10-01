package com.kaysonmirain.telemetry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvEventReader {
    private CsvEventReader() {}

    public static List<TelemetryEvent> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<TelemetryEvent> events = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) continue;
            String[] values = line.split(",");
            if (values.length != 6) throw new IOException("invalid telemetry row " + (index + 1));
            events.add(new TelemetryEvent(
                    Double.parseDouble(values[0]),
                    Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]),
                    Integer.parseInt(values[3]),
                    Double.parseDouble(values[4]),
                    values[5].equals("1") || Boolean.parseBoolean(values[5])));
        }
        return events;
    }
}
