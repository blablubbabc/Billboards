package de.blablubbabc.billboards;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class SoftLocation {

	private String worldName;
	private int x;
	private int y;
	private int z;

	public SoftLocation(String worldName, int x, int y, int z) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public Location getBukkitLocation(Plugin plugin) {
		World world = plugin.getServer().getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	public boolean isSameLocation(Location location) {
		if (location == null) return false;
		return location.getWorld().getName().equals(worldName) && location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
	}

	public String toString() {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	// statics

	public static List<SoftLocation> getFromStringList(List<String> strings) {
		List<SoftLocation> softLocs = new ArrayList<SoftLocation>();
		for (String s : strings) {
			SoftLocation soft = getFromString(s);
			if (soft != null) softLocs.add(soft);
		}
		return softLocs;
	}

	public static List<String> toStringList(List<SoftLocation> softLocs) {
		List<String> strings = new ArrayList<String>();
		for (SoftLocation soft : softLocs) {
			if (soft != null) strings.add(soft.toString());
		}
		return strings;
	}

	public static SoftLocation getFromString(String string) {
		if (string == null) return null;
		String[] split = string.split(";");
		if (split.length != 4) return null;
		String worldName = split[0];
		if (worldName == null) return null;
		Integer x = parseInteger(split[1]);
		if (x == null) return null;
		Integer y = parseInteger(split[2]);
		if (y == null) return null;
		Integer z = parseInteger(split[3]);
		if (z == null) return null;
		return new SoftLocation(worldName, x, y, z);
	}

	private static Integer parseInteger(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return null;
		}
	}
}