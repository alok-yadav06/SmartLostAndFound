# "My Reports" Quick Test & Verification Guide

## 🚀 Quick Start (5 minutes)

### Step 1: Run Your Application
```
mvn clean compile exec:java
```

### Step 2: Login
```
Username: (any existing user)
Password: (their password)
```

### Step 3: Navigate to "My Reports"
- Click the sidebar button "👤 My Reports"

### Step 4: Verify Data Loads
- ✅ Should see tables with data
- ✅ Should see items YOU reported
- ✅ Should NOT see items from other users

---

## ✅ Test Cases

### Test 1: Basic Data Display

```
Steps:
  1. Login as "john_student"
  2. Click "My Reports"
  3. Look at "Lost Reports" tab
  
Expected Result:
  ✓ Shows items where YOU are the reporter
  ✓ Shows columns: ID, Name, Category, Location, Status, Date
  ✓ At least 1 item appears (if you reported any)
```

### Test 2: Add Item and See It in My Reports

```
Steps:
  1. Go to "Report Lost Item"
  2. Fill in form (required fields marked with *)
  3. Submit
  4. Go to "My Reports"
  5. Click "Lost Reports" tab
  
Expected Result:
  ✓ Your new item appears in the table
  ✓ Item shows YOUR ID as reporter (in reported_by column)
```

### Test 3: Switch Users

```
Steps:
  1. Login as "john_student"
  2. Note items shown in "My Reports"
  3. Logout
  4. Login as "mary_student" (different user)
  5. Go to "My Reports"
  
Expected Result:
  ✓ Shows DIFFERENT items than before
  ✓ Only items reported by "mary_student"
  ✓ NOT showing john's items
```

### Test 4: Guest Mode

```
Steps:
  1. Start app without logging in
  2. Click "My Reports"
  
Expected Result:
  ✓ Shows message: "Sign in to view your reports..."
  ✓ No data tables visible
  ✓ Login and come back
```

### Test 5: Data Persistence

```
Steps:
  1. Login and go to "My Reports"
  2. Note the items shown
  3. Navigate away to "Lost Items"
  4. Navigate back to "My Reports"
  
Expected Result:
  ✓ Same data still shows
  ✓ No duplicates in table
  ✓ Data refreshes from DB each time
```

---

## 🐛 Debug Checklist

Run through this if data isn't showing:

- [ ] **Are you logged in?**
  - Check: Look at top bar, should NOT say "Guest Mode"
  
- [ ] **Did you report any items?**
  - Check: Go to "Report Lost Item" and submit one
  - Check database: `SELECT COUNT(*) FROM lost_items;`
  
- [ ] **Is the reported_by field set?**
  - Check database:
    ```sql
    SELECT id, name, reported_by FROM lost_items LIMIT 3;
    ```
  - Should show numbers in reported_by, NOT NULL
  
- [ ] **Does your user ID match the reported_by?**
  - Get your user ID:
    ```sql
    SELECT id, username FROM users WHERE username = 'your_username';
    ```
  - Check items you reported:
    ```sql
    SELECT id, name, reported_by FROM lost_items WHERE name LIKE '%your_item%';
    ```
  - The reported_by should match your user id
  
- [ ] **Are refresh() calls working?**
  - Look at console output for debug messages
  - Add `System.out.println()` in MyReportsPanel.refresh()
  - Should see: "Loading reports for user ID: X"

---

## 🔧 Database Verification

### Verify Database Structure

```sql
-- Check lost_items table has reported_by column
DESCRIBE lost_items;
-- Look for: reported_by  INT

-- Same for found_items
DESCRIBE found_items;
-- Look for: reported_by  INT
```

### Check Your Data

Replace `5` with your actual user ID:

```sql
-- See all items YOU reported
SELECT id, name, category, reported_by FROM lost_items WHERE reported_by = 5;

-- See ALL items and who reported them
SELECT l.id, l.name, u.username as reporter 
FROM lost_items l 
LEFT JOIN users u ON l.reported_by = u.id;

-- Count items per reporter
SELECT reported_by, COUNT(*) as count FROM lost_items GROUP BY reported_by;

-- Find problematic records (no reporter)
SELECT id, name, reported_by FROM lost_items WHERE reported_by IS NULL;
```

---

## 📊 Expected Console Output

When working correctly, you should see in console:

```
Loading reports for user ID: 5
✓ Loaded 3 lost items and 1 found items
```

If you see:
```
Loading reports for user ID: null
```
→ User is not logged in properly

If you see:
```
✓ Loaded 0 lost items and 0 found items
```
→ Either no items reported by you, or reported_by doesn't match your ID

---

## 📱 UI Elements to Check

### In "My Reports" Panel

