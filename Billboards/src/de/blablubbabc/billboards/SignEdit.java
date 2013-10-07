package de.blablubbabc.billboards;

import org.bukkit.Location;

public class SignEdit {
	public final BillboardSign billboard;
	public final Location source;
	
	public SignEdit(Location source, BillboardSign billboard) {
		this.source = source;
		this.billboard = billboard;
	}
}
