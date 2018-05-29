package de.blablubbabc.billboards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

import net.milkbowl.vault.economy.Economy;

public class BillboardsPlugin extends JavaPlugin {

	private static BillboardsPlugin instance;

	public BillboardsPlugin getInstance() {
		return instance;
	}

	public static Economy economy = null;
	public static final String ADMIN_PERMISSION = "billboards.admin";
	public static final String RENT_PERMISSION = "billboards.rent";
	public static final String CREATE_PERMISSION = "billboards.create";

	private static final String SIGNS_DATA_FILE = "signs.yml";
	private static final String SIGNS_DATA_FILE_ENCODING = "UTF-8";

	// settings:
	public int defaultPrice = 10;
	public int defaultDurationDays = 7;
	public int maxRent = -1; // no limit by default
	public boolean bypassSignChangeBlocking = false;

	// data:
	private final Set<BillboardSign> signs = new LinkedHashSet<>();

	@Override
	public void onEnable() {
		instance = this;
		if (!setupEconomy()) {
			this.getLogger().severe("No economy plugin was found! Disables now!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// load messages:
		Messages.loadMessages("plugins" + File.separator + "Billboards" + File.separator + "messages.yml", this.getLogger());

		// load config (and write defaults back to file):
		this.reloadConfig();

		// loads signs:
		this.loadSigns();

		// register listener:
		Bukkit.getPluginManager().registerEvents(new EventListener(this), this);

		// register command handler:
		BillboardCommands commands = new BillboardCommands(this);
		this.getCommand("billboard").setExecutor(commands);

		// start refresh timer:
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

			@Override
			public void run() {
				refreshAllSigns();
			}
		}, 5L, 20L * 60 * 10);
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		instance = null;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	// BILLBOARDS

	public void addBillboard(BillboardSign billboard) {
		signs.add(billboard);
	}

	public void removeBillboard(BillboardSign billboard) {
		signs.remove(billboard);
	}

	public BillboardSign getBillboard(Location loc) {
		for (BillboardSign billboard : signs) {
			if (billboard.getLocation().isSameLocation(loc)) return billboard;
		}
		return null;
	}

	public List<BillboardSign> getRentBillboards(String playerName) {
		List<BillboardSign> playerSigns = new ArrayList<BillboardSign>();
		for (BillboardSign sign : signs) {
			if (sign.getOwnerName().equals(playerName)) {
				playerSigns.add(sign);
			}
		}
		return playerSigns;
	}

	// return true if the sign is still valid
	public boolean refreshSign(BillboardSign billboard) {
		if (!signs.contains(billboard)) {
			this.getLogger().warning("Billboard '" + billboard.getLocation().toString() + "' is no longer an valid billboard sign, but was refreshed.");
			return false;
		}

		Location location = billboard.getLocation().getBukkitLocation(this);
		if (location == null) {
			this.getLogger().warning("World '" + billboard.getLocation().getWorldName() + "' not found. Removing this billboard sign.");
			this.removeBillboard(billboard);
			this.saveSigns();
			return false;
		}

		Block block = location.getBlock();
		Material type = block.getType();
		if (type != Material.WALL_SIGN && type != Material.SIGN_POST) {
			this.getLogger().warning("Billboard '" + billboard.getLocation().toString() + "' is no longer a sign. Removing this billboard sign.");
			this.removeBillboard(billboard);
			this.saveSigns();
			return false;
		}

		// check rent time if it has an owner:
		if (billboard.hasOwner() && billboard.isRentOver()) {
			billboard.resetOwner();
		}
		// update text if it has no owner:
		if (!billboard.hasOwner()) {
			Sign sign = (Sign) block.getState();
			this.setRentableText(billboard, sign);
		}

		return true;
	}

	private void setRentableText(BillboardSign billboard, Sign sign) {
		String[] args = new String[] { String.valueOf(billboard.getPrice()), String.valueOf(billboard.getDurationInDays()), billboard.getCreatorName() };

		sign.setLine(0, Utils.trimTo16(Messages.getMessage(Message.SIGN_LINE_1, args)));
		sign.setLine(1, Utils.trimTo16(Messages.getMessage(Message.SIGN_LINE_2, args)));
		sign.setLine(2, Utils.trimTo16(Messages.getMessage(Message.SIGN_LINE_3, args)));
		sign.setLine(3, Utils.trimTo16(Messages.getMessage(Message.SIGN_LINE_4, args)));
		sign.update();
	}

	public void refreshAllSigns() {
		List<BillboardSign> forRemoval = new ArrayList<BillboardSign>();
		for (BillboardSign billboard : signs) {
			Location location = billboard.getLocation().getBukkitLocation(this);
			if (location == null) {
				this.getLogger().warning("World '" + billboard.getLocation().getWorldName() + "' not found. Removing this billboard sign.");
				forRemoval.add(billboard);
				continue;
			}
			Block block = location.getBlock();
			if (!(block.getState() instanceof Sign)) {
				this.getLogger().warning("Billboard sign '" + billboard.getLocation().toString() + "' is no longer a sign. Removing this billboard sign.");
				forRemoval.add(billboard);
				continue;
			}

			// check rent time if has owner:
			if (billboard.hasOwner() && billboard.isRentOver()) {
				billboard.resetOwner();
			}
			// update text if has no owner:
			if (!billboard.hasOwner()) {
				Sign sign = (Sign) block.getState();
				setRentableText(billboard, sign);
			}

		}
		// remove invalid billboards:
		if (forRemoval.size() > 0) {
			for (BillboardSign billboard : forRemoval) {
				this.removeBillboard(billboard);
			}
			this.saveSigns();
		}
	}

	@Override
	public void reloadConfig() {
		super.reloadConfig();
		// load settings:
		FileConfiguration config = this.getConfig();
		ConfigurationSection settingsSection = config.getConfigurationSection("Settings");
		if (settingsSection != null) {
			defaultPrice = settingsSection.getInt("DefaultPrice", 10);
			defaultDurationDays = settingsSection.getInt("DefaultDurationInDays", 7);
			maxRent = settingsSection.getInt("MaxRentPerPlayer", -1);
			bypassSignChangeBlocking = settingsSection.getBoolean("BypassSignChangeBlocking", false);
		}
		// write changes back to config:
		this.saveConfig();
	}

	@Override
	public void saveConfig() {
		// write current settings to config:
		FileConfiguration config = this.getConfig();
		config.set("Settings.DefaultPrice", defaultPrice);
		config.set("Settings.DefaultDurationInDays", defaultDurationDays);
		config.set("Settings.MaxRentPerPlayer", maxRent);
		config.set("Settings.BypassSignChangeBlocking", bypassSignChangeBlocking);
		super.saveConfig();
	}

	private File getSignsDataFile() {
		return new File(this.getDataFolder(), SIGNS_DATA_FILE);
	}

	private void loadSigns() {
		// loads signs data from file:
		File signsDataFile = this.getSignsDataFile();
		YamlConfiguration signsData = new YamlConfiguration();
		try (	FileInputStream stream = new FileInputStream(signsDataFile);
				InputStreamReader reader = new InputStreamReader(stream, SIGNS_DATA_FILE_ENCODING)) {
			signsData.load(reader);
		} catch (FileNotFoundException ex) {
			// ignore
		} catch (Exception e) {
			this.getLogger().log(Level.SEVERE, "Failed to load signs data file!", e);
			return;
		}

		// unload currently loaded signs:
		signs.clear();

		// freshly load signs:
		for (String signLocationString : signsData.getKeys(false)) {
			ConfigurationSection signSection = signsData.getConfigurationSection(signLocationString);
			if (signSection == null) {
				this.getLogger().warning("Couldn't load sign (invalid config section): " + signLocationString);
				continue;
			}

			SoftBlockLocation signLocation = SoftBlockLocation.getFromString(signLocationString);
			if (signLocation == null) {
				this.getLogger().warning("Couldn't load sign (invalid location): " + signLocationString);
				continue;
			}

			String creator = signSection.getString("Creator", null);
			String owner = signSection.getString("Owner", null);
			int durationInDays = signSection.getInt("Duration", defaultDurationDays);
			int price = signSection.getInt("Price", defaultPrice);
			long startTime = signSection.getLong("StartTime", 0L);

			signs.add(new BillboardSign(signLocation, creator, owner, durationInDays, price, startTime));
		}
	}

	public boolean saveSigns() {
		YamlConfiguration signsData = new YamlConfiguration();
		// store signs in signs data config:
		for (BillboardSign billboard : signs) {
			String node = "Signs." + billboard.getLocation().toString();
			signsData.set(node + ".Creator", billboard.getCreatorName());
			signsData.set(node + ".Owner", billboard.getOwnerName());
			signsData.set(node + ".Duration", billboard.getDurationInDays());
			signsData.set(node + ".Price", billboard.getPrice());
			signsData.set(node + ".StartTime", billboard.getStartTime());
		}

		// save signs data to file:
		File signsDataFile = this.getSignsDataFile();
		File parent = signsDataFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		String data = signsData.saveToString();
		try (	FileOutputStream stream = new FileOutputStream(signsDataFile);
				OutputStreamWriter writer = new OutputStreamWriter(stream, SIGNS_DATA_FILE_ENCODING)) {
			writer.write(data);
		} catch (Exception e) {
			this.getLogger().log(Level.SEVERE, "Failed to save signs data file!", e);
			return false;
		}
		return true;
	}
}
