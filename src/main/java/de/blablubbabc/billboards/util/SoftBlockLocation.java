package de.blablubbabc.billboards.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SoftBlockLocation {

	private final String worldName; // not empty
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
		Validate.isTrue(!Utils.isEmpty(worldName), "World name is empty!");
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

	public Location getBukkitLocation() {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	public boolean isSameLocation(Location location) {
		if (location == null) return false;
		return location.getWorld().getName().equals(worldName) && location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
	}

	@Override
	public String toString() {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldName.hashCode();
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SoftBlockLocation)) return false;
		SoftBlockLocation other = (SoftBlockLocation) obj;
		if (!worldName.equals(other.worldName)) return false;
		if (x != other.x) return false;
		if (y != other.y) return false;
		if (z != other.z) return false;
		return true;
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
