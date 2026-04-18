package util;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║              UITheme — Design System                 ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * WHY THIS EXISTS:
 * Without a theme class, you'd write Color(41,128,185) in 50 places.
 * When you want to change blue to teal → 50 edits, 50 bugs.
 * With UITheme → change ONE constant → entire app updates.
 *
 * This is called the "Single Source of Truth" principle.
 * Real design systems (Material UI, Ant Design) work the same way.
 */
public class UITheme {

    // ── Brand Colors ───────────────────────────────────────
    public static final Color PRIMARY        = new Color(67, 97, 238);   // Indigo
    public static final Color PRIMARY_DARK   = new Color(58, 12, 163);
    public static final Color PRIMARY_LIGHT  = new Color(114, 9, 183);
    public static final Color SECONDARY      = new Color(76, 201, 240);  // Cyan
    public static final Color SUCCESS        = new Color(40, 167, 69);
    public static final Color WARNING        = new Color(255, 193, 7);
    public static final Color DANGER         = new Color(220, 53, 69);
    public static final Color INFO           = new Color(23, 162, 184);

    // ── Neutral Colors ─────────────────────────────────────
    public static final Color BG_DARK        = new Color(15, 23, 42);    // Sidebar bg
    public static final Color BG_MAIN        = new Color(248, 249, 252); // Main content bg
    public static final Color BG_CARD        = Color.WHITE;
    public static final Color BG_HOVER       = new Color(241, 245, 249);
    public static final Color BORDER_COLOR   = new Color(226, 232, 240);
    public static final Color TEXT_PRIMARY   = new Color(15, 23, 42);
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    public static final Color TEXT_LIGHT     = new Color(148, 163, 184);
    public static final Color TEXT_WHITE     = Color.WHITE;

    // Backward-compatible aliases referenced by existing views.
    public static final Color BG_SIDEBAR     = BG_DARK;
    public static final Color BORDER         = BORDER_COLOR;

    // ── Status Colors ──────────────────────────────────────
    public static final Color STATUS_PENDING  = new Color(255, 193, 7);
    public static final Color STATUS_APPROVED = new Color(40, 167, 69);
    public static final Color STATUS_REJECTED = new Color(220, 53, 69);
    public static final Color STATUS_CLAIMED  = new Color(23, 162, 184);

    // ── Fonts ──────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,   24);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,   16);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD,   13);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN,  13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN,  11);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN,  12);

    // ── Sizing ─────────────────────────────────────────────
    public static final int SIDEBAR_WIDTH  = 220;
    public static final int CARD_RADIUS    = 12;
    public static final int BUTTON_HEIGHT  = 36;
    public static final int PADDING        = 16;

    // ── Component Factories ────────────────────────────────

    /** Creates a styled primary button */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(PRIMARY);
        btn.setForeground(TEXT_WHITE);
        btn.setFont(FONT_SUBHEAD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, BUTTON_HEIGHT));
        return btn;
    }

    /** Creates a styled danger (red) button */
    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(DANGER);
        return btn;
    }

    /** Creates a styled success (green) button */
    public static JButton successButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(SUCCESS);
        return btn;
    }

    /** Creates a ghost/outline button */
    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_CARD);
        btn.setForeground(PRIMARY);
        btn.setFont(FONT_SUBHEAD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(PRIMARY, 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Creates a styled text field */
    public static JTextField styledField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 40));
        return field;
    }

    /** Creates a styled text area */
    public static JTextArea styledArea() {
        JTextArea area = new JTextArea();
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_PRIMARY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return area;
    }

    /** Creates a styled combo box */
    public static JComboBox<String> styledCombo(String[] options) {
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setFont(FONT_BODY);
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 38));
        return combo;
    }

    /** Card border with shadow effect */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        );
    }

    /** Sets the look and feel to system native */
    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Panel.background",     BG_MAIN);
            UIManager.put("Button.font",          FONT_BODY);
            UIManager.put("Label.font",           FONT_BODY);
            UIManager.put("TextField.font",       FONT_BODY);
            UIManager.put("ComboBox.font",        FONT_BODY);
            UIManager.put("Table.font",           FONT_BODY);
            UIManager.put("TableHeader.font",     FONT_SUBHEAD);
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Fall back to default
        }
    }

    /** Returns a color for a given status string */
    public static Color statusColor(String status) {
        if (status == null) return STATUS_PENDING;
        return switch (status.toUpperCase()) {
            case "APPROVED", "CLAIMED" -> STATUS_APPROVED;
            case "REJECTED"            -> STATUS_REJECTED;
            case "FOUND"               -> STATUS_CLAIMED;
            default                    -> STATUS_PENDING;
        };
    }
}