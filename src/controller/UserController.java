package controller;

import dao.UserDAO;
import java.util.List;
import model.User;

/**
 * UserController — handles login, registration, session.
 *
 * CONCEPT: Session Management (simplified)
 * In web apps, sessions are stored server-side and tracked via cookies.
 * In desktop apps, we keep the logged-in user in memory as a static field.
 * This is the "Application Context" pattern.
 *
 * currentUser = null → not logged in
 * currentUser = User → logged in as that user
 */
public class UserController {

    private static UserController instance;
    private final UserDAO userDAO;
    private User currentUser;   // The currently logged-in user (session)

    public static UserController getInstance() {
        if (instance == null) instance = new UserController();
        return instance;
    }

    private UserController() {
        this.userDAO = new UserDAO();
    }

    // ── Authentication ─────────────────────────────────────

    /**
     * Attempts to log in with username + password.
     * Returns true on success, sets currentUser.
     */
    public boolean login(String username, String password) {
        String normalized = username == null ? "" : username.trim().toLowerCase();
        User user = userDAO.findByUsername(normalized);
        if (user != null && user.authenticate(password)) {
            this.currentUser = user;
            userDAO.recordLoginEvent(user, "LOGIN", "Successful login");
            System.out.println("✅ Logged in: " + user);
            return true;
        }
        return false;
    }

    public void logout() {
        if (currentUser != null) {
            userDAO.recordLoginEvent(currentUser, "LOGOUT", "User logged out");
        }
        System.out.println("👋 Logged out: " + currentUser);
        this.currentUser = null;
    }

    public boolean isLoggedIn()  { return currentUser != null; }
    public boolean isAdmin()     { return currentUser != null && currentUser.isAdmin(); }
    public User    getCurrentUser() { return currentUser; }

    // ── User Management (admin only) ───────────────────────

    public boolean register(String username, String password,
                            String email, String fullName) {
        try {
            User newUser = new User(username, password, email, fullName, User.Role.USER);
            userDAO.insertUser(newUser, password);
            return true;
        } catch (RuntimeException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public List<User> getAllUsers()         { return userDAO.getAllUsers(); }
    public int        getTotalUsers()       { return userDAO.countUsers(); }

    public void disableUser(int userId) {
        userDAO.setUserActive(userId, false);
    }

    public void enableUser(int userId) {
        userDAO.setUserActive(userId, true);
    }
}