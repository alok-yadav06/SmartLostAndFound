// src/Main.java
import dao.DatabaseConnection;
import javax.swing.*;
import util.UITheme;
import view.MainFrame;

public class Main {
    public static void main(String[] args) {
        // Phase 1: Apply theme BEFORE building any UI
        UITheme.applyTheme();

        // Phase 2: Initialize database (creates tables if not exist)
        DatabaseConnection.getInstance().initializeDatabase();

        // Phase 3: Launch UI on the Event Dispatch Thread (EDT)
        // RULE: ALL Swing UI must be created/modified on the EDT.
        // invokeLater() schedules this runnable to run on the EDT.
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.promptLoginAtStartup();
        });
    }
}