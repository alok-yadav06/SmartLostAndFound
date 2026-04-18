package controller;

import dao.UserDAO;
import java.util.List;
import java.util.regex.Pattern;
import model.LoginAuditEntry;
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

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
    );

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
        userDAO.recordFailedLogin(normalized, "Invalid username or password");
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
            validateRegistrationInput(username, password, email, fullName);
            User newUser = new User(username, password, email, fullName, User.Role.USER);
            userDAO.insertUser(newUser, password);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean resetPasswordPlaceholder(String username, String email, String newPassword) {
        String normalizedUser = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUser.isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 chars with upper, lower, number and symbol.");
        }

        boolean ok = userDAO.resetPasswordByUsernameAndEmail(normalizedUser, email, newPassword);
        if (ok) {
            User current = userDAO.findByUsername(normalizedUser);
            userDAO.recordLoginEvent(current, "PASSWORD_RESET", "Password reset via placeholder flow");
        }
        return ok;
    }

    public String getPasswordRulesText() {
        return "Password must be at least 8 characters and include upper/lower letters, a number, and a symbol.";
    }

    private void validateRegistrationInput(String username, String password, String email, String fullName) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!isStrongPassword(password)) {
            throw new IllegalArgumentException(getPasswordRulesText());
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    public List<User> getAllUsers()         { return userDAO.getAllUsers(); }
    public List<LoginAuditEntry> getLoginAuditEntries() { return userDAO.getLoginAuditEntries(); }
    public int        getTotalUsers()       { return userDAO.countUsers(); }

    public void disableUser(int userId) {
        userDAO.setUserActive(userId, false);
    }

    public void enableUser(int userId) {
        userDAO.setUserActive(userId, true);
    }
}