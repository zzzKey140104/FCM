package com.example.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class frmClient extends JFrame {
	private final JTextField serverUrlField = new JTextField("http://localhost:8080");
	private final JTextField tokenField = new JTextField();
	private final JTextField labelField = new JTextField();
	private final JTextField titleField = new JTextField("Hello");
	private final JTextField bodyField = new JTextField("From Swing client");
	private final JTextArea outputArea = new JTextArea(8, 40);

	public frmClient() {
		super("LTM Swing Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 420);
		setLocationRelativeTo(null);

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
		form.add(new JLabel("Server URL"));
		form.add(serverUrlField);
		form.add(new JLabel("Token"));
		form.add(tokenField);
		form.add(new JLabel("Label"));
		form.add(labelField);
		form.add(new JLabel("Title"));
		form.add(titleField);
		form.add(new JLabel("Body"));
		form.add(bodyField);

		JButton btnRegister = new JButton(new AbstractAction("Register Token") {
			@Override public void actionPerformed(ActionEvent e) {
				doRegister();
			}
		});
		JButton btnSend = new JButton(new AbstractAction("Send Notification") {
			@Override public void actionPerformed(ActionEvent e) {
				doSend();
			}
		});
		JButton btnTestToken = new JButton(new AbstractAction("Test Token") {
			@Override public void actionPerformed(ActionEvent e) {
				doTestToken();
			}
		});
		JButton btnDebugToken = new JButton(new AbstractAction("Debug Token") {
			@Override public void actionPerformed(ActionEvent e) {
				doDebugToken();
			}
		});
		JButton btnTestServiceAccount = new JButton(new AbstractAction("Test Service Account") {
			@Override public void actionPerformed(ActionEvent e) {
				doTestServiceAccount();
			}
		});
		JPanel actions = new JPanel();
		actions.add(btnRegister);
		actions.add(btnSend);
		actions.add(btnTestToken);
		actions.add(btnDebugToken);
		actions.add(btnTestServiceAccount);

		outputArea.setEditable(false);
		JScrollPane scroll = new JScrollPane(outputArea);

		setLayout(new BorderLayout(8, 8));
		add(form, BorderLayout.NORTH);
		add(actions, BorderLayout.CENTER);
		add(scroll, BorderLayout.SOUTH);
	}

	private void doRegister() {
		String base = serverUrlField.getText().trim();
		String token = tokenField.getText().trim();
		String label = labelField.getText().trim();
		if (token.isEmpty()) {
			appendOut("Token is required");
			return;
		}
		try {
			String data = "token=" + enc(token) + "&label=" + enc(label);
			String res = post(base + "/register", data);
			appendOut(res);
		} catch (Exception ex) {
			appendOut("Error: " + ex.getMessage());
		}
	}

	private void doSend() {
		String base = serverUrlField.getText().trim();
		try {
			String data = "title=" + enc(titleField.getText()) + "&body=" + enc(bodyField.getText());
			String res = post(base + "/send", data);
			appendOut(res);
		} catch (Exception ex) {
			appendOut("Error: " + ex.getMessage());
		}
	}

	private void doTestToken() {
		String base = serverUrlField.getText().trim();
		String token = tokenField.getText().trim();
		if (token.isEmpty()) {
			appendOut("Token is required for testing");
			return;
		}
		try {
			String data = "token=" + enc(token);
			String res = post(base + "/test-token", data);
			appendOut("Token test result: " + res);
		} catch (Exception ex) {
			appendOut("Error: " + ex.getMessage());
		}
	}

	private void doDebugToken() {
		String base = serverUrlField.getText().trim();
		String token = tokenField.getText().trim();
		if (token.isEmpty()) {
			appendOut("Token is required for debugging");
			return;
		}
		try {
			String data = "token=" + enc(token);
			String res = post(base + "/debug-token", data);
			appendOut("Debug token result: " + res);
		} catch (Exception ex) {
			appendOut("Error: " + ex.getMessage());
		}
	}

	private void doTestServiceAccount() {
		String base = serverUrlField.getText().trim();
		try {
			String res = post(base + "/test-service-account", "");
			appendOut("Service account test result: " + res);
		} catch (Exception ex) {
			appendOut("Error: " + ex.getMessage());
		}
	}

	private String post(String url, String form) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		byte[] bytes = form.getBytes(StandardCharsets.UTF_8);
		conn.setFixedLengthStreamingMode(bytes.length);
		try (OutputStream os = conn.getOutputStream()) {
			os.write(bytes);
		}
		int code = conn.getResponseCode();
		String text;
		try {
			text = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Read error stream for non-2xx responses
			InputStream es = conn.getErrorStream();
			if (es != null) {
				text = new String(es.readAllBytes(), StandardCharsets.UTF_8);
			} else {
				text = e.getMessage();
			}
		}
		return code + ":" + text;
	}

	private String enc(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}

	private void appendOut(String s) {
		outputArea.append(s + "\n");
	}
}
