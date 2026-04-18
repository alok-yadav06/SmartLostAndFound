// src/model/LostItem.java
package model;

import java.time.LocalDate;

/**
 * Represents an item that was lost.
 *
 * INHERITANCE: Extends Item → gets all fields/methods for free.
 * Only adds what's UNIQUE to lost items:
 *   - lastSeenLocation (might differ from reported location)
 *   - rewardOffered    (money offered for return)
 *   - contactInfo      (how to reach the owner)
 *
 * POLYMORPHISM: Provides its own implementation of abstract methods.
 */
public class LostItem extends Item {

    private String  lastSeenLocation;
    private double  rewardOffered;    // 0.0 means no reward
    private String  contactInfo;      // Phone or email of owner

    // ── Constructor ────────────────────────────────────────────
    public LostItem(String name, String description, String category,
                    String location, LocalDate date, int reportedBy,
                    String lastSeenLocation, double rewardOffered,
                    String contactInfo) {

        // super() calls the parent constructor — MUST be first line
        super(name, description, category, location, date, reportedBy);

        this.lastSeenLocation = lastSeenLocation;
        this.rewardOffered    = rewardOffered;
        this.contactInfo      = contactInfo;
    }

    // ── Implementing Abstract Methods ──────────────────────────
    @Override
    public String getItemType() {
        return "Lost"; // This is our identity
    }

    @Override
    public String getExtraInfo() {
        // Build a human-readable summary of the extra fields
        StringBuilder sb = new StringBuilder();
        sb.append("Last seen: ").append(
            lastSeenLocation != null ? lastSeenLocation : "Unknown");
        if (rewardOffered > 0) {
            sb.append(" | Reward: ₹").append(String.format("%.0f", rewardOffered));
        }
        if (contactInfo != null && !contactInfo.isEmpty()) {
            sb.append(" | Contact: ").append(contactInfo);
        }
        return sb.toString();
    }

    // ── LostItem-specific behavior ─────────────────────────────
    /**
     * Returns true if a reward is being offered.
     * This will be used by the UI to show a "🏆 Reward" badge.
     */
    public boolean hasReward() {
        return rewardOffered > 0;
    }

    // ── Getters & Setters ──────────────────────────────────────
    public String getLastSeenLocation()               { return lastSeenLocation; }
    public void   setLastSeenLocation(String loc)     { this.lastSeenLocation = loc; }

    public double getRewardOffered()                  { return rewardOffered; }
    public void   setRewardOffered(double reward)     {
        if (reward < 0) throw new IllegalArgumentException("Reward cannot be negative.");
        this.rewardOffered = reward;
    }

    public String getContactInfo()                    { return contactInfo; }
    public void   setContactInfo(String contactInfo)  { this.contactInfo = contactInfo; }
}