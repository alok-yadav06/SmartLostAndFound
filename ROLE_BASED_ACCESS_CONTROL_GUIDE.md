# Role-Based Access Control (RBAC) Implementation Guide

## Overview
This guide demonstrates how to implement role-based access control in your Lost & Found Java Swing application. Currently, the "Delete Selected" button is shown to all logged-in users, but it should only be visible to admin users.

---

## Architecture Overview

Your application already has the foundation for RBAC in place:

### 1. **User Role Model** (Already Implemented ✓)
Location: `src/model/User.java`

```java
public enum Role {
    ADMIN("Admin"),
    USER("User");
    
    private final String displayName;
    Role(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

### 2. **Current User Session** (Already Implemented ✓)
Location: `src/controller/UserController.java`

```java
private static User currentUser;  // Logged-in user stored here

public boolean isAdmin() {
    return currentUser != null && currentUser.isAdmin();
}

public boolean isLoggedIn() {
    return currentUser != null;
}

public User getCurrentUser() {
    return currentUser;
}
```

---

## What Was Already Done

✅ **Backend Protection**: The `canModifyItem()` method in LostItemsPanel already checks:
- If user is admin → allow delete
- If user is owner of item → allow delete
- Otherwise → deny

✅ **Database Role Field**: User model has `role` column

---

## What Needs To Be Fixed

❌ **Frontend Button Visibility**: The "Delete Selected" button is shown to all logged-in users regardless of role
- Should only be visible to admins
- Should be hidden/disabled for regular users

---

## Solution

### Step 1: Update Button Visibility Logic

**File**: `src/view/LostItemsPanel.java` (Lines 144-157)

**Current Code**:
```java
if (UserController.getInstance().isLoggedIn()) {
    leftActions.add(editBtn);
    leftActions.add(delBtn);  // ❌ Shown for all logged-in users!
}
```

**Updated Code**:
```java
// Show edit & delete buttons ONLY for admins
if (UserController.getInstance().isAdmin()) {
    leftActions.add(editBtn);
    leftActions.add(delBtn);  // ✅ Only for admins
}
```

**Why This Works**:
- `isAdmin()` returns true only if a user with Role.ADMIN is logged in
- Regular users won't see the button at all
- Better UX than showing a disabled button

---

### Step 2: Apply Same Logic to FoundItemsPanel

**File**: `src/view/FoundItemsPanel.java` (Same pattern)

Update the delete button visibility to check `isAdmin()` instead of `isLoggedIn()`.

---

### Step 3: Verify Backend Protection

**File**: `src/view/LostItemsPanel.java` - `canModifyItem()` method (Line 521)

```java
private boolean canModifyItem(int itemId) {
    UserController userController = UserController.getInstance();
    if (!userController.isLoggedIn()) return false;
    if (userController.isAdmin()) return true;  // ✅ Admins can delete any item
    // Regular users can only delete their own items
    return ItemController.getInstance().isLostItemOwnedBy(
        itemId, 
        userController.getCurrentUser().getId()
    );
}
```

This is **already secure**! It ensures:
1. Even if someone manually clicks delete through a hack, the backend checks role
2. Admins can delete ANY item
3. Regular users can only delete items they reported

---

## Data Flow Diagram

```
LOGIN (User enters credentials)
    ↓
UserController.login()
    ↓
UserDAO.findByUsername() → loads User object with Role
    ↓
UserController.currentUser = user  ← Role stored here!
    ↓
UI Updates:
  - isAdmin() = true? Show delete button
  - isAdmin() = false? Hide delete button
    ↓
DELETE ACTION:
    ↓
canModifyItem() checks:
  - Is admin? YES → allow
  - Is admin? NO → check if owner
```

---

## Code Examples for Common Scenarios

### Example 1: Show/Hide UI Elements by Role

```java
// In any panel, after creating buttons:
UserController uc = UserController.getInstance();

if (uc.isAdmin()) {
    panel.add(deleteButton);        // Only admins see this
    panel.add(editButton);
} else if (uc.isLoggedIn()) {
    // Regular user logged in
    // Don't add delete/edit buttons
}
```

### Example 2: Check Role Before Action

```java
private void deleteSelected() {
    // First check: frontend validation
    if (!UserController.getInstance().isAdmin()) {
        JOptionPane.showMessageDialog(this, "Only admins can delete items");
        return;
    }
    
    int row = table.getSelectedRow();
    if (row < 0) return;
    
    int id = (int) tableModel.getValueAt(row, 0);
    ItemController.getInstance().deleteLostItem(id);  // ✓ Safe
}
```

### Example 3: Get Current User Role

```java
UserController uc = UserController.getInstance();

