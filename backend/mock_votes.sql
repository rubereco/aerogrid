-- Insert dummy users for testing the trust score thresholds
INSERT INTO users (username, email, password, role, created_at, updated_at)
VALUES
    ('testuser1', 'test1@aerogrid.com', 'dummy_hash', 'USER', NOW(), NOW()),
    ('testuser2', 'test2@aerogrid.com', 'dummy_hash', 'USER', NOW(), NOW()),
    ('testuser3', 'test3@aerogrid.com', 'dummy_hash', 'USER', NOW(), NOW()),
    ('testuser4', 'test4@aerogrid.com', 'dummy_hash', 'USER', NOW(), NOW()),
    ('testuser5', 'test5@aerogrid.com', 'dummy_hash', 'USER', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Assume we want to test station ID 1
-- Insert 5 positive votes to boost the station above the minimum threshold (5)
-- Using dummy votes to ensure ratio makes the station > 80% (Excel·lent)
INSERT INTO votes (user_id, station_id, vote_value, timestamp)
VALUES
    ((SELECT id FROM users WHERE email='test1@aerogrid.com'), 760, 1, NOW()),
    ((SELECT id FROM users WHERE email='test2@aerogrid.com'), 760, 1, NOW()),
    ((SELECT id FROM users WHERE email='test3@aerogrid.com'), 760, 1, NOW()),
    ((SELECT id FROM users WHERE email='test4@aerogrid.com'), 760, 1, NOW()),
    ((SELECT id FROM users WHERE email='test5@aerogrid.com'), 760, 1, NOW())
ON CONFLICT (user_id, station_id) DO UPDATE SET vote_value = EXCLUDED.vote_value;
