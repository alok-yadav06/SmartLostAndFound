package view;

import controller.ItemController;
import controller.UserController;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import model.Item;
import model.LostItem;
import util.ImageUtil;
import util.UITheme;

/**
 * LostItemsPanel — displays all lost items + search + add dialog.
 *
 * CONCEPT: JTable with custom renderer
 * JTable shows data in rows/columns using a TableModel.
 * DefaultTableModel is the simple built-in model.
 * Custom renderers (TableCellRenderer) let you style cells.
 *
 * CONCEPT: JDialog
 * Modal dialog blocks input to parent window until closed.
 * Used for forms (add item). setModal(true) = blocking.
 */
public class LostItemsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private final List<LostItem> visibleItems = new ArrayList<>();
    private JLabel pageLabel;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    public LostItemsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(),  BorderLayout.NORTH);

        // Table
        String[] columns = {"ID","Name","Category","Location","Date","Status","Reward"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = buildTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        add(scroll, BorderLayout.CENTER);

        add(buildActions(), BorderLayout.SOUTH);

        loadItems(null, "All");
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("😢 Lost Items");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        // Search + filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);

        searchField = UITheme.styledField("Search...");
        searchField.setPreferredSize(new Dimension(200, 36));

        String[] cats = new String[Item.CATEGORIES.length + 1];
        cats[0] = "All";
        System.arraycopy(Item.CATEGORIES, 0, cats, 1, Item.CATEGORIES.length);
        categoryFilter = UITheme.styledCombo(cats);
        categoryFilter.setPreferredSize(new Dimension(140, 36));

        JButton searchBtn = UITheme.primaryButton("🔍 Search");
        searchBtn.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());   // Enter key

        JButton clearBtn  = UITheme.ghostButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            loadItems(null, "All");
        });

        filterRow.add(new JLabel("Category:"));
        filterRow.add(categoryFilter);
        filterRow.add(searchField);
        filterRow.add(searchBtn);
        filterRow.add(clearBtn);

        header.add(title, BorderLayout.WEST);
        header.add(filterRow, BorderLayout.EAST);
        return header;
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setRowHeight(36);
        t.setFont(UITheme.FONT_BODY);
        t.setGridColor(UITheme.BORDER_COLOR);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(UITheme.TEXT_PRIMARY);
        t.getTableHeader().setFont(UITheme.FONT_SUBHEAD);
        t.getTableHeader().setBackground(UITheme.BG_MAIN);
        t.getTableHeader().setReorderingAllowed(false);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));

        // Status column: colored badge renderer
        t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    tbl, val, sel, focus, row, col);
                String status = String.valueOf(val);
                lbl.setForeground(UITheme.statusColor(status));
                lbl.setFont(UITheme.FONT_SUBHEAD);
                return lbl;
            }
        });

        return t;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftActions.setOpaque(false);

        JButton addBtn = UITheme.primaryButton("+ Report Lost Item");
        addBtn.addActionListener(e -> showAddDialog());

        JButton editBtn = UITheme.ghostButton("✏ Edit Selected");
        editBtn.addActionListener(e -> editSelected());

        JButton delBtn = UITheme.dangerButton("🗑 Delete Selected");
        delBtn.addActionListener(e -> deleteSelected());

        JButton viewBtn = UITheme.ghostButton("👁 View Details");
        viewBtn.addActionListener(e -> viewDetails());

        leftActions.add(addBtn);
        if (UserController.getInstance().isLoggedIn()) {
            leftActions.add(editBtn);
            leftActions.add(delBtn);
        }
        leftActions.add(viewBtn);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pager.setOpaque(false);
        JButton prevBtn = UITheme.ghostButton("Previous");
        JButton nextBtn = UITheme.ghostButton("Next");
        pageLabel = new JLabel("Page 1/1");
        pageLabel.setFont(UITheme.FONT_BODY);
        pageLabel.setForeground(UITheme.TEXT_SECONDARY);

        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages()) {
                currentPage++;
                refreshPage();
            }
        });

        pager.add(pageLabel);
        pager.add(prevBtn);
        pager.add(nextBtn);

        actions.add(leftActions, BorderLayout.WEST);
        actions.add(pager, BorderLayout.EAST);
        return actions;
    }

    // ── Data Loading ───────────────────────────────────────

    public final void loadItems(String query, String category) {
        List<LostItem> items = (query == null && "All".equals(category))
            ? ItemController.getInstance().getAllLostItems()
            : ItemController.getInstance().searchLostItems(query, category);

        visibleItems.clear();
        visibleItems.addAll(items);
        currentPage = 1;
        refreshPage();
    }

    private void doSearch() {
        String q   = searchField.getText().trim();
        String cat = (String) categoryFilter.getSelectedItem();
        loadItems(q.isEmpty() ? null : q, cat);
    }

    // ── Add Item Dialog ────────────────────────────────────

    private void showAddDialog() {
        if (!UserController.getInstance().isLoggedIn()) {
            JOptionPane.showMessageDialog(this,
                "Please sign in to report a lost item. Guest mode is view-only.",
                "Sign In Required",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Report Lost Item", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0; gbc.weightx = 1.0;

        JTextField nameField     = UITheme.styledField("Item name");
        JTextArea  descArea      = UITheme.styledArea();
        descArea.setRows(3);
        JComboBox<String> catBox = UITheme.styledCombo(Item.CATEGORIES);
        JComboBox<String> locAreaBox = UITheme.styledCombo(new String[]{
            "Library", "Cafeteria", "Classroom Block", "Hostel", "Sports Ground",
            "Auditorium", "Admin Office", "Parking", "Bus Stop", "Other"
        });
        JTextField locDetailField = UITheme.styledField("Exact spot (floor/room/near landmark)");
        JTextField lastSeenField = UITheme.styledField("Last seen at?");
        JTextField rewardField   = UITheme.styledField("0 = no reward");
        JTextField contactField  = UITheme.styledField("Your contact number");

        // Image
        JLabel imagePreview = new JLabel(ImageUtil.createPlaceholder(80, 60));
        imagePreview.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        JButton imgBtn = UITheme.ghostButton("📷 Upload Image");
        String[] imagePath = {null};
        imgBtn.addActionListener(e -> {
            String path = ImageUtil.chooseAndSaveImage(dialog);
            if (path != null) {
                imagePath[0] = path;
                imagePreview.setIcon(ImageUtil.loadScaled(path, 80, 60));
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Could not save selected image. Please try another file.",
                    "Image Upload Failed",
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        int row = 0;
        addFormRow(form, gbc, row++, "Item Name *",    nameField);
        addFormRow(form, gbc, row++, "Description",    new JScrollPane(descArea));
        addFormRow(form, gbc, row++, "Category *",     catBox);
        JPanel locPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        locPanel.setOpaque(false);
        locPanel.add(locAreaBox);
        locPanel.add(locDetailField);

        addFormRow(form, gbc, row++, "Location *",     locPanel);
        addFormRow(form, gbc, row++, "Last Seen At",   lastSeenField);
        addFormRow(form, gbc, row++, "Reward (₹)",     rewardField);
        addFormRow(form, gbc, row++, "Contact Info",   contactField);

        // Image row
        JPanel imageCard = new JPanel(new BorderLayout(8, 8));
        imageCard.setOpaque(false);
        imageCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel imageLabel = new JLabel("Upload Images (optional)");
        imageLabel.setFont(UITheme.FONT_SUBHEAD);
        imageLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel imgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        imgRow.setOpaque(false);
        imgRow.add(imagePreview);
        imgRow.add(imgBtn);

        imageCard.add(imageLabel, BorderLayout.NORTH);
        imageCard.add(imgRow, BorderLayout.CENTER);

        gbc.gridy = 14;
        form.add(imageCard, gbc);

        // Submit
        gbc.gridy = 15;
        gbc.insets = new Insets(16, 0, 0, 0);
        JButton submitBtn = UITheme.primaryButton("Submit Report");
        submitBtn.setPreferredSize(new Dimension(200, 40));
        form.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Item name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String location = formatLocation((String) locAreaBox.getSelectedItem(), locDetailField.getText().trim());
            if (location.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Location is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double reward = 0;
            try { reward = Double.parseDouble(rewardField.getText().trim()); } catch (NumberFormatException ignored) {}

            LostItem item = ItemController.getInstance().addLostItem(
                name, descArea.getText().trim(),
                (String) catBox.getSelectedItem(),
                location,
                lastSeenField.getText().trim(),
                reward, contactField.getText().trim(),
                imagePath[0]
            );
            loadItems(null, "All");
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "✅ Lost item reported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(new JScrollPane(form));
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row * 2;
        gbc.insets = new Insets(8, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SUBHEAD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(lbl, gbc);

        gbc.gridy = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(field, gbc);
    }

    // ── Delete & View ──────────────────────────────────────

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);

        if (!canModifyItem(id)) {
            JOptionPane.showMessageDialog(this,
                "You can edit/delete only items you reported.",
                "Permission Denied",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete item #" + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ItemController.getInstance().deleteLostItem(id);
            reloadCurrentFilter();
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        if (!canModifyItem(id)) {
            JOptionPane.showMessageDialog(this,
                "You can edit/delete only items you reported.",
                "Permission Denied",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        LostItem selected = findVisibleItemById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Unable to load selected item details.");
            return;
        }

        JTextField nameField = UITheme.styledField("Item name");
        nameField.setText(selected.getName());
        JTextArea descArea = UITheme.styledArea();
        descArea.setRows(3);
        descArea.setText(selected.getDescription() == null ? "" : selected.getDescription());
        JComboBox<String> catBox = UITheme.styledCombo(Item.CATEGORIES);
        catBox.setSelectedItem(selected.getCategory());
        JTextField locationField = UITheme.styledField("Location");
        locationField.setText(selected.getLocation());
        JTextField lastSeenField = UITheme.styledField("Last seen");
        lastSeenField.setText(selected.getLastSeenLocation());
        JTextField rewardField = UITheme.styledField("Reward");
        rewardField.setText(String.valueOf(selected.getRewardOffered()));
        JTextField contactField = UITheme.styledField("Contact");
        contactField.setText(selected.getContactInfo());

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Item Name")); form.add(nameField);
        form.add(new JLabel("Description")); form.add(new JScrollPane(descArea));
        form.add(new JLabel("Category")); form.add(catBox);
        form.add(new JLabel("Location")); form.add(locationField);
        form.add(new JLabel("Last Seen At")); form.add(lastSeenField);
        form.add(new JLabel("Reward (₹)")); form.add(rewardField);
        form.add(new JLabel("Contact Info")); form.add(contactField);

        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(form),
            "Edit Lost Item #" + id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            selected.setName(nameField.getText().trim());
            selected.setDescription(descArea.getText().trim());
            selected.setCategory((String) catBox.getSelectedItem());
            selected.setLocation(locationField.getText().trim());
            selected.setLastSeenLocation(lastSeenField.getText().trim());
            selected.setRewardOffered(Double.parseDouble(rewardField.getText().trim()));
            selected.setContactInfo(contactField.getText().trim());
            ItemController.getInstance().updateLostItem(selected);
            reloadCurrentFilter();
            JOptionPane.showMessageDialog(this, "Item updated successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to update item: " + ex.getMessage(),
                "Update Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        LostItem item = findVisibleItemById(id);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Unable to load selected item details.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        String[] cols = {"ID","Name","Category","Location","Date","Status","Reward"};
        for (int i = 0; i < cols.length; i++) {
            sb.append(cols[i]).append(": ").append(tableModel.getValueAt(row, i)).append("\n");
        }

        JPanel detailsPanel = new JPanel(new BorderLayout(12, 12));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel imageLabel = new JLabel(ImageUtil.loadScaled(item.getImagePath(), 220, 160));
        imageLabel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        detailsPanel.add(imageLabel, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea(sb.toString());
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(UITheme.FONT_BODY);
        infoArea.setBackground(UITheme.BG_CARD);
        detailsPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, detailsPanel, "Item Details", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refresh() { loadItems(null, "All"); }

    private void refreshPage() {
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, visibleItems.size());

        for (int i = start; i < end; i++) {
            LostItem item = visibleItems.get(i);
            tableModel.addRow(new Object[]{
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getLocation(),
                item.getDate(),
                item.getStatus(),
                item.hasReward() ? "₹" + item.getRewardOffered() : "—"
            });
        }

        if (pageLabel != null) {
            pageLabel.setText("Page " + currentPage + "/" + totalPages());
        }
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) visibleItems.size() / PAGE_SIZE));
    }

    private void reloadCurrentFilter() {
        String q = searchField != null ? searchField.getText().trim() : "";
        String cat = categoryFilter != null ? (String) categoryFilter.getSelectedItem() : "All";
        loadItems(q.isEmpty() ? null : q, cat == null ? "All" : cat);
    }

    private String formatLocation(String area, String detail) {
        String safeArea = area == null ? "" : area.trim();
        if (safeArea.isEmpty()) return "";
        if (detail == null || detail.isBlank()) return safeArea;
        return safeArea + " - " + detail.trim();
    }

    private boolean canModifyItem(int itemId) {
        UserController userController = UserController.getInstance();
        if (!userController.isLoggedIn()) return false;
        if (userController.isAdmin()) return true;
        return ItemController.getInstance().isLostItemOwnedBy(itemId, userController.getCurrentUser().getId());
    }

    private LostItem findVisibleItemById(int itemId) {
        for (LostItem item : visibleItems) {
            if (item.getId() == itemId) return item;
        }
        return null;
    }
}