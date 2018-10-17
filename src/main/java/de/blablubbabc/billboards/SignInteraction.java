package de.blablubbabc.billboards;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

import net.milkbowl.vault.economy.EconomyResponse;

public class SignInteraction implements Listener {

	private final BillboardsPlugin plugin;
	// player name -> currently interacting billboard sign
	public final Map<String, BillboardSign> confirmations = new HashMap<String, BillboardSign>();
	private SimpleDateFormat dateFormat;

	SignInteraction(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	void onPluginEnable() {
		dateFormat = new SimpleDateFormat(Messages.getMessage(Message.DATE_FORMAT));
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	void onPluginDisable() {
		this.confirmations.clear();
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
				ItemStack itemInHand = event.getItem();
				if (itemInHand != null && itemInHand.getType() == Material.SIGN && billboard.canEdit(player)) {
					// do not cancel, so that the block place event is called that initializes sign editing:
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

	@EventHandler
	void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		confirmations.remove(player.getName());
	}
}
