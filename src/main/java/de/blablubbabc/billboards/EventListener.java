package de.blablubbabc.billboards;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

import net.milkbowl.vault.economy.EconomyResponse;

public class EventListener implements Listener {

	private final BillboardsPlugin plugin;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(Messages.getMessage(Message.DATE_FORMAT));

	// player name -> editing information
	private final Map<String, SignEdit> editing = new HashMap<String, SignEdit>();
	// player name -> currently interacting billboard sign
	public final Map<String, BillboardSign> confirmations = new HashMap<String, BillboardSign>();

	EventListener(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onBlockBreak(BlockBreakEvent event) {
		// only allow breaking if has permission and is sneaking
		Player player = event.getPlayer();
		Block block = event.getBlock();
		SoftBlockLocation blockLocation = new SoftBlockLocation(block);
		BillboardSign billboard = plugin.getBillboard(blockLocation);
		if (billboard == null) return;
		if (!plugin.refreshSign(billboard)) return; // billboard is no longer valid

		boolean breakFailed = false;
		if (!player.isSneaking()) {
			breakFailed = true;
			player.sendMessage(Messages.getMessage(Message.YOU_HAVE_TO_SNEAK));
		} else if (!billboard.canBreak(player)) {
			breakFailed = true;
			player.sendMessage(Messages.getMessage(Message.NO_PERMISSION));
		}

		if (breakFailed) {
			event.setCancelled(true);
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				// refresh sign to display text:
				plugin.refreshSign(billboard);
			});
		} else {
			// remove billboard:
			plugin.removeBillboard(billboard);
			plugin.saveBillboards();
			player.sendMessage(Messages.getMessage(Message.SIGN_REMOVED));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Player player = event.getPlayer();
		String playerName = player.getName();

		// reset confirmation status:
		BillboardSign confirmationBillboard = confirmations.remove(playerName);
		if (!Utils.isSignBlock(clickedBlock.getType())) return; // not a sign

		SoftBlockLocation blockLocation = new SoftBlockLocation(clickedBlock);
		BillboardSign billboard = plugin.getBillboard(blockLocation);
		if (billboard == null || !plugin.refreshSign(billboard)) return; // not a valid billboard sign

		// cancel all block-placing against a billboard sign already here:
		event.setCancelled(true);

		// can rent?
		if (!player.hasPermission(BillboardsPlugin.RENT_PERMISSION)) {
			player.sendMessage(Messages.getMessage(Message.NO_PERMISSION));
			return;
		}
		// own sign?
		if (billboard.isCreator(player)) {
			player.sendMessage(Messages.getMessage(Message.CANT_RENT_OWN_SIGN));
			return;
		}

		if (confirmationBillboard != null && confirmationBillboard == billboard) {
			// check if it's still available:
			if (billboard.hasOwner()) {
				// no longer available:
				player.sendMessage(Messages.getMessage(Message.NO_LONGER_AVAILABLE));
				return;
			}

			// check if player has enough money:
			if (!BillboardsPlugin.economy.has(player, billboard.getPrice())) {
				// not enough money:
				player.sendMessage(Messages.getMessage(Message.NOT_ENOUGH_MONEY, String.valueOf(billboard.getPrice()), String.valueOf(BillboardsPlugin.economy.getBalance(player))));
				return;
			}

			// rent:
			// take money:
			EconomyResponse withdraw = BillboardsPlugin.economy.withdrawPlayer(player, billboard.getPrice());
			// transaction successful ?
			if (!withdraw.transactionSuccess()) {
				// something went wrong
				player.sendMessage(Messages.getMessage(Message.TRANSACTION_FAILURE, withdraw.errorMessage));
				return;
			}

			if (billboard.hasCreator()) {
				// give money to the creator:
				OfflinePlayer creatorPlayer = Bukkit.getOfflinePlayer(billboard.getCreatorUUID());
				// note: OfflinePlayer.getName() will return null if the player's last known name is unknown to the
				// server
				EconomyResponse deposit = BillboardsPlugin.economy.depositPlayer(creatorPlayer, billboard.getPrice());
				// transaction successful ?
				if (!deposit.transactionSuccess()) {
					// something went wrong :(
					player.sendMessage(Messages.getMessage(Message.TRANSACTION_FAILURE, deposit.errorMessage));

					// try to refund the withdraw
					EconomyResponse withdrawUndo = BillboardsPlugin.economy.depositPlayer(player, withdraw.amount);
					if (!withdrawUndo.transactionSuccess()) {
						// this is really bad:
						player.sendMessage(Messages.getMessage(Message.TRANSACTION_FAILURE, withdrawUndo.errorMessage));
					}
					player.updateInventory();
					return;
				}
			}
			player.updateInventory();

			// set new owner:
			billboard.setOwner(player);
			billboard.setStartTime(System.currentTimeMillis());
			plugin.saveBillboards();

			// initialize new sign text:
			Sign sign = (Sign) clickedBlock.getState();
			String[] msgArgs = billboard.getMessageArgs();
			sign.setLine(0, Messages.getMessage(Message.RENT_SIGN_LINE_1, msgArgs));
			sign.setLine(1, Messages.getMessage(Message.RENT_SIGN_LINE_2, msgArgs));
			sign.setLine(2, Messages.getMessage(Message.RENT_SIGN_LINE_3, msgArgs));
			sign.setLine(3, Messages.getMessage(Message.RENT_SIGN_LINE_4, msgArgs));
			sign.update();

			player.sendMessage(Messages.getMessage(Message.YOU_HAVE_RENT_A_SIGN, msgArgs));
		} else {
			// check if available:
			if (!billboard.hasOwner()) {
				// check if the player already owns to many billboards:
				if (plugin.maxBillboardsPerPlayer >= 0 && plugin.getRentBillboards(player.getUniqueId()).size() >= plugin.maxBillboardsPerPlayer) {
					player.sendMessage(Messages.getMessage(Message.MAX_RENT_LIMIT_REACHED, String.valueOf(plugin.maxBillboardsPerPlayer)));
					return;
				}

				// check if player has enough money:
				if (!BillboardsPlugin.economy.has(player, billboard.getPrice())) {
					// no enough money:
					player.sendMessage(Messages.getMessage(Message.NOT_ENOUGH_MONEY, String.valueOf(billboard.getPrice()),
							String.valueOf(BillboardsPlugin.economy.getBalance(player))));
					return;
				}

				// click again to rent:
				confirmations.put(playerName, billboard);
				player.sendMessage(Messages.getMessage(Message.CLICK_TO_RENT, billboard.getMessageArgs()));
			} else {
				// is owner -> edit
				if (event.getItem().getType() == Material.SIGN && billboard.canEdit(player)) {
					// do not cancel, so that the place event is called:
					event.setCancelled(false);
					return;
				}

				// print information of sign:
				player.sendMessage(Messages.getMessage(Message.INFO_HEADER));
				player.sendMessage(Messages.getMessage(Message.INFO_CREATOR, billboard.getMessageCreatorName(), billboard.getMessageCreatorUUID()));
				player.sendMessage(Messages.getMessage(Message.INFO_OWNER, billboard.getMessageOwnerName(), billboard.getMessageOwnerUUID()));
				player.sendMessage(Messages.getMessage(Message.INFO_PRICE, String.valueOf(billboard.getPrice())));
				player.sendMessage(Messages.getMessage(Message.INFO_DURATION, String.valueOf(billboard.getDurationInDays())));
				player.sendMessage(Messages.getMessage(Message.INFO_RENT_SINCE, dateFormat.format(new Date(billboard.getStartTime()))));

				long endTime = billboard.getEndTime();
				player.sendMessage(Messages.getMessage(Message.INFO_RENT_UNTIL, dateFormat.format(new Date(endTime))));

				long left = endTime - System.currentTimeMillis();
				long days = TimeUnit.MILLISECONDS.toDays(left);
				long hours = TimeUnit.MILLISECONDS.toHours(left) - TimeUnit.DAYS.toHours(days);
				long minutes = TimeUnit.MILLISECONDS.toMinutes(left) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours);
				String timeLeft = String.format(Messages.getMessage(Message.TIME_REMAINING_FORMAT), days, hours, minutes);

				player.sendMessage(Messages.getMessage(Message.INFO_TIME_LEFT, timeLeft));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onBlockPlaceEarly(BlockPlaceEvent event) {
		Block placedBlock = event.getBlockPlaced();
		if (!Utils.isSignBlock(placedBlock.getType())) return;
		// making sure the player is actually holding a sign, just in case:
		ItemStack itemInHand = event.getItemInHand();
		if (itemInHand == null || itemInHand.getType() != Material.SIGN) return;

		Block placedAgainstBlock = event.getBlockAgainst();
		if (!Utils.isSignBlock(placedAgainstBlock.getType())) return;

		SoftBlockLocation placedAgainstBlockLocation = new SoftBlockLocation(placedAgainstBlock);
		BillboardSign billboard = plugin.getBillboard(placedAgainstBlockLocation);
		if (billboard == null) return;

		Player player = event.getPlayer();
		String playerName = player.getName();

		// cancel event, so other plugins ignore it and don't print messages for canceling it:
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

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	void onSignEdit(SignChangeEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		SignEdit signEdit = editing.remove(playerName);
		if (signEdit == null) return;

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
					// some other plugin cancelled sign updating (ex. anti-swearing plugins):
				}
			}
		}

		// cancel and give sign item back:
		event.setCancelled(true);
		signEdit.source.getBlock().setType(Material.AIR);
		if (player.getGameMode() != GameMode.CREATIVE) {
			// return sign item:
			ItemStack signItem = signEdit.originalSignItem.clone();
			signItem.setAmount(1);

			PlayerInventory playerInventory = player.getInventory();
			if (!addItemToHand(playerInventory, signEdit.originalHand, signItem)) {
				// hand full: try to add to inventory:
				if (!playerInventory.addItem(signItem).isEmpty()) {
					// inventory full: drop the item:
					player.getWorld().dropItemNaturally(player.getEyeLocation(), signItem);
				}
			}
			player.updateInventory();
		}
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

	@EventHandler
	void onQuit(PlayerQuitEvent event) {
		confirmations.remove(event.getPlayer().getName());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.updateLastKnownName(player.getUniqueId(), player.getName());
	}
}
