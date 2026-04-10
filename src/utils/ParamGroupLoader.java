package utils;

import java.io.*;
import java.util.*;

/**
 * Loads per-city parameter groups for transport tuning.
 *
 * Reads city_code_to_param_group.csv to map city codes to param group names,
 * then loads the corresponding .properties files from param_groups/.
 *
 * At runtime, each city's trip generation uses its assigned parameter overlay.
 */
public class ParamGroupLoader {

	/** city_code -> param_group_name */
	private final Map<String, String> cityToGroup;

	/** param_group_name -> Properties overlay */
	private final Map<String, Properties> groupParams;

	private ParamGroupLoader(Map<String, String> cityToGroup,
							  Map<String, Properties> groupParams) {
		this.cityToGroup = cityToGroup;
		this.groupParams = groupParams;
	}

	/**
	 * Load param groups from the standard project layout.
	 *
	 * @param mappingCsv Path to city_code_to_param_group.csv
	 * @param paramGroupDir Directory containing {group_name}.properties files
	 * @return loaded ParamGroupLoader
	 */
	public static ParamGroupLoader load(String mappingCsv, String paramGroupDir) {
		Map<String, String> cityToGroup = new LinkedHashMap<>();
		Set<String> groupNames = new LinkedHashSet<>();

		// Read CSV mapping
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(mappingCsv), "UTF-8"))) {
			String line = br.readLine(); // skip header
			if (line != null && line.startsWith("\uFEFF")) {
				line = line.substring(1); // strip BOM
			}
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) continue;
				String[] parts = line.split(",", -1);
				if (parts.length < 2) continue;
				String cityCode = parts[0].trim();
				String group = parts[1].trim();
				if (!cityCode.isEmpty() && !group.isEmpty()) {
					cityToGroup.put(cityCode, group);
					groupNames.add(group);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load param group mapping: " + mappingCsv, e);
		}

		// Load each unique param group's properties
		Map<String, Properties> groupParams = new HashMap<>();
		int loaded = 0;
		for (String groupName : groupNames) {
			File propFile = new File(paramGroupDir, groupName + ".properties");
			if (propFile.exists()) {
				Properties p = new Properties();
				try (FileInputStream fis = new FileInputStream(propFile)) {
					p.load(fis);
				} catch (IOException e) {
					throw new RuntimeException(
						"Failed to load param group file: " + propFile, e);
				}
				groupParams.put(groupName, p);
				loaded++;
			}
			// Missing .properties file is OK during development —
			// getParamsForCity will return null, caller decides policy
		}

		System.out.println("[ParamGroupLoader] Loaded mapping: " + cityToGroup.size()
			+ " cities -> " + groupNames.size() + " groups ("
			+ loaded + " param files found)");

		return new ParamGroupLoader(cityToGroup, groupParams);
	}

	/**
	 * Get the param group name for a city code.
	 * @return group name, or null if city has no mapping
	 */
	public String getGroupName(String cityCode) {
		return cityToGroup.get(cityCode);
	}

	/**
	 * Get the parameter overlay Properties for a city code.
	 * @return Properties to overlay, or null if no group or no param file
	 */
	public Properties getParamsForCity(String cityCode) {
		String group = cityToGroup.get(cityCode);
		if (group == null) return null;
		return groupParams.get(group);
	}

	/**
	 * Create an overlaid Properties: base + city's param group on top.
	 * Returns a new Properties object; base is not modified.
	 */
	public Properties overlayForCity(Properties base, String cityCode) {
		Properties merged = new Properties();
		merged.putAll(base);
		Properties overlay = getParamsForCity(cityCode);
		if (overlay != null) {
			merged.putAll(overlay);
		}
		return merged;
	}

	/** Get all unique param group names that have loaded .properties files. */
	public Set<String> getLoadedGroups() {
		return Collections.unmodifiableSet(groupParams.keySet());
	}

	/** Get the total number of city mappings. */
	public int getCityCount() {
		return cityToGroup.size();
	}
}
