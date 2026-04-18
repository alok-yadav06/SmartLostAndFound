# "My Reports" Feature - Implementation & Troubleshooting Guide

## Overview

The "My Reports" section displays items reported by the currently logged-in user. This guide explains how it works and how to verify it's working correctly.

---

## ✅ How It Works (Data Flow)

```
User Logs In
    ↓
User object with ID is stored in UserController.currentUser
    ↓
User clicks "My Reports" tab
    ↓
MainFrame.showPanel("MY_REPORTS") called
    ↓
MyReportsPanel.refresh() called (UPDATED ✓)
    ↓
refresh() gets currentUser.getId()
    ↓
ItemController.getLostItemsByReporter(userId)
ItemController.getFoundItemsByReporter(userId)
    ↓
ItemDAO executes SQL:
  SELECT * FROM lost_items WHERE reported_by = ? (with userId)
  SELECT * FROM found_items WHERE reported_by = ? (with userId)
    ↓
Results added to JTable:
  lostModel.addRow()
  foundModel.addRow()
    ↓
UI displays filtered items in "Lost Reports" & "Found Reports" tabs
```

---

## 🔧 What Was Fixed

### Update 1: MainFrame.java - Added Refresh on Navigation

**Location**: `src/view/MainFrame.java` - `showPanel()` method

**Change**: Now when user navigates to a panel, it refreshes the data:

```java
public void showPanel(String name) {
    // ... existing code ...
    cardLayout.show(contentArea, name);
    
    // ✅ NEW: Refresh data-dependent panels when navigating
    if (MY_REPORTS.equals(name) && myReportsPanel != null) {
        myReportsPanel.refresh();
    } else if (LOST_ITEMS.equals(name) && lostItemsPanel != null) {
        lostItemsPanel.refresh();
    }
    // ... etc ...
}
```

**Why**: When you navigate to "My Reports", the panel now immediately refreshes the data from the database, ensuring you always see current information.

---

## 🗄️ Database Structure

Ensure your database tables have these columns:

### `lost_items` table
```sql
CREATE TABLE lost_items (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    location VARCHAR(100),
    date_reported DATE,
    reported_by INTEGER,        -- ← User ID of who reported it
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reported_by) REFERENCES users(id)
);
```

### `found_items` table
```sql
CREATE TABLE found_items (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    location VARCHAR(100),
    date_reported DATE,
    reported_by INTEGER,        -- ← User ID of who reported it
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reported_by) REFERENCES users(id)
);
```

**Key**: The `reported_by` column must contain the user's ID (integer), not username.

---

## 📋 SQL Queries Used

### Get Lost Items by Reporter

```sql
SELECT * FROM lost_items 
WHERE reported_by = ? 
ORDER BY created_at DESC
```

**Parameters**: 
- `?` = User ID (from `UserController.getInstance().getCurrentUser().getId()`)

### Get Found Items by Reporter

```sql
SELECT * FROM found_items 
WHERE reported_by = ? 
ORDER BY created_at DESC
```

**Parameters**: 
- `?` = User ID (from `UserController.getInstance().getCurrentUser().getId()`)

---

## 🎯 Code Flow (Java Swing)

### Step 1: User Clicks "My Reports" Navigation

```java
// In MainFrame.buildNavItem() → MouseListener
@Override
public void mouseClicked(MouseEvent e) {
    setActiveNav(item, textLabel);
    showPanel("MY_REPORTS");  // ← Triggers navigation
}
```

### Step 2: MainFrame Routes to Correct Panel

```java
// In MainFrame.showPanel()
public void showPanel(String name) {
    cardLayout.show(contentArea, name);  // Show the panel
    
    // ✅ NEW: Refresh the data
    if (MY_REPORTS.equals(name) && myReportsPanel != null) {
        myReportsPanel.refresh();  // ← Loads current user's items
    }
}
```

### Step 3: MyReportsPanel Loads Data

```java
// In MyReportsPanel.refresh()
public final void refresh() {
    User current = UserController.getInstance().getCurrentUser();
    if (current == null) return;  // Not logged in
    
    int userId = current.getId();
    
    // Get items reported by this user
    List<LostItem> lostItems = 
        ItemController.getInstance().getLostItemsByReporter(userId);
    
    List<FoundItem> foundItems = 
        ItemController.getInstance().getFoundItemsByReporter(userId);
    
    // Populate tables
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
    
    // Same for foundItems...
}
```

### Step 4: ItemController Calls DAO

```java
// In ItemController
public List<LostItem> getLostItemsByReporter(int userId) {
    return itemDAO.getLostItemsByReporter(userId);
}
```

### Step 5: ItemDAO Executes SQL

```java
// In ItemDAO
public List<LostItem> getLostItemsByReporter(int reporterId) {
    List<LostItem> items = new ArrayList<>();
    String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY created_at DESC";
    
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, reporterId);  // ← Filter by user ID
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapLostItem(rs));  // Convert DB row to LostItem
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch user lost items: " + e.getMessage(), e);
    }
    return items;
}
```

