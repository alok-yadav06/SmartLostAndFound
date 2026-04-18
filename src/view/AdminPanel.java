package view;

import controller.ClaimController;
import controller.ItemController;
import controller.UserController;
import dao.UserDAO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import model.ClaimRequest;
import model.LoginAuditEntry;
import model.User;
import util.ToastUtil;
import util.UITheme;

/**
 * AdminPanel — restricted to ADMIN role users.
 *
 * CONCEPT: Role-Based Access Control (RBAC)
 * Different UI/actions shown based on user role.
 * In production: backend also validates permissions (never trust UI alone).
 *
 * CONCEPT: JTabbedPane
 * Organizes related content into tabs — like a browser.
 * Each tab is a separate JPanel, shown/hidden by the tabbed pane.
 *
 * CONCEPT: TableModel listener
 * We listen for selection changes with ListSelectionListener
 * to enable/disable buttons only when a row is selected.
 */
public class AdminPanel extends JPanel {

    private final UserDAO userDAO = new UserDAO();

    private DefaultTableModel claimTableModel;
    private DefaultTableModel claimHistoryTableModel;
    private DefaultTableModel userTableModel;
    private JTable claimTable;
    private JTable claimHistoryTable;
    private JTable userTable;
    private JTable auditTable;
    private final List<ClaimRequest> pendingClaims = new ArrayList<>();
    private final List<ClaimRequest> filteredPendingClaims = new ArrayList<>();
    private final List<ClaimRequest> historyClaims = new ArrayList<>();
    private final List<ClaimRequest> filteredHistoryClaims = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> visibleUsers = new ArrayList<>();
    private final List<LoginAuditEntry> allAuditEntries = new ArrayList<>();
    private final List<LoginAuditEntry> filteredAuditEntries = new ArrayList<>();
    private int pendingPage = 1;
    private int historyPage = 1;
    private int userPage = 1;
    private int auditPage = 1;
    private static final int PAGE_SIZE = 10;
    private JLabel pendingPageLabel;
    private JLabel historyPageLabel;
    private JLabel userPageLabel;
    private JLabel auditPageLabel;
    private JComboBox<String> historyFilter;
    private JTextField pendingSearchField;
    private JTextField userSearchField;
    private JTextField auditUserFilterField;
    private JTextField auditFromDateField;
    private JTextField auditToDateField;
    private DefaultTableModel auditTableModel;

