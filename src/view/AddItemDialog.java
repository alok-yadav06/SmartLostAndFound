// // src/view/AddItemDialog.java
// package view;

// import controller.ItemController;
// import model.Item;
// import util.UITheme;

// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.*;

// /**
//  * Modal dialog for adding a Lost or Found item.
//  *
//  * KEY CONCEPTS:
//  * - JDialog: modal window (blocks parent until closed)
//  * - GridBagLayout: most flexible layout manager in Swing
//  * - Input validation: never trust user input
//  * - "was submitted" flag: clean way to tell caller what happened
//  */
// public class AddItemDialog extends JDialog {

//     private final String type; // "lost" or "found"
//     private boolean submitted = false;

//     // Form fields
//     private JTextField     nameField;
//     private JTextArea      descField;
//     private JComboBox<String> categoryBox;
//     private JTextField     locationField;
//     private JTextField     extraField1;  // lastSeen or turnedIn
//     private JTextField     contactField;
//     private JTextField     rewardField;  // Only for lost
//     private JCheckBox      authorityCheck; // Only for found

//     public AddItemDialog(Window parent, String type) {
//         super(parent, (type.equals("lost") ? "Report Lost Item" : "Report Found Item"),
//               ModalityType.APPLICATION_MODAL);
//         this.type = type;
//         buildUI();
//         pack();
//         setLocationRelativeTo(parent);
//         setResizable(false);
//     }

//     private void buildUI() {
//         JPanel root = new JPanel(new BorderLayout());
//         root.setBackground(UITheme.BG_DARK);
//         root.setBorder(BorderFactory.createEmptyBorder(
//             UITheme.PADDING_LG, UITheme.PADDING_LG,
//             UITheme.PADDING_LG, UITheme.PADDING_LG));

//         // Header
//         JLabel header = new JLabel(
//             type.equals("lost") ? "🔍 Report a Lost Item" : "📦 Report a Found Item");
//         header.setFont(UITheme.FONT_TITLE);
//         header.setForeground(UITheme.TEXT_PRIMARY);
//         header.setBorder(BorderFactory.createEmptyBorder(0, 0, UITheme.PADDING_LG, 0));

//         root.add(header,       BorderLayout.NORTH);
//         root.add(buildForm(),  BorderLayout.CENTER);
//         root.add(buildFooter(),BorderLayout.SOUTH);

//         setContentPane(root);
//         getContentPane().setBackground(UITheme.BG_DARK);
//     }

//     /**
//      * GridBagLayout — the most powerful layout in Swing.
//      *
//      * GridBagConstraints lets you control:
//      * - gridx, gridy: column and row position
//      * - gridwidth: how many columns a component spans
//      * - fill: how the component stretches
//      * - weightx/weighty: how extra space is distributed
//      * - insets: margins around the component
//      *
//      * It's verbose, but gives pixel-level control.
//      */
//     private JPanel buildForm() {
//         JPanel form = new JPanel(new GridBagLayout());
//         form.setBackground(UITheme.BG_DARK);
//         GridBagConstraints gbc = new GridBagConstraints();
//         gbc.fill = GridBagConstraints.HORIZONTAL;
//         gbc.insets = new Insets(6, 0, 6, 12);

//         int row = 0;

//         // Item Name
//         nameField = UITheme.createTextField("e.g., Black Wallet");
//         addFormRow(form, gbc, row++, "Item Name *", nameField);

//         // Description
//         descField = new JTextArea(3, 20);
//         descField.setBackground(UITheme.BG_CARD);
//         descField.setForeground(UITheme.TEXT_PRIMARY);
//         descField.setCaretColor(UITheme.TEXT_PRIMARY);
//         descField.setFont(UITheme.FONT_BODY);
//         descField.setLineWrap(true);
//         descField.setWrapStyleWord(true);
//         descField.setBorder(BorderFactory.createCompoundBorder(
//             BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
//             BorderFactory.createEmptyBorder(8, 12, 8, 12)));
//         addFormRow(form, gbc, row++, "Description", new JScrollPane(descField) {{
//             setBorder(BorderFactory.createEmptyBorder());
//         }});

//         // Category
//         categoryBox = new JComboBox<>(Item.ALL_CATEGORIES);
//         categoryBox.setBackground(UITheme.BG_CARD);
//         categoryBox.setForeground(UITheme.TEXT_PRIMARY);
//         categoryBox.setFont(UITheme.FONT_BODY);
//         addFormRow(form, gbc, row++, "Category *", categoryBox);

//         // Location
//         locationField = UITheme.createTextField("Where was it lost/found?");
//         addFormRow(form, gbc, row++, "Location *", locationField);

