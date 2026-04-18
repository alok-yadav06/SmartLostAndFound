package controller;

import dao.ClaimDAO;
import dao.ItemDAO;
import java.util.List;
import model.ClaimRequest;
import model.Item;

/**
 * ClaimController — manages claim submission and admin review.
 *
 * CONCEPT: Coordinating multiple DAOs
 * When a claim is approved, we need to:
 * 1. Update claim_requests table (ClaimDAO)
 * 2. Update the item's status to "Claimed" (ItemDAO)
 * Both must succeed — if one fails, the other shouldn't commit.
 * In production: wrap in a database TRANSACTION.
 */
public class ClaimController {

    private static ClaimController instance;
    private final ClaimDAO claimDAO;
    private final ItemDAO  itemDAO;

    public static ClaimController getInstance() {
        if (instance == null) instance = new ClaimController();
        return instance;
    }

    private ClaimController() {
        this.claimDAO = new ClaimDAO();
        this.itemDAO  = new ItemDAO();
    }

    /**
     * Submit a new claim for a found item.
     */
    public ClaimRequest submitClaim(int userId, int itemId,
                                    String reason, String proof) {
        ClaimRequest claim = new ClaimRequest(userId, itemId, reason, proof);
        claimDAO.insertClaim(claim);
        System.out.println("📝 Claim submitted: " + claim);
        return claim;
    }

    /**
     * Admin approves a claim → updates claim + item status.
     */
    public void approveClaim(int claimId, int itemId, String adminNote) {
        claimDAO.updateClaimStatus(claimId, "APPROVED", adminNote);
        itemDAO.updateFoundItemStatus(itemId, Item.STATUS_CLAIMED);
        System.out.println("✅ Claim #" + claimId + " approved.");
    }

    /**
     * Admin rejects a claim.
     */
    public void rejectClaim(int claimId, String adminNote) {
        claimDAO.updateClaimStatus(claimId, "REJECTED", adminNote);
        System.out.println("❌ Claim #" + claimId + " rejected.");
    }

    public List<ClaimRequest> getAllClaims()     { return claimDAO.getAllClaims(); }
    public List<ClaimRequest> getPendingClaims() { return claimDAO.getPendingClaims(); }
    public List<ClaimRequest> getClaimsByUser(int userId) { return claimDAO.getClaimsByUser(userId); }
    public int getPendingCount()                 { return claimDAO.countPendingClaims(); }
}