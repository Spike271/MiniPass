package db;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager
{
    private static final String DB_URL = "jdbc:sqlite:minipass.db";

    /**
     * Establishes a connection to the SQLite database.
     */
    public Connection connect(String masterPassword) throws SQLException
    {
        Properties props = new Properties();
        if (masterPassword != null && !masterPassword.isEmpty())
        {
            props.setProperty("password", masterPassword);
        }

        Connection conn = DriverManager.getConnection(DB_URL, props);
        try (Statement stmt = conn.createStatement())
        {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Creates the necessary tables if they do not already exist.
     */
    public void initializeDatabase(String masterPassword)
    {
        // Table 1: Categories (Folders for organization)
        String createCategoriesTable = """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );
            """;

        // Table 2: Entries (The actual credentials)
        String createEntriesTable = """
            CREATE TABLE IF NOT EXISTS entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER,
                title TEXT NOT NULL,
                username TEXT,
                password TEXT NOT NULL,
                url TEXT,
                notes TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
            );
            """;

        try (Connection conn = connect(masterPassword);
             Statement stmt = conn.createStatement())
        {
            stmt.execute(createCategoriesTable);
            stmt.execute(createEntriesTable);
            insertDefaultCategories(conn);
        }
        catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Helper method to populate the category table so the user has folders to start with.
     */
    private void insertDefaultCategories(Connection conn)
    {
        String insertSQL = """
            INSERT OR IGNORE INTO categories (id, name) VALUES
            (1, 'Personal'),
            (2, 'Work'),
            (3, 'Banking'),
            (4, 'Email');
           """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(insertSQL);
        }
        catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error inserting default categories: " + e.getMessage());
        }
    }
}