---

## 🧪 Testing Checklist

### Test Case 1: Login and View Reports

```
1. Start application
2. Navigate to "My Reports" (without logging in)
3. VERIFY: Shows "Sign in to view your reports..."
4. Click login
5. Login with username: john_student
6. VERIFY: Redirects to "My Reports" tab
7. VERIFY: Shows items where reported_by = john's user_id
```

### Test Case 2: Report Item and See It in My Reports

```
1. Login as john_student (id = 5)
2. Go to "Report Lost Item"
3. Fill in form:
   - Name: "Blue Backpack"
   - Category: "Bags"
   - etc.
4. Submit
5. Navigate to "My Reports"
6. VERIFY: "Blue Backpack" appears in Lost Reports tab
   (The SQL ran: SELECT * FROM lost_items WHERE reported_by = 5)
```

### Test Case 3: Check Database Directly

```sql
-- Verify data in database
SELECT id, name, reported_by FROM lost_items WHERE reported_by = 5;
-- Should show items reported by user ID 5

-- If nothing shows:
SELECT COUNT(*) FROM lost_items;
-- Tells you if ANY items exist

-- Check if reported_by is NULL:
SELECT id, name, reported_by FROM lost_items WHERE reported_by IS NULL;
-- If this has results, the reported_by field isn't being set!
```

---

## 🐛 Troubleshooting Guide

### Problem 1: "My Reports" Shows No Data (Blank Tables)

**Possible Causes**:

1. **No items in database**
   - Solution: Go to "Report Lost Item" and add an item
   - Verify it appears in database: `SELECT * FROM lost_items LIMIT 1;`

2. **Items exist but `reported_by` is NULL**
   - Solution: Check ItemController when adding items
   - Make sure: `reporterId = UserController.getInstance().getCurrentUser().getId();`
   - Fix in database: `UPDATE lost_items SET reported_by = 5 WHERE id = 1;`

3. **Wrong user ID being used**
   - Solution: Check your login is working
   - Add debug logging:
     ```java
     public final void refresh() {
         User current = UserController.getInstance().getCurrentUser();
         System.out.println("DEBUG: Current user: " + current);
         System.out.println("DEBUG: User ID: " + current.getId());
         // ... rest of code
     }
     ```

4. **showPanel() not calling refresh()**
   - Solution: Ensure MainFrame.showPanel() has the refresh() calls (already updated ✓)

### Problem 2: Sees All Items (Not Filtered)

**Solution**: The ItemDAO query must have `WHERE reported_by = ?`

Check ItemDAO:
```java
// ❌ WRONG - Shows all items
String sql = "SELECT * FROM lost_items ORDER BY created_at DESC";

// ✅ CORRECT - Filters by user
String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY created_at DESC";
```

### Problem 3: "My Reports" Shows Items From Other Users

**Possible Causes**:

1. **Wrong user ID passed to DAO**
   ```java
   // ❌ WRONG
   int userId = 999;  // Hardcoded!
   
   // ✅ CORRECT
   int userId = UserController.getInstance().getCurrentUser().getId();
   ```

2. **reported_by field has wrong data**
   - Solution: Clear and re-enter test data:
     ```sql
     UPDATE lost_items SET reported_by = 5 WHERE id = 1;
     UPDATE lost_items SET reported_by = 6 WHERE id = 2;
     ```

### Problem 4: Application Crashes When Viewing My Reports

**Debug Steps**:

1. Check console for error message
2. Common issues:
   - Null pointer if `UserController.getCurrentUser()` returns null
   - SQL error if column names don't match
   - Connection error if database is offline

3. Add try-catch debugging:
   ```java
   public final void refresh() {
       try {
           User current = UserController.getInstance().getCurrentUser();
           if (current == null) {
               System.out.println("DEBUG: User not logged in");
               return;
           }
           System.out.println("DEBUG: Loading reports for user ID: " + current.getId());
           // ... rest of code
       } catch (Exception e) {
           System.err.println("ERROR in refresh(): " + e.getMessage());
           e.printStackTrace();
       }
   }
   ```

---

## 📊 Data Verification Query

Use this SQL to verify everything is set up correctly:

```sql
-- 1. Check user exists
SELECT id, username, full_name FROM users WHERE username = 'john_student';
-- Expected: Shows John's user ID (e.g., 5)

-- 2. Check items reported by John
SELECT id, name, reported_by FROM lost_items WHERE reported_by = 5;
-- Expected: Shows items with reported_by = 5

-- 3. Check all items and their reporters
SELECT id, name, reported_by, 
       (SELECT username FROM users WHERE id = lost_items.reported_by) as reporter_name
FROM lost_items;
-- Expected: Shows each item with its reporter's username

-- 4. Find orphaned items (no reported_by)
SELECT id, name, reported_by FROM lost_items WHERE reported_by IS NULL;
-- Expected: Should be empty (or shows problematic items)
```

