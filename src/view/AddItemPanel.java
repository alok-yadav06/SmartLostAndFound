package view;

import java.awt.*;
import javax.swing.*;
import util.UITheme;

/**
 * AddItemPanel provides quick navigation to report Lost/Found items.
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
		lostBtn.addActionListener(e -> navigateTo(MainFrame.LOST_ITEMS));

		JButton foundBtn = UITheme.successButton("+ Report Found Item");
		foundBtn.addActionListener(e -> navigateTo(MainFrame.FOUND_ITEMS));

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

	private void navigateTo(String panelName) {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window instanceof MainFrame frame) {
			frame.showPanel(panelName);
		}
	}
}
