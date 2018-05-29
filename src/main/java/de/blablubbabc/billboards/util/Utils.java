package de.blablubbabc.billboards.util;

import org.bukkit.Material;

public class Utils {

	private Utils() {
	}

	public static boolean isSignBlock(Material material) {
		return (material == Material.SIGN_POST || material == Material.WALL_SIGN);
	}

	public static String trimTo16(String input) {
		return input.length() > 16 ? input.substring(0, 16) : input;
	}

	public static Integer parseInteger(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
