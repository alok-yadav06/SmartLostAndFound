# Visual Data Flow Diagrams

## 1. User Login & Session Storage

```
┌─────────────────────────────────────────────────────┐
│ User enters credentials in LoginPanel               │
│ Username: "john_student"                            │
│ Password: "SecurePass123"                           │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ UserController.login(username, password)            │
│ Calls: UserDAO.findByUsername("john_student")      │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ Database Query:                                     │
│ SELECT * FROM users WHERE username = ?             │
│ Result: User(id=5, username="john_student", ...)   │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ Password verification succeeds                      │
│ UserController.currentUser = user                   │
│ ✓ User ID 5 now stored in session                   │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ UI Updates:                                         │
│ • Top bar shows: "john_student"                     │
│ • "Sign In" button changes to "Logout"              │
│ • Navigation buttons become available               │
└─────────────────────────────────────────────────────┘
```

---

## 2. Report Item Flow

```
User clicks "Report Lost Item"
        ↓
┌──────────────────────────────────┐
│ AddItemPanel                      │
│ • Name: "Blue Backpack"           │
│ • Category: "Bags"                │
│ • Submit button clicked           │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ ItemController.addLostItem(...)   │
│ • Gets currentUser.getId() → 5    │
│ • Creates LostItem with           │
│   reportedBy = 5                  │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ ItemDAO.insertLostItem(item)      │
│ INSERT INTO lost_items VALUES:    │
│ • name = "Blue Backpack"          │
│ • category = "Bags"               │
│ • reported_by = 5  ← USER ID!     │
│ • ...other fields...              │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ Database:                         │
│ ✓ Item inserted with reported_by  │
│   = 5 (john_student's ID)         │
└──────────────────────────────────┘
```

---

## 3. Viewing "My Reports" - The Main Flow

```
User clicks "👤 My Reports" Navigation
        ↓
┌──────────────────────────────────────────┐
│ MainFrame.buildNavItem() MouseListener    │
│ mouseClicked() → showPanel("MY_REPORTS")  │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ MainFrame.showPanel("MY_REPORTS")         │
│ • cardLayout.show() → displays panel     │
│ • Checks if MY_REPORTS ✓                  │
│ • Calls myReportsPanel.refresh() ✅ NEW   │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ MyReportsPanel.refresh()                  │
│ • Gets current user                       │
│ • userId = getCurrentUser().getId() → 5  │
│ • Clears existing table data              │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ ItemController.getLostItemsByReporter(5)  │
│ • Calls: itemDAO.getLostItemsByReporter(5)
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ ItemDAO.getLostItemsByReporter(5)         │
│                                           │
│ SQL Query:                                │
│ SELECT * FROM lost_items                 │
│ WHERE reported_by = 5                    │
│ ORDER BY created_at DESC                 │
│                                           │
│ Parameters:                               │
│ ps.setInt(1, 5)  ← User ID bound         │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ Database Execution:                       │
│ Finds all rows where reported_by = 5     │
│                                           │
│ Results:                                  │
│ • Item 1: "Blue Backpack" (reported_by=5)│
│ • Item 3: "Lost ID Card" (reported_by=5) │
│ • Item 5: "Brown Shoes" (reported_by=5)  │
│ • Item 2: "Red Wallet" (reported_by=5)   │
│ (NOT including items from other users)   │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ ItemDAO Returns List<LostItem>            │
│ Returns to ItemController                │
│ Returns to MyReportsPanel.refresh()      │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ MyReportsPanel.refresh() continues:       │
│ for (LostItem item : lostItems) {         │
│   lostModel.addRow(new Object[]{          │
│     item.getId(),                         │
│     item.getName(),                       │
│     item.getCategory(),                   │
│     item.getLocation(),                   │
│     item.getStatus(),                     │
│     item.getDate()                        │
│   });                                     │
│ }                                         │
└────────────┬─────────────────────────────┘
             ↓
┌──────────────────────────────────────────┐
│ UI Updates:                               │
│ JTable refreshes with new data:           │
│ ┌─────────────────────────────────────┐  │
│ │ ID │ Name      │ Category│Location │ │  │
│ ├─────────────────────────────────────┤  │
│ │ 1  │ Backpack  │ Bags    │ Library │ │  │
│ │ 3  │ ID Card   │ Docs    │ Cafe    │ │  │
│ │ 5  │ Shoes     │ Clothing│ Hostel  │ │  │
│ │ 2  │ Wallet    │ Accessories│Gym  │ │  │
│ └─────────────────────────────────────┘  │
│ ✓ Only items from john_student (ID=5)    │
└──────────────────────────────────────────┘
```

---

## 4. Data Filtering Logic

```
┌─────────────────────────────────────────────────────┐
│ Total Database Items:                               │
│ • Item 1: reported_by = 5 (john)     ✓ User sees  │
│ • Item 2: reported_by = 5 (john)     ✓ User sees  │
│ • Item 3: reported_by = 6 (mary)     ✗ User hides │
│ • Item 4: reported_by = 5 (john)     ✓ User sees  │
│ • Item 5: reported_by = 7 (admin)    ✗ User hides │
└─────────────────────────────────────────────────────┘
                        ↓
         WHERE clause in SQL filters:
            reported_by = current_user_id
                        ↓
┌─────────────────────────────────────────────────────┐
│ john_student (ID=5) sees:                           │
│ • Item 1: reported_by = 5 ✓                         │
│ • Item 2: reported_by = 5 ✓                         │
│ • Item 4: reported_by = 5 ✓                         │
│                                                     │
│ Items 3, 5 not in results (different user IDs)     │
└─────────────────────────────────────────────────────┘
                        ↓
         mary_student (ID=6) logs in:
                        ↓
┌─────────────────────────────────────────────────────┐
│ mary_student (ID=6) sees:                           │
│ • Item 3: reported_by = 6 ✓                         │
│                                                     │
│ Items 1, 2, 4, 5 not in results (different IDs)    │
└─────────────────────────────────────────────────────┘
```

