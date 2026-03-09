package utils;

import java.io.*;
import java.util.Properties;

/**
 * Layered configuration loader.
 *
 * Precedence (lowest to highest):
 * 1. config.properties              — base defaults (committed)
 * 2. config.pref.{N}.properties     — prefecture-specific overrides (committed)
 * 3. config.local.properties        — machine-specific overrides (gitignored)
 * 4. -Dconfig.file=/path/to/file    — external override (highest priority)
 */
public class ConfigLoader {

	public static Properties load() {
		return load(null, -1);
	}

	public static Properties load(int prefCode) {
		return load(null, prefCode);
	}

	public static Properties load(Class<?> caller, int prefCode) {
		ClassLoader cl = caller != null ? caller.getClassLoader()
				: ConfigLoader.class.getClassLoader();
		Properties prop = new Properties();

		// Layer 1: base defaults
		loadFromClasspath(cl, "config.properties", prop, true);

		// Layer 2: prefecture-specific overrides
		if (prefCode > 0) {
			String prefFile = "config.pref." + prefCode + ".properties";
			if (loadFromClasspath(cl, prefFile, prop, false)) {
				System.out.println("Loaded prefecture config: " + prefFile);
			}
		}

		// Layer 3: machine-specific local overrides
		if (loadFromClasspath(cl, "config.local.properties", prop, false)) {
			System.out.println("Loaded local config: config.local.properties");
		}

		// Layer 4: external file via system property
		String extFile = System.getProperty("config.file");
		if (extFile != null) {
			try (InputStream is = new FileInputStream(extFile)) {
				prop.load(is);
				System.out.println("Loaded external config: " + extFile);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load " + extFile, e);
			}
		}

		return prop;
	}

	private static boolean loadFromClasspath(ClassLoader cl, String name, Properties prop, boolean required) {
		try (InputStream is = cl.getResourceAsStream(name)) {
			if (is == null) {
				if (required) {
					throw new FileNotFoundException(name + " not found in classpath");
				}
				return false;
			}
			prop.load(is);
			return true;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load " + name, e);
		}
	}
}
