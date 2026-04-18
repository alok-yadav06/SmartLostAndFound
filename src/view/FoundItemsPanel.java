package view;

import controller.ClaimController;
import controller.ItemController;
import controller.UserController;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import model.FoundItem;
import model.Item;
import util.ImageUtil;
import util.ToastUtil;
import util.UITheme;

/**
 * FoundItemsPanel — displays all found items + search + add + claim.
 * Very similar structure to LostItemsPanel (DRY principle violation here
 * is acceptable for readability; in production you'd extract a BaseItemPanel).
 */
public class FoundItemsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private final List<FoundItem> visibleItems = new ArrayList<>();
    private JLabel pageLabel;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    public FoundItemsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(),  BorderLayout.NORTH);

        String[] columns = {"ID","Name","Category","Location","Date","Status","Handed to Authority"};
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

        JLabel title = new JLabel("😊 Found Items");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

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
        searchField.addActionListener(e -> doSearch());

        JButton clearBtn  = UITheme.ghostButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText(""); categoryFilter.setSelectedIndex(0);
            loadItems(null, "All");
        });

        filterRow.add(new JLabel("Category:")); filterRow.add(categoryFilter);
        filterRow.add(searchField); filterRow.add(searchBtn); filterRow.add(clearBtn);

        header.add(title, BorderLayout.WEST);
        header.add(filterRow, BorderLayout.EAST);
        return header;
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setRowHeight(36);
        t.setFont(UITheme.FONT_BODY);
        t.setGridColor(UITheme.BORDER_COLOR);
        t.setSelectionBackground(new Color(209, 250, 229));
        t.setSelectionForeground(UITheme.TEXT_PRIMARY);
        t.getTableHeader().setFont(UITheme.FONT_SUBHEAD);
        t.getTableHeader().setBackground(UITheme.BG_MAIN);
        t.getTableHeader().setReorderingAllowed(false);
        t.setShowVerticalLines(false);

        // Status column renderer
        t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    tbl, val, sel, focus, row, col);
                lbl.setForeground(UITheme.statusColor(String.valueOf(val)));
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

        JButton addBtn   = UITheme.primaryButton("+ Report Found Item");
        JButton claimBtn = UITheme.successButton("📝 Claim This Item");
        JButton delBtn   = UITheme.dangerButton("🗑 Delete");
        JButton viewBtn  = UITheme.ghostButton("👁 View Details");

        addBtn.addActionListener(e -> showAddDialog());
        claimBtn.addActionListener(e -> showClaimDialog());
        delBtn.addActionListener(e -> deleteSelected());
        viewBtn.addActionListener(e -> viewDetails());

        leftActions.add(addBtn);
        leftActions.add(claimBtn);
        if (UserController.getInstance().isAdmin()) leftActions.add(delBtn);
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

    public final void loadItems(String query, String category) {
        List<FoundItem> items = (query == null && "All".equals(category))
            ? ItemController.getInstance().getAllFoundItems()
            : ItemController.getInstance().searchFoundItems(query, category);

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

    // ── Add Dialog ─────────────────────────────────────────

    private void showAddDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Report Found Item", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0; gbc.weightx = 1.0;

        JTextField nameField     = UITheme.styledField("What did you find?");
        JTextArea  descArea      = UITheme.styledArea(); descArea.setRows(3);
        JComboBox<String> catBox = UITheme.styledCombo(Item.CATEGORIES);
        JTextField locField      = UITheme.styledField("Where did you find it?");
        JTextField turnedInField = UITheme.styledField("Security desk / your room?");
        JTextField contactField  = UITheme.styledField("Your contact (optional: anonymous)");
        JCheckBox  authorityBox  = new JCheckBox("Handed to authority / security");
        authorityBox.setOpaque(false);
        authorityBox.setFont(UITheme.FONT_BODY);

        String[] imagePath = {null};
        JLabel imagePreview = new JLabel(ImageUtil.createPlaceholder(80, 60));
        JButton imgBtn = UITheme.ghostButton("📷 Upload Image");
        imgBtn.addActionListener(e -> {
            String path = ImageUtil.chooseAndSaveImage(dialog);
            if (path != null) { imagePath[0] = path; imagePreview.setIcon(ImageUtil.loadScaled(path, 80, 60)); }
        });

        addRow(form, gbc, 0, "Item Name *",      nameField);
        addRow(form, gbc, 1, "Description",       new JScrollPane(descArea));
        addRow(form, gbc, 2, "Category *",        catBox);
        addRow(form, gbc, 3, "Found Location *",  locField);
        addRow(form, gbc, 4, "Turned In At",      turnedInField);
        addRow(form, gbc, 5, "Your Contact",      contactField);

        gbc.gridy = 12; form.add(authorityBox, gbc);
        gbc.gridy = 13;
        JPanel imgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        imgRow.setOpaque(false); imgRow.add(imagePreview); imgRow.add(imgBtn);
        form.add(imgRow, gbc);

        gbc.gridy = 14; gbc.insets = new Insets(16, 0, 0, 0);
        JButton submitBtn = UITheme.primaryButton("Submit Found Item");
        submitBtn.setPreferredSize(new Dimension(200, 40));
        form.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Item name is required.");
                return;
            }
            String contact = contactField.getText().trim();
            if (contact.isEmpty()) contact = "anonymous";

            FoundItem item = ItemController.getInstance().addFoundItem(
                nameField.getText().trim(), descArea.getText().trim(),
                (String) catBox.getSelectedItem(), locField.getText().trim(),
                turnedInField.getText().trim(), contact,
                authorityBox.isSelected()
            );
            item.setImagePath(imagePath[0]);
            loadItems(null, "All");
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "✅ Found item reported!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(new JScrollPane(form));
        dialog.setVisible(true);
    }

    // ── Claim Dialog ───────────────────────────────────────

    private void showClaimDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item to claim."); return; }
        if (!UserController.getInstance().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Please sign in from the top-right header to claim items."); return;
        }

        int itemId    = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 5);
        if ("Claimed".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This item is already claimed."); return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Claim Item #" + itemId, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 340);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0; gbc.weightx = 1.0;

        JTextArea reasonArea = UITheme.styledArea(); reasonArea.setRows(3);
        JTextArea proofArea  = UITheme.styledArea(); proofArea.setRows(3);

        addRow(form, gbc, 0, "Why is this yours?",   new JScrollPane(reasonArea));
        addRow(form, gbc, 1, "Proof / Details",       new JScrollPane(proofArea));

        gbc.gridy = 4; gbc.insets = new Insets(16, 0, 0, 0);
        JButton submitBtn = UITheme.successButton("Submit Claim");
        submitBtn.setPreferredSize(new Dimension(200, 40));
        form.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            if (reasonArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please provide a reason."); return;
            }
            int userId = UserController.getInstance().getCurrentUser().getId();
            ClaimController.getInstance().submitClaim(
                userId, itemId, reasonArea.getText().trim(), proofArea.getText().trim());
            dialog.dispose();
            ToastUtil.showInfo(this, "Claim submitted. Waiting for admin confirmation.");
        });

        dialog.add(form);
        dialog.setVisible(true);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int rowIdx, String label, JComponent field) {
        gbc.gridy = rowIdx * 2;
        gbc.insets = new Insets(8, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SUBHEAD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(lbl, gbc);
        gbc.gridy = rowIdx * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(field, gbc);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete item #" + id + "?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ItemController.getInstance().deleteFoundItem(id);
            reloadCurrentFilter();
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        StringBuilder sb = new StringBuilder();
        String[] cols = {"ID","Name","Category","Location","Date","Status","Authority"};
        for (int i = 0; i < cols.length; i++)
            sb.append(cols[i]).append(": ").append(tableModel.getValueAt(row, i)).append("\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Item Details", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refresh() { loadItems(null, "All"); }

    private void refreshPage() {
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, visibleItems.size());

        for (int i = start; i < end; i++) {
            FoundItem item = visibleItems.get(i);
            tableModel.addRow(new Object[]{
                item.getId(), item.getName(), item.getCategory(),
                item.getLocation(), item.getDate(), item.getStatus(),
                item.isHandedToAuthority() ? "✅ Yes" : "No"
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
}