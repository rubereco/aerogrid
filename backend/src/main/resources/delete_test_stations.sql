-- Script per esborrar les dues estacions de prova de baixa fiabilitat
-- Així neteges la BD quan ja no necessitis veure la prova

DELETE FROM stations WHERE code IN ('TEST-LOW-TRUST', 'TEST-CRIT-TRUST');

