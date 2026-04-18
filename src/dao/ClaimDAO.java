package dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import model.ClaimRequest;

public class ClaimDAO {

    private final Connection conn;

    public ClaimDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    public void insertClaim(ClaimRequest claim) {
        String sql = """
            INSERT INTO claim_requests (user_id, item_id, claim_reason, proof_details, status)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, claim.getUserId());
            ps.setInt(2, claim.getItemId());
            ps.setString(3, claim.getClaimReason());
            ps.setString(4, claim.getProofDetails());
            ps.setString(5, claim.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) claim.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to submit claim: " + e.getMessage(), e);
        }
    }

    public List<ClaimRequest> getAllClaims() {
        List<ClaimRequest> claims = new ArrayList<>();
        String sql = "SELECT * FROM claim_requests ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) claims.add(mapClaim(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch claims: " + e.getMessage(), e);
        }
        return claims;
    }

    public List<ClaimRequest> getPendingClaims() {
        List<ClaimRequest> claims = new ArrayList<>();
        String sql = "SELECT * FROM claim_requests WHERE status='PENDING' ORDER BY created_at ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) claims.add(mapClaim(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch pending claims", e);
        }
        return claims;
    }

    public List<ClaimRequest> getClaimsByUser(int userId) {
        List<ClaimRequest> claims = new ArrayList<>();
        String sql = "SELECT * FROM claim_requests WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) claims.add(mapClaim(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user claims", e);
        }
        return claims;
    }

    public void updateClaimStatus(int claimId, String status, String adminNote) {
        String sql = "UPDATE claim_requests SET status=?, admin_note=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminNote);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setInt(4, claimId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update claim: " + e.getMessage(), e);
        }
    }

    public int countPendingClaims() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(
                 "SELECT COUNT(*) FROM claim_requests WHERE status='PENDING'")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    private ClaimRequest mapClaim(ResultSet rs) throws SQLException {
        ClaimRequest claim = new ClaimRequest(
            rs.getInt("user_id"),
            rs.getInt("item_id"),
            rs.getString("claim_reason"),
            rs.getString("proof_details")
        );
        claim.setId(rs.getInt("id"));
        claim.setStatus(rs.getString("status"));
        claim.setAdminNote(rs.getString("admin_note"));
        claim.setCreatedAt(parseDateTimeSafely(rs.getString("created_at")));
        claim.setUpdatedAt(parseDateTimeSafely(rs.getString("updated_at")));
        return claim;
    }

    private LocalDateTime parseDateTimeSafely(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (Exception ignoredAgain) {
                return LocalDateTime.now();
            }
        }
    }
}