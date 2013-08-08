package de.blablubbabc.billboards;

import org.bukkit.Location;

public class SignEdit {
	public final AdSign adsign;
	public final Location source;
	
	public SignEdit(Location source, AdSign adsign) {
		this.source = source;
		this.adsign = adsign;
	}
}
