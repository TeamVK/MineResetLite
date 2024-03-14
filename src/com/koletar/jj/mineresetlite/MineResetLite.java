package com.koletar.jj.mineresetlite;

import com.koletar.jj.mineresetlite.commands.MineCommands;
import com.koletar.jj.mineresetlite.commands.PluginCommands;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.vk2gpz.mc.material.MaterialUtil;
import com.vk2gpz.mc.plugin.Updater;
import com.vk2gpz.mineresetlite.listeners.*;
import com.vk2gpz.mineresetlite.util.MRLUtil;
import com.vk2gpz.vklib.logging.ColorConsoleLogger;
import com.vk2gpz.vklib.text.WildcardUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * @author jjkoletar
 * @author vk2gpz
 */
public class MineResetLite extends JavaPlugin {
	@SuppressWarnings("unused")
	public static final String POLYMART_DATA = "%%__DATA__%%";
	
	public List<Mine> mines;
	private CommandManager commandManager;
	private WorldEditPlugin worldEdit = null;
	private int saveTaskId = -1;
	private int resetTaskId = -1;
	private Updater updater;
	
	static {
		ConfigurationSerialization.registerClass(Mine.class);
		MRLUtil.registerClasses();
	}
	
	public static Mine findMine(@NotNull Location location) {
		return getInstance()
				.getMines()
				.stream()
				.filter(mi -> mi.isInside(location))
				.findFirst()
				.orElse(null);
	}
	
	public static Mine[] findMines(String in) {
		return getInstance().matchMines(in);
	}
	
	private static class IsMineFile implements FilenameFilter {
		public boolean accept(File file, String s) {
			return s.contains(".mine.yml");
		}
	}
	
	private static MineResetLite INSTANCE;
	
	public static MineResetLite getInstance() {
		return INSTANCE;
	}
	
	public void onEnable() {
		INSTANCE = this;
		ColorConsoleLogger.initLogger(getLogger());
		mines = new ArrayList<>();
		if (!setupConfig()) {
			getLogger().severe("Since I couldn't setup config files properly, I guess this is goodbye. ");
			getLogger().severe("Plugin Loading Aborted!");
			return;
		}
		commandManager = new CommandManager();
		commandManager.register(MineCommands.class, new MineCommands(this));
		commandManager.register(CommandManager.class, commandManager);
		commandManager.register(PluginCommands.class, new PluginCommands(this));
		Locale locale = new Locale(Config.getLocale());
		Phrases.getInstance().initialize(locale);
		File overrides = new File(getDataFolder(), "phrases.properties");
		if (overrides.exists()) {
			Properties overridesProps = new Properties();
			try {
				overridesProps.load(Files.newInputStream(overrides.toPath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			Phrases.getInstance().overrides(overridesProps);
		}
		// Look for worldedit
		if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
			worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
		}
		
		// All you have to do is adding this line in your onEnable method:
		// Metrics metrics = new Metrics(this);
		
		// Optional: Add custom charts
		// metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
		
		// Load mines
		File[] mineFiles = new File(getDataFolder(), "mines").listFiles(new IsMineFile());
		assert mineFiles != null;
		for (File file : mineFiles) {
			getLogger().info("Loading mine from file '" + file.getName() + "'...");
			try {
				FileConfiguration fileConf = YamlConfiguration.loadConfiguration(file);
				Object o = fileConf.get("mine");
				if (!(o instanceof Mine)) {
					getLogger().severe("Mine wasn't a mine object! Something is off with serialization!");
					continue;
				}
				Mine mine = (Mine) o;
				mines.add(mine);
				if (Config.getMineResetAtStart()) {
					mine.reset();
				}
			} catch (Throwable t) {
				t.printStackTrace();
				getLogger().severe("Unable to load mine!");
			}
		}
		MRLUtil.updateMine();
		resetTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (Mine mine : mines) {
				mine.cron();
			}
		}, 60 * 20L, 60 * 20L);
		// Check for updates
		
		if (!getDescription().getVersion().contains("dev")) {
			updater = new Updater(this);
			updater.addSite(Updater.UpdaterType.GITHUB, "https://api.github.com/repos/TeamVK/MineResetLite/releases");
			updater.addSite(Updater.UpdaterType.POLYMART, "https://api.polymart.org/v1/getResourceUpdates?resource_id=137&start=0&limit=1");
			updater.checkUpdate();
		}
		
		registerListener();
		
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new com.vk2gpz.mineresetlite.PAPIExpansion(this).register();
			getLogger().info("Registered PAPI expansion");
		}
		
