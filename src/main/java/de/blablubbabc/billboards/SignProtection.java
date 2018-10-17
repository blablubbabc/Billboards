package de.blablubbabc.billboards;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import de.blablubbabc.billboards.util.SoftBlockLocation;

public class SignProtection implements Listener {

	private final BillboardsPlugin plugin;

	SignProtection(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	void onPluginEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	void onPluginDisable() {
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
}
