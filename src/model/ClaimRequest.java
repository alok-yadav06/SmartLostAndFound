// src/model/ClaimRequest.java
package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a user's claim on a found item.
 *
 * This is a "join" model — it connects User ↔ FoundItem.
 * Real-world analogy: Think of this like an Amazon order.
 * It links YOU (user) to a PRODUCT (item) with a STATUS.
 *
 * STATUS LIFECYCLE:
 *   PENDING → APPROVED (admin approves)
 *   PENDING → REJECTED (admin rejects)
 *   APPROVED → CLOSED  (item physically handed over)
 */
public class ClaimRequest {

    // ── Status Enum ────────────────────────────────────────────
    public enum ClaimStatus {
        PENDING("Pending", "⏳"),
        APPROVED("Approved", "✅"),
        REJECTED("Rejected", "❌"),
        CLOSED("Closed", "🔒");

        private final String label;
        private final String emoji;

        ClaimStatus(String label, String emoji) {
            this.label = label;
            this.emoji = emoji;
        }

        public String getLabel() { return label; }
        public String getEmoji() { return emoji; }

        @Override
        public String toString() { return emoji + " " + label; }
    }

    // ── Fields ─────────────────────────────────────────────────
    private int           id;
    private int           userId;       // FK → User.id
    private int           itemId;       // FK → FoundItem.id
    private String        claimReason;  // Why do they think it's theirs?
    private String        proofDetails; // Describe proof they can provide
    private ClaimStatus   status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        adminNote;    // Admin's message on approval/rejection

    // ── Constructor ────────────────────────────────────────────
    public ClaimRequest(int userId, int itemId,
                        String claimReason, String proofDetails) {
        this.userId       = userId;
        this.itemId       = itemId;
        this.claimReason  = claimReason;
        this.proofDetails = proofDetails;
        this.status       = ClaimStatus.PENDING;
        this.createdAt    = LocalDateTime.now();
        this.updatedAt    = LocalDateTime.now();
    }

    // ── Business Logic Methods ─────────────────────────────────
    /**
     * Approves this claim.
     * Notice: the method updates both status AND timestamp.
     * Keeping related state changes together = less bugs.
     */
    public void approve(String adminNote) {
        if (this.status != ClaimStatus.PENDING) {
            throw new IllegalStateException(
                "Only PENDING claims can be approved. Current: " + status);
        }
        this.status    = ClaimStatus.APPROVED;
        this.adminNote = adminNote;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String adminNote) {
        if (this.status != ClaimStatus.PENDING) {
            throw new IllegalStateException(
                "Only PENDING claims can be rejected. Current: " + status);
        }
        this.status    = ClaimStatus.REJECTED;
        this.adminNote = adminNote;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if (this.status != ClaimStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED claims can be closed.");
        }
        this.status    = ClaimStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPending()  { return status == ClaimStatus.PENDING; }
    public boolean isApproved() { return status == ClaimStatus.APPROVED; }

    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    // ── Getters & Setters ──────────────────────────────────────
    public int         getId()          { return id; }
    public void        setId(int id)    { this.id = id; }

    public int         getUserId()      { return userId; }
    public int         getItemId()      { return itemId; }

    public String      getClaimReason() { return claimReason; }
    public String      getProofDetails(){ return proofDetails; }

    public ClaimStatus getStatus()      { return status; }
    public void        setStatus(ClaimStatus s) {
        this.status    = s;
        this.updatedAt = LocalDateTime.now();
    }

    public void        setStatus(String s) {
        if (s == null) {
            this.status = ClaimStatus.PENDING;
        } else {
            try {
                this.status = ClaimStatus.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                this.status = ClaimStatus.PENDING;
            }
        }
        this.updatedAt = LocalDateTime.now();
    }

    public String         getAdminNote()  { return adminNote; }
    public void           setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime  getCreatedAt()  { return createdAt; }
    public LocalDateTime  getUpdatedAt()  { return updatedAt; }
    public void           setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public void           setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }

    @Override
    public String toString() {
        return String.format("Claim[%d] User:%d → Item:%d | %s | %s",
            id, userId, itemId, status, getFormattedCreatedAt());
    }
}