-- uzytkownicy-- Tworzenie bazy danych 'tst' (jeśli nie istnieje)
CREATE DATABASE IF NOT EXISTS tst;

-- Używanie bazy danych 'tst'
USE tst;

-- Tworzenie tabeli 'uzytkownicy' do przechowywania informacji o użytkownikach
CREATE TABLE IF NOT EXISTS uzytkownicy (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(255) NOT NULL,
    haslo VARCHAR(255) NOT NULL
);

-- Tworzenie tabeli 'posty' do przechowywania wiadomości czatu
CREATE TABLE IF NOT EXISTS posty (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nick VARCHAR(255) NOT NULL,
    tresc TEXT,
    data TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ustawianie kodowania znaków na UTF-8
ALTER DATABASE tst CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE uzytkownicy CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE posty CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;

select * from uzytkownicy;