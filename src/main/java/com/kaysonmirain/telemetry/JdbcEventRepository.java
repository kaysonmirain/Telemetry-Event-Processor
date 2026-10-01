package com.kaysonmirain.telemetry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcEventRepository implements EventRepository {
    private final Connection connection;
    private final PreparedStatement insert;

    public JdbcEventRepository(String jdbcUrl, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl, user, password);
        insert = connection.prepareStatement("""
                INSERT INTO telemetry_event
                    (event_key, time_seconds, agent_id, from_node, to_node, progress, completed)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_key) DO NOTHING
                """);
    }

    @Override
    public synchronized void save(TelemetryEvent event) throws SQLException {
        insert.setString(1, event.eventKey());
        insert.setDouble(2, event.timeSeconds());
        insert.setInt(3, event.agentId());
        insert.setInt(4, event.fromNode());
        insert.setInt(5, event.toNode());
        insert.setDouble(6, event.progress());
        insert.setBoolean(7, event.completed());
        insert.executeUpdate();
    }

    @Override
    public long size() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM telemetry_event");
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        } catch (SQLException error) {
            throw new IllegalStateException("could not count events", error);
        }
    }

    @Override
    public void close() throws SQLException {
        insert.close();
        connection.close();
    }
}
