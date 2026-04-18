// src/controller/ItemController.java  — Phase 4 version
package controller;

import dao.ItemDAO;
import java.time.LocalDate;
import java.util.List;
import model.*;

/**
 * Now delegates ALL data operations to ItemDAO.
 * The controller never writes SQL — it speaks in domain objects.
 * The DAO never has business logic — it only reads/writes DB.
 *
 * This separation means:
 * - Switch from SQLite to MySQL? Change DAO only.
 * - Change business rules? Change Controller only.
 * - Change UI? Change View only.
 * Perfect layered architecture.
 */
public class ItemController {

    private static ItemController instance;
    private final ItemDAO itemDAO;

    public static ItemController getInstance() {
        if (instance == null) instance = new ItemController();
        return instance;
    }

    private ItemController() {
        this.itemDAO = new ItemDAO();
    }

    // ── Lost Items ─────────────────────────────────────────────

    public LostItem addLostItem(String name, String description,
                                String category, String location,
                                String lastSeen, double reward,
                                String contact) {
        return addLostItem(name, description, category, location, lastSeen, reward, contact, null);
    }

    public LostItem addLostItem(String name, String description,
                                String category, String location,
                                String lastSeen, double reward,
                                String contact, String imagePath) {
        if (!UserController.getInstance().isLoggedIn()) {
            throw new IllegalStateException("Sign in required to report lost items.");
        }
        int reporterId = UserController.getInstance().getCurrentUser().getId();
        LostItem item = new LostItem(name, description, category, location,
            LocalDate.now(), reporterId, lastSeen, reward, contact);
        item.setImagePath(imagePath);
        itemDAO.insertLostItem(item);
        return item;
    }

    public List<LostItem>  getAllLostItems()                       { return itemDAO.getAllLostItems(); }
    public List<LostItem>  getLostItemsByReporter(int userId)      { return itemDAO.getLostItemsByReporter(userId); }
    public List<LostItem>  searchLostItems(String q, String cat)  { return itemDAO.searchLostItems(q, cat); }
    public void            deleteLostItem(int id)                 { itemDAO.deleteLostItem(id); }
    public void            updateLostItem(LostItem item)          { itemDAO.updateLostItem(item); }
    public boolean         isLostItemOwnedBy(int itemId, int userId) { return itemDAO.isLostItemOwnedBy(itemId, userId); }

    // ── Found Items ────────────────────────────────────────────

    public FoundItem addFoundItem(String name, String description,
                                  String category, String location,
                                  String turnedIn, String contact,
                                  boolean authority) {
        return addFoundItem(name, description, category, location, turnedIn, contact, authority, null);
    }

    public FoundItem addFoundItem(String name, String description,
                                  String category, String location,
                                  String turnedIn, String contact,
                                  boolean authority, String imagePath) {
        if (!UserController.getInstance().isLoggedIn()) {
            throw new IllegalStateException("Sign in required to report found items.");
        }
        int reporterId = UserController.getInstance().getCurrentUser().getId();
        FoundItem item = new FoundItem(name, description, category, location,
            LocalDate.now(), reporterId, turnedIn, contact, authority);
        item.setImagePath(imagePath);
        itemDAO.insertFoundItem(item);
        return item;
    }

    public List<FoundItem> getAllFoundItems()                       { return itemDAO.getAllFoundItems(); }
    public List<FoundItem> getFoundItemsByReporter(int userId)      { return itemDAO.getFoundItemsByReporter(userId); }
    public List<FoundItem> searchFoundItems(String q, String cat)  { return itemDAO.searchFoundItems(q, cat); }
    public void            deleteFoundItem(int id)                 { itemDAO.deleteFoundItem(id); }
    public void            updateFoundItem(FoundItem item)         { itemDAO.updateFoundItem(item); }
    public boolean         isFoundItemOwnedBy(int itemId, int userId) { return itemDAO.isFoundItemOwnedBy(itemId, userId); }

    // ── Analytics ──────────────────────────────────────────────

    public int getTotalItems()  { return itemDAO.countByTable("lost_items") + itemDAO.countByTable("found_items"); }
    public int getLostCount()   { return itemDAO.countByTable("lost_items"); }
    public int getFoundCount()  { return itemDAO.countByTable("found_items"); }
    public int getClaimedCount(){ return itemDAO.countByStatus("found_items", Item.STATUS_CLAIMED); }
}