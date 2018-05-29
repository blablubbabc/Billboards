package de.blablubbabc.billboards.util;

import java.util.UUID;

import org.bukkit.Material;

public class Utils {

	private Utils() {
	}

	public static boolean isEmpty(String string) {
		return (string == null || string.isEmpty());
	}

	public static boolean isSignBlock(Material material) {
		return (material == Material.SIGN_POST || material == Material.WALL_SIGN);
	}

	public static Integer parseInteger(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static UUID parseUUID(String string) {
		try {
			return UUID.fromString(string);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