//         // Type-specific fields
//         if (type.equals("lost")) {
//             extraField1 = UITheme.createTextField("e.g., Near study tables");
//             addFormRow(form, gbc, row++, "Last Seen At", extraField1);

//             rewardField = UITheme.createTextField("0 (₹ reward offered)");
//             addFormRow(form, gbc, row++, "Reward (₹)", rewardField);

//             contactField = UITheme.createTextField("Your phone or email");
//             addFormRow(form, gbc, row++, "Contact Info", contactField);

//         } else {
//             extraField1 = UITheme.createTextField("e.g., Admin Office");
//             addFormRow(form, gbc, row++, "Turned In At", extraField1);

//             contactField = UITheme.createTextField("Your contact (optional)");
//             addFormRow(form, gbc, row++, "Finder Contact", contactField);

//             authorityCheck = new JCheckBox("Handed to security/authority");
//             authorityCheck.setForeground(UITheme.TEXT_PRIMARY);
//             authorityCheck.setFont(UITheme.FONT_BODY);
//             authorityCheck.setBackground(UITheme.BG_DARK);
//             authorityCheck.setSelected(true);
//             addFormRow(form, gbc, row++, "", authorityCheck);
//         }

//         return form;
//     }

//     /**
//      * Helper: adds a label + component pair to the GridBag form.
//      * DRY principle — Don't Repeat Yourself.
//      * Without this, every field = 15 lines of GBC setup. Ugly.
//      */
//     private void addFormRow(JPanel form, GridBagConstraints gbc,
//                             int row, String labelText, Component comp) {
//         gbc.gridx = 0; gbc.gridy = row;
//         gbc.weightx = 0.3; gbc.gridwidth = 1;
//         JLabel label = new JLabel(labelText);
//         label.setFont(UITheme.FONT_BODY);
//         label.setForeground(UITheme.TEXT_SECONDARY);
//         form.add(label, gbc);

//         gbc.gridx = 1; gbc.weightx = 0.7;
//         form.add(comp, gbc);
//     }

//     private JPanel buildFooter() {
//         JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
//         footer.setBackground(UITheme.BG_DARK);
//         footer.setBorder(BorderFactory.createEmptyBorder(UITheme.PADDING_LG, 0, 0, 0));

//         JButton cancelBtn = UITheme.createPrimaryButton("Cancel");
//         cancelBtn.setBackground(UITheme.BG_CARD);
//         cancelBtn.addActionListener(e -> dispose());

//         JButton submitBtn = UITheme.createPrimaryButton("Submit Report");
//         submitBtn.addActionListener(e -> handleSubmit());

//         footer.add(cancelBtn);
//         footer.add(submitBtn);
//         return footer;
//     }

//     /**
//      * Validation + submission.
//      *
//      * Rule: NEVER trust user input.
//      * Always validate before touching the controller.
//      */
//     private void handleSubmit() {
//         // Validation
//         String name = nameField.getText().trim();
//         String location = locationField.getText().trim();

//         if (name.isEmpty()) {
//             showError("Item name is required.");
//             nameField.requestFocus();
//             return;
//         }
//         if (location.isEmpty()) {
//             showError("Location is required.");
//             locationField.requestFocus();
//             return;
//         }

//         String desc     = descField.getText().trim();
//         String category = (String) categoryBox.getSelectedItem();
//         String extra    = extraField1 != null ? extraField1.getText().trim() : "";
//         String contact  = contactField != null ? contactField.getText().trim() : "";

//         ItemController ctrl = ItemController.getInstance();

//         if (type.equals("lost")) {
//             double reward = 0;
//             try {
//                 String rText = rewardField.getText().trim();
//                 if (!rText.isEmpty()) reward = Double.parseDouble(rText);
//             } catch (NumberFormatException ex) {
//                 showError("Reward must be a number (e.g., 500).");
//                 return;
//             }
//             ctrl.addLostItem(name, desc, category, location, extra, reward, contact);

//         } else {
//             boolean authority = authorityCheck != null && authorityCheck.isSelected();
//             ctrl.addFoundItem(name, desc, category, location, extra, contact, authority);
//         }

//         submitted = true;
//         JOptionPane.showMessageDialog(this,
//             "✅ Item reported successfully!", "Success",
//             JOptionPane.INFORMATION_MESSAGE);
//         dispose();
//     }

//     private void showError(String msg) {
//         JOptionPane.showMessageDialog(this, msg, "Validation Error",
//             JOptionPane.ERROR_MESSAGE);
//     }

//     /** Called by the parent panel after dialog closes */
//     public boolean wasSubmitted() { return submitted; }
// }