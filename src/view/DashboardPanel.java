package view;

import controller.ClaimController;
import controller.ItemController;
import controller.UserController;
import java.awt.*;
import javax.swing.*;
import util.UITheme;

/**
 * DashboardPanel — shows analytics cards + recent activity.
 *
 * CONCEPT: Custom painting with paintComponent()
 * For gradient backgrounds and rounded cards, we override paintComponent()
 * in JPanel. This is the foundation of all custom Swing graphics.
 *
 * CONCEPT: GridLayout vs GridBagLayout vs BorderLayout
 * - BorderLayout: 5 regions (N,S,E,W,Center) — used for outer frame
 * - GridLayout(rows,cols): equal-size grid — used for stat cards
 * - GridBagLayout: fine-grained control — used for forms
 * Choose based on what you're building. Mix them by nesting panels.
 */
public class DashboardPanel extends JPanel {

    private JLabel totalLabel, lostLabel, foundLabel, claimedLabel, pendingLabel;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildContent(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        UserController uc = UserController.getInstance();
        String greeting = "Welcome back, " +
            (uc.getCurrentUser() != null ? uc.getCurrentUser().getFullName() : "User") + "!";
        JLabel sub = new JLabel(greeting);
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(sub);

        JButton refreshBtn = UITheme.ghostButton("Refresh");
        refreshBtn.setPreferredSize(new Dimension(96, 32));
        refreshBtn.addActionListener(e -> refresh());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);

        content.add(buildStatCards(), BorderLayout.NORTH);
        content.add(buildInfoRow(),   BorderLayout.CENTER);

        return content;
    }

    private JPanel buildStatCards() {
        JPanel cards = new JPanel(new GridLayout(1, 5, 16, 0));
        cards.setOpaque(false);

        // Create 5 stat cards
        ItemController    ic = ItemController.getInstance();
        ClaimController   cc = ClaimController.getInstance();

        totalLabel   = addCard(cards, "📦 Total Items",  str(ic.getTotalItems()),   UITheme.PRIMARY);
        lostLabel    = addCard(cards, "😢 Lost",          str(ic.getLostCount()),    UITheme.DANGER);
        foundLabel   = addCard(cards, "😊 Found",         str(ic.getFoundCount()),   UITheme.SUCCESS);
        claimedLabel = addCard(cards, "✅ Claimed",        str(ic.getClaimedCount()), UITheme.INFO);
        pendingLabel = addCard(cards, "⏳ Pending Claims", str(cc.getPendingCount()),  UITheme.WARNING);

        return cards;
    }

    private JLabel addCard(JPanel parent, String title, String value, Color accent) {
        // Custom card panel with left-colored accent bar
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // White card background
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.CARD_RADIUS, UITheme.CARD_RADIUS);
                // Left accent stripe
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setPreferredSize(new Dimension(0, 110));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel inner = new JPanel(new GridLayout(2, 1, 0, 6));
        inner.setOpaque(false);
        inner.add(titleLabel);
        inner.add(valueLabel);

        card.add(inner, BorderLayout.CENTER);
        parent.add(card);

        // Return the value label so we can update it on refresh
        return valueLabel;
    }

    private JPanel buildInfoRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);

        row.add(buildQuickActions());
        row.add(buildTips());

        return row;
    }

    private JPanel buildQuickActions() {
        JPanel card = createCard("Quick Actions");

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);

        grid.add(buildActionCard("Report Lost Item", MainFrame.LOST_ITEMS));
        grid.add(buildActionCard("Report Found Item", MainFrame.FOUND_ITEMS));
        grid.add(buildActionCard("View All Items", MainFrame.LOST_ITEMS));
        grid.add(buildActionCard("Search Items", MainFrame.FOUND_ITEMS));

        card.add(grid);

        return card;
    }

    private JPanel buildActionCard(String label, String targetPanel) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(UITheme.BG_CARD);
        box.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));

        JButton button = new JButton(label);
        button.setFont(UITheme.FONT_BODY);
        button.setForeground(UITheme.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> navigateTo(targetPanel));

        box.add(button, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildTips() {
        JPanel card = createCard("💡 System Tips");

        String[] tips = {
            "Use categories to narrow down your search.",
            "Upload clear photos for better matching.",
            "Admin can approve/reject claims from Admin Panel.",
            "Smart Matching finds similar lost & found items automatically.",
            "Partial search works — type just part of the item name.",
        };

        for (String tip : tips) {
            JLabel label = new JLabel("• " + tip);
            label.setFont(UITheme.FONT_SMALL);
            label.setForeground(UITheme.TEXT_SECONDARY);
            label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            card.add(label);
        }

        return card;
    }

    private JPanel createCard(String title) {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.CARD_RADIUS, UITheme.CARD_RADIUS);
            }
        };
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_HEADING);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        outer.add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        outer.add(content, BorderLayout.CENTER);

        return content; // Caller adds to content
    }

    /** Called when Refresh is clicked — updates all numbers */
    public void refresh() {
        ItemController  ic = ItemController.getInstance();
        ClaimController cc = ClaimController.getInstance();

        totalLabel.setText(str(ic.getTotalItems()));
        lostLabel.setText(str(ic.getLostCount()));
        foundLabel.setText(str(ic.getFoundCount()));
        claimedLabel.setText(str(ic.getClaimedCount()));
        pendingLabel.setText(str(cc.getPendingCount()));

        revalidate();
        repaint();
    }

    private String str(int n) { return String.valueOf(n); }

    private void navigateTo(String panelName) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainFrame frame) {
            frame.showPanel(panelName);
        }
    }
}