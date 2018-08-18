package de.blablubbabc.billboards;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SignEdit {

	public final BillboardSign billboard;
	public final Location source;
	public final ItemStack originalSignItem;
	public final EquipmentSlot originalHand;

	public SignEdit(BillboardSign billboard, Location source, ItemStack originalSignItem, EquipmentSlot originalHand) {
		Validate.notNull(billboard, "Billboard is null!");
		Validate.notNull(source, "Source location is null!");
		Validate.isTrue(originalSignItem != null && originalSignItem.getType() == Material.SIGN, "Not a sign item!");
		Validate.notNull(originalHand, "Original hand is null!");
		this.billboard = billboard;
		this.source = source;
		this.originalSignItem = originalSignItem;
		this.originalHand = originalHand;
	}
}
