// src/model/ItemNotFoundException.java
package model;

/**
 * Custom Exception — a key Java concept.
 *
 * WHY CUSTOM EXCEPTIONS?
 * Generic exceptions like Exception or RuntimeException
 * tell you nothing about WHAT went wrong in your domain.
 *
 * Compare:
 *   throw new Exception("error");            ← useless
 *   throw new ItemNotFoundException(42);     ← meaningful!
 *
 * Any catch block can now specifically catch this type
 * and handle it differently from, say, a DB connection error.
 *
 * Extends RuntimeException → unchecked exception.
 * Meaning callers are NOT forced to wrap in try-catch.
 * Use checked (extends Exception) for recoverable errors,
 * unchecked (extends RuntimeException) for programmer errors.
 */
public class ItemNotFoundException extends RuntimeException {

    private final int itemId;

    public ItemNotFoundException(int itemId) {
        super("Item not found with ID: " + itemId);
        this.itemId = itemId;
    }

    public ItemNotFoundException(String message) {
        super(message);
        this.itemId = -1;
    }

    public int getItemId() { return itemId; }
}