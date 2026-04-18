package view;

import controller.UserController;
import java.awt.*;
import javax.swing.*;
import util.UITheme;

/**
 * AddItemPanel provides quick navigation to report Lost/Found items.
 * 
 * LOGIN REQUIREMENT: Users must be logged in to report items.
 * Check is performed in button ActionListeners before navigation.
 */
public class AddItemPanel extends JPanel {

	public AddItemPanel() {
		setLayout(new GridBagLayout());
		setBackground(UITheme.BG_MAIN);
		setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

		JPanel card = new JPanel(new GridBagLayout());
		card.setBackground(UITheme.BG_CARD);
		card.setBorder(UITheme.cardBorder());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.weightx = 1.0;

		JLabel title = new JLabel("Add Item");
		title.setFont(UITheme.FONT_TITLE);
		title.setForeground(UITheme.TEXT_PRIMARY);

		JLabel sub = new JLabel("Choose where you want to report a new item.");
		sub.setFont(UITheme.FONT_BODY);
		sub.setForeground(UITheme.TEXT_SECONDARY);

		JButton lostBtn = UITheme.primaryButton("+ Report Lost Item");
		lostBtn.addActionListener(e -> {
			if (checkLoginAndProceed()) {
				navigateTo(MainFrame.LOST_ITEMS);
			}
		});

		JButton foundBtn = UITheme.successButton("+ Report Found Item");
		foundBtn.addActionListener(e -> {
			if (checkLoginAndProceed()) {
				navigateTo(MainFrame.FOUND_ITEMS);
			}
		});

		gbc.gridy = 0;
		card.add(title, gbc);
		gbc.gridy = 1;
		card.add(sub, gbc);
		gbc.gridy = 2;
		card.add(lostBtn, gbc);
		gbc.gridy = 3;
		card.add(foundBtn, gbc);

		add(card);
	}

	/**
	 * Checks if user is logged in before allowing item report.
	 * If not logged in, shows error dialog and prompts to login.
	 * 
	 * @return true if user is logged in, false otherwise
	 */
	private boolean checkLoginAndProceed() {
		UserController userCtrl = UserController.getInstance();
		
		// Check if user is logged in
		if (!userCtrl.isLoggedIn()) {
			JOptionPane.showMessageDialog(
				this,
				"Please login first to report an item",
				"Login Required",
				JOptionPane.WARNING_MESSAGE
			);
			
			// Prompt user to login
			Window window = SwingUtilities.getWindowAncestor(this);
			if (window instanceof MainFrame frame) {
				frame.promptLogin();
			}
			return false;
		}
		
		return true;
	}

	private void navigateTo(String panelName) {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window instanceof MainFrame frame) {
			frame.showPanel(panelName);
		}
	}
}
