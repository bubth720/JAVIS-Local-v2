-- Frozen schema from LocalDatabase.onCreate in commits
-- 5c7375c0deb4209bff1a4895356967e3927b59ca (v1) and
-- e1541151a8ff7a4f2f499b95a2926286e0d083c7 (v2).
-- Both versions have exactly the same tables and index.
CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL, source TEXT, created_at INTEGER NOT NULL);
CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE documents(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, mime TEXT, created_at INTEGER NOT NULL);
CREATE TABLE chunks(id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER NOT NULL, document_name TEXT NOT NULL, chunk_index INTEGER NOT NULL, text TEXT NOT NULL);
CREATE INDEX idx_chunks_doc ON chunks(document_id);

INSERT INTO messages VALUES (11, 'user', 'Où est mon document ?', NULL, 1700000000000);
INSERT INTO messages VALUES (12, 'assistant', 'Voici le passage demandé.', 'Résumé.pdf', 1700000000001);
INSERT INTO memories VALUES (21, 'Mon projet s''appelle JARVIS.', 1700000000002);
INSERT INTO documents VALUES (31, 'Résumé.pdf', 'application/pdf', 1700000000003);
INSERT INTO documents VALUES (32, 'Notes.pdf', NULL, 1700000000004);
INSERT INTO chunks VALUES (41, 31, 'Résumé.pdf', 0, '[PAGE 1] Premier passage.');
INSERT INTO chunks VALUES (42, 31, 'Résumé.pdf', 1, 'Deuxième passage.');
INSERT INTO chunks VALUES (43, 32, 'Notes.pdf', 0, 'Une note privée.');
