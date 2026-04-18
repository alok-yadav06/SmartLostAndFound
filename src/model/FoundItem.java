// src/model/FoundItem.java
package model;

import java.time.LocalDate;

/**
 * Represents an item that was found.
 *
 * Notice how DIFFERENT the extra fields are from LostItem,
 * yet both share the same base contract (Item).
 * That's the power of inheritance + abstraction working together.
 */
public class FoundItem extends Item {

    private String turnedInLocation; // Where the item was handed in
    private String finderContact;    // Finder's contact (optional)
    private boolean handedToAuthority; // Was it given to security/admin?

    // ── Constructor ────────────────────────────────────────────
    public FoundItem(String name, String description, String category,
                     String location, LocalDate date, int reportedBy,
                     String turnedInLocation, String finderContact,
                     boolean handedToAuthority) {

        super(name, description, category, location, date, reportedBy);

        this.turnedInLocation   = turnedInLocation;
        this.finderContact      = finderContact;
        this.handedToAuthority  = handedToAuthority;
    }

    // ── Implementing Abstract Methods ──────────────────────────
    @Override
    public String getItemType() {
        return "Found";
    }

    @Override
    public String getExtraInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Turned in at: ").append(
            turnedInLocation != null ? turnedInLocation : "Unknown");
        sb.append(" | Authority: ").append(handedToAuthority ? "Yes" : "No");
        if (finderContact != null && !finderContact.isEmpty()) {
            sb.append(" | Finder: ").append(finderContact);
        }
        return sb.toString();
    }

    // ── Getters & Setters ──────────────────────────────────────
    public String  getTurnedInLocation()               { return turnedInLocation; }
    public void    setTurnedInLocation(String loc)     { this.turnedInLocation = loc; }

    public String  getFinderContact()                  { return finderContact; }
    public void    setFinderContact(String contact)    { this.finderContact = contact; }

    public boolean isHandedToAuthority()               { return handedToAuthority; }
    public void    setHandedToAuthority(boolean val)   { this.handedToAuthority = val; }
}