---

## 5. Multi-User Scenario

```
┌────────────────────────────────────────────────────┐
│ User 1: john_student (ID=5)                        │
│ • Logs in                                          │
│ • Goes to "My Reports"                             │
│ • UserController.getCurrentUser().getId() = 5     │
│ • SQL: WHERE reported_by = 5                       │
│ • Sees: Items 1, 2, 4                              │
└────────────────────────────────────────────────────┘
                        ↓
              [User logs out]
                        ↓
┌────────────────────────────────────────────────────┐
│ UserController.currentUser = null                   │
│ "My Reports" shows: "Sign in to view reports"      │
└────────────────────────────────────────────────────┘
                        ↓
         [User 2 logs in as mary_student]
                        ↓
┌────────────────────────────────────────────────────┐
│ User 2: mary_student (ID=6)                        │
│ • Logs in                                          │
│ • Goes to "My Reports"                             │
│ • UserController.getCurrentUser().getId() = 6     │
│ • SQL: WHERE reported_by = 6                       │
│ • Sees: Item 3                                     │
│ • Does NOT see: Items 1, 2, 4 (different user)    │
└────────────────────────────────────────────────────┘
```

---

## 6. Database Table Structure

```
┌─────────────────────────────────────────────────────┐
│ TABLE: users                                        │
├─────────────────────────────────────────────────────┤
│ id │ username       │ full_name        │ ...       │
├─────────────────────────────────────────────────────┤
│ 5  │ john_student   │ John Student     │           │
│ 6  │ mary_student   │ Mary Smith       │           │
│ 7  │ admin_user     │ Admin User       │           │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ TABLE: lost_items                                    │
├──────────────────────────────────────────────────────┤
│ id │ name           │ category │ reported_by │ ... │
├──────────────────────────────────────────────────────┤
│ 1  │ Blue Backpack  │ Bags     │ 5           │     │ ← john
│ 2  │ Red Wallet     │ Accessories│ 5         │     │ ← john
│ 3  │ Book           │ Stationery│ 6          │     │ ← mary
│ 4  │ Brown Shoes    │ Clothing │ 5          │     │ ← john
│ 5  │ Pen Set        │ Stationery│ 7          │     │ ← admin
└──────────────────────────────────────────────────────┘

Query: SELECT * FROM lost_items WHERE reported_by = 5
         ↓
Returns rows 1, 2, 4 (john's items only)
```

---

## 7. Code Execution Path

```
showPanel("MY_REPORTS")
    ↓
cardLayout.show(contentArea, "MY_REPORTS")
    ↓
if (MY_REPORTS.equals(name) && myReportsPanel != null)
    ↓
myReportsPanel.refresh()
    ↓
User current = UserController.getInstance().getCurrentUser()
    ↓
int userId = current.getId()  // Gets 5 if john is logged in
    ↓
ItemController.getInstance().getLostItemsByReporter(userId)
    ↓
ItemDAO.getLostItemsByReporter(userId)
    ↓
PreparedStatement ps = conn.prepareStatement(sql)
ps.setInt(1, userId)  // Binds 5 to the ? parameter
ResultSet rs = ps.executeQuery()
    ↓
"SELECT * FROM lost_items WHERE reported_by = 5"
    ↓
Database returns items with reported_by = 5
    ↓
for (LostItem item : items) {
    lostModel.addRow(...)
}
    ↓
JTable displays filtered items
```

---

## 8. Before & After Comparison

```
BEFORE (No Refresh):
  User clicks "My Reports"
      ↓
  Panel displayed
      ↓
  Data is stale/not loaded ❌

AFTER (With Refresh):
  User clicks "My Reports"
      ↓
  Panel displayed
      ↓
  myReportsPanel.refresh() called ✅
      ↓
  Fresh data from database loaded
      ↓
  User sees current items ✓
```

---

## 9. Error Prevention Checks

```
┌──────────────────────────────────────┐
│ 1. Is user logged in?                │
│    if (current == null) return;       │ ✓ Prevents crash
├──────────────────────────────────────┤
│ 2. Is panel initialized?             │
│    if (lostModel == null) return;     │ ✓ Prevents crash
├──────────────────────────────────────┤
│ 3. Clear old data first              │
│    lostModel.setRowCount(0);          │ ✓ Prevents duplicates
├──────────────────────────────────────┤
│ 4. Use PreparedStatement              │
│    WHERE reported_by = ?              │ ✓ Prevents SQL injection
├──────────────────────────────────────┤
│ 5. Set parameters correctly           │
│    ps.setInt(1, userId);              │ ✓ Prevents wrong user data
└──────────────────────────────────────┘
```

---

## 10. Security Layers

```
┌─────────────────────────────────────┐
│ Layer 1: UI                          │
│ • Delete button hidden for users     │
│ • Only shows user's items            │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│ Layer 2: Business Logic             │
│ • ItemController gets userID         │
│ • Passes to DAO                      │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│ Layer 3: SQL Query                  │
│ • WHERE reported_by = userId         │
│ • Only items from that user          │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│ Layer 4: Parameter Binding          │
│ • PreparedStatement with ?           │
│ • Prevents SQL injection             │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│ Result: ✓ Secure & Correct          │
│ Only logged-in user's items shown    │
└─────────────────────────────────────┘
```

---

## Quick Reference Legend

```
✓ = Working / Good / Correct
✗ = Not working / Bad / Wrong
❌ = Error / Problem
✅ = NEW / Updated / Fixed
→ = Flow / Process
↓ = Next step
```

