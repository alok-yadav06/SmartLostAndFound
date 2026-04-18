// src/view/MainFrame.java
package view;

import controller.UserController;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import model.User;
import util.ToastUtil;
import util.UITheme;

/**
 * The main application window.
 * 
 * Architecture: MainFrame is a "shell" — it holds navigation
 * and a content area. Panels are swapped in/out via CardLayout.
 * 
 * Real-world pattern: Single Page Application (SPA) concept,
 * but in Swing. One window, many views.
 */
public class MainFrame extends JFrame {

    private JPanel contentArea;       // Where panels are displayed
    private JPanel contentHost;       // Host wrapper to allow panel reloads
    private CardLayout cardLayout;    // Lets us switch panels by name
    private JPanel activeNavButton;   // Track which nav item is selected
    private JLabel sessionLabel;
    private JLabel roleBadge;
    private JButton sessionButton;
    private DashboardPanel dashboardPanel;
    private LostItemsPanel lostItemsPanel;
    private FoundItemsPanel foundItemsPanel;
    private AddItemPanel addItemPanel;
    private AdminPanel adminPanel;
    
    // Panel name constants — avoids magic strings
    public static final String DASHBOARD   = "DASHBOARD";
    public static final String LOST_ITEMS  = "LOST_ITEMS";
    public static final String FOUND_ITEMS = "FOUND_ITEMS";
    public static final String ADD_ITEM    = "ADD_ITEM";
    public static final String ADMIN       = "ADMIN";

    public MainFrame() {
        initWindow();
        buildUI();
        cardLayout.show(contentArea, DASHBOARD);
    }

    private void initWindow() {
        setTitle("SmartFind — Lost & Found System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null); // Center on screen
        setBackground(UITheme.BG_DARK);

        // Use system look and feel as a base, then override
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Fall back to default look and feel.
        }
    }

    private void buildUI() {
        // Root layout: sidebar on left, content on right
        setLayout(new BorderLayout(0, 0));

        JPanel sidebar = buildSidebar();
        contentArea = buildContentArea();
        JPanel mainArea = buildMainArea();

        add(sidebar, BorderLayout.WEST);
        add(mainArea, BorderLayout.CENTER);
    }

    private JPanel buildMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout(0, 0));
        mainArea.setBackground(UITheme.BG_MAIN);

        mainArea.add(buildTopBar(), BorderLayout.NORTH);

        contentHost = new JPanel(new BorderLayout());
        contentHost.setOpaque(false);
        contentHost.add(contentArea, BorderLayout.CENTER);
        mainArea.add(contentHost, BorderLayout.CENTER);

        return mainArea;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBackground(UITheme.BG_CARD);
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JLabel title = new JLabel("SmartFind Campus Portal");
        title.setFont(UITheme.FONT_SUBHEAD);
        title.setForeground(UITheme.TEXT_SECONDARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        sessionLabel = new JLabel();
        sessionLabel.setFont(UITheme.FONT_BODY);
        sessionLabel.setForeground(UITheme.TEXT_PRIMARY);

        roleBadge = new JLabel("GUEST");
        roleBadge.setFont(UITheme.FONT_SMALL);
        roleBadge.setOpaque(true);
        roleBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        sessionButton = UITheme.ghostButton("Sign In");
        sessionButton.setPreferredSize(new Dimension(100, 32));
        sessionButton.addActionListener(e -> handleSessionButton());

        right.add(sessionLabel);
        right.add(roleBadge);
        right.add(sessionButton);

        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        refreshSessionUI();
        return top;
    }

    // ── Sidebar ──────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER));

        // Logo area
        sidebar.add(buildLogoPanel());
        sidebar.add(Box.createVerticalStrut(20));

        // Nav items
        sidebar.add(buildNavItem("🏠", "Dashboard",  DASHBOARD,  true));
        sidebar.add(buildNavItem("🔍", "Lost Items",  LOST_ITEMS, false));
        sidebar.add(buildNavItem("📦", "Found Items", FOUND_ITEMS,false));
        sidebar.add(buildNavItem("➕", "Add Item",    ADD_ITEM,   false));

        sidebar.add(Box.createVerticalGlue()); // Pushes admin to bottom

        sidebar.add(buildNavItem("⚙️", "Admin Panel", ADMIN, false));
        sidebar.add(Box.createVerticalStrut(20));

        return sidebar;
    }

    private JPanel buildLogoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        panel.setBackground(UITheme.BG_SIDEBAR);
        panel.setMaximumSize(new Dimension(220, 80));

        JLabel icon = new JLabel("📍");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("SmartFind");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_WHITE);

        panel.add(icon);
        panel.add(title);
        return panel;
    }

    /**
     * Builds a single nav item.
     * 
     * Notice: We use JPanel not JButton here.
     * Why? Full control over styling — hover, active state, rounded corners.
     * JButton fights us. JPanel cooperates.
     */
    private JPanel buildNavItem(String emoji, String label, String panelName, boolean isActive) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        item.setBackground(isActive ? UITheme.PRIMARY : UITheme.BG_SIDEBAR);
        item.setMaximumSize(new Dimension(220, 50));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel textLabel = new JLabel(label);
        textLabel.setFont(UITheme.FONT_BODY);
        textLabel.setForeground(isActive ? UITheme.TEXT_WHITE : UITheme.TEXT_LIGHT);

        item.add(emojiLabel);
        item.add(textLabel);

        // Attach the same listener to panel and child labels so clicks always work.
        MouseAdapter navListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (activeNavButton != item) {
                    item.setBackground(new Color(30, 41, 59));
                    textLabel.setForeground(UITheme.TEXT_WHITE);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (activeNavButton != item) {
                    item.setBackground(UITheme.BG_SIDEBAR);
                    textLabel.setForeground(UITheme.TEXT_LIGHT);
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                setActiveNav(item, textLabel);
                showPanel(panelName);
            }
        };

        item.addMouseListener(navListener);
        emojiLabel.addMouseListener(navListener);
        textLabel.addMouseListener(navListener);

        if (isActive) activeNavButton = item;
        return item;
    }

    private void setActiveNav(JPanel clicked, JLabel label) {
        // Reset previous active
        if (activeNavButton != null) {
            activeNavButton.setBackground(UITheme.BG_SIDEBAR);
            // Reset all labels on previous item.
            for (Component c : activeNavButton.getComponents()) {
                if (c instanceof JLabel lbl) {
                    lbl.setForeground(UITheme.TEXT_LIGHT);
                }
            }
        }
        clicked.setBackground(UITheme.PRIMARY);
        label.setForeground(UITheme.TEXT_WHITE);
        for (Component c : clicked.getComponents()) {
            if (c instanceof JLabel lbl) {
                lbl.setForeground(UITheme.TEXT_WHITE);
            }
        }
        activeNavButton = clicked;
    }

    // ── Content Area ─────────────────────────────────────────────

