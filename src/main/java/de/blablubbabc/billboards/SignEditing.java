package de.blablubbabc.billboards;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

public class SignEditing implements Listener {

	private final BillboardsPlugin plugin;
	// player name -> editing information
	private final Map<String, SignEdit> editing = new HashMap<String, SignEdit>();

	SignEditing(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	void onPluginEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	void onPluginDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.endSignEdit(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onBlockPlaceEarly(BlockPlaceEvent event) {
		Block placedBlock = event.getBlockPlaced();
		if (!Utils.isSign(placedBlock.getType())) return;
		// making sure the player is actually holding a sign, just in case:
		ItemStack itemInHand = event.getItemInHand();
		if (itemInHand == null || !Utils.isSign(itemInHand.getType())) return;

		Block placedAgainstBlock = event.getBlockAgainst();
		if (!Utils.isSign(placedAgainstBlock.getType())) return;

		SoftBlockLocation placedAgainstBlockLocation = new SoftBlockLocation(placedAgainstBlock);
		BillboardSign billboard = plugin.getBillboard(placedAgainstBlockLocation);
		if (billboard == null) return;

		Player player = event.getPlayer();
		String playerName = player.getName();

		// Cancel the event early so that other plugins ignore it and don't print their cancellation messages:
		event.setCancelled(true);

		if (billboard.canEdit(player)) {
			editing.put(playerName, new SignEdit(billboard, placedBlock.getLocation(), itemInHand.clone(), event.getHand()));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onBlockPlaceLate(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();

		if (editing.containsKey(playerName)) {
			// make sure the sign can be placed, so that the sign edit window opens for the player
			event.setCancelled(false);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onSignEditEarly(SignChangeEvent event) {
		// We only handle the event early if bypassing of other plugins is enabled:
		if (!plugin.bypassSignChangeBlocking) return;

		Player player = event.getPlayer();
		String playerName = player.getName();
		if (!editing.containsKey(playerName)) return;

		// Cancel the event early so that other plugins ignore it and don't print their cancellation messages:
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	void onSignEditLate(SignChangeEvent event) {
		Player player = event.getPlayer();
		SignEdit signEdit = this.endSignEdit(player);
		if (signEdit == null) return; // player wasn't editing

		if (plugin.refreshSign(signEdit.billboard)) {
			// still owner and has still the permission?
			if (signEdit.billboard.canEdit(player) && player.hasPermission(BillboardsPlugin.RENT_PERMISSION)) {
				if (!event.isCancelled() || plugin.bypassSignChangeBlocking) {
					// update billboard sign content:
					Sign target = (Sign) signEdit.billboard.getLocation().getBukkitLocation().getBlock().getState();
					for (int i = 0; i < 4; i++) {
						target.setLine(i, event.getLine(i));
					}
					target.update();
				} else {
					// some other plugin cancelled sign updating (ex. anti-swearing plugins)
				}
			}
		}

		// cancel event:
		event.setCancelled(true);
	}

	private static boolean addItemToHand(PlayerInventory playerInventory, EquipmentSlot hand, ItemStack itemStack) {
		if (hand == EquipmentSlot.HAND) {
			// add to main hand:
			ItemStack itemInMainHand = playerInventory.getItemInMainHand();
			if (itemInMainHand == null || itemInMainHand.getType() == Material.AIR) {
				playerInventory.setItemInMainHand(itemStack);
				return true;
			} else if (itemStack.isSimilar(itemInMainHand) && itemInMainHand.getAmount() < itemInMainHand.getMaxStackSize()) {
				itemInMainHand.setAmount(itemInMainHand.getAmount() + 1);
				return true;
			}
		} else if (hand == EquipmentSlot.OFF_HAND) {
			// add to off hand:
			ItemStack itemInOffHand = playerInventory.getItemInOffHand();
			if (itemInOffHand == null || itemInOffHand.getType() == Material.AIR) {
				playerInventory.setItemInOffHand(itemStack);
				return true;
			} else if (itemStack.isSimilar(itemInOffHand) && itemInOffHand.getAmount() < itemInOffHand.getMaxStackSize()) {
				itemInOffHand.setAmount(itemInOffHand.getAmount() + 1);
				return true;
			}
		}
		// couldn't add the item:
		return false;
	}

	// returns null if the player was editing
	public SignEdit endSignEdit(Player player) {
		String playerName = player.getName();
		SignEdit signEdit = editing.remove(playerName);
		if (signEdit == null) return null;

		// remove editing sign:
		signEdit.source.getBlock().setType(Material.AIR);

		// give sign item back:
		if (player.getGameMode() != GameMode.CREATIVE) {
			// return sign item:
			ItemStack signItem = signEdit.originalSignItem.clone();
			signItem.setAmount(1);

			PlayerInventory playerInventory = player.getInventory();
			if (!addItemToHand(playerInventory, signEdit.originalHand, signItem)) {
				// hand full: try to add to inventory:
				if (!playerInventory.addItem(signItem).isEmpty()) {
					// inventory full: drop the item:
					Block block = signEdit.source.getBlock();
					block.getWorld().dropItem(block.getLocation().add(0.5D, 0.5D, 0.5D), signItem);
				}
			}
			player.updateInventory();
		}
		return signEdit;
	}

	@EventHandler
	void onQuit(PlayerQuitEvent event) {
		this.endSignEdit(event.getPlayer());
	}
}
