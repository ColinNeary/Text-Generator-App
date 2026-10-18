package com.example.backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads MySQL connection settings from db.properties in the project root
 * (copy db.properties.example to get started). Falls back to a local
 * team33db with user root and an empty password if the file is missing.
 */
public final class DbConfig {
    public static final String URL;
    public static final String USER;
    public static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("⚠️ db.properties not found, using default MySQL settings");
        }
        URL = props.getProperty("db.url", "jdbc:mysql://localhost:3306/team33db");
        USER = props.getProperty("db.user", "root");
        PASSWORD = props.getProperty("db.password", "");
    }

    private DbConfig() {}
}
