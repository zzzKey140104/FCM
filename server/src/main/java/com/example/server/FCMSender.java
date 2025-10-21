package com.example.server;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class FCMSender {
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final String projectId;
	private final String serviceAccountFile;

	public FCMSender() {
		ConfigLoader.loadOnce();
		this.projectId = ConfigLoader.get("fcm.projectId");
		this.serviceAccountFile = ConfigLoader.get("fcm.serviceAccountFile");
	}

	public String sendToTokens(List<String> tokens, String title, String body) throws IOException, InterruptedException {
		String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
		String json = buildJson(tokens, title, body);
		String accessToken = getAccessToken();
		
		// Debug logging
		System.out.println("=== FCM Debug Info ===");
		System.out.println("Project ID: " + projectId);
		System.out.println("Tokens count: " + tokens.size());
		for (int i = 0; i < tokens.size(); i++) {
			System.out.println("Token " + i + ": " + tokens.get(i));
		}
		System.out.println("JSON payload: " + json);
		System.out.println("Access token length: " + (accessToken != null ? accessToken.length() : "null"));
		System.out.println("=====================");
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json; charset=UTF-8")
				.header("Authorization", "Bearer " + accessToken)
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		return response.statusCode() + ":" + response.body();
	}

	public String getAccessToken() throws IOException {
		try (FileInputStream fis = new FileInputStream(serviceAccountFile)) {
			GoogleCredentials creds = GoogleCredentials.fromStream(fis)
					.createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
			creds.refreshIfExpired();
			String token = creds.getAccessToken().getTokenValue();
			System.out.println("Access token obtained: " + (token != null ? "SUCCESS" : "FAILED"));
			System.out.println("Access token length: " + (token != null ? token.length() : "null"));
			return token;
		} catch (Exception e) {
			System.err.println("Failed to get access token: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	private String buildJson(List<String> tokens, String title, String body) {
		// FCM v1 format: send to first token only
		String singleToken = tokens.isEmpty() ? "" : tokens.get(0);
		
		// Validate token format
		if (singleToken.isEmpty()) {
			throw new IllegalArgumentException("No token provided");
		}
		if (!isValidFCMToken(singleToken)) {
			throw new IllegalArgumentException("Invalid FCM token format: " + singleToken);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"message\":{")
				.append("\"token\":\"").append(escape(singleToken)).append("\",")
				.append("\"notification\":{")
				.append("\"title\":\"").append(escape(title)).append("\",")
				.append("\"body\":\"").append(escape(body)).append("\"")
				.append("}")
				.append("}}");
		return sb.toString();
	}
	
	public boolean isValidFCMToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			return false;
		}
		// FCM token should be at least 140 characters and contain valid characters including colons
		return token.length() >= 140 && token.matches("[A-Za-z0-9_:.-]+");
	}

	private String escape(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
