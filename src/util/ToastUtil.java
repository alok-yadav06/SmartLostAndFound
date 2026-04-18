package util;

import java.awt.*;
import javax.swing.*;

/**
 * Lightweight toast notifications for Swing.
 */
public final class ToastUtil {

    private ToastUtil() {}

    public static void showSuccess(Component parent, String message) {
        show(parent, message, new Color(22, 163, 74));
    }

    public static void showInfo(Component parent, String message) {
        show(parent, message, new Color(37, 99, 235));
    }

    public static void showWarning(Component parent, String message) {
        show(parent, message, new Color(202, 138, 4));
    }

    private static void show(Component parent, String message, Color accent) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog toast = new JDialog(owner);
        toast.setUndecorated(true);
        toast.setAlwaysOnTop(true);

        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setBackground(Color.WHITE);

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(6, 1));

        JLabel label = new JLabel(message);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_PRIMARY);

        card.add(accentBar, BorderLayout.WEST);
        card.add(label, BorderLayout.CENTER);
        toast.setContentPane(card);
        toast.pack();

        Rectangle bounds;
        if (owner != null && owner.isShowing()) {
            bounds = owner.getBounds();
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            bounds = new Rectangle(0, 0, screen.width, screen.height);
        }

        int x = bounds.x + bounds.width - toast.getWidth() - 24;
        int y = bounds.y + bounds.height - toast.getHeight() - 48;
        toast.setLocation(x, y);
        toast.setVisible(true);

        Timer timer = new Timer(1800, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
