package dao;

import model.User;
import java.sql.*;
import java.util.*;

/**
 * UserDAO — Database operations for users.
 *
 * Mirrors ItemDAO pattern:
 * - All SQL stays here
 * - Controller calls DAO methods using domain objects
 * - DAO maps ResultSet rows → User objects
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // ── Authentication ─────────────────────────────────────

    /**
     * Finds a user by username. Returns null if not found.
     * Controller calls authenticate() on the returned User object.
     *
     * WHY NOT SELECT WHERE username=? AND password=? ?
     * In real apps, passwords are hashed (BCrypt).
     * We fetch the user first, then compare hash in Java.
     * This keeps the hashing logic OUT of SQL.
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("User lookup failed: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("User lookup failed: " + e.getMessage(), e);
        }
        return null;
    }

    // ── CRUD ───────────────────────────────────────────────

    public void insertUser(User user, String password) {
        String sql = """
            INSERT INTO users (username, password, email, full_name, role)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, password);  // In production: BCrypt.hash(password)
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Username already exists: " + user.getUsername());
            }
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role DESC, username ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users: " + e.getMessage(), e);
        }
        return users;
    }

    public void setUserActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    public int countUsers() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE is_active=1")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    // ── Mapping ────────────────────────────────────────────

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("username"),
            rs.getString("password"),   // Needed for authentication
            rs.getString("email"),
            rs.getString("full_name"),
            User.Role.valueOf(rs.getString("role"))
        );
        user.setId(rs.getInt("id"));
        user.setActive(rs.getInt("is_active") == 1);
        return user;
    }
}