package de.blablubbabc.billboards;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

public class BillboardCommands implements CommandExecutor {

	private final BillboardsPlugin plugin;

	BillboardCommands(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Messages.getMessage(Message.ONLY_AS_PLAYER));
			return true;
		}
		Player player = (Player) sender;

		// check permission:
		boolean hasAdminPermission = player.hasPermission(BillboardsPlugin.ADMIN_PERMISSION);
		if (!hasAdminPermission && !player.hasPermission(BillboardsPlugin.CREATE_PERMISSION)) {
			player.sendMessage(Messages.getMessage(Message.NO_PERMISSION));
			return true;
		}

		if (args.length > 3) {
			return false;
		}

		// get targeted sign:
		Block targetBlock = player.getTargetBlock((Set<Material>) null, 10);
		if (targetBlock == null || !Utils.isSignBlock(targetBlock.getType())) {
			player.sendMessage(Messages.getMessage(Message.NO_TARGETED_SIGN));
			return true;
		}
		Location blockLocation = targetBlock.getLocation();

		// already a billboard sign?
		if (plugin.getBillboard(blockLocation) != null) {
			player.sendMessage(Messages.getMessage(Message.ALREADY_BILLBOARD_SIGN));
			return true;
		}

		// create new billboard:
		int duration = plugin.defaultDurationDays;
		int price = plugin.defaultPrice;

		// /billboard [<price> <duration>] [creator]
		if (args.length >= 2) {
			Integer priceArgument = Utils.parseInteger(args[0]);
			if (priceArgument == null) {
				player.sendMessage(Messages.getMessage(Message.INVALID_NUMBER, args[0]));
				return true;
			}
			Integer durationArgument = Utils.parseInteger(args[1]);
			if (durationArgument == null) {
				player.sendMessage(Messages.getMessage(Message.INVALID_NUMBER, args[1]));
				return true;
			}
			price = priceArgument.intValue();
			duration = durationArgument.intValue();
		}

		String creator = hasAdminPermission ? null : player.getName();

		if (args.length == 1 || args.length == 3) {
			if (!hasAdminPermission) {
				player.sendMessage(Messages.getMessage(Message.NO_PERMISSION));
				return true;
			}
			creator = args[args.length == 1 ? 0 : 2];
		}

		// add and setup billboard sign:
		BillboardSign billboard = new BillboardSign(new SoftBlockLocation(blockLocation), creator, null, duration, price, 0);
		plugin.addBillboard(billboard);
		if (plugin.refreshSign(billboard)) {
			plugin.saveSigns();
		}

		player.sendMessage(Messages.getMessage(Message.ADDED_SIGN, String.valueOf(price), String.valueOf(duration), billboard.getCreatorName()));
		return true;
	}

}
