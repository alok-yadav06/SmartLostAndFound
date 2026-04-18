// src/model/Item.java
package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base class for all items in the system.
 *
 * WHY ABSTRACT?
 * You will never create a plain "Item" object in real life.
 * Every item is either Lost or Found — never just floating.
 * Making it abstract enforces this rule at compile time.
 * The compiler won't let you write: new Item(...) — and that's good.
 *
 * ENCAPSULATION:
 * All fields are private. Access is only through getters/setters.
 * This lets us add validation logic later (e.g., name can't be empty)
 * without changing any code that uses this class.
 */
public abstract class Item {

    // ── Fields (all private = Encapsulation) ───────────────────
    private int    id;           // Database primary key
    private String name;         // Item name e.g. "Black wallet"
    private String description;  // Detailed description
    private String category;     // e.g. ELECTRONICS, CLOTHING, etc.
    private String location;     // Where lost/found
    private LocalDate date;      // Date lost/found
    private String status;       // PENDING, CLAIMED, CLOSED
    private String imagePath;    // Path to uploaded image (nullable)
    private int    reportedBy;   // FK → User.id who reported this

    // ── Category Constants ─────────────────────────────────────
    // Using constants prevents typos like "Elecronics" vs "Electronics"
    public static final String CAT_ELECTRONICS = "Electronics";
    public static final String CAT_CLOTHING    = "Clothing";
    public static final String CAT_DOCUMENTS   = "Documents";
    public static final String CAT_ACCESSORIES = "Accessories";
    public static final String CAT_KEYS        = "Keys";
    public static final String CAT_BAGS        = "Bags";
    public static final String CAT_OTHER       = "Other";

    public static final String[] ALL_CATEGORIES = {
        CAT_ELECTRONICS, CAT_CLOTHING, CAT_DOCUMENTS,
        CAT_ACCESSORIES, CAT_KEYS, CAT_BAGS, CAT_OTHER
    };

    // Backward-compatible alias used by existing UI code.
    public static final String[] CATEGORIES = ALL_CATEGORIES;

    // ── Status Constants ───────────────────────────────────────
    public static final String STATUS_PENDING  = "Pending";
    public static final String STATUS_CLAIMED  = "Claimed";
    public static final String STATUS_CLOSED   = "Closed";

    // ── Constructor ────────────────────────────────────────────
    /**
     * WHY no no-arg constructor?
     * An item without a name/category/date makes no sense.
     * We force callers to provide the minimum required data.
     * This is called "making invalid state unrepresentable."
     */
    public Item(String name, String description, String category,
                String location, LocalDate date, int reportedBy) {
        // Validation — real projects always validate in constructors
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }
        this.name        = name.trim();
        this.description = description;
        this.category    = category;
        this.location    = location;
        this.date        = (date != null) ? date : LocalDate.now();
        this.reportedBy  = reportedBy;
        this.status      = STATUS_PENDING; // Default status
    }

    // ── Abstract Method ────────────────────────────────────────
    /**
     * POLYMORPHISM in action.
     * Every subclass MUST implement this — but each does it differently.
     * LostItem returns "Lost", FoundItem returns "Found".
     *
     * Why abstract? Because Item itself doesn't know what type it is.
     * It forces each subclass to define its own identity.
     */
    public abstract String getItemType();

    /**
     * Another abstract method — each subclass shows different
     * "extra" info. LostItem shows reward info, FoundItem shows
     * where it was handed in.
     */
    public abstract String getExtraInfo();

    // ── Concrete Method (shared behavior) ─────────────────────
    /**
     * This is INHERITANCE benefit:
     * Both LostItem and FoundItem get this for free.
     * We write it once here, they both inherit it.
     */
    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    /**
     * toString() for debugging — always override this.
     * Without it, printing an object shows "model.Item@3f99bd52" — useless.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s | Category: %s | Status: %s | Date: %s",
            getItemType(), name, category, status, getFormattedDate());
    }

    // ── Getters & Setters ──────────────────────────────────────
    public int       getId()          { return id; }
    public void      setId(int id)    { this.id = id; }

    public String    getName()        { return name; }
    public void      setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Name cannot be empty.");
        this.name = name.trim();
    }

    public String    getDescription() { return description; }
    public void      setDescription(String description) { this.description = description; }

    public String    getCategory()    { return category; }
    public void      setCategory(String category) { this.category = category; }

    public String    getLocation()    { return location; }
    public void      setLocation(String location) { this.location = location; }

    public LocalDate getDate()        { return date; }
    public void      setDate(LocalDate date) { this.date = date; }

    public String    getStatus()      { return status; }
    public void      setStatus(String status) { this.status = status; }

    public String    getImagePath()   { return imagePath; }
    public void      setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int       getReportedBy()  { return reportedBy; }
    public void      setReportedBy(int reportedBy) { this.reportedBy = reportedBy; }
}