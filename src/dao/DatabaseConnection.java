// src/dao/DatabaseConnection.java
package dao;

import java.sql.*;

/**
 * Singleton database connection manager.
 *
 * WHY SINGLETON?
 * Creating a DB connection is expensive (milliseconds).
 * We don't want 50 connections — we want ONE shared connection.
 * Same singleton pattern as ItemController, but for the DB.
 *
 * JDBC Flow:
 * 1. Load driver  (Class.forName — auto in modern JDBC)
 * 2. Get connection (DriverManager.getConnection)
 * 3. Create statement (conn.createStatement / prepareStatement)
 * 4. Execute SQL
 * 5. Process ResultSet
 * 6. Close resources (ALWAYS in finally or try-with-resources)
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:database/lostfound.db";
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        connect();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void connect() {
        try {
            // Ensure the database folder exists
            java.io.File dbDir = new java.io.File("database");
            if (!dbDir.exists()) dbDir.mkdirs();

            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            System.out.println("✅ Database connected: " + DB_URL);
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    public Connection getConnection() {
        try {
            // Auto-reconnect if connection dropped
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    /**
     * Creates all tables if they don't exist.
     * Call this once at app startup.
     *
     * Notice: IF NOT EXISTS — safe to call every startup.
     */
    public void initializeDatabase() {
        try (Statement stmt = getConnection().createStatement()) {

            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    username     TEXT UNIQUE NOT NULL,
                    password     TEXT NOT NULL,
                    email        TEXT,
                    full_name    TEXT,
                    role         TEXT DEFAULT 'USER',
                    is_active    INTEGER DEFAULT 1,
                    created_at   TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Lost items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lost_items (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    name               TEXT NOT NULL,
                    description        TEXT,
                    category           TEXT,
                    location           TEXT,
                    date_reported      TEXT,
                    status             TEXT DEFAULT 'Pending',
                    image_path         TEXT,
                    reported_by        INTEGER,
                    last_seen_location TEXT,
                    reward_offered     REAL DEFAULT 0,
                    contact_info       TEXT,
                    created_at         TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(reported_by) REFERENCES users(id)
                )
            """);
            ensureColumnExists(stmt, "lost_items", "image_path", "TEXT");

            // Found items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS found_items (
                    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name                 TEXT NOT NULL,
                    description          TEXT,
                    category             TEXT,
                    location             TEXT,
                    date_reported        TEXT,
                    status               TEXT DEFAULT 'Pending',
                    image_path           TEXT,
                    reported_by          INTEGER,
                    turned_in_location   TEXT,
                    finder_contact       TEXT,
                    handed_to_authority  INTEGER DEFAULT 0,
                    created_at           TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(reported_by) REFERENCES users(id)
                )
            """);
            ensureColumnExists(stmt, "found_items", "image_path", "TEXT");

            // Claim requests table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claim_requests (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id       INTEGER,
                    item_id       INTEGER,
                    claim_reason  TEXT,
                    proof_details TEXT,
                    status        TEXT DEFAULT 'PENDING',
                    admin_note    TEXT,
                    created_at    TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at    TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
            """);

            // Login audit table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS login_audit (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    username    TEXT,
                    event_type  TEXT NOT NULL,
                    event_time  TEXT DEFAULT CURRENT_TIMESTAMP,
                    notes       TEXT,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
            """);

            // Insert default admin if not exists
            stmt.execute("""
                INSERT OR IGNORE INTO users (username, password, email, full_name, role)
                VALUES ('admin', 'admin123', 'admin@college.edu', 'System Admin', 'ADMIN')
            """);

            System.out.println("✅ Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("❌ DB init failed: " + e.getMessage());
        }
    }

    private void ensureColumnExists(Statement stmt, String tableName, String columnName, String columnDefinition) throws SQLException {
        boolean exists = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }
}