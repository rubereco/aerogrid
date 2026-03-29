-- Ensure idempotency
DELETE FROM measurements;
DELETE FROM stations;

-- Insert Stations (Using ST_GeomFromText for PostGIS Geometry)
INSERT INTO stations (id, code, name, municipality, location, source_type, trust_score, is_active, created_at, updated_at)
VALUES
(1, 'OFF-001', 'Central Park Station', 'New York', ST_GeomFromText('POINT(-73.9654 40.7829)', 4326), 'OFFICIAL', 100, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'AG-A1B2C3D4', 'Citizen Station 1', 'Brooklyn', ST_GeomFromText('POINT(-73.9442 40.6782)', 4326), 'CITIZEN', 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Measurements
INSERT INTO measurements (id, station_id, timestamp, pollutant, value, aqi)
VALUES
(1, 1, '2026-03-29 10:00:00', 'PM2_5', 12.5, 50),
(2, 1, '2026-03-29 10:00:00', 'PM10', 25.0, 45),
(3, 2, '2026-03-29 10:05:00', 'NO2', 15.3, 30);

