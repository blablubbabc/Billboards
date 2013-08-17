package de.blablubbabc.billboards;

import org.bukkit.Location;

public class SignEdit {
	public final Billboard billboard;
	public final Location source;
	
	public SignEdit(Location source, Billboard billboard) {
		this.source = source;
		this.billboard = billboard;
	}
}
