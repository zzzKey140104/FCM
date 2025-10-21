package com.example.client;

import javax.swing.*;

public class ClientApp {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new frmClient().setVisible(true);
		});
	}
}
