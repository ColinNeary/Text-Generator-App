-- =====================
-- 1. POPULATE DATA
-- =====================

-- Insert new file record
INSERT INTO files (file_name, word_count_total, notes)
VALUES (?, ?, ?);

-- Insert or update word entry
INSERT INTO words (word_text, word_count_total, word_count_starting, word_count_ending)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
    word_count_total = word_count_total + VALUES(word_count_total),
    word_count_starting = word_count_starting + VALUES(word_count_starting),
    word_count_ending = word_count_ending + VALUES(word_count_ending);

-- Insert or update word pair 
INSERT INTO word_pairs (left_word_id, right_word_id, pair_count)
VALUES (?, ?, 1)
ON DUPLICATE KEY UPDATE
    pair_count = pair_count + 1;

-- Update total word count in file
UPDATE files
SET word_count_total = ?
WHERE file_name = ?;

-- =====================
-- 2. READ / RETRIEVE DATA
-- =====================

-- Retrieve all words
SELECT * FROM words ORDER BY word_text;

-- Retrieve single word by text
SELECT word_id, word_text, word_count_total, word_count_starting, word_count_ending
FROM words
WHERE word_text = ?;

-- Retrieve files data
SELECT file_id, file_name, word_count_total, date_imported, notes
FROM files
ORDER BY date_imported DESC;

-- Retrieve top N frequent words
SELECT word_text, word_count_total
FROM words
ORDER BY word_count_total DESC
LIMIT ?;