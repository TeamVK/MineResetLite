package com.koletar.jj.mineresetlite;

import com.vk2gpz.mc.sound.SoundUtil;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jjkoletar
 */
public class Config {
	private Config() {
	}

	private static boolean broadcastInWorldOnly = false;
	private static boolean broadcastNearbyOnly = false;
	private static boolean checkForUpdates = true;
	private static String locale = "en";
	private static int resetDelay = 20; // in ticks.
	private static int resetBlocksPerStep = 256;
	private static int resetTaskInterval = 3; // in ticks.
	private static boolean mineResetAtStart = false;
	private static int bufferSaveDelay = 60;
	private static boolean globalReset = false;
	private static int globalResetInterval = 20;
	private static List<Integer> globalResetWarnings;
	private static List<Integer> globalResetWarningsLastMinute;
	private static int globalResetClock = 20;
	private static boolean globalResetSilent = false;

	public static int getResetBlocksPerStep() {
		return resetBlocksPerStep;
	}

	public static void setResetBlocksPerStep(int resetBlocksPerStep) {
		Config.resetBlocksPerStep = resetBlocksPerStep;
	}

	private static void writeResetBlocksPerStep(BufferedWriter out) throws IOException {
		out.write("# This option defines the number of blocks to be reset at the time.");
		out.newLine();
		out.write("reset-blocks-per-step: 256");
		out.newLine();
	}

	public static int getResetTaskInterval() {
		return resetTaskInterval;
	}

	public static void setResetTaskInterval(int resetTaskInterval) {
		Config.resetTaskInterval = resetTaskInterval;
	}

	private static void writeResetTaskInterval(BufferedWriter out) throws IOException {
		out.write("# This option defines the delay (in ticks , 20 ticks = 1 sec) between the gradual reset steps.");
		out.newLine();
		out.write("reset-task-interval: 3");
		out.newLine();
	}

	public static int getResetDelay() {
		return resetDelay;
	}

	public static void setResetDelay(int resetDelay) {
		Config.resetDelay = resetDelay;
	}

	private static void writeResetDelay(BufferedWriter out) throws IOException {
		out.write("# This option defines the delay (in ticks , 20 ticks = 1 sec) between the issue of reset command");
		out.newLine();
		out.write("# and the actual execution of the reset action.  If you have large explosions in the mine, you might need to ");
		out.newLine();
		out.write("# increase this value to make sore the explosion action completes before the reset process commences.");
		out.newLine();
		out.write("reset-delay: 20");
		out.newLine();
	}

	static boolean getBroadcastInWorldOnly() {
		return broadcastInWorldOnly;
	}

	static boolean getBroadcastNearbyOnly() {
		return broadcastNearbyOnly;
	}

	private static void setBroadcastInWorldOnly(boolean broadcastInWorldOnly) {
		Config.broadcastInWorldOnly = broadcastInWorldOnly;
	}

	private static void setBroadcastNearbyOnly(boolean broadcastNearbyOnly) {
		Config.broadcastNearbyOnly = broadcastNearbyOnly;
	}

	private static void writeBroadcastInWorldOnly(BufferedWriter out) throws IOException {
		out.write("# If you have multiple worlds, and wish for only the worlds in which your mine resides to receive");
		out.newLine();
		out.write("# reset notifications, and automatic reset warnings, set this to true.");
		out.newLine();
		out.write("broadcast-in-world-only: false");
		out.newLine();
	}

	private static void writeBroadcastNearbyOnly(BufferedWriter out) throws IOException {
		out.write("# If you only want players nearby the mines to receive reset notifications,");
		out.newLine();
		out.write("# and automatic reset warnings, set this to true. Note: Currently only broadcasts to players in the mine");
		out.newLine();
		out.write("broadcast-nearby-only: false");
		out.newLine();
	}

	public static boolean getCheckForUpdates() {
		return checkForUpdates;
	}

	private static void setCheckForUpdates(boolean checkForUpdates) {
		Config.checkForUpdates = checkForUpdates;
	}

	private static void writeCheckForUpdates(BufferedWriter out) throws IOException {
		out.write("# When true, this config option enables update alerts. I do not send any extra information along when ");
		out.newLine();
		out.write("# checking, and query a static file hosted on Dropbox. ");
		out.newLine();
		out.write("check-for-updates: true");
		out.newLine();
	}