// In MainFrame.java — replace buildContentArea() method:
private JPanel buildContentArea() {
    JPanel area = new JPanel();
    cardLayout = new CardLayout();
    area.setLayout(cardLayout);
    area.setBackground(UITheme.BG_DARK);

    // ✅ Now using REAL panels instead of placeholders
    dashboardPanel = new DashboardPanel();
    lostItemsPanel = new LostItemsPanel();
    foundItemsPanel = new FoundItemsPanel();
    addItemPanel = new AddItemPanel();
    adminPanel = new AdminPanel();

    area.add(dashboardPanel,  DASHBOARD);
    area.add(lostItemsPanel,  LOST_ITEMS);
    area.add(foundItemsPanel, FOUND_ITEMS);
    area.add(addItemPanel,    ADD_ITEM);
    area.add(adminPanel,      ADMIN);

    return area;
}

    public void showPanel(String name) {
        if (ADMIN.equals(name) && !UserController.getInstance().isAdmin()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Admin access requires admin login. Sign in now?",
                "Admin Access Required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                openLoginDialog(false);
            }
            if (!UserController.getInstance().isAdmin()) {
                return;
            }
            reloadContentPanels();
        }
        cardLayout.show(contentArea, name);
    }

    public void promptLoginAtStartup() {
        if (UserController.getInstance().isLoggedIn()) {
            refreshSessionUI();
            return;
        }
        openLoginDialog(true);
        refreshSessionUI();
        reloadContentPanels();
    }

    private void handleSessionButton() {
        if (UserController.getInstance().isLoggedIn()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Log out from current session?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                UserController.getInstance().logout();
                refreshSessionUI();
                reloadContentPanels();
                showPanel(DASHBOARD);
                ToastUtil.showInfo(this, "Logged out successfully");
            }
            return;
        }

        openLoginDialog(false);
        refreshSessionUI();
        reloadContentPanels();
        showPanel(DASHBOARD);
        if (UserController.getInstance().isLoggedIn()) {
            ToastUtil.showSuccess(this, "Login successful");
        }
    }

    private void openLoginDialog(boolean allowGuest) {
        JDialog dialog = new JDialog(this, "Sign In", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        LoginPanel loginPanel = new LoginPanel(dialog::dispose);
        dialog.add(loginPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.setBackground(UITheme.BG_CARD);

        if (allowGuest) {
            JButton guestBtn = UITheme.ghostButton("Continue as Guest");
            guestBtn.setPreferredSize(new Dimension(150, 32));
            guestBtn.addActionListener(e -> dialog.dispose());
            footer.add(guestBtn);
        }

        JButton closeBtn = UITheme.ghostButton("Close");
        closeBtn.setPreferredSize(new Dimension(90, 32));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);

        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void refreshSessionUI() {
        if (sessionLabel == null || sessionButton == null || roleBadge == null) return;

        User current = UserController.getInstance().getCurrentUser();
        if (current == null) {
            sessionLabel.setText("Guest Mode");
            sessionButton.setText("Sign In");
            roleBadge.setText("GUEST");
            roleBadge.setBackground(new Color(226, 232, 240));
            roleBadge.setForeground(new Color(30, 41, 59));
        } else {
            sessionLabel.setText(current.getFullName() + " (" + current.getRole().name() + ")");
            sessionButton.setText("Logout");
            boolean admin = current.isAdmin();
            roleBadge.setText(admin ? "ADMIN" : "USER");
            roleBadge.setBackground(admin ? new Color(220, 38, 38) : new Color(37, 99, 235));
            roleBadge.setForeground(Color.WHITE);
        }
    }

    private void reloadContentPanels() {
        contentHost.removeAll();
        contentArea = buildContentArea();
        contentHost.add(contentArea, BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
    }

    public void refreshAfterClaimDecision() {
        if (foundItemsPanel != null) foundItemsPanel.refresh();
        if (dashboardPanel != null) dashboardPanel.refresh();
    }

    // ── Entry Point ───────────────────────────────────────────────

    public static void main(String[] args) {
        // Always create Swing UIs on the Event Dispatch Thread (EDT)
        // This is thread safety in Swing — critical to understand
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}