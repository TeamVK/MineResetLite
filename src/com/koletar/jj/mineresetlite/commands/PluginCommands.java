package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.MineResetLite;
import com.vk2gpz.vklib.debug.DebugHandler;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class PluginCommands {
	private final MineResetLite plugin;

	public PluginCommands(MineResetLite plugin) {
		this.plugin = plugin;
	}

	@Command(aliases = {"about"},
			description = "List version and project information about MRL",
			permissions = {},
			help = {"Show version information about this installation of MRL, in addition", "to the authors of the plugin."},
			usage = "<keylist|on|off> [keys...]",
			min = 0, max = 0, onlyPlayers = false)
	public void about(CommandSender sender, String[] args) {
		sender.sendMessage(phrase("aboutTitle"));
		sender.sendMessage(phrase("aboutAuthors"));
		sender.sendMessage(phrase("aboutVersion", plugin.getDescription().getVersion()));
	}

	@Command(aliases = {"debug"},
			description = "Turn on/off debug statements",
			permissions = {"mineresetlite.debug"},
			help = {"Turn on/off debug statements. Usage: /mrl debug on|off [keyword...] "},
			min = 1, max = -1, onlyPlayers = false)
	public void debug(CommandSender sender, String[] args) {
		boolean status = args[0].equalsIgnoreCase("on");
		boolean list = args[0].equalsIgnoreCase("keylist");

		String[] keys = getDebugKeys(args);
		if (list) {
			StringBuffer sb = new StringBuffer("Debug Keys: ");
			DebugHandler.getKeys(MineResetLite.class).forEach(s -> sb.append(s).append(","));
			sender.sendMessage(sb.toString());
		} else {
			if (status) {
				DebugHandler.debugOn(MineResetLite.class, keys);
			} else {
				DebugHandler.debugOff(MineResetLite.class);
			}
		}
		//ReflectionUtil.setValue(TokenEnchant.getInstance(), "DEBUG", Boolean.parseBoolean(args[1]), false);
		plugin.getLogger().info("turned the DEBUG mode " + status + " for : " + getList(keys));
	}

	private String[] getDebugKeys(String... args) {
		return (args.length > 1) ?
				Arrays.stream(args, 1, args.length).toArray(String[]::new) :
				new String[]{"*"};
	}

	private String getList(String... keys) {
		StringBuffer sb = new StringBuffer();
		Arrays.asList(keys).forEach(s -> sb.append(s).append(","));
		return sb.toString();
	}
}