	static String getLocale() {
		return locale;
	}

	private static void setLocale(String locale) {
		Config.locale = locale;
	}

	private static void writeLocale(BufferedWriter out) throws IOException {
		out.write("# MineResetLite supports multiple languages. Indicate the language to be used here.");
		out.newLine();
		out.write("# Languages available at the time this config was generated: Danish (thanks Beijiru), Spanish (thanks enetocs), Portuguese (thanks FelipeMarques14), Italian (thanks JoLong)");
		out.newLine();
		out.write("# Use the following values for these languages: English: 'en', Danish: 'da', Spanish: 'es', Portuguese: 'pt', Italian: 'it', French: 'fr', Dutch: 'nl', Polish: 'pl'");
		out.newLine();
		out.write("# A fully up-to-date list of languages is available at http://dev.bukkit.org/server-mods/mineresetlite/pages/internationalization/");
		out.newLine();
		out.write("locale: en");
		out.newLine();
	}

	public static Effect getLuckyEffect() {
		return luckyEffect;
	}

	private static void setLuckyEffect(Effect effect) {
		luckyEffect = effect;
	}

	public static Sound getLuckySound() {
		return luckySound;
	}

	private static void setLuckySound(Sound sound) {
		luckySound = sound;
	}

	private static Effect luckyEffect;
	private static Sound luckySound;

	private static void writeLuckyEffect(BufferedWriter out) throws IOException {
		out.write("# This option specifies the visual effect played when a player mines a lucky block.");
		out.newLine();
		out.write("lucky_block_effect: MOBSPAWNER_FLAMES");
		out.newLine();
	}

	private static void writeLuckySound(BufferedWriter out) throws IOException {
		out.write("# This option specifies the sound effect played when a player mines a lucky block.");
		out.newLine();
		out.write("lucky_block_sound: AMBIENCE_THUNDER");
		out.newLine();
	}

	public static boolean getMineResetAtStart() {
		return Config.mineResetAtStart;
	}

	private static void setMineResetAtStart(boolean mineResetAtStart) {
		Config.mineResetAtStart = mineResetAtStart;
	}

	private static void writeMineResetAtStart(BufferedWriter out) throws IOException {
		out.write("# When true, all mines are reset at the server startup.");
		out.newLine();
		out.write("mine-reset-at-start: true");
		out.newLine();
	}

	public static int getBufferSaveDelay() {
		return Config.bufferSaveDelay;
	}

	private static void setBufferSaveDelay(int bufferSaveDelay) {
		Config.bufferSaveDelay = bufferSaveDelay;
	}

	private static void writeBufferSaveDelay(BufferedWriter out) throws IOException {
		out.write("# Time in second indicating the delay in saving the mine changes you made.");
		out.newLine();
		out.write("buffer-save-delay: 60");
		out.newLine();
	}


	public static boolean getGlobalReset() {
		return Config.globalReset;
	}

	private static void setGlobalReset(boolean globalReset) {
		Config.globalReset = globalReset;
	}

	public static int getGlobalResetInterval() {
		return Config.globalResetInterval;
	}

	private static void setGlobalResetInterval(int globalResetInterval) {
		Config.globalResetInterval = globalResetInterval;
	}

	public static List<Integer> getGlobalResetWarnings() {
		return Config.globalResetWarnings;
	}

	public static List<Integer> getGlobalResetWarningsLastMinute() {
		return Config.globalResetWarningsLastMinute;
	}

	private static void setGlobalResetWarnings(List<String> warnings) {
		Config.globalResetWarnings = new LinkedList<>();
		Config.globalResetWarningsLastMinute = new LinkedList<>();

		if (warnings != null) {
			for (String warning : warnings) {
				try {
					if (warning.toLowerCase().endsWith("s")) {
						warning = warning.toLowerCase().replace("s", "");
						globalResetWarningsLastMinute.add(Integer.valueOf(warning));
					} else {
						globalResetWarnings.add(Integer.valueOf(warning));
					}
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Non-numeric reset warnings supplied");
				}
			}
		}
	}

	public static int getGlobalResetClock() {
		return Config.globalResetClock;
	}

	public static void setGlobalResetClock(int globalResetClock) {
		Config.globalResetClock = globalResetClock;
	}

	public static boolean getGlobalResetSilent() {
		return Config.globalResetSilent;
	}

