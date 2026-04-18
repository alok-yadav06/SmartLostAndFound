// src/model/Authenticatable.java
package model;

/**
 * Interface: defines a CONTRACT, not an implementation.
 *
 * WHY AN INTERFACE HERE?
 * Imagine tomorrow you add:
 *   - GuestUser (can browse but not claim)
 *   - OAuthUser (logs in via Google)
 *   - LDAPUser  (logs in via company directory)
 *
 * Each authenticates differently, but all must satisfy:
 *   1. Can they be authenticated with a password?
 *   2. Are they an admin?
 *
 * The rest of the system only needs to know about
 * Authenticatable — not the specific class.
 * That's ABSTRACTION.
 *
 * Interface vs Abstract Class:
 * Use Interface  → when defining a CAPABILITY ("can do X")
 * Use Abstract   → when defining an IDENTITY  ("is a X")
 * Item is abstract because LostItem/FoundItem ARE items.
 * Authenticatable is an interface because Users CAN authenticate.
 */
public interface Authenticatable {

    /**
     * Checks if the provided raw password matches the stored credential.
     * @param rawPassword the password entered by the user
     * @return true if authentication succeeds
     */
    boolean authenticate(String rawPassword);

    /**
     * Checks if this entity has admin-level privileges.
     */
    boolean isAdmin();
}