**Should See**:
- [ ] Title: "My Reports"
- [ ] Your name and username shown
- [ ] Two tabs: "Lost Reports" and "Found Reports"
- [ ] Activity Timeline on the right
- [ ] Table with columns for each item

**Should NOT See** (if logged in):
- [ ] Message "Sign in to view your reports"
- [ ] Items from other users
- [ ] Broken layout or missing elements

---

## 🎯 Step-by-Step Walkthrough

### Scenario: John reports a lost item

```
1. John logs in
   → UserController.currentUser = User(id=5, username="john_student")

2. John goes to "Report Lost Item"
   → addLostItem() is called
   → reporterId = 5 (from currentUser.getId())
   → Item inserted with reported_by = 5

3. John navigates to "My Reports"
   → MainFrame.showPanel("MY_REPORTS") called
   → myReportsPanel.refresh() called
   → Gets userId = 5
   → Calls ItemController.getLostItemsByReporter(5)
   → DAO executes: SELECT * FROM lost_items WHERE reported_by = 5
   → Returns list with John's item
   → Table populated with John's items
   → UI displays: ✓

4. Mary logs in
   → UserController.currentUser = User(id=6, username="mary_student")

5. Mary navigates to "My Reports"
   → myReportsPanel.refresh() called
   → Gets userId = 6
   → DAO executes: SELECT * FROM lost_items WHERE reported_by = 6
   → Returns Mary's items only
   → John's item NOT shown
   → UI displays: ✓
```

---

## 🚨 Common Issues & Quick Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| No data showing | Not logged in | Login first |
| No data showing | Haven't reported items | Report an item |
| No data showing | reported_by is NULL | Check ItemController when saving |
| Shows all items | WHERE clause missing | Check ItemDAO SQL |
| Shows other users' items | Wrong user ID used | Check UserController.currentUser |
| Crashes with NullPointerException | currentUser is null | Add null check |
| Shows duplicates | Table not cleared | Check lostModel.setRowCount(0) |
| Data stale after reporting | refresh() not called | Check MainFrame.showPanel() |

---

## ✅ Full Verification Checklist

Go through this before reporting issues:

- [ ] Application starts without errors
- [ ] Can login successfully
- [ ] Top bar shows your username (not "Guest")
- [ ] Can report a lost item
- [ ] Item appears in "Lost Items" tab
- [ ] Item appears in "My Reports" > "Lost Reports" tab
- [ ] Can report a found item
- [ ] Found item appears in "My Reports" > "Found Reports" tab
- [ ] Logout works
- [ ] After logout, "My Reports" shows login message
- [ ] Can login as different user
- [ ] Different user sees their own items only
- [ ] No console errors or warnings
- [ ] UI renders without glitches
- [ ] Tables scroll properly if many items
- [ ] Activity timeline shows your actions

---

## 📞 Debug Commands

### Enable Full Debugging

Add this to MyReportsPanel.refresh():

```java
System.out.println("\n=== MY REPORTS DEBUG INFO ===");
User current = UserController.getInstance().getCurrentUser();
System.out.println("Current user: " + (current == null ? "null" : current.getUsername()));
System.out.println("User ID: " + (current == null ? "null" : current.getId()));
System.out.println("Lost items table model rows: " + lostModel.getRowCount());
System.out.println("Found items table model rows: " + foundModel.getRowCount());
System.out.println("==============================\n");
```

### Check Database from Console

```bash
# Connect to database
sqlite3 lost_and_found.db

# Then run queries
sqlite> SELECT id, username FROM users;
sqlite> SELECT id, name, reported_by FROM lost_items;
sqlite> SELECT COUNT(*) FROM lost_items WHERE reported_by = 5;
```

---

## 🎓 Key Things to Remember

1. **User ID is a number**, not username
   - When you login, `currentUser.getId()` returns an integer
   - This integer is stored in `reported_by` column

2. **PreparedStatement prevents SQL injection**
   - Uses `?` as placeholder for parameters
   - Parameters are escaped automatically

3. **Tables are refreshed on navigation**
   - When you click "My Reports", data is loaded fresh from DB
   - Not showing cached data

4. **NULL in reported_by is a problem**
   - If reported_by is NULL, your item won't show in "My Reports"
   - Check that ItemController properly sets reporterId

5. **Refresh clears old data**
   - `lostModel.setRowCount(0)` removes all rows
   - Then new data is added
   - Prevents duplicates and stale data

---

## 🎉 Success Criteria

Your "My Reports" feature is working when:

✅ After login, you see items YOU reported  
✅ Other users don't see your items  
✅ Data updates when you report new items  
✅ No console errors  
✅ UI displays cleanly  
✅ Logout/login works properly  
✅ Database has `reported_by` values  

---

**Current Status**: 🟢 READY FOR TESTING

If issues persist after checking this guide, see MY_REPORTS_GUIDE.md for detailed troubleshooting.