	private static void setGlobalResetSilent(boolean globalResetSilent) {
		Config.globalResetSilent = globalResetSilent;
	}

	private static void writeGlobalReset(BufferedWriter out) throws IOException {
		out.newLine();
		out.write("# When true, all mines are reset by the common cron task.");
		out.newLine();
		out.write("global-reset: false");
		out.newLine();

		out.write("# This option defines the reset interval (in minutes) (between the issue of reset command.");
		out.newLine();
		out.write("global-reset-interval: 5");
		out.newLine();

		out.write("global-reset-warnings:");
		out.newLine();
		out.write("  - '1'");
		out.newLine();
		out.write("  - 30s");
		out.newLine();
		out.write("  - 10s");
		out.newLine();

		out.write("global-reset-silent: false");
		out.newLine();
	}

	static void initConfig(File dataFolder) throws IOException {
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}
		File configFile = new File(dataFolder, "config.yml");
		if (!configFile.exists()) {
			configFile.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
			out.write("# MineResetLite Configuration File");
			out.newLine();
			Config.writeBroadcastInWorldOnly(out);
			Config.writeBroadcastNearbyOnly(out);
			Config.writeCheckForUpdates(out);
			Config.writeLocale(out);
			Config.writeResetDelay(out);
			Config.writeLuckyEffect(out);
			Config.writeLuckySound(out);
			out.close();
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		BufferedWriter out = new BufferedWriter(new FileWriter(configFile, true));
		if (!config.contains("broadcast-in-world-only")) {
			Config.writeBroadcastInWorldOnly(out);
		}
		Config.setBroadcastInWorldOnly(config.getBoolean("broadcast-in-world-only"));

		if (!config.contains("broadcast-nearby-only")) {
			Config.writeBroadcastNearbyOnly(out);
		}
		Config.setBroadcastNearbyOnly(config.getBoolean("broadcast-nearby-only"));

		if (!config.contains("check-for-updates")) {
			Config.writeCheckForUpdates(out);
		}
		Config.setCheckForUpdates(config.getBoolean("check-for-updates"));


		if (!config.contains("locale")) {
			Config.writeLocale(out);
		}
		Config.setLocale(config.getString("locale"));

		if (!config.contains("reset-delay")) {
			Config.writeResetDelay(out);
		}
		Config.setResetDelay(config.getInt("reset-delay", 20));

		if (!config.contains("reset-task-interval")) {
			Config.writeResetTaskInterval(out);
		}
		Config.setResetTaskInterval(config.getInt("reset-task-interval", 5));

		if (!config.contains("reset-blocks-per-step")) {
			Config.writeResetBlocksPerStep(out);
		}
		Config.setResetBlocksPerStep(config.getInt("reset-blocks-per-step", 256));


		if (!config.contains("lucky_block_effect")) {
			Config.writeLuckyEffect(out);
		}
		try {
			Config.setLuckyEffect(Effect.valueOf(config.getString("lucky_block_effect", "MOBSPAWNER_FLAMES")));
		} catch (
				Throwable ignore) {
			Config.setLuckyEffect(null);
		}


		if (!config.contains("lucky_block_sound")) {
			Config.writeLuckySound(out);
		}
		try {
			Config.setLuckySound(SoundUtil.getSound(config.getString("lucky_block_sound", "AMBIENCE_THUNDER")));
		} catch (
				Throwable ignore) {
			Config.setLuckySound(null);
		}


		if (!config.contains("mine-reset-at-start")) {
			Config.writeMineResetAtStart(out);
		}
		Config.setMineResetAtStart(config.getBoolean("mine-reset-at-start", true));

		if (!config.contains("buffer-save-delay")) {
			Config.writeBufferSaveDelay(out);
		}
		Config.setBufferSaveDelay(config.getInt("buffer-save-delay", 60));

		if (!config.contains("global-reset")) {
			Config.writeGlobalReset(out);
		}
		Config.setGlobalReset(config.getBoolean("global-reset", false));
		Config.setGlobalResetInterval(config.getInt("global-reset-interval", 5));
		Config.setGlobalResetWarnings((List<String>) config.getList("global-reset-warnings"));
		Config.setGlobalResetClock(Config.getGlobalResetInterval());
		Config.setGlobalResetSilent(config.getBoolean("global-reset-silent", false));

		out.close();
	}
}
