// src/model/User.java
package model;

/**
 * Represents a system user.
 *
 * INTERFACE usage coming here:
 * We define an Authenticatable interface that User implements.
 * Why? Tomorrow you might add OAuth users, LDAP users, etc.
 * They all "authenticate" differently but share the same contract.
 *
 * ENUM for roles: Much safer than String.
 * "ADMIN" as a String can be typo'd as "Admin", "admin", "ADMON".
 * An enum makes that impossible at compile time.
 */
public class User implements Authenticatable {

    // ── Role Enum ──────────────────────────────────────────────
    /**
     * Enum = a fixed set of constants.
     * Why not static Strings? Enums are type-safe.
     * You can't pass Role.SUPERADMIN (doesn't exist) by mistake.
     */
    public enum Role {
        ADMIN("Admin"),
        USER("User");

        private final String displayName;

        Role(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }
    }

    // ── Fields ─────────────────────────────────────────────────
    private int    id;
    private String username;
    private String passwordHash; // NEVER store plain passwords
    private String email;
    private String fullName;
    private Role   role;
    private boolean isActive;

    // ── Constructor ────────────────────────────────────────────
    public User(String username, String passwordHash,
                String email, String fullName, Role role) {
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username cannot be empty.");
        if (passwordHash == null || passwordHash.isEmpty())
            throw new IllegalArgumentException("Password cannot be empty.");

        this.username     = username.trim().toLowerCase(); // normalize
        this.passwordHash = passwordHash;
        this.email        = email;
        this.fullName     = fullName;
        this.role         = (role != null) ? role : Role.USER;
        this.isActive     = true;
    }

    // ── Implementing Authenticatable Interface ─────────────────
    /**
     * INTERFACE:
     * Authenticatable defines the "can authenticate" contract.
     * Any class implementing it MUST provide these two methods.
     * See Authenticatable.java below.
     */
    @Override
    public boolean authenticate(String rawPassword) {
        // We compare against the stored hash
        // In Phase 3 we'll use real hashing — for now, direct compare
        return this.passwordHash.equals(rawPassword);
    }

    @Override
    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    // ── Role Checks ────────────────────────────────────────────
    public boolean canDelete() { return isAdmin(); }
    public boolean canApprove(){ return isAdmin(); }

    // ── Getters & Setters ──────────────────────────────────────
    public int     getId()           { return id; }
    public void    setId(int id)     { this.id = id; }

    public String  getUsername()     { return username; }
    public String  getEmail()        { return email; }
    public void    setEmail(String e){ this.email = e; }

    public String  getFullName()     { return fullName; }
    public void    setFullName(String n) { this.fullName = n; }

    public String  getPasswordHash() { return passwordHash; }
    public void    setPasswordHash(String h) { this.passwordHash = h; }

    public Role    getRole()         { return role; }
    public void    setRole(Role r)   { this.role = r; }

    public boolean isActive()        { return isActive; }
    public void    setActive(boolean active) { this.isActive = active; }

    @Override
    public String toString() {
        return String.format("User[%d] %s (%s) — %s",
            id, fullName, username, role.getDisplayName());
    }
}