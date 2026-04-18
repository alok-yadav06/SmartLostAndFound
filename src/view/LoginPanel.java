package view;

import controller.UserController;
import java.awt.*;
import javax.swing.*;
import util.UITheme;

/**
 * LoginPanel — the first screen users see.
 *
 * CONCEPT: Anonymous Inner Class vs Lambda for ActionListener
 * Old way: btn.addActionListener(new ActionListener() { public void actionPerformed(...) {...} });
 * Modern: btn.addActionListener(e -> handleLogin());
 * Both work. Lambda is cleaner. Use lambdas for single-method interfaces.
 *
 * CONCEPT: CardLayout
 * MainFrame uses CardLayout to switch between Login/Main panels
 * without opening new windows. This is like React's <Routes>.
 */
public class LoginPanel extends JPanel {

    private final Runnable    onLoginSuccess;  // Callback to MainFrame

    public LoginPanel(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        setBackground(UITheme.BG_DARK);

        JPanel card = buildCard();
        add(card);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(40, 50, 40, 50)
        ));
        card.setPreferredSize(new Dimension(400, 480));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0; gbc.weightx = 1.0;

        // ── Logo / Title ─────────────────────────────────
        gbc.gridy = 0;
        JLabel icon = new JLabel("🔍", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        card.add(icon, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(4, 0, 4, 0);
        JLabel title = new JLabel("Smart Lost & Found", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, gbc);

        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 24, 0);
        JLabel sub = new JLabel("College Item Recovery System", SwingConstants.CENTER);
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        card.add(sub, gbc);

        // ── Username ──────────────────────────────────────
        gbc.gridy = 3; gbc.insets = new Insets(6, 0, 2, 0);
        JLabel uLabel = new JLabel("Username");
        uLabel.setFont(UITheme.FONT_SUBHEAD);
        uLabel.setForeground(UITheme.TEXT_PRIMARY);
        card.add(uLabel, gbc);

        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 8, 0);
        JTextField uField = UITheme.styledField("Enter username");
        uField.setText("admin");  // Prefill for demo
        card.add(uField, gbc);

        // ── Password ──────────────────────────────────────
        gbc.gridy = 5; gbc.insets = new Insets(6, 0, 2, 0);
        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(UITheme.FONT_SUBHEAD);
        pLabel.setForeground(UITheme.TEXT_PRIMARY);
        card.add(pLabel, gbc);

        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 4, 0);
        JPasswordField pField = new JPasswordField();
        pField.setFont(UITheme.FONT_BODY);
        pField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        pField.setPreferredSize(new Dimension(300, 40));
        pField.setText("admin123");
        card.add(pField, gbc);

        // ── Status ────────────────────────────────────────
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 16, 0);
        JLabel status = new JLabel(" ", SwingConstants.CENTER);
        status.setFont(UITheme.FONT_SMALL);
        status.setForeground(UITheme.DANGER);
        card.add(status, gbc);

        // ── Login Button ──────────────────────────────────
        gbc.gridy = 8; gbc.insets = new Insets(0, 0, 12, 0);
        JButton loginBtn = UITheme.primaryButton("Sign In →");
        loginBtn.setPreferredSize(new Dimension(300, 42));
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(loginBtn, gbc);

        // ── Register Link ─────────────────────────────────
        gbc.gridy = 9; gbc.insets = new Insets(0, 0, 0, 0);
        JButton regBtn = new JButton("Create account →");
        regBtn.setFont(UITheme.FONT_SMALL);
        regBtn.setForeground(UITheme.PRIMARY);
        regBtn.setBorderPainted(false);
        regBtn.setContentAreaFilled(false);
        regBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(regBtn, gbc);

        // ── Event Handlers ────────────────────────────────
        loginBtn.addActionListener(e -> doLogin(uField, pField, status));
        pField.addActionListener(e -> doLogin(uField, pField, status));   // Enter key
        regBtn.addActionListener(e -> showRegisterDialog(card));

        // Store refs via client property (avoids complex field management)
        card.putClientProperty("uField",  uField);
        card.putClientProperty("pField",  pField);
        card.putClientProperty("status",  status);

        return card;
    }

    private void doLogin(JTextField uField, JPasswordField pField, JLabel status) {
        String user = uField.getText().trim();
        String pass = new String(pField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            status.setText("⚠ Please fill in all fields");
            return;
        }

        boolean ok = UserController.getInstance().login(user, pass);
        if (ok) {
            status.setForeground(UITheme.SUCCESS);
            status.setText("✅ Login successful!");
            onLoginSuccess.run();
        } else {
            status.setForeground(UITheme.DANGER);
            status.setText("❌ Invalid username or password");
            pField.setText("");
        }
    }

    private void showRegisterDialog(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Register");
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 360);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        form.setBackground(UITheme.BG_CARD);

        JTextField nameField  = UITheme.styledField("Full Name");
        JTextField userField  = UITheme.styledField("Username");
        JTextField emailField = UITheme.styledField("Email");
        JPasswordField passF  = new JPasswordField();

        form.add(new JLabel("Full Name:")); form.add(nameField);
        form.add(new JLabel("Username:")); form.add(userField);
        form.add(new JLabel("Email:"));    form.add(emailField);
        form.add(new JLabel("Password:")); form.add(passF);

        JButton regBtn = UITheme.primaryButton("Register");
        JLabel msg = new JLabel(" ", SwingConstants.CENTER);
        msg.setForeground(UITheme.DANGER);

        regBtn.addActionListener(e -> {
            boolean ok = UserController.getInstance().register(
                userField.getText().trim(),
                new String(passF.getPassword()),
                emailField.getText().trim(),
                nameField.getText().trim()
            );
            if (ok) {
                msg.setForeground(UITheme.SUCCESS);
                msg.setText("✅ Account created! You can now log in.");
                Timer t = new Timer(2000, ev -> dialog.dispose());
                t.setRepeats(false); t.start();
            } else {
                msg.setText("❌ Username taken or invalid input.");
            }
        });

        dialog.add(form);
        dialog.add(regBtn);
        dialog.add(msg);
        dialog.setVisible(true);
    }
}