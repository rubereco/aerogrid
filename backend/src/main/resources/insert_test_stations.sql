-- Script per inserir les dues estacions de prova de baixa fiabilitat
-- Pots executar això directament al teu pgAdmin o DBeaver per la BD 'aerogrid'

INSERT INTO stations (code, name, municipality, source_type, location, trust_score, is_active)
VALUES
    ('TEST-LOW-TRUST', 'Estació Baixa Fiabilitat', 'Barcelona', 'CITIZEN', ST_SetSRID(ST_MakePoint(2.15, 41.38), 4326), 20, true),
    ('TEST-CRIT-TRUST', 'Estació Crítica Fiabilitat', 'Barcelona', 'CITIZEN', ST_SetSRID(ST_MakePoint(2.18, 41.38), 4326), 10, true)
    ON CONFLICT (code) DO NOTHING;

DELETE FROM stations WHERE code IN ('TEST-LOW-TRUST', 'TEST-CRIT-TRUST');
