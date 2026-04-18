/*
 * MY REPORTS FEATURE - CODE EXAMPLES & QUICK REFERENCE
 * 
 * This file contains ready-to-use code patterns for the "My Reports" feature
 * Copy-paste these examples into your code as needed
 */

// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 1: How Items Are Stored with reported_by
// ═══════════════════════════════════════════════════════════════════════════

// File: src/controller/ItemController.java
// When a user reports a lost item:

public LostItem addLostItem(String name, String description,
                            String category, String location,
                            String lastSeen, double reward,
                            String contact, String imagePath) {
    
    // ✅ KEY: Get the logged-in user's ID
    if (!UserController.getInstance().isLoggedIn()) {
        throw new IllegalStateException("Sign in required to report lost items.");
    }
    
    int reporterId = UserController.getInstance().getCurrentUser().getId();
    //   ↑↑↑ This ID gets stored in reported_by column
    
    // Create item with reporter ID
    LostItem item = new LostItem(name, description, category, location,
        LocalDate.now(), reporterId, lastSeen, reward, contact);
    //                           ↑↑↑
    //                    This goes to reported_by
    
    item.setImagePath(imagePath);
    itemDAO.insertLostItem(item);  // Saves to DB with reported_by
    return item;
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 2: DAO Query - Filter by Reporter
// ═══════════════════════════════════════════════════════════════════════════

// File: src/dao/ItemDAO.java

public List<LostItem> getLostItemsByReporter(int reporterId) {
    List<LostItem> items = new ArrayList<>();
    
    // ✅ SQL: WHERE clause filters by reported_by
    String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY created_at DESC";
    //                                           reported_by = ?
    //                                           ↑ Only return items from specific user
    
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, reporterId);  // ← Set the user ID parameter
        
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapLostItem(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch user lost items: " + e.getMessage(), e);
    }
    
    return items;  // Only returns items where reported_by matches
}

// SAME PATTERN for found items:
public List<FoundItem> getFoundItemsByReporter(int reporterId) {
    List<FoundItem> items = new ArrayList<>();
    String sql = "SELECT * FROM found_items WHERE reported_by = ? ORDER BY created_at DESC";
    //                                           reported_by = ?
    //                                           ↑ SAME PATTERN
    
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, reporterId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapFoundItem(rs));
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch user found items: " + e.getMessage(), e);
    }
    return items;
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 3: Controller - Delegate to DAO
// ═══════════════════════════════════════════════════════════════════════════

// File: src/controller/ItemController.java

public List<LostItem> getLostItemsByReporter(int userId) {
    // ✅ Simple delegation to DAO
    return itemDAO.getLostItemsByReporter(userId);
}

