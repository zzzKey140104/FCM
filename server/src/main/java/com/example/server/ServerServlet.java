package com.example.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "ServerServlet", urlPatterns = {"/register", "/send", "/test-token", "/debug-token", "/test-service-account"})
public class ServerServlet extends HttpServlet {
	private transient DatabaseHelper db;
	private transient FCMSender fcm;

	@Override
	public void init() throws ServletException {
		super.init();
		this.db = new DatabaseHelper();
		this.fcm = new FCMSender();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getServletPath();
		// Basic request logging for debugging
		System.out.println("[ServerServlet] Incoming POST " + path + " from " + req.getRemoteAddr());
		resp.setContentType("application/json; charset=UTF-8");
		try (PrintWriter out = resp.getWriter()) {
			if ("/register".equals(path)) {
				String token = req.getParameter("token");
				String label = req.getParameter("label");
				if (token == null || token.isBlank()) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"token required\"}");
					return;
				}
				db.upsertDeviceToken(token, label);
				out.write("{\"status\":\"ok\"}");
				return;
			}
			if ("/send".equals(path)) {
				String title = req.getParameter("title");
				String body = req.getParameter("body");
				List<String> tokens = db.getAllTokens();
				
				if (tokens.isEmpty()) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"No registered tokens found\"}");
					return;
				}
				
				try {
					String result = fcm.sendToTokens(tokens, title == null ? "" : title, body == null ? "" : body);
					out.write("{\"result\":\"" + escapeForJson(result) + "\"}");
				} catch (IllegalArgumentException e) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"" + escapeForJson(e.getMessage()) + "\"}");
				}
				return;
			}
			if ("/test-token".equals(path)) {
				String token = req.getParameter("token");
				if (token == null || token.isBlank()) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"token parameter required\"}");
					return;
				}
				
				// Test token validation
				boolean isValid = fcm.isValidFCMToken(token);
				out.write("{\"token\":\"" + escapeForJson(token) + "\",\"valid\":" + isValid + ",\"length\":" + token.length() + "}");
				return;
			}
			if ("/debug-token".equals(path)) {
				String token = req.getParameter("token");
				if (token == null || token.isBlank()) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"token parameter required\"}");
					return;
				}
				
				// Test token by sending a real FCM request
				try {
					List<String> tokens = List.of(token);
					String result = fcm.sendToTokens(tokens, "Debug Test", "Testing token validity");
					out.write("{\"result\":\"" + escapeForJson(result) + "\"}");
				} catch (Exception e) {
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":\"" + escapeForJson(e.getMessage()) + "\"}");
				}
				return;
			}
			if ("/test-service-account".equals(path)) {
				// Test service account access
				try {
					String accessToken = fcm.getAccessToken();
					out.write("{\"status\":\"success\",\"accessTokenLength\":" + accessToken.length() + "}");
				} catch (Exception e) {
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					out.write("{\"error\":\"" + escapeForJson(e.getMessage()) + "\"}");
				}
				return;
			}
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			out.write("{\"error\":\"not found\"}");
		}
		catch (SQLException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("{\"error\":\"db error\"}");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("{\"error\":\"fcm interrupted\"}");
		}
	}

	private String escapeForJson(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