// Singleton access to current user
User user = uc.getCurrentUser();
String roleDisplay = user.getRole().getDisplayName();  // "Admin" or "User"

// Quick checks
if (uc.isAdmin()) { /* admin-only action */ }
if (uc.isLoggedIn()) { /* any logged-in user */ }
```

### Example 4: Store Role After Login

```java
// In UserController.login():
public boolean login(String username, String password) {
    User user = userDAO.findByUsername(username);
    if (user != null && user.authenticate(password)) {
        this.currentUser = user;  // ✓ Role is stored here via User object
        // currentUser.getRole() is now available
        System.out.println("Logged in as: " + currentUser.getRole().getDisplayName());
        return true;
    }
    return false;
}
```

### Example 5: Disable Button Instead of Hiding

If you prefer to keep the button visible but disabled for regular users:

```java
// Option: Show button but disabled for non-admins
leftActions.add(delBtn);

if (!UserController.getInstance().isAdmin()) {
    delBtn.setEnabled(false);
    delBtn.setToolTipText("Only admins can delete items");
} else {
    delBtn.setEnabled(true);
}
```

---

## Testing the Implementation

### Test Case 1: Admin Login
```
1. Login as admin user
2. Navigate to Lost Items
3. ✓ Verify "Delete Selected" button IS visible
4. Click delete on any item
5. ✓ Verify item is deleted (admin can delete any item)
```

### Test Case 2: Regular User Login
```
1. Login as regular user
2. Navigate to Lost Items
3. ✓ Verify "Delete Selected" button is HIDDEN
4. Try to manually hack/call delete
5. ✓ Verify backend rejects it (canModifyItem() returns false)
```

### Test Case 3: User Logout
```
1. Logout
2. ✓ Verify all admin-only buttons disappear
3. Re-login as different role
4. ✓ Verify UI updates correctly
```

---

## Security Best Practices

1. ✅ **Never Trust Frontend**: Always validate role on backend
   - Frontend UI is just for UX
   - Backend must enforce permissions

2. ✅ **Use Enums for Roles**: Prevents typos and runtime errors
   - `Role.ADMIN` is type-safe
   - Can't accidentally write `Role.ADMINN`

3. ✅ **Store Role in Session**: Keep it in `currentUser`
   - Avoid passing role as parameter (can be spoofed)
   - Use controller methods like `isAdmin()`

4. ✅ **Audit Logging**: Your code already does this!
   - `userDAO.recordLoginEvent()` tracks who did what

5. ✅ **Fail Securely**: Default to denying access
   - `canModifyItem()` returns false by default
   - Only grants access if conditions met

---

## Complete Implementation Checklist

- [ ] Update `LostItemsPanel.java` - change delete button visibility check to `isAdmin()`
- [ ] Update `FoundItemsPanel.java` - change delete button visibility check to `isAdmin()`
- [ ] Test: Login as admin → verify delete button visible
- [ ] Test: Login as user → verify delete button hidden
- [ ] Test: Try to delete as user → verify rejected by backend
- [ ] (Optional) Add role badge/indicator to top bar showing current user's role

---

## FAQ

**Q: Why not just hide the button for non-admins?**
A: That's exactly what we're doing! Hiding the button is cleaner UX than showing a disabled button.

**Q: What if I want to allow certain users to delete their own items?**
A: The `canModifyItem()` method already does this! Non-admins can delete items they reported.

**Q: Can users spoof the role?**
A: No. The role is stored server-side in the database and loaded at login. Users can't modify it without database access.

**Q: How do I make a user admin?**
A: Either:
- Insert directly into database: `UPDATE users SET role='ADMIN' WHERE id=1;`
- Add UI method in AdminPanel to promote users

---

## Summary

| Aspect | Status | Details |
|--------|--------|---------|
| Role Storage | ✅ Done | User.Role enum + UserController.currentUser |
| Backend Check | ✅ Done | canModifyItem() already validates role |
| Frontend Button Hide | ❌ TODO | Change isLoggedIn() to isAdmin() in UI |
| Delete Protection | ✅ Done | Backend rejects unauthorized deletes |
| Audit Log | ✅ Done | recordLoginEvent() tracks all actions |

