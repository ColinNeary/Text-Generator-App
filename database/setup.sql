-- Creates the team33db schema the app expects.
-- Run once in MySQL Workbench (File > Open SQL Script, then Execute).
-- The app adds the rest on its own: grammar_rules, words.pos_tag and autocomplete_words.

CREATE DATABASE IF NOT EXISTS team33db
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE team33db;

CREATE TABLE IF NOT EXISTS files (
  file_id          INT NOT NULL AUTO_INCREMENT,
  file_name        VARCHAR(255) NOT NULL,
  word_count_total INT NOT NULL DEFAULT 0,
  date_imported    TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  notes            TEXT,
  PRIMARY KEY (file_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS words (
  word_id             INT NOT NULL AUTO_INCREMENT,
  word_text           VARCHAR(100) NOT NULL,
  word_count_total    INT NOT NULL DEFAULT 0,
  word_count_starting INT NOT NULL DEFAULT 0,
  word_count_ending   INT NOT NULL DEFAULT 0,
  PRIMARY KEY (word_id),
  UNIQUE KEY word_text (word_text)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS word_pairs (
  pair_id    INT NOT NULL AUTO_INCREMENT,
  left_word  VARCHAR(45) NOT NULL,
  right_word VARCHAR(45) NOT NULL,
  pair_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (pair_id),
  UNIQUE KEY unique_pair (left_word, right_word),
  KEY right_word_id (right_word)
) ENGINE=InnoDB;
