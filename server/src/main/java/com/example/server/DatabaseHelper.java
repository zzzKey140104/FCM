package com.example.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
	private final String url;
	private final String user;
	private final String password;

	public DatabaseHelper() {
		ConfigLoader.loadOnce();
		this.url = ConfigLoader.get("db.url");
		this.user = ConfigLoader.get("db.user");
		this.password = ConfigLoader.get("db.password");
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	public void upsertDeviceToken(String token, String label) throws SQLException {
		String sql = "INSERT INTO device(token, label) VALUES(?, ?) ON DUPLICATE KEY UPDATE label = VALUES(label)";
		try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, token);
			ps.setString(2, label);
			ps.executeUpdate();
		}
	}

	public List<String> getAllTokens() throws SQLException {
		String sql = "SELECT token FROM device";
		List<String> tokens = new ArrayList<>();
		try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					tokens.add(rs.getString(1));
				}
			}
		}
		return tokens;
	}

	public List<DeviceDto> getAllDevices() throws SQLException {
		String sql = "SELECT id, token, COALESCE(label, '') FROM device ORDER BY id DESC";
		List<DeviceDto> devices = new ArrayList<>();
		try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					devices.add(new DeviceDto(rs.getInt(1), rs.getString(2), rs.getString(3)));
				}
			}
		}
		return devices;
	}

	public List<String> getTokensByIds(List<Integer> ids) throws SQLException {
		if (ids == null || ids.isEmpty()) return List.of();
		StringBuilder placeholders = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) placeholders.append(',');
			placeholders.append('?');
		}
		String sql = "SELECT token FROM device WHERE id IN (" + placeholders + ")";
		List<String> tokens = new ArrayList<>();
		try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			for (int i = 0; i < ids.size(); i++) {
				ps.setInt(i + 1, ids.get(i));
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					tokens.add(rs.getString(1));
				}
			}
		}
		return tokens;
	}
}
