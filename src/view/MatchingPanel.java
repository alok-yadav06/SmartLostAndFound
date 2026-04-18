package view;

import controller.ItemController;
import model.FoundItem;
import model.Item;
import model.LostItem;
import util.MatchingEngine;
import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MatchingPanel extends JPanel {

    private JComboBox<String> lostItemCombo;
    private JPanel resultsPanel;
    private JLabel statusLabel;

    private List<LostItem> allLost;
    private final MatchingEngine engine = new MatchingEngine();

    public MatchingPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadLostItems();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Smart Matching");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Select a lost item to find potential matches from found items.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        header.add(left, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        JPanel selectorCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        selectorCard.setOpaque(false);
        selectorCard.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel selectLabel = new JLabel("Select Lost Item:");
        selectLabel.setFont(UITheme.FONT_SUBHEAD);
        selectLabel.setForeground(UITheme.TEXT_PRIMARY);

        lostItemCombo = new JComboBox<>();
        lostItemCombo.setFont(UITheme.FONT_BODY);
        lostItemCombo.setPreferredSize(new Dimension(320, 36));

        JButton matchBtn = UITheme.primaryButton("Find Matches");
        matchBtn.addActionListener(e -> runMatching());

        JButton refreshBtn = UITheme.ghostButton("Reload Items");
        refreshBtn.addActionListener(e -> loadLostItems());

        statusLabel = new JLabel("");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        selectorCard.add(selectLabel);
        selectorCard.add(lostItemCombo);
        selectorCard.add(matchBtn);
        selectorCard.add(refreshBtn);
        selectorCard.add(statusLabel);

        content.add(selectorCard, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);
        resultsPanel.add(buildEmptyState());

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);

        return content;
    }

    private void loadLostItems() {
        allLost = ItemController.getInstance().getAllLostItems();
        lostItemCombo.removeAllItems();

        if (allLost.isEmpty()) {
            lostItemCombo.addItem("No lost items reported yet");
            return;
        }

        for (LostItem item : allLost) {
            lostItemCombo.addItem("#" + item.getId() + " - " + item.getName() + " [" + item.getCategory() + "]");
        }
    }

    private void runMatching() {
        if (allLost == null || allLost.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No lost items available for matching.");
            return;
        }

        int selectedIdx = lostItemCombo.getSelectedIndex();
        if (selectedIdx < 0 || selectedIdx >= allLost.size()) {
            return;
        }

        LostItem selectedLost = allLost.get(selectedIdx);
        statusLabel.setText("Matching in progress...");
        resultsPanel.removeAll();

        SwingWorker<List<MatchingEngine.MatchResult>, Void> worker =
            new SwingWorker<List<MatchingEngine.MatchResult>, Void>() {
                @Override
                protected List<MatchingEngine.MatchResult> doInBackground() {
                    List<FoundItem> allFound = ItemController.getInstance().getAllFoundItems();
                    return engine.findMatchesForLost(selectedLost, allFound);
                }

                @Override
                protected void done() {
                    try {
                        List<MatchingEngine.MatchResult> matches = get();
                        showResults(selectedLost, matches);
                    } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                        statusLabel.setText("Error while matching: " + e.getMessage());
                    }
                }
            };

        worker.execute();
    }

    private void showResults(LostItem lost, List<MatchingEngine.MatchResult> matches) {
        resultsPanel.removeAll();

        JLabel header = new JLabel("Results for: " + lost.getName() + " (" + lost.getCategory() + ")");
        header.setFont(UITheme.FONT_HEADING);
        header.setForeground(UITheme.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsPanel.add(header);

        if (matches.isEmpty()) {
            statusLabel.setText("No matches found");
            resultsPanel.add(buildEmptyState());
        } else {
            statusLabel.setText(matches.size() + " match(es) found");
            for (MatchingEngine.MatchResult result : matches) {
                resultsPanel.add(buildMatchCard(result));
                resultsPanel.add(Box.createVerticalStrut(10));
            }
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel buildMatchCard(MatchingEngine.MatchResult result) {
        Item item = result.getItem();
        int score = result.getScore();

        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color scoreColor = score >= 75 ? UITheme.SUCCESS : (score >= 50 ? UITheme.PRIMARY : UITheme.WARNING);

        JPanel scoreBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(scoreColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
        };
        scoreBadge.setOpaque(false);
        scoreBadge.setPreferredSize(new Dimension(80, 70));

        JLabel scoreLabel = new JLabel(score + "%", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        scoreLabel.setForeground(Color.WHITE);
        scoreBadge.add(scoreLabel);

        JPanel info = new JPanel(new GridLayout(3, 1, 2, 2));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(UITheme.FONT_HEADING);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel detailLabel = new JLabel(item.getCategory() + " | " + item.getLocation() + " | " + item.getDate());
        detailLabel.setFont(UITheme.FONT_SMALL);
        detailLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel scoreWordLabel = new JLabel(result.getScoreLabel());
        scoreWordLabel.setFont(UITheme.FONT_SUBHEAD);
        scoreWordLabel.setForeground(scoreColor);

        info.add(nameLabel);
        info.add(detailLabel);
        info.add(scoreWordLabel);

        JButton contactBtn = UITheme.ghostButton("Contact Finder");
        if (item instanceof FoundItem) {
            FoundItem fi = (FoundItem) item;
            contactBtn.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Finder contact: " + (fi.getFinderContact() != null ? fi.getFinderContact() : "anonymous") +
                    "\nTurned in at: " + fi.getTurnedInLocation(),
                "Contact Info",
                JOptionPane.INFORMATION_MESSAGE
            ));
        }

        card.add(scoreBadge, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(contactBtn, BorderLayout.EAST);

        return card;
    }

    private JPanel buildEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        empty.setPreferredSize(new Dimension(400, 200));

        JLabel label = new JLabel(
            "<html><center>Select a lost item above and click <b>Find Matches</b>.</center></html>",
            SwingConstants.CENTER
        );
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_SECONDARY);
        empty.add(label);
        return empty;
    }

    public void refresh() {
        loadLostItems();
    }
}