		getLogger().info("MineResetLite version " + getDescription().getVersion() + " enabled!");
	}
	
	private boolean tePresent() {
		try {
			Class.forName("com.vk2gpz.tokenenchant.event.TEBlockExplodeEvent");
			return true;
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
			getLogger().info("TokenEnchant was not found... skipping.");
		}
		
		return false;
	}
	
	private void registerListener() {
		PluginManager pm = getServer().getPluginManager();
		getLogger().info("Registering BlockEventListener");
		pm.registerEvents(new BlockEventListener(this), this);
		
		if (tePresent()) {
			getLogger().info("Registering TEBlockExplodeEventListener");
			pm.registerEvents(new TEBlockExplodeEventListener(this), this);
			getLogger().info("Registering TokenEnchantEventListener");
			pm.registerEvents(new TokenEnchantEventListener(this), this);
		} else {
			getLogger().info("Registering ExplodeEventListener");
			pm.registerEvents(new ExplodeEventListener(this), this);
			getLogger().info("Registering PlayerEventListener");
			pm.registerEvents(new PlayerEventListener(this), this);
		}
	}
	
	public void onDisable() {
		getServer().getScheduler().cancelTask(resetTaskId);
		getServer().getScheduler().cancelTask(saveTaskId);
		if (updater != null) {
			updater.stop();
		}
		HandlerList.unregisterAll(this);
		// save();
		getLogger().info("MineResetLite disabled");
	}
	
	public Material matchMaterial(String name) {
		Material ret = MaterialUtil.getMaterial(name);
		if (ret == null) {
			// If anyone can think of a more elegant way to serve this function, let me
			// know. ~
			if (name.equalsIgnoreCase("diamondore")) {
				ret = Material.DIAMOND_ORE;
			} else if (name.equalsIgnoreCase("diamondblock")) {
				ret = Material.DIAMOND_BLOCK;
			} else if (name.equalsIgnoreCase("ironore")) {
				ret = Material.IRON_ORE;
			} else if (name.equalsIgnoreCase("ironblock")) {
				ret = Material.IRON_BLOCK;
			} else if (name.equalsIgnoreCase("goldore")) {
				ret = Material.GOLD_ORE;
			} else if (name.equalsIgnoreCase("goldblock")) {
				ret = Material.GOLD_BLOCK;
			} else if (name.equalsIgnoreCase("coalore")) {
				ret = Material.COAL_ORE;
			} else if (name.equalsIgnoreCase("cake") || name.equalsIgnoreCase("cakeblock")) {
				ret = MaterialUtil.getMaterial("CAKE_BLOCK");
			} else if (name.equalsIgnoreCase("emeraldore")) {
				ret = Material.EMERALD_ORE;
			} else if (name.equalsIgnoreCase("emeraldblock")) {
				ret = Material.EMERALD_BLOCK;
			} else if (name.equalsIgnoreCase("lapisore")) {
				ret = Material.LAPIS_ORE;
			} else if (name.equalsIgnoreCase("lapisblock")) {
				ret = Material.LAPIS_BLOCK;
			} else if (name.equalsIgnoreCase("snowblock") || name.equalsIgnoreCase("snow")) { // I've never seen a mine
				// with snowFALL in it.
				ret = Material.SNOW_BLOCK; // Maybe I'll be proven wrong, but it helps 99% of admins.
			} else if (name.equalsIgnoreCase("redstoneore")) {
				ret = Material.REDSTONE_ORE;
			} else {
				ret = Material.matchMaterial(name);
			}
		}
		return ret;
	}
	
	public Mine[] matchMines(String in) {
		List<Mine> matches = new LinkedList<>();
		//getLogger().info("test : " + in);
		for (Mine mine : mines) {
			//getLogger().info("against : " + mine.getName());
			try {
				if (WildcardUtil.matches(in, mine.getName())) {
					matches.add(mine);
				}
			} catch (IllegalArgumentException e) {
				getLogger().info("mine name : " + mine.getName());
				getLogger().info("skipping... you can safely ignore the following exception.");
				e.printStackTrace();
			}
		}
		return matches.toArray(new Mine[0]);
	}
	
	public String toString(Mine @NotNull [] mines) {
		StringJoiner sj = new StringJoiner(", ");
		for (Mine mine : mines) {
			sj.add(mine.getName());
		}
		return sj.toString();
	}
	
	/**
	 * Alert the plugin that changes have been made to mines, but wait 60 seconds
	 * before we save.
	 * This process saves on disk I/O by waiting until a long string of changes have
	 * finished before writing to disk.
	 */
	public void buffSave() {
		BukkitScheduler scheduler = getServer().getScheduler();
		if (saveTaskId != -1) {
			// Cancel old task
			scheduler.cancelTask(saveTaskId);
		}
		// Schedule save
		final MineResetLite plugin = this;
		scheduler.scheduleSyncDelayedTask(this, plugin::save, Config.getBufferSaveDelay() * 20L);
	}
	
	private void save() {
		for (Mine mine : mines) {
			File mineFile = getMineFile(mine);
			FileConfiguration mineConf = YamlConfiguration.loadConfiguration(mineFile);
			mineConf.set("mine", mine);
			try {
				mineConf.save(mineFile);
			} catch (IOException e) {
				getLogger().severe("Unable to serialize mine!");
				e.printStackTrace();
			}
		}
	}
	
	private File getMineFile(Mine mine) {
		return new File(new File(getDataFolder(), "mines"), mine.getName().replace(" ", "") + ".mine.yml");
	}
	
	public void eraseMine(Mine mine) {
		mines.remove(mine);
		//noinspection ResultOfMethodCallIgnored
		getMineFile(mine).delete();
	}
	
	public void addMine(Mine mine) {
		mines.add(mine);
		buffSave();
	}
	
	public boolean hasWorldEdit() {
		return worldEdit != null;
	}
	
	public WorldEditPlugin getWorldEdit() {
		return worldEdit;
	}
	
	private boolean setupConfig() {
		File pluginFolder = getDataFolder();
		if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
			getLogger().severe("Could not make plugin folder! This won't end well...");
			return false;
		}
		File mineFolder = new File(getDataFolder(), "mines");
		if (!mineFolder.exists() && !mineFolder.mkdir()) {
			getLogger().severe("Could not make mine folder! Abort! Abort!");
			return false;
		}
		try {
			Config.initConfig(getDataFolder());
		} catch (IOException e) {
			getLogger().severe("Could not make config file!");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (command.getName().equalsIgnoreCase("mineresetlite")) {
			if (args.length == 0) {
				String[] helpArgs = new String[0];
				commandManager.callCommand("help", sender, helpArgs);
				return true;
			}
			// Spoof args array to account for the initial subcommand specification
			String[] spoofedArgs = new String[args.length - 1];
			System.arraycopy(args, 1, spoofedArgs, 0, args.length - 1);
			commandManager.callCommand(args[0], sender, spoofedArgs);
			return true;
		}
		return false; // Fallthrough
	}
	
	public static void broadcast(String message, Mine mine) {
		if (Config.getBroadcastNearbyOnly()) {
			for (Player p : mine.getWorld().getPlayers()) {
				if (mine.isInside(p)) {
					p.sendMessage(message);
				}
			}
			getInstance().getLogger().info(message);
		} else if (Config.getBroadcastInWorldOnly()) {
			for (Player p : mine.getWorld().getPlayers()) {
				p.sendMessage(message);
			}
			getInstance().getLogger().info(message);
		} else {
			Bukkit.getServer().broadcastMessage(message);
		}
	}
	
	public List<Mine> getMines() {
		return Collections.unmodifiableList(this.mines);
	}
	
}
