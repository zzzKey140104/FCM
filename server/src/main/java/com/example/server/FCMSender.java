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
		String accessToken = getAccessToken();
		
		// Debug logging
		System.out.println("=== FCM Debug Info ===");
		System.out.println("Project ID: " + projectId);
		System.out.println("Tokens count: " + tokens.size());
		for (int i = 0; i < tokens.size(); i++) {
			System.out.println("Token " + i + ": " + tokens.get(i));
		}
		System.out.println("Access token length: " + (accessToken != null ? accessToken.length() : "null"));
		System.out.println("=====================");
		
		// Send to each token individually
		StringBuilder results = new StringBuilder();
		int successCount = 0;
		int failCount = 0;
		
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			String json = buildJsonForSingleToken(token, title, body);
			
			System.out.println("Sending to token " + i + ": " + token);
			System.out.println("JSON payload: " + json);
			
			try {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.header("Content-Type", "application/json; charset=UTF-8")
						.header("Authorization", "Bearer " + accessToken)
						.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
						.build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				
				if (response.statusCode() == 200) {
					successCount++;
					System.out.println("Success for token " + i + ": " + response.body());
				} else {
					failCount++;
					System.out.println("Failed for token " + i + ": " + response.statusCode() + " - " + response.body());
				}
				
				if (results.length() > 0) results.append("; ");
				results.append("Token ").append(i).append(": ").append(response.statusCode()).append(":").append(response.body());
				
			} catch (Exception e) {
				failCount++;
				System.err.println("Exception for token " + i + ": " + e.getMessage());
				if (results.length() > 0) results.append("; ");
				results.append("Token ").append(i).append(": ERROR:").append(e.getMessage());
			}
		}
		
		String summary = String.format("Sent to %d devices: %d success, %d failed. Details: %s", 
				tokens.size(), successCount, failCount, results.toString());
		System.out.println("=== FCM Send Summary ===");
		System.out.println(summary);
		System.out.println("========================");
		
		return "200:" + summary;
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

	private String buildJsonForSingleToken(String token, String title, String body) {
		// Validate token
		if (token == null || token.trim().isEmpty()) {
			throw new IllegalArgumentException("No token provided");
		}
		if (!isValidFCMToken(token)) {
			throw new IllegalArgumentException("Invalid FCM token format: " + token);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"message\":{")
				.append("\"token\":\"").append(escape(token)).append("\",")
				.append("\"notification\":{")
				.append("\"title\":\"").append(escape(title)).append("\",")
				.append("\"body\":\"").append(escape(body)).append("\"")
				.append("}")
				.append("}}");
		return sb.toString();
	}
	
	// Keep the old method for backward compatibility, but it now uses the new approach
	private String buildJson(List<String> tokens, String title, String body) {
		// This method is now deprecated in favor of sendToTokens which handles multiple tokens properly
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("No tokens provided");
		}
		return buildJsonForSingleToken(tokens.get(0), title, body);
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