public List<FoundItem> getFoundItemsByReporter(int userId) {
    // ✅ Simple delegation to DAO
    return itemDAO.getFoundItemsByReporter(userId);
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 4: MyReportsPanel - Load and Display User's Items
// ═══════════════════════════════════════════════════════════════════════════

// File: src/view/MyReportsPanel.java

public final void refresh() {
    // ✅ Step 1: Get the currently logged-in user
    User current = UserController.getInstance().getCurrentUser();
    
    // Not logged in? Exit early
    if (current == null || lostModel == null || foundModel == null) {
        if (current != null) {
            rebuild();  // Rebuild UI if needed
        }
        return;
    }

    // ✅ Step 2: Clear existing table data
    lostModel.setRowCount(0);
    foundModel.setRowCount(0);
    timelineModel.setRowCount(0);

    // ✅ Step 3: Get user's ID
    int userId = current.getId();
    System.out.println("Loading reports for user ID: " + userId);

    // ✅ Step 4: Fetch items where reported_by = userId
    List<LostItem> lostItems = ItemController.getInstance()
        .getLostItemsByReporter(userId);
    //   ↑ Returns only items where reported_by = userId (from DB)
    
    List<FoundItem> foundItems = ItemController.getInstance()
        .getFoundItemsByReporter(userId);
    //   ↑ Returns only items where reported_by = userId (from DB)

    // ✅ Step 5: Add to UI table model
    for (LostItem item : lostItems) {
        lostModel.addRow(new Object[]{
            item.getId(), 
            item.getName(), 
            item.getCategory(), 
            item.getLocation(),
            item.getStatus(), 
            item.getDate()
        });
    }

    for (FoundItem item : foundItems) {
        foundModel.addRow(new Object[]{
            item.getId(), 
            item.getName(), 
            item.getCategory(), 
            item.getLocation(),
            item.getStatus(), 
            item.getDate()
        });
    }

    System.out.println("✓ Loaded " + lostItems.size() + " lost and " 
                       + foundItems.size() + " found items");
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 5: MainFrame - Refresh on Navigation (UPDATED)
// ═══════════════════════════════════════════════════════════════════════════

// File: src/view/MainFrame.java

public void showPanel(String name) {
    // ... existing admin check code ...
    
    // Show the selected panel
    cardLayout.show(contentArea, name);
    
    // ✅ UPDATED: Refresh data when navigating to panels
    // This ensures fresh data is loaded from the database
    if (MY_REPORTS.equals(name) && myReportsPanel != null) {
        myReportsPanel.refresh();  // ← Reloads user's items
    } else if (DASHBOARD.equals(name) && dashboardPanel != null) {
        dashboardPanel.refresh();
    } else if (LOST_ITEMS.equals(name) && lostItemsPanel != null) {
        lostItemsPanel.refresh();
    } else if (FOUND_ITEMS.equals(name) && foundItemsPanel != null) {
        foundItemsPanel.refresh();
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 6: User Login - Flow Overview
// ═══════════════════════════════════════════════════════════════════════════

// File: src/controller/UserController.java

public boolean login(String username, String password) {
    // Fetch user from database
    User user = userDAO.findByUsername(username);
    
    if (user != null && user.authenticate(password)) {
        // ✅ Store user in session
        this.currentUser = user;  // ← User object with ID stored here
        userDAO.recordLoginEvent(user, "LOGIN", "Successful login");
        System.out.println("✅ Logged in: " + user);
        return true;
    }
    userDAO.recordFailedLogin(username, "Invalid username or password");
    return false;
}

// Later, when accessing current user's data:
public User getCurrentUser() {
    return currentUser;  // ← This user has .getId() available
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 7: Database Verification Queries
// ═══════════════════════════════════════════════════════════════════════════

/*
 * Run these SQL queries to verify your data is set up correctly
 */

// Check 1: Verify users table exists and has data
SELECT id, username, full_name FROM users LIMIT 5;
// Expected: Shows user IDs (e.g., 1, 2, 3)

// Check 2: Verify lost_items has reported_by column
DESCRIBE lost_items;
// Expected: Column "reported_by" should exist and be INTEGER

// Check 3: Check items reported by user ID 5
SELECT id, name, reported_by FROM lost_items WHERE reported_by = 5;
// Expected: Shows items with reported_by = 5

// Check 4: Find ALL items with their reporter
SELECT l.id, l.name, l.reported_by, u.username 
FROM lost_items l 
LEFT JOIN users u ON l.reported_by = u.id
ORDER BY l.reported_by;
// Expected: Shows each item with its reporter's username

// Check 5: Find items with NULL reported_by (PROBLEM!)
SELECT id, name, reported_by FROM lost_items WHERE reported_by IS NULL;
// Expected: Should be empty. If NOT empty, reported_by isn't being set!

// Check 6: Verify found_items table
SELECT id, name, reported_by FROM found_items WHERE reported_by = 5;
// Expected: Shows found items reported by user 5


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 8: Debugging - Add Logging
// ═══════════════════════════════════════════════════════════════════════════

// Add this to MyReportsPanel.refresh() to debug:

public final void refresh() {
    User current = UserController.getInstance().getCurrentUser();
    System.out.println("DEBUG: MyReportsPanel.refresh() called");
    System.out.println("DEBUG: Current user: " + current);
    
    if (current == null) {
        System.out.println("DEBUG: User is null - showing guest message");
        return;
    }
    
    int userId = current.getId();
    System.out.println("DEBUG: User ID: " + userId);
    System.out.println("DEBUG: User name: " + current.getFullName());
    
    List<LostItem> lostItems = ItemController.getInstance()
        .getLostItemsByReporter(userId);
    
    System.out.println("DEBUG: Found " + lostItems.size() + " lost items");
    
    for (LostItem item : lostItems) {
        System.out.println("DEBUG: Item - ID: " + item.getId() 
                          + ", Name: " + item.getName() 
                          + ", ReportedBy: " + item.getReportedBy());
    }
    
    // ... rest of code ...
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 9: Getting User ID in Different Contexts
// ═══════════════════════════════════════════════════════════════════════════

// Context 1: In a panel class
int userId = UserController.getInstance().getCurrentUser().getId();

// Context 2: In a controller
int userId = UserController.getInstance().getCurrentUser().getId();

// Context 3: In a DAO (already has it as parameter)
public void someMethod(int userId) {
    // userId is the parameter passed from controller
}

// Context 4: Safely (with null check)
User user = UserController.getInstance().getCurrentUser();
if (user != null) {
    int userId = user.getId();
    // Use userId safely
}


// ═══════════════════════════════════════════════════════════════════════════
// EXAMPLE 10: Common Mistakes & Fixes
// ═══════════════════════════════════════════════════════════════════════════

/*
 * MISTAKE 1: Hardcoded user ID
 */
// ❌ WRONG
List<LostItem> items = itemDAO.getLostItemsByReporter(5);  // Always user 5!

// ✅ CORRECT
int userId = UserController.getInstance().getCurrentUser().getId();
List<LostItem> items = itemDAO.getLostItemsByReporter(userId);


/*
 * MISTAKE 2: Using username instead of ID in SQL
 */
// ❌ WRONG
String sql = "SELECT * FROM lost_items WHERE reporter_name = ?";
//                                           reporter_name (doesn't exist!)

// ✅ CORRECT
String sql = "SELECT * FROM lost_items WHERE reported_by = ?";
//                                           reported_by (is user ID)


/*
 * MISTAKE 3: Not clearing table before refreshing
 */
// ❌ WRONG
for (LostItem item : lostItems) {
    lostModel.addRow(...);  // Adds to existing rows - duplicates!
}

// ✅ CORRECT
lostModel.setRowCount(0);  // Clear existing rows
for (LostItem item : lostItems) {
    lostModel.addRow(...);  // Now only new items
}


/*
 * MISTAKE 4: Not checking if user is logged in
 */
// ❌ WRONG
int userId = UserController.getInstance().getCurrentUser().getId();
// Crashes if user is null!

// ✅ CORRECT
User current = UserController.getInstance().getCurrentUser();
if (current == null) return;  // Check first
int userId = current.getId();


/*
 * MISTAKE 5: Not refreshing when navigating
 */
// ❌ WRONG
public void showPanel(String name) {
    cardLayout.show(contentArea, name);
    // Data is stale!
}

// ✅ CORRECT
public void showPanel(String name) {
    cardLayout.show(contentArea, name);
    if (MY_REPORTS.equals(name) && myReportsPanel != null) {
        myReportsPanel.refresh();  // Fresh data from DB
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// SUMMARY: Data Flow for "My Reports"
// ═══════════════════════════════════════════════════════════════════════════

/*
 * 1. User clicks "My Reports" navigation
 *    ↓
 * 2. MainFrame.showPanel("MY_REPORTS") called
 *    ↓
 * 3. MainFrame calls myReportsPanel.refresh()
 *    ↓
 * 4. MyReportsPanel gets currentUser.getId()
 *    ↓
 * 5. ItemController.getLostItemsByReporter(userId) called
 *    ↓
 * 6. ItemDAO queries: SELECT * FROM lost_items WHERE reported_by = ?
 *    ↓
 * 7. Results returned to MyReportsPanel
 *    ↓
 * 8. JTable model populated with filtered items
 *    ↓
 * 9. UI displays only current user's items ✓
 */

