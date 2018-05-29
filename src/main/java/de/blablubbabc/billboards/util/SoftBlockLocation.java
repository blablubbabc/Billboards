package de.blablubbabc.billboards.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

public class SoftBlockLocation {

	private final String worldName;
	private final int x;
	private final int y;
	private final int z;

	public SoftBlockLocation(Block block) {
		this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public SoftBlockLocation(Location location) {
		this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public SoftBlockLocation(String worldName, int x, int y, int z) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public String getWorldName() {
		return worldName;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
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

	public static List<SoftBlockLocation> getFromStringList(List<String> strings) {
		List<SoftBlockLocation> softLocs = new ArrayList<SoftBlockLocation>();
		for (String s : strings) {
			SoftBlockLocation soft = getFromString(s);
			if (soft != null) softLocs.add(soft);
		}
		return softLocs;
	}

	public static List<String> toStringList(List<SoftBlockLocation> softLocs) {
		List<String> strings = new ArrayList<String>();
		for (SoftBlockLocation soft : softLocs) {
			if (soft != null) strings.add(soft.toString());
		}
		return strings;
	}

	public static SoftBlockLocation getFromString(String string) {
		if (string == null) return null;
		String[] split = string.split(";");
		if (split.length != 4) return null;
		String worldName = split[0];
		if (worldName == null) return null;
		Integer x = Utils.parseInteger(split[1]);
		if (x == null) return null;
		Integer y = Utils.parseInteger(split[2]);
		if (y == null) return null;
		Integer z = Utils.parseInteger(split[3]);
		if (z == null) return null;
		return new SoftBlockLocation(worldName, x, y, z);
	}
}
