package view;

import controller.ClaimController;
import controller.ItemController;
import controller.UserController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import model.ClaimRequest;
import model.FoundItem;
import model.LoginAuditEntry;
import model.LostItem;
import model.User;
import util.UITheme;

public class MyReportsPanel extends JPanel {

    private DefaultTableModel lostModel;
    private DefaultTableModel foundModel;
    private DefaultTableModel timelineModel;
    private JPanel rootContent;

    public MyReportsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        rebuild();
    }

    private JPanel buildGuestMessage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel msg = new JLabel("Sign in to view your reports and activity timeline.", JLabel.CENTER);
        msg.setFont(UITheme.FONT_SUBHEAD);
        msg.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(msg, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHeader(User current) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("My Reports");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel profile = new JLabel(current.getFullName() + " (" + current.getUsername() + ")");
        profile.setFont(UITheme.FONT_BODY);
        profile.setForeground(UITheme.TEXT_SECONDARY);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(profile);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);

        JTabbedPane reportsTabs = new JTabbedPane();
        reportsTabs.setFont(UITheme.FONT_SUBHEAD);
        reportsTabs.addTab("Lost Reports", wrapTable(createTable(lostModel)));
        reportsTabs.addTab("Found Reports", wrapTable(createTable(foundModel)));

        JPanel timelineCard = new JPanel(new BorderLayout(0, 8));
        timelineCard.setOpaque(true);
        timelineCard.setBackground(Color.WHITE);
        timelineCard.setBorder(UITheme.cardBorder());
        timelineCard.setPreferredSize(new Dimension(420, 0));

        JLabel timelineTitle = new JLabel("Activity Timeline");
        timelineTitle.setFont(UITheme.FONT_SUBHEAD);
        timelineTitle.setForeground(UITheme.TEXT_PRIMARY);
        timelineTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JTable timelineTable = createTable(timelineModel);
        timelineCard.add(timelineTitle, BorderLayout.NORTH);
        timelineCard.add(new JScrollPane(timelineTable), BorderLayout.CENTER);

        body.add(reportsTabs, BorderLayout.CENTER);
        body.add(timelineCard, BorderLayout.EAST);
        return body;
    }

    private void rebuild() {
        removeAll();

        User current = UserController.getInstance().getCurrentUser();
        if (current == null) {
            add(buildGuestMessage(), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        add(buildHeader(current), BorderLayout.NORTH);

        String[] lostCols = {"ID", "Name", "Category", "Location", "Status", "Date"};
        lostModel = new DefaultTableModel(lostCols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        String[] foundCols = {"ID", "Name", "Category", "Location", "Status", "Date"};
        foundModel = new DefaultTableModel(foundCols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        String[] timelineCols = {"Time", "Activity"};
        timelineModel = new DefaultTableModel(timelineCols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        rootContent = buildBody();
        add(rootContent, BorderLayout.CENTER);
        refresh();
        revalidate();
        repaint();
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        return pane;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(UITheme.FONT_BODY);
        table.getTableHeader().setFont(UITheme.FONT_SUBHEAD);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(UITheme.BORDER_COLOR);
        return table;
    }

    public final void refresh() {
        User current = UserController.getInstance().getCurrentUser();
        if (current == null || lostModel == null || foundModel == null || timelineModel == null) {
            if (current != null) {
                rebuild();
            }
            return;
        }

        lostModel.setRowCount(0);
        foundModel.setRowCount(0);
        timelineModel.setRowCount(0);

        int userId = current.getId();
        List<LostItem> lostItems = ItemController.getInstance().getLostItemsByReporter(userId);
        List<FoundItem> foundItems = ItemController.getInstance().getFoundItemsByReporter(userId);

        for (LostItem item : lostItems) {
            lostModel.addRow(new Object[]{
                item.getId(), item.getName(), item.getCategory(), item.getLocation(),
                item.getStatus(), item.getDate()
            });
        }

        for (FoundItem item : foundItems) {
            foundModel.addRow(new Object[]{
                item.getId(), item.getName(), item.getCategory(), item.getLocation(),
                item.getStatus(), item.getDate()
            });
        }

        List<ActivityEvent> events = new ArrayList<>();
        for (LostItem item : lostItems) {
            events.add(new ActivityEvent(item.getDate().atStartOfDay(),
                "Reported lost item #" + item.getId() + ": " + item.getName()));
        }
        for (FoundItem item : foundItems) {
            events.add(new ActivityEvent(item.getDate().atStartOfDay(),
                "Reported found item #" + item.getId() + ": " + item.getName()));
        }

        List<ClaimRequest> claims = ClaimController.getInstance().getClaimsByUser(userId);
        for (ClaimRequest c : claims) {
            events.add(new ActivityEvent(c.getCreatedAt(),
                "Claim submitted for item #" + c.getItemId()));
            if (c.getStatus() == ClaimRequest.ClaimStatus.APPROVED) {
                events.add(new ActivityEvent(c.getUpdatedAt(),
                    "Claim approved for item #" + c.getItemId()));
            }
        }

        for (LoginAuditEntry entry : UserController.getInstance().getLoginAuditEntries()) {
            if (entry.getUserId() != null && entry.getUserId() == userId) {
                events.add(new ActivityEvent(parseAuditTime(entry.getEventTime()),
                    "Account activity: " + entry.getEventType()));
            }
        }

        events.sort(Comparator.comparing(ActivityEvent::time).reversed());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        for (ActivityEvent e : events) {
            timelineModel.addRow(new Object[]{fmt.format(e.time()), e.message()});
        }
    }

    private LocalDateTime parseAuditTime(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (Exception ignoredAgain) {
                if (raw.length() >= 19) {
                    String candidate = raw.substring(0, 19).replace(' ', 'T');
                    try {
                        return LocalDateTime.parse(candidate);
                    } catch (Exception ignoredThird) {
                        return LocalDateTime.now();
                    }
                }
                return LocalDateTime.now();
            }
        }
    }

    private record ActivityEvent(LocalDateTime time, String message) {}
}
