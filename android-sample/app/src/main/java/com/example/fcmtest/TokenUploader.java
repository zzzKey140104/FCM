package com.example.fcmtest;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class TokenUploader {
	// Emulator Android -> host machine use 10.0.2.2
	private static final String SERVER_BASE = "http://10.0.2.2:8080";

	public static void uploadToken(String token, String label) {
		new Thread(() -> {
			try {
				String body = "token=" + enc(token) + "&label=" + enc(label);
				URL url = new URL(SERVER_BASE + "/register");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				conn.setFixedLengthStreamingMode(bytes.length);
				try (OutputStream os = conn.getOutputStream()) {
					os.write(bytes);
				}
				int code = conn.getResponseCode();
				System.out.println("Upload token result: " + code);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private static String enc(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}
}
