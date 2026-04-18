// src/dao/ItemDAO.java
package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import model.*;

/**
 * Data Access Object for Items.
 *
 * ALL SQL lives here. No SQL in controllers, no SQL in views.
 * This is the ONLY class that touches the database for items.
 *
 * PreparedStatement vs Statement:
 * NEVER use Statement with user input — SQL Injection risk!
 *   Statement:  "SELECT * WHERE name = '" + userInput + "'"
 *               If userInput = "'; DROP TABLE users; --" → DISASTER
 *
 * PreparedStatement: "SELECT * WHERE name = ?"
 *               The ? is parameterized — user input is ESCAPED.
 *               Injection impossible. Always use this.
 *
 * try-with-resources:
 * Automatically closes PreparedStatement and ResultSet.
 * No need for finally blocks. Modern Java pattern.
 */
public class ItemDAO {

    private final Connection conn;

    public ItemDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // ── Lost Items ─────────────────────────────────────────────

    public void insertLostItem(LostItem item) {
        String sql = """
            INSERT INTO lost_items
              (name, description, category, location, date_reported,
               status, image_path, reported_by, last_seen_location,
               reward_offered, contact_info)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getLocation());
            ps.setString(5, item.getDate().toString());
            ps.setString(6, item.getStatus());
            ps.setString(7, item.getImagePath());
            ps.setInt   (8, item.getReportedBy());
            ps.setString(9, item.getLastSeenLocation());
            ps.setDouble(10, item.getRewardOffered());
            ps.setString(11, item.getContactInfo());

            ps.executeUpdate();

            // Get the auto-generated ID back
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert lost item: " + e.getMessage(), e);
        }
    }

    public List<LostItem> getAllLostItems() {
        List<LostItem> items = new ArrayList<>();
        String sql = "SELECT * FROM lost_items ORDER BY created_at DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapLostItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch lost items: " + e.getMessage(), e);
        }
        return items;
    }

    public List<LostItem> getLostItemsByReporter(int reporterId) {
        List<LostItem> items = new ArrayList<>();
        String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapLostItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user lost items: " + e.getMessage(), e);
        }
        return items;
    }

    public List<LostItem> searchLostItems(String query, String category) {
        List<LostItem> items = new ArrayList<>();
        boolean hasQuery    = query != null && !query.trim().isEmpty();
        boolean hasCat      = category != null && !category.equals("All");

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM lost_items WHERE 1=1");
        if (hasQuery)  sql.append(" AND (name LIKE ? OR description LIKE ? OR location LIKE ?)");
        if (hasCat)    sql.append(" AND category = ?");
        sql.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasQuery) {
                String like = "%" + query.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (hasCat) ps.setString(idx, category);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapLostItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
        return items;
    }

    public void updateLostItemStatus(int id, String status) {
        String sql = "UPDATE lost_items SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    public void updateLostItem(LostItem item) {
        String sql = """
            UPDATE lost_items
            SET name = ?, description = ?, category = ?, location = ?,
                last_seen_location = ?, reward_offered = ?, contact_info = ?, image_path = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getLocation());
            ps.setString(5, item.getLastSeenLocation());
            ps.setDouble(6, item.getRewardOffered());
            ps.setString(7, item.getContactInfo());
            ps.setString(8, item.getImagePath());
            ps.setInt(9, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    public boolean isLostItemOwnedBy(int itemId, int userId) {
        String sql = "SELECT 1 FROM lost_items WHERE id = ? AND reported_by = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public void deleteLostItem(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM lost_items WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a ResultSet row → LostItem object.
     * This is called "Object-Relational Mapping" (ORM) — manually.
     * Frameworks like Hibernate do this automatically,
     * but we do it by hand to understand what's happening.
     */
    private LostItem mapLostItem(ResultSet rs) throws SQLException {
        LostItem item = new LostItem(
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("category"),
            rs.getString("location"),
            LocalDate.parse(rs.getString("date_reported")),
            rs.getInt("reported_by"),
            rs.getString("last_seen_location"),
            rs.getDouble("reward_offered"),
            rs.getString("contact_info")
        );
        item.setId(rs.getInt("id"));
        item.setStatus(rs.getString("status"));
        item.setImagePath(rs.getString("image_path"));
        return item;
    }

    // ── Found Items ────────────────────────────────────────────

    public void insertFoundItem(FoundItem item) {
        String sql = """
            INSERT INTO found_items
              (name, description, category, location, date_reported,
               status, image_path, reported_by, turned_in_location,
               finder_contact, handed_to_authority)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  item.getName());
            ps.setString(2,  item.getDescription());
            ps.setString(3,  item.getCategory());
            ps.setString(4,  item.getLocation());
            ps.setString(5,  item.getDate().toString());
            ps.setString(6,  item.getStatus());
            ps.setString(7,  item.getImagePath());
            ps.setInt(8,     item.getReportedBy());
            ps.setString(9,  item.getTurnedInLocation());
            ps.setString(10, item.getFinderContact());
            ps.setInt(11,    item.isHandedToAuthority() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert found item: " + e.getMessage(), e);
        }
    }

    public List<FoundItem> getAllFoundItems() {
        List<FoundItem> items = new ArrayList<>();
        String sql = "SELECT * FROM found_items WHERE status <> ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Item.STATUS_CLAIMED);
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapFoundItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch found items: " + e.getMessage(), e);
        }
        return items;
    }

    public List<FoundItem> getFoundItemsByReporter(int reporterId) {
        List<FoundItem> items = new ArrayList<>();
        String sql = "SELECT * FROM found_items WHERE reported_by = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapFoundItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user found items: " + e.getMessage(), e);
        }
        return items;
    }

    public List<FoundItem> searchFoundItems(String query, String category) {
        List<FoundItem> items = new ArrayList<>();
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCat   = category != null && !category.equals("All");

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM found_items WHERE status <> ?");
        if (hasQuery) sql.append(" AND (name LIKE ? OR description LIKE ? OR location LIKE ?)");
        if (hasCat)   sql.append(" AND category = ?");
        sql.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, Item.STATUS_CLAIMED);
            if (hasQuery) {
                String like = "%" + query.trim() + "%";
                ps.setString(idx++, like); ps.setString(idx++, like); ps.setString(idx++, like);
            }
            if (hasCat) ps.setString(idx, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapFoundItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
        return items;
    }

    public void deleteFoundItem(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM found_items WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }

    public void updateFoundItemStatus(int id, String status) {
        String sql = "UPDATE found_items SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    public void updateFoundItem(FoundItem item) {
        String sql = """
            UPDATE found_items
            SET name = ?, description = ?, category = ?, location = ?,
                turned_in_location = ?, finder_contact = ?, handed_to_authority = ?, image_path = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getLocation());
            ps.setString(5, item.getTurnedInLocation());
            ps.setString(6, item.getFinderContact());
            ps.setInt(7, item.isHandedToAuthority() ? 1 : 0);
            ps.setString(8, item.getImagePath());
            ps.setInt(9, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    public boolean isFoundItemOwnedBy(int itemId, int userId) {
        String sql = "SELECT 1 FROM found_items WHERE id = ? AND reported_by = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private FoundItem mapFoundItem(ResultSet rs) throws SQLException {
        FoundItem item = new FoundItem(
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("category"),
            rs.getString("location"),
            LocalDate.parse(rs.getString("date_reported")),
            rs.getInt("reported_by"),
            rs.getString("turned_in_location"),
            rs.getString("finder_contact"),
            rs.getInt("handed_to_authority") == 1
        );
        item.setId(rs.getInt("id"));
        item.setStatus(rs.getString("status"));
        item.setImagePath(rs.getString("image_path"));
        return item;
    }

    // ── Analytics ──────────────────────────────────────────────

    public int countByTable(String table) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public int countByStatus(String table, String status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE status = ?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { return 0; }
    }
}