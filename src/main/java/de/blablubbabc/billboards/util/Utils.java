package de.blablubbabc.billboards.util;

import java.util.UUID;

import org.bukkit.Material;

public class Utils {

	private Utils() {
	}

	public static boolean isEmpty(String string) {
		return (string == null || string.isEmpty());
	}

	public static boolean isSign(Material material) {
		if (material == null) return false;
		return material.data == org.bukkit.block.data.type.Sign.class || material.data == org.bukkit.block.data.type.WallSign.class;
	}

	public static Integer parseInteger(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static UUID parseUUID(String string) {
		if (string == null) return null;
		try {
			return UUID.fromString(string);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static long addSaturated(long x, long y) {
		try {
			return Math.addExact(x, y);
		} catch (ArithmeticException e) {
			if (y > 0) {
				return Long.MAX_VALUE;
			} else {
				return Long.MIN_VALUE;
			}
		}
	}
}