---

## ✨ Complete Code Example

### MyReportsPanel.java - refresh() method

```java
public final void refresh() {
    // Get currently logged-in user
    User current = UserController.getInstance().getCurrentUser();
    
    // If not logged in or UI not initialized yet
    if (current == null || lostModel == null || foundModel == null) {
        if (current != null) rebuild();  // Rebuild UI if needed
        return;
    }

    // Clear existing data
    lostModel.setRowCount(0);
    foundModel.setRowCount(0);
    timelineModel.setRowCount(0);

    // Get user ID
    int userId = current.getId();
    System.out.println("Loading reports for user ID: " + userId);

    // Fetch items reported by this user
    List<LostItem> lostItems = ItemController.getInstance()
        .getLostItemsByReporter(userId);
    List<FoundItem> foundItems = ItemController.getInstance()
        .getFoundItemsByReporter(userId);

    // Add lost items to table
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

    // Add found items to table
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

    System.out.println("Loaded " + lostItems.size() + " lost items and " + 
                       foundItems.size() + " found items");
}
```

### ItemDAO.java - getLostItemsByReporter()

```java
public List<LostItem> getLostItemsByReporter(int reporterId) {
    List<LostItem> items = new ArrayList<>();
    
    // SQL with WHERE clause to filter by reporter
    String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY created_at DESC";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        // Set the parameter (reporterId)
        ps.setInt(1, reporterId);
        
        // Execute query
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Convert each row to a LostItem object
                items.add(mapLostItem(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch user lost items: " + e.getMessage(), e);
    }
    
    return items;
}
```

---

## 🔑 Key Concepts

### 1. **PreparedStatement with Parameters**
```java
String sql = "SELECT * FROM lost_items WHERE reported_by = ?";
//                                                           ↑
//                    Parameterized query - SAFE from SQL injection

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, reporterId);  // Replace ? with reporterId
    // Execute...
}
```

### 2. **User Session Storage**
```java
UserController.getInstance().getCurrentUser();
// Returns the logged-in user (or null if not logged in)
// Contains: id, username, email, role, etc.

currentUser.getId();
// Gets the database user ID (integer)
// Used to filter items by reported_by = userId
```

### 3. **Table Model Updates**
```java
lostModel.setRowCount(0);  // Clear existing rows
// ... populate from database ...
lostModel.addRow(new Object[]{id, name, category, location, status, date});
// UI automatically updates to show new data
```

---

## 📈 Scalability Notes

As your system grows:

1. **Add Pagination**: If user has 1000+ items, don't load all at once
   ```java
   public List<LostItem> getLostItemsByReporter(int reporterId, int page, int pageSize) {
       int offset = (page - 1) * pageSize;
       String sql = "SELECT * FROM lost_items WHERE reported_by = ? " +
                    "ORDER BY created_at DESC LIMIT ? OFFSET ?";
       // ps.setInt(2, pageSize);
       // ps.setInt(3, offset);
   }
   ```

2. **Add Caching**: Cache user's items locally
   ```java
   private Map<Integer, List<LostItem>> cache = new HashMap<>();
   
   public List<LostItem> getLostItemsByReporter(int reporterId) {
       if (cache.containsKey(reporterId)) {
           return cache.get(reporterId);
       }
       // ... fetch from DB ...
       cache.put(reporterId, items);
       return items;
   }
   ```

3. **Add Filtering**: Sort by date, status, etc.
   ```java
   public List<LostItem> getLostItemsByReporter(int reporterId, String sortBy) {
       String sql = "SELECT * FROM lost_items WHERE reported_by = ? ORDER BY " + sortBy;
       // Validate sortBy to prevent SQL injection
   }
   ```

---

## ✅ Implementation Checklist

- ✅ Database has `reported_by` column in both `lost_items` and `found_items`
- ✅ ItemDAO has `getLostItemsByReporter()` and `getFoundItemsByReporter()`
- ✅ ItemController delegates to ItemDAO methods
- ✅ MainFrame.showPanel() calls refresh() for data panels
- ✅ MyReportsPanel.refresh() gets currentUser.getId() and filters items
- ✅ JTable models are properly populated with filtered data
- ✅ User can login and see their items in "My Reports"
- ✅ Console debug logs show correct user ID and item counts
- ✅ Database verification confirms `reported_by` is set correctly

---

## 🎯 Summary

| Component | Purpose | Status |
|-----------|---------|--------|
| `reported_by` column | Store which user reported item | ✅ Required |
| ItemDAO filters query | SQL WHERE clause filters by user | ✅ Already exists |
| ItemController methods | Delegates to DAO | ✅ Already exists |
| MainFrame.showPanel() | Refresh on navigation | ✅ UPDATED |
| MyReportsPanel.refresh() | Load current user's items | ✅ Already works |
| UserController.getCurrentUser() | Get logged-in user | ✅ Already works |

**Current Status**: 🟢 **READY TO TEST**

