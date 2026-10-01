-- Completed agents and elapsed simulation time by destination region.
SELECT
    to_node / 8 AS region_id,
    COUNT(*) FILTER (WHERE completed) AS completed_trips,
    MAX(time_seconds) AS latest_event_time
FROM telemetry_event
GROUP BY region_id
ORDER BY completed_trips DESC, region_id;

-- Agent progress deltas identify stalled or unusually slow movement.
WITH progress_window AS (
    SELECT
        agent_id,
        time_seconds,
        progress,
        progress - LAG(progress) OVER (
            PARTITION BY agent_id, from_node, to_node ORDER BY time_seconds
        ) AS progress_delta
    FROM telemetry_event
)
SELECT *
FROM progress_window
WHERE progress_delta = 0
ORDER BY time_seconds;