    public AdminPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Only admins can see this panel — but add guard anyway
        if (!UserController.getInstance().isAdmin()) {
            add(buildAccessDenied(), BorderLayout.CENTER);
            return;
        }

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTabs(),    BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("🛡 Admin Panel");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Manage claims, users, and system settings");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel left = new JPanel(new GridLayout(2,1,0,4));
        left.setOpaque(false);
        left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton refreshBtn = UITheme.ghostButton("⟳ Refresh All");
        JButton backupBtn = UITheme.ghostButton("Backup DB");
        JButton exportAllBtn = UITheme.primaryButton("Export All Reports");
        refreshBtn.addActionListener(e -> refresh());
        backupBtn.addActionListener(e -> backupDatabase());
        exportAllBtn.addActionListener(e -> exportAllReports());
        right.add(refreshBtn);
        right.add(backupBtn);
        right.add(exportAllBtn);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_SUBHEAD);
        tabs.setBackground(UITheme.BG_MAIN);

        tabs.addTab("📝 Pending Claims",  buildClaimsTab());
        tabs.addTab("📚 Claim History",   buildClaimHistoryTab());
        tabs.addTab("👥 User Management", buildUsersTab());
        tabs.addTab("🔐 Login Audit",     buildLoginAuditTab());
        tabs.addTab("🗑 Item Management", buildItemMgmtTab());

        return tabs;
    }

    // ── Claims Tab ─────────────────────────────────────────

    private JPanel buildClaimsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Table
        String[] cols = {"Claim ID", "Item ID", "User ID", "Reason", "Proof", "Status", "Admin Note"};
        claimTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        claimTable = new JTable(claimTableModel);
        claimTable.setRowHeight(36);
        claimTable.setFont(UITheme.FONT_BODY);
        claimTable.setGridColor(UITheme.BORDER_COLOR);
        claimTable.getTableHeader().setFont(UITheme.FONT_SUBHEAD);
        claimTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Color-code status column (col 5)
        claimTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String s = String.valueOf(val);
                lbl.setForeground(switch (s) {
                    case "APPROVED" -> UITheme.SUCCESS;
                    case "REJECTED" -> UITheme.DANGER;
                    default         -> UITheme.WARNING;
                });
                lbl.setFont(UITheme.FONT_SUBHEAD);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(claimTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        pendingSearchField = UITheme.styledField("Search claim id/item id/user id/reason");
        pendingSearchField.setPreferredSize(new Dimension(320, 34));
        JButton applySearchBtn = UITheme.ghostButton("Apply");
        JButton clearSearchBtn = UITheme.ghostButton("Clear");
        applySearchBtn.addActionListener(e -> {
            pendingPage = 1;
            applyPendingClaimFilter();
        });
        clearSearchBtn.addActionListener(e -> {
            pendingSearchField.setText("");
            pendingPage = 1;
            applyPendingClaimFilter();
        });
        pendingSearchField.addActionListener(e -> {
            pendingPage = 1;
            applyPendingClaimFilter();
        });
        filters.add(new JLabel("Filter:"));
        filters.add(pendingSearchField);
        filters.add(applySearchBtn);
        filters.add(clearSearchBtn);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(filters, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);

        JButton approveBtn = UITheme.successButton("✅ Approve Claim");
        JButton rejectBtn  = UITheme.dangerButton("❌ Reject Claim");
        JButton exportBtn  = UITheme.ghostButton("Export CSV");
        pendingPageLabel = new JLabel("Page 1/1");
        pendingPageLabel.setFont(UITheme.FONT_BODY);
        pendingPageLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton prevBtn = UITheme.ghostButton("Previous");
        JButton nextBtn = UITheme.ghostButton("Next");

        approveBtn.addActionListener(e -> handleClaim(true));
        rejectBtn.addActionListener(e  -> handleClaim(false));
        exportBtn.addActionListener(e -> exportTableToCsv(claimTableModel, "pending-claims.csv"));
        prevBtn.addActionListener(e -> {
            if (pendingPage > 1) {
                pendingPage--;
                refreshPendingClaimsPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (pendingPage < totalPages(filteredPendingClaims.size())) {
                pendingPage++;
                refreshPendingClaimsPage();
            }
        });

        actions.add(approveBtn);
        actions.add(rejectBtn);
        actions.add(exportBtn);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(pendingPageLabel);
        actions.add(prevBtn);
        actions.add(nextBtn);
        panel.add(actions, BorderLayout.SOUTH);

        loadClaims();
        return panel;
    }

    private JPanel buildClaimHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        String[] cols = {"Claim ID", "Item ID", "User ID", "Reason", "Proof", "Status", "Admin Note"};
        claimHistoryTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        claimHistoryTable = new JTable(claimHistoryTableModel);
        claimHistoryTable.setRowHeight(36);
        claimHistoryTable.setFont(UITheme.FONT_BODY);
        claimHistoryTable.setGridColor(UITheme.BORDER_COLOR);
        claimHistoryTable.getTableHeader().setFont(UITheme.FONT_SUBHEAD);

        JScrollPane scroll = new JScrollPane(claimHistoryTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);
        historyFilter = UITheme.styledCombo(new String[]{"All", "APPROVED", "REJECTED", "CLOSED"});
        historyFilter.setPreferredSize(new Dimension(140, 32));
        JButton exportBtn  = UITheme.ghostButton("Export CSV");
        historyPageLabel = new JLabel("Page 1/1");
        historyPageLabel.setFont(UITheme.FONT_BODY);
        historyPageLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton prevBtn = UITheme.ghostButton("Previous");
        JButton nextBtn = UITheme.ghostButton("Next");

        historyFilter.addActionListener(e -> {
            historyPage = 1;
            refreshHistoryClaimsPage();
        });
        exportBtn.addActionListener(e -> exportTableToCsv(claimHistoryTableModel, "claim-history.csv"));
        prevBtn.addActionListener(e -> {
            if (historyPage > 1) {
                historyPage--;
                refreshHistoryClaimsPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (historyPage < totalPages(filteredHistoryClaims.size())) {
                historyPage++;
                refreshHistoryClaimsPage();
            }
        });

        actions.add(new JLabel("Status:"));
        actions.add(historyFilter);
        actions.add(exportBtn);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(historyPageLabel);
        actions.add(prevBtn);
        actions.add(nextBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void loadClaims() {
        List<ClaimRequest> claims = ClaimController.getInstance().getAllClaims();
        pendingClaims.clear();
        historyClaims.clear();
        for (ClaimRequest c : claims) {
            if (c.getStatus() == ClaimRequest.ClaimStatus.PENDING) pendingClaims.add(c);
            else historyClaims.add(c);
        }
        pendingPage = 1;
        historyPage = 1;
        applyPendingClaimFilter();
        refreshPendingClaimsPage();
        refreshHistoryClaimsPage();
    }

    private void applyPendingClaimFilter() {
        filteredPendingClaims.clear();
        String q = pendingSearchField == null ? "" : pendingSearchField.getText().trim().toLowerCase();
        for (ClaimRequest c : pendingClaims) {
            if (q.isEmpty()
                    || String.valueOf(c.getId()).contains(q)
                    || String.valueOf(c.getItemId()).contains(q)
                    || String.valueOf(c.getUserId()).contains(q)
                    || (c.getClaimReason() != null && c.getClaimReason().toLowerCase().contains(q))) {
                filteredPendingClaims.add(c);
            }
        }
    }

    private void handleClaim(boolean approve) {
        int row = claimTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a claim first."); return; }

        int claimId = (int) claimTableModel.getValueAt(row, 0);
        int itemId  = (int) claimTableModel.getValueAt(row, 1);
        String status = (String) claimTableModel.getValueAt(row, 5);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this, "This claim is already " + status + ".");
            return;
        }

        String note = JOptionPane.showInputDialog(this,
            approve ? "Enter approval note (optional):" : "Enter rejection reason:",
            approve ? "Approval Note" : "Rejection Reason",
            JOptionPane.PLAIN_MESSAGE);
        if (note == null) return; // Cancelled

        if (approve) {
            ClaimController.getInstance().approveClaim(claimId, itemId, note);
            ToastUtil.showSuccess(this, "Claim approved and item marked as claimed.");
        } else {
            ClaimController.getInstance().rejectClaim(claimId, note);
            ToastUtil.showInfo(this, "Claim rejected.");
        }
        loadClaims();

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner instanceof MainFrame frame) {
            frame.refreshAfterClaimDecision();
        }
    }

    // ── Users Tab ──────────────────────────────────────────

    private JPanel buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        String[] cols = {"ID", "Username", "Full Name", "Email", "Role", "Active"};
        userTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userTableModel);
        userTable.setRowHeight(34);
        userTable.setFont(UITheme.FONT_BODY);
        userTable.setGridColor(UITheme.BORDER_COLOR);
        userTable.getTableHeader().setFont(UITheme.FONT_SUBHEAD);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        userSearchField = UITheme.styledField("Search username/full name/email/role");
        userSearchField.setPreferredSize(new Dimension(320, 34));
        JButton userApplyBtn = UITheme.ghostButton("Apply");
        JButton userClearBtn = UITheme.ghostButton("Clear");
        userApplyBtn.addActionListener(e -> {
            userPage = 1;
            applyUserFilter();
        });
        userClearBtn.addActionListener(e -> {
            userSearchField.setText("");
            userPage = 1;
            applyUserFilter();
        });
        userSearchField.addActionListener(e -> {
            userPage = 1;
            applyUserFilter();
        });
        filters.add(new JLabel("Filter:"));
        filters.add(userSearchField);
        filters.add(userApplyBtn);
        filters.add(userClearBtn);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(filters, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);

        JButton disableBtn = UITheme.dangerButton("🚫 Disable User");
        JButton enableBtn  = UITheme.successButton("✅ Enable User");
        JButton exportBtn  = UITheme.ghostButton("Export CSV");
        userPageLabel = new JLabel("Page 1/1");
        userPageLabel.setFont(UITheme.FONT_BODY);
        userPageLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton prevBtn = UITheme.ghostButton("Previous");
        JButton nextBtn = UITheme.ghostButton("Next");

        disableBtn.addActionListener(e -> setUserActive(false));
        enableBtn.addActionListener(e  -> setUserActive(true));
        exportBtn.addActionListener(e -> exportTableToCsv(userTableModel, "users.csv"));
        prevBtn.addActionListener(e -> {
            if (userPage > 1) {
                userPage--;
                refreshUsersPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (userPage < totalPages(visibleUsers.size())) {
                userPage++;
                refreshUsersPage();
            }
        });

        actions.add(enableBtn);
        actions.add(disableBtn);
        actions.add(exportBtn);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(userPageLabel);
        actions.add(prevBtn);
        actions.add(nextBtn);
        panel.add(actions, BorderLayout.SOUTH);

        loadUsers();
        return panel;
    }

    private void loadUsers() {
        List<User> users = UserController.getInstance().getAllUsers();
        allUsers.clear();
        allUsers.addAll(users);
        visibleUsers.clear();
        visibleUsers.addAll(users);
        userPage = 1;
        applyUserFilter();
        refreshUsersPage();
    }

    private void applyUserFilter() {
        visibleUsers.clear();
        String q = userSearchField == null ? "" : userSearchField.getText().trim().toLowerCase();
        for (User u : allUsers) {
            if (q.isEmpty()
                    || String.valueOf(u.getId()).contains(q)
                    || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                    || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    || u.getRole().name().toLowerCase().contains(q)) {
                visibleUsers.add(u);
            }
        }
    }

    private void loadAuditEntries() {
        allAuditEntries.clear();
        allAuditEntries.addAll(userDAO.getLoginAuditEntries());
        auditPage = 1;
        applyAuditFilter();
        refreshAuditPage();
    }

    private void applyAuditFilter() {
        filteredAuditEntries.clear();

        String userQ = auditUserFilterField == null ? "" : auditUserFilterField.getText().trim().toLowerCase();
        LocalDate fromDate = parseDateOrNull(auditFromDateField == null ? "" : auditFromDateField.getText().trim());
        LocalDate toDate = parseDateOrNull(auditToDateField == null ? "" : auditToDateField.getText().trim());

        for (LoginAuditEntry e : allAuditEntries) {
            String username = e.getUsername() == null ? "" : e.getUsername().toLowerCase();
            boolean userMatch = userQ.isEmpty() || username.contains(userQ);
            boolean dateMatch = isDateInRange(e.getEventTime(), fromDate, toDate);
            if (userMatch && dateMatch) {
                filteredAuditEntries.add(e);
            }
        }
    }

    private void refreshAuditPage() {
        if (auditTableModel == null) return;
        auditTableModel.setRowCount(0);
        int start = (auditPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredAuditEntries.size());

        for (int i = start; i < end; i++) {
            LoginAuditEntry e = filteredAuditEntries.get(i);
            auditTableModel.addRow(new Object[]{
                e.getId(),
                e.getUserId() == null ? "—" : e.getUserId(),
                e.getUsername() == null || e.getUsername().isBlank() ? "—" : e.getUsername(),
                e.getEventType(),
                e.getEventTime(),
                e.getNotes() == null ? "" : e.getNotes()
            });
        }

        if (auditPageLabel != null) {
            auditPageLabel.setText("Page " + auditPage + "/" + totalPages(filteredAuditEntries.size()));
        }
    }

    private LocalDate parseDateOrNull(String dateText) {
        if (dateText == null || dateText.isBlank()) return null;
        try {
            return LocalDate.parse(dateText);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private boolean isDateInRange(String eventTime, LocalDate fromDate, LocalDate toDate) {
        if (eventTime == null || eventTime.length() < 10) return true;
        try {
            LocalDate eventDate = LocalDate.parse(eventTime.substring(0, 10));
            if (fromDate != null && eventDate.isBefore(fromDate)) return false;
            if (toDate != null && eventDate.isAfter(toDate)) return false;
            return true;
        } catch (DateTimeParseException ignored) {
            return true;
        }
    }

    private void setUserActive(boolean active) {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        int userId = (int) userTableModel.getValueAt(row, 0);
        String username = (String) userTableModel.getValueAt(row, 1);
        if ("admin".equals(username)) { JOptionPane.showMessageDialog(this, "Cannot disable admin!"); return; }
        if (active) UserController.getInstance().enableUser(userId);
        else        UserController.getInstance().disableUser(userId);
        ToastUtil.showInfo(this, active ? "User enabled." : "User disabled.");
        loadUsers();
    }

    // ── Login Audit Tab ───────────────────────────────────

    private JPanel buildLoginAuditTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        auditUserFilterField = UITheme.styledField("Username contains...");
        auditUserFilterField.setPreferredSize(new Dimension(180, 34));
        auditFromDateField = UITheme.styledField("From (yyyy-mm-dd)");
        auditFromDateField.setPreferredSize(new Dimension(150, 34));
        auditToDateField = UITheme.styledField("To (yyyy-mm-dd)");
        auditToDateField.setPreferredSize(new Dimension(150, 34));

        JButton applyBtn = UITheme.ghostButton("Apply");
        JButton clearBtn = UITheme.ghostButton("Clear");
        applyBtn.addActionListener(e -> {
            auditPage = 1;
            applyAuditFilter();
        });
        clearBtn.addActionListener(e -> {
            auditUserFilterField.setText("");
            auditFromDateField.setText("");
            auditToDateField.setText("");
            auditPage = 1;
            applyAuditFilter();
        });

        filters.add(new JLabel("User:"));
        filters.add(auditUserFilterField);
        filters.add(new JLabel("From:"));
        filters.add(auditFromDateField);
        filters.add(new JLabel("To:"));
        filters.add(auditToDateField);
        filters.add(applyBtn);
        filters.add(clearBtn);

        String[] cols = {"ID", "User ID", "Username", "Event", "Time", "Notes"};
        auditTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditTableModel);
        auditTable.setRowHeight(34);
        auditTable.setFont(UITheme.FONT_BODY);
        auditTable.setGridColor(UITheme.BORDER_COLOR);
        auditTable.getTableHeader().setFont(UITheme.FONT_SUBHEAD);

        JScrollPane scroll = new JScrollPane(auditTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);
        JButton exportBtn = UITheme.ghostButton("Export CSV");
        exportBtn.addActionListener(e -> exportTableToCsv(auditTableModel, "login-audit.csv"));

        auditPageLabel = new JLabel("Page 1/1");
        auditPageLabel.setFont(UITheme.FONT_BODY);
        auditPageLabel.setForeground(UITheme.TEXT_SECONDARY);
        JButton prevBtn = UITheme.ghostButton("Previous");
        JButton nextBtn = UITheme.ghostButton("Next");
        prevBtn.addActionListener(e -> {
            if (auditPage > 1) {
                auditPage--;
                refreshAuditPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (auditPage < totalPages(filteredAuditEntries.size())) {
                auditPage++;
                refreshAuditPage();
            }
        });

        actions.add(exportBtn);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(auditPageLabel);
        actions.add(prevBtn);
        actions.add(nextBtn);

        panel.add(filters, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        loadAuditEntries();
        return panel;
    }

    // ── Item Management Tab ────────────────────────────────

    private JPanel buildItemMgmtTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);

        ItemController ic = ItemController.getInstance();
        addStatCard(cards, "📦 Total Items", String.valueOf(ic.getTotalItems()), UITheme.PRIMARY);
        addStatCard(cards, "😢 Lost Items",  String.valueOf(ic.getLostCount()),  UITheme.DANGER);
        addStatCard(cards, "😊 Found Items", String.valueOf(ic.getFoundCount()), UITheme.SUCCESS);

        panel.add(cards, BorderLayout.NORTH);

        JLabel note = new JLabel(
            "<html><center>Use the Lost/Found tabs to delete individual items.<br>" +
            "Only admins can delete items — regular users only see the View button.</center></html>",
            SwingConstants.CENTER
        );
        note.setFont(UITheme.FONT_BODY);
        note.setForeground(UITheme.TEXT_SECONDARY);
        note.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        panel.add(note, BorderLayout.CENTER);

        return panel;
    }

    private void addStatCard(JPanel parent, String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(0, 100));

        JLabel tLabel = new JLabel(title); tLabel.setFont(UITheme.FONT_SMALL); tLabel.setForeground(UITheme.TEXT_SECONDARY);
        JLabel vLabel = new JLabel(value); vLabel.setFont(new Font("Segoe UI", Font.BOLD, 30)); vLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel inner = new JPanel(new GridLayout(2,1,0,4)); inner.setOpaque(false);
        inner.add(tLabel); inner.add(vLabel);
        card.add(inner, BorderLayout.CENTER);
        parent.add(card);
    }

    private JPanel buildAccessDenied() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JLabel msg = new JLabel("🔒 Admin Access Required", SwingConstants.CENTER);
        msg.setFont(UITheme.FONT_TITLE);
        msg.setForeground(UITheme.DANGER);
        p.add(msg);
        return p;
    }

    public void refresh() {
        if (claimTableModel != null) loadClaims();
        if (userTableModel  != null) loadUsers();
        if (auditTableModel != null) loadAuditEntries();
        ToastUtil.showInfo(this, "Admin data refreshed");
    }

    private void refreshPendingClaimsPage() {
        claimTableModel.setRowCount(0);
        int start = (pendingPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredPendingClaims.size());
        for (int i = start; i < end; i++) {
            ClaimRequest c = filteredPendingClaims.get(i);
            claimTableModel.addRow(new Object[]{
                c.getId(), c.getItemId(), c.getUserId(),
                truncate(c.getClaimReason(), 30),
                truncate(c.getProofDetails(), 25),
                c.getStatus().name(),
                c.getAdminNote() != null ? c.getAdminNote() : "—"
            });
        }
        if (pendingPageLabel != null) {
            pendingPageLabel.setText("Page " + pendingPage + "/" + totalPages(filteredPendingClaims.size()));
        }
    }

    private void refreshHistoryClaimsPage() {
        if (claimHistoryTableModel == null) return;
        filteredHistoryClaims.clear();
        String selected = historyFilter != null ? (String) historyFilter.getSelectedItem() : "All";
        for (ClaimRequest c : historyClaims) {
            if (selected == null || "All".equals(selected) || c.getStatus().name().equals(selected)) {
                filteredHistoryClaims.add(c);
            }
        }

        claimHistoryTableModel.setRowCount(0);
        int start = (historyPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredHistoryClaims.size());
        for (int i = start; i < end; i++) {
            ClaimRequest c = filteredHistoryClaims.get(i);
            claimHistoryTableModel.addRow(new Object[]{
                c.getId(), c.getItemId(), c.getUserId(),
                truncate(c.getClaimReason(), 30),
                truncate(c.getProofDetails(), 25),
                c.getStatus().name(),
                c.getAdminNote() != null ? c.getAdminNote() : "—"
            });
        }
        if (historyPageLabel != null) {
            historyPageLabel.setText("Page " + historyPage + "/" + totalPages(filteredHistoryClaims.size()));
        }
    }

    private void refreshUsersPage() {
        userTableModel.setRowCount(0);
        int start = (userPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, visibleUsers.size());
        for (int i = start; i < end; i++) {
            User u = visibleUsers.get(i);
            userTableModel.addRow(new Object[]{
                u.getId(), u.getUsername(), u.getFullName(),
                u.getEmail(), u.getRole().name(),
                u.isActive() ? "✅ Yes" : "🚫 No"
            });
        }
        if (userPageLabel != null) {
            userPageLabel.setText("Page " + userPage + "/" + totalPages(visibleUsers.size()));
        }
    }

    private int totalPages(int totalItems) {
        return Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
    }

    private void exportTableToCsv(DefaultTableModel model, String defaultFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName));
        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                writer.print(escapeCsv(String.valueOf(model.getColumnName(c))));
                writer.print(c == model.getColumnCount() - 1 ? "\n" : ",");
            }
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object value = model.getValueAt(r, c);
                    writer.print(escapeCsv(value == null ? "" : String.valueOf(value)));
                    writer.print(c == model.getColumnCount() - 1 ? "\n" : ",");
                }
            }
            ToastUtil.showSuccess(this, "CSV exported: " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to export CSV: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void exportAllReports() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select folder to export all reports");
        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) return;

        File folder = chooser.getSelectedFile();
        if (folder == null) return;

        try {
            exportClaimsListToCsv(new File(folder, "pending-claims.csv"), pendingClaims);
            exportClaimsListToCsv(new File(folder, "claim-history.csv"), historyClaims);
            exportUsersListToCsv(new File(folder, "users.csv"), allUsers);
            exportAuditListToCsv(new File(folder, "login-audit.csv"), filteredAuditEntries);
            ToastUtil.showSuccess(this, "All reports exported to: " + folder.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to export all reports: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportClaimsListToCsv(File file, List<ClaimRequest> claims) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("\"Claim ID\",\"Item ID\",\"User ID\",\"Reason\",\"Proof\",\"Status\",\"Admin Note\"");
            for (ClaimRequest c : claims) {
                writer.println(String.join(",",
                    escapeCsv(String.valueOf(c.getId())),
                    escapeCsv(String.valueOf(c.getItemId())),
                    escapeCsv(String.valueOf(c.getUserId())),
                    escapeCsv(c.getClaimReason()),
                    escapeCsv(c.getProofDetails()),
                    escapeCsv(c.getStatus().name()),
                    escapeCsv(c.getAdminNote() == null ? "" : c.getAdminNote())
                ));
            }
        }
    }

    private void exportUsersListToCsv(File file, List<User> users) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("\"ID\",\"Username\",\"Full Name\",\"Email\",\"Role\",\"Active\"");
            for (User u : users) {
                writer.println(String.join(",",
                    escapeCsv(String.valueOf(u.getId())),
                    escapeCsv(u.getUsername()),
                    escapeCsv(u.getFullName()),
                    escapeCsv(u.getEmail()),
                    escapeCsv(u.getRole().name()),
                    escapeCsv(u.isActive() ? "Yes" : "No")
                ));
            }
        }
    }

    private void exportAuditListToCsv(File file, List<LoginAuditEntry> entries) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("\"ID\",\"User ID\",\"Username\",\"Event\",\"Time\",\"Notes\"");
            for (LoginAuditEntry e : entries) {
                writer.println(String.join(",",
                    escapeCsv(String.valueOf(e.getId())),
                    escapeCsv(e.getUserId() == null ? "" : String.valueOf(e.getUserId())),
                    escapeCsv(e.getUsername() == null ? "" : e.getUsername()),
                    escapeCsv(e.getEventType() == null ? "" : e.getEventType()),
                    escapeCsv(e.getEventTime() == null ? "" : e.getEventTime()),
                    escapeCsv(e.getNotes() == null ? "" : e.getNotes())
                ));
            }
        }
    }

    private void backupDatabase() {
        try {
            Path source = Path.of("database", "lostfound.db");
            if (!Files.exists(source)) {
                JOptionPane.showMessageDialog(this, "Database file not found: " + source, "Backup", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Path backupDir = Path.of("database", "backups");
            Files.createDirectories(backupDir);
            String ts = java.time.LocalDateTime.now().toString().replace(':', '-').replace('.', '-');
            Path target = backupDir.resolve("lostfound-backup-" + ts + ".db");

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            ToastUtil.showSuccess(this, "Database backup created: " + target.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Backup failed: " + ex.getMessage(),
                "Backup Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}