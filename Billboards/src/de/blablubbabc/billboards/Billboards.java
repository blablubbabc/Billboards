package de.blablubbabc.billboards;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Billboards extends JavaPlugin {

	public static Billboards instance;
	public static Logger logger;
	public static Economy economy = null;
	public static String PERMISSION_ADMIN = "billboards.admin";
	public static String PERMISSION_PLAYER = "billboards.rent";
	
	private int defaultPrice = 10;
	private int defaultDurationDays = 7;
	
	public Map<String, AdSign> customers = new HashMap<String, AdSign>();
	
	private List<AdSign> signs = new ArrayList<AdSign>();
	
	@Override
	public void onEnable() {
		instance = this;
		logger = getLogger();
		if (!setupEconomy()) {
			logger.severe("No economy plugin was found! Disables now!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		// load messages
		Messages.loadMessages("plugins" + File.separator + "Billboards" + File.separator + "messages.yml");
		
		// load config and signs:
		loadConfig();
		
		// refresh signs:
		refreshAllSigns();
		
		// register listener
		getServer().getPluginManager().registerEvents(new EventListener(), this);
		
		// start refresh timer:
		getServer().getScheduler().runTaskTimer(this, new Runnable() {
			
			@Override
			public void run() {
				refreshAllSigns();
			}
		}, 20L, 20L * 60 * 10);
	}
	
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		instance = null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Messages.getMessage(Message.ONLY_AS_PLAYER));
			return true;
		}
		Player player = (Player) sender;
		if (!player.hasPermission(PERMISSION_ADMIN)) {
			player.sendMessage(Messages.getMessage(Message.NO_PERMISSION));
			return true;
		}
		Block block = player.getTargetBlock(null, 10);
		if (block == null || !(block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN)) {
			player.sendMessage(Messages.getMessage(Message.NO_TARGETED_SIGN));
		} else {
			Location loc = block.getLocation();
			if (getAdSign(loc) != null) {
				player.sendMessage(Messages.getMessage(Message.ALREADY_BILLBOARD_SIGN));
			} else {
				AdSign adsign = new AdSign(new SoftLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), null, defaultDurationDays, defaultPrice, 0);
				signs.add(adsign);
				refreshSign(adsign);
				saveCurrentConfig();
				
				player.sendMessage(Messages.getMessage(Message.ADDED_SIGN));
			}
		}
		return true;
	}
	
	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		
		return (economy != null);
	}
	
	public void removeAdSign(AdSign adsign) {
		signs.remove(adsign);
		saveCurrentConfig();
	}
	
	public AdSign getAdSign(Location loc) {
		for (AdSign adsign : signs) {
			if (adsign.getLocation().isSameLocation(loc)) return adsign;
		}
		return null;
	}
	
	// return true if the sign is still valid
	public boolean refreshSign(AdSign adsign) {
		if (!signs.contains(adsign)) {
			logger.warning("AdSign'" + adsign.getLocation().toString() + "' is no longer an valid billboard sign, but was refreshed.");
			return false;
		}
		
		Location location = adsign.getLocation().getBukkitLocation(this);
		if (location == null) {
			logger.warning("World '" + adsign.getLocation().getWorldName() + "' not found. Removing this billboard sign.");
			removeAdSign(adsign);
			return false;
		}
		
		Block block = location.getBlock();
		if (!(block.getState() instanceof Sign)) {
			logger.warning("AdSign'" + adsign.getLocation().toString() + "' is no longer a sign. Removing this billboard sign.");
			removeAdSign(adsign);
			return false;
		}
		
		// check rent time if has owner:
		if (adsign.hasOwner() && adsign.isRentOver()) {
			adsign.resetOwner();
		}
		// update text if has no owner:
		if (!adsign.hasOwner()) {
			Sign sign = (Sign) block.getState();
			updateText(adsign, sign);
		}
		
		return true;
	}
	
	private void updateText(AdSign adsign, Sign sign) {
		String line0 = Messages.getMessage(Message.SIGN_LINE_1);
		if (line0.length() >= 16) line0 = line0.substring(0, 16);
		
		String line1 = Messages.getMessage(Message.SIGN_LINE_2);
		if (line1.length() >= 16) line1 = line1.substring(0, 16);
		
		String line2 = Messages.getMessage(Message.SIGN_LINE_3, String.valueOf(adsign.getPrice()));
		if (line2.length() >= 16) line2 = line2.substring(0, 16);
		
		String line3 = Messages.getMessage(Message.SIGN_LINE_4, String.valueOf(adsign.getDurationInDays()));
		if (line3.length() >= 16) line3 = line3.substring(0, 16);
		
		sign.setLine(0, line0);
		sign.setLine(1, line1);
		sign.setLine(2, line2);
		sign.setLine(3, line3);
		sign.update();
	}
	
	public void refreshAllSigns() {
		List<AdSign> forRemoval = new ArrayList<AdSign>();
		for (AdSign adsign : signs) {
			Location location = adsign.getLocation().getBukkitLocation(this);
			if (location == null) {
				logger.warning("World '" + adsign.getLocation().getWorldName() + "' not found. Removing this billboard sign.");
				forRemoval.add(adsign);
				continue;
			}
			Block block = location.getBlock();
			if (!(block.getState() instanceof Sign)) {
				logger.warning("Billboard sign '" + adsign.getLocation().toString() + "' is no longer a sign. Removing this billboard sign.");
				forRemoval.add(adsign);
				continue;
			}
			
			// check rent time if has owner:
			if (adsign.hasOwner() && adsign.isRentOver()) {
				adsign.resetOwner();
			}
			// update text if has no owner:
			if (!adsign.hasOwner()) {
				Sign sign = (Sign) block.getState();
				updateText(adsign, sign);
			}
			
		}
		// remove invalid AdSigns:
		if (forRemoval.size() > 0) {
			for (AdSign adsign : forRemoval) {
				signs.remove(adsign);
			}
			saveCurrentConfig();
		}
	}
	
	public void loadConfig() {
		FileConfiguration config = getConfig();
		
		// load settings:
		ConfigurationSection settingsSection = config.getConfigurationSection("Settings");
		if (settingsSection != null) {
			defaultPrice = settingsSection.getInt("DefaultPrice", 10);
			defaultDurationDays = settingsSection.getInt("DefaultDurationInDays", 7);
		}
		
		// load signs:
		ConfigurationSection signsSection = config.getConfigurationSection("Signs");
		if (signsSection != null) {
			for (String softString : signsSection.getKeys(false)) {
				
				ConfigurationSection signSection = signsSection.getConfigurationSection(softString);
				if (signSection == null) {
					logger.warning("Couldn't load a sign section: " + softString);
					continue;
				}
				
				SoftLocation soft = SoftLocation.getFromString(softString);
				if (soft == null) {
					logger.warning("Couldn't load a signs location: " + softString);
					continue;
				}
				
				String owner = signSection.getString("Owner", null);
				int durationInDays = signSection.getInt("Duration", defaultDurationDays);
				int price = signSection.getInt("Price", defaultPrice);
				long startTime = signSection.getLong("StartTime", 0L);
				
				signs.add(new AdSign(soft, owner, durationInDays, price, startTime));
			}
		}
		
		// write changes back to config:
		saveCurrentConfig();
		
	}
	
	public void saveCurrentConfig() {
		FileConfiguration config = getConfig();
		// write settings to config:
		config.set("Settings.DefaultPrice", defaultPrice);
		config.set("Settings.DefaultDurationInDays", defaultDurationDays);
		// write signs to config:
		// first clear signs section:
		config.set("Signs", null);
		for (AdSign adsign : signs) {
			String node = "Signs." + adsign.getLocation().toString();
			config.set(node + ".Owner", adsign.getOwner());
			config.set(node + ".Duration", adsign.getDurationInDays());
			config.set(node + ".Price", adsign.getPrice());
			config.set(node + ".StartTime", adsign.getStartTime());
		}
		
		saveConfig();
	}
}
