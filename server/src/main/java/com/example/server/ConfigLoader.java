package com.example.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigLoader {
	private static final Properties PROPS = new Properties();
	private static volatile boolean loaded = false;

	private ConfigLoader() {}

	public static synchronized void loadOnce() {
		if (loaded) return;
		try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
			if (is == null) {
				throw new IllegalStateException("Missing config.properties in resources");
			}
			PROPS.load(is);
			loaded = true;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config.properties", e);
		}
	}

	public static String get(String key) {
		if (!loaded) loadOnce();
		String v = PROPS.getProperty(key);
		if (v == null) throw new IllegalStateException("Missing config key: " + key);
		return v;
	}
}
