package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.*;
import com.vk2gpz.mc.material.MaterialUtil;
import com.vk2gpz.mineresetlite.mine.MineType;
import com.vk2gpz.mineresetlite.util.MRLUtil;
import com.vk2gpz.vklib.math.MathUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class MineCommands {
	private final MineResetLite plugin;
	private final Map<Player, Location> point1;
	private final Map<Player, Location> point2;

	public MineCommands(MineResetLite plugin) {
		this.plugin = plugin;
		point1 = new HashMap<>();
		point2 = new HashMap<>();
	}

	@Command(aliases = {"list", "l"},
			description = "List the names of all Mines",
			permissions = {"mineresetlite.mine.list"},
			help = {"List the names of all Mines currently created, across all worlds."},
			min = 0, max = 0, onlyPlayers = false)
	public void listMines(CommandSender sender, String[] args) {
		sender.sendMessage(phrase("mineList", StringTools.buildList(plugin.getMines(), "&c", "&d, ")));
	}

	@Command(aliases = {"pos1", "p1"},
			description = "Change your first selection point",
			help = {"Run this command to set your first selection point to the block you are looking at.",
					"Use /mrl pos1 -feet to set your first point to the location you are standing on."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint1(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point1.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point1.put(player, player.getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}

	@Command(aliases = {"pos2", "p2"},
			description = "Change your first selection point",
			help = {"Run this command to set your second selection point to the block you are looking at.",
					"Use /mrl pos2 -feet to set your second point to the location you are standing on."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint2(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point2.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point2.put(player, player.getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}

	@Command(aliases = {"create", "save"},
			description = "Create a mine from either your WorldEdit selection or by manually specifying the points",
			help = {"Provided you have a selection made via either WorldEdit or selecting the points using MRL,",
					"an empty mine will be created. This mine will have no composition and default settings."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.create"},
			min = 1, max = -1, onlyPlayers = true)
	public void createMine(CommandSender sender, String[] args) {
		//Find out how they selected the region
		Player player = (Player) sender;
		World world = null;
		Vector p1 = null;
		Vector p2 = null;

		//Native selection techniques?
		if (point1.containsKey(player) && point2.containsKey(player)) {
			world = point1.get(player).getWorld();
			if (!world.equals(point2.get(player).getWorld())) {
				player.sendMessage(phrase("crossWorldSelection"));
				return;
			}
			p1 = point1.get(player).toVector();
			p2 = point2.get(player).toVector();
		}
		Object[] selections = MRLUtil.getSelection(player);
		if (selections != null) {
			if (selections[1] != null && selections[2] != null) {
				world = (World) selections[0];
				p1 = (Vector) selections[1];
				p2 = (Vector) selections[2];
			}
		}
		//Construct mine name
		String name = StringTools.buildSpacedArgument(args);
		//Verify uniqueness of mine name
		Mine[] mines = plugin.matchMines(name);
		if (mines.length > 0) {
			player.sendMessage(phrase("nameInUse", name));
			return;
		}

		if (p1 == null) {
			player.sendMessage(phrase("emptySelection"));
			return;
		}

		//Sort coordinates
		sortCoordinates(p1, p2);

		//Create!
		Mine newMine = MRLUtil.createMine(name, world, p1, p2, (MineType) selections[3], selections[4]);
		if (newMine != null) {
			plugin.addMine(newMine);
			player.sendMessage(phrase("mineCreated", newMine));
		}
	}

	private void sortCoordinates(@NotNull Vector p1, @NotNull Vector p2) {
		if (p1.getX() > p2.getX()) {
			//Swap
			double x = p1.getX();
			p1.setX(p2.getX());
			p2.setX(x);
		}
		if (p1.getY() > p2.getY()) {
			double y = p1.getY();
			p1.setY(p2.getY());
			p2.setY(y);
		}
		if (p1.getZ() > p2.getZ()) {
			double z = p1.getZ();
			p1.setZ(p2.getZ());
			p2.setZ(z);
		}
	}

	@Command(aliases = {"info", "i"},
			description = "List information about a mine",
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.info"},
			min = 1, max = -1, onlyPlayers = false)
	public void mineInfo(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMines(sender, mines)) return;
		sender.sendMessage(phrase("mineInfoName", mines[0]));
		sender.sendMessage(phrase("mineInfoWorld", mines[0].getWorld()));
		//Build composition list
		StringBuilder csb = new StringBuilder();
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			csb.append(entry.getValue() * 100);
			csb.append("% ");
			csb.append(MaterialUtil.getMaterial("" + entry.getKey().getBlockId()).toString());
			if (entry.getKey().getData() != 0) {
				csb.append(":");
				csb.append(entry.getKey().getData());
			}
			csb.append(", ");
		}
		if (csb.length() > 2) {
			csb.delete(csb.length() - 2, csb.length() - 1);
		}
		sender.sendMessage(phrase("mineInfoComposition", csb));
		if (mines[0].getResetDelay() != 0) {
			sender.sendMessage(phrase("mineInfoResetDelay", mines[0].getResetDelay()));
			sender.sendMessage(phrase("mineInfoTimeUntilReset", mines[0].getTimeUntilReset()));
		}
		sender.sendMessage(phrase("mineInfoSilence", mines[0].isSilent()));
		if (!mines[0].getResetWarnings().isEmpty()) {
			sender.sendMessage(phrase("mineInfoWarningTimes", StringTools.buildList(mines[0].getResetWarnings(), "", ", ")));
		}
		if (mines[0].getSurface() != null) {
			sender.sendMessage(phrase("mineInfoSurface", mines[0].getSurface()));
		}
		if (mines[0].getFillMode()) {
			sender.sendMessage(phrase("mineInfoFillMode"));
		}
	}

	private boolean invalidMines(CommandSender sender, Mine[] mines) {
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines", plugin.toString(mines)));
			return true;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return true;
		}
		return false;
	}

	@Command(aliases = {"set", "add", "+"},
			description = "Set the percentage of a block in the mine",
			help = {"This command will always overwrite the current percentage for the specified block,",
					"if a percentage has already been set. You cannot set the percentage of any specific",
					"block, such that the percentage would then total over 100%."},
			usage = "<mine name> <block>:(data) <percentage>%",
			permissions = {"mineresetlite.mine.composition"},
			min = 3, max = -1, onlyPlayers = false)
	public void setComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 2));
		if (invalidMines(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 2].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (!SerializableBlock.isBlock(bits[0], m)) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}

		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}

		//Parse percentage
		String percentageS = args[args.length - 1];
		if (!percentageS.endsWith("%")) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}

		StringBuilder psb = new StringBuilder(percentageS);
		psb.deleteCharAt(psb.length() - 1);
		double percentage;
		try {
			percentage = Double.valueOf(psb.toString());
		} catch (
				NumberFormatException nfe) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		if (percentage > 100 || percentage <= 0) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}

		percentage = MathUtil.round(percentage / 100, 4); //Make it a programmatic percentage
		SerializableBlock block = new SerializableBlock(bits[0], data);
		Double oldPercentage = mines[0].getComposition().get(block);
		double total = 0;
		for (
				Map.Entry<SerializableBlock, Double> entry : mines[0].

				getComposition().

				entrySet()) {
			if (!entry.getKey().equals(block)) {
				total += entry.getValue();
			} else {
				block = entry.getKey();
			}
		}

		total += percentage;
		if (total > 1) {
			sender.sendMessage(phrase("insaneCompositionChange", total, percentage));
			if (oldPercentage == null) {
				mines[0].getComposition().remove(block);
			} else {
				mines[0].getComposition().put(block, oldPercentage);
			}
			return;
		}

		mines[0].getComposition().put(block, percentage);
		sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block, MathUtil.round((1 - mines[0].getCompositionTotal()) * 100, 4)));
		plugin.buffSave();
	}

	@Command(aliases = {"unset", "remove", "-"},
			description = "Remove a block from the composition of a mine",
			usage = "<mine name> <block>:(data)",
			permissions = {"mineresetlite.mine.composition"},
			min = 2, max = -1, onlyPlayers = false)
	public void unsetComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		if (invalidMines(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 1].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (!SerializableBlock.isBlock(bits[0], m)) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Does the mine contain this block?
		SerializableBlock block = new SerializableBlock(bits[0], data);
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			if (entry.getKey().equals(block)) {
				mines[0].getComposition().remove(entry.getKey());
				sender.sendMessage(phrase("blockRemovedFromMine", mines[0], block, MathUtil.round((1 - mines[0].getCompositionTotal()) * 100, 4)));
				return;
			}
		}
		sender.sendMessage(phrase("blockNotInMine", mines[0], block));
		plugin.buffSave();
	}

	@Command(aliases = {"reset", "r"},
			description = "Reset a mine",
			help = {"If you supply the -s argument, the mine will silently reset. Resets triggered via",
					"this command will not show a 1 minute warning, unless this mine is flagged to always",
					"have a warning. If the mine's composition doesn't equal 100%, the composition will be",
					"padded with air until the total equals 100%."},
			usage = "<mine name> (-s)",
			permissions = {"mineresetlite.mine.reset"},
			min = 1, max = -1, onlyPlayers = false)
	public void resetMine(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, args.length - 1));
		//if (invalidMines(sender, mines)) return;
		for (Mine mine : mines) {
			if (isSilent(args)) {
				//Silent reset
				mine.reset();
			} else {
				MineResetLite.broadcast(phrase("mineResetBroadcast", mine, sender), mine);
				mine.reset();
			}
		}
	}

	private static final HashSet<String> silentFlags = new HashSet<>();

	static {
		silentFlags.add("-s");
		// Add other silent flags if needed
	}

	private boolean isSilent(String @NotNull [] args) {
		for (String s : args) {
			if (silentFlags.contains(s)) {
				return true;
			}
		}
		return false;
	}

	@Command(aliases = {"flag", "f"},
			description = "Set various properties of a mine, including automatic resets",
			help = {"Available flags:",
					"resetPercent: A integer number (0 < x < 100) specifying the percentage of mined blocks triggering the reset. Set to -1 to disable automatic percent resets.",
					"resetDelay: An integer number of minutes specifying the time between automatic resets. Set to 0 to disable automatic resets.",
					"resetWarnings: A comma separated list of integer minutes to warn before the automatic reset. Warnings must be less than the reset delay.",
					"surface: A block that will cover the entire top surface of the mine when reset, obscuring surface ores. Set surface to air to clear the value.",
					"fillMode: An alternate reset algorithm that will only \"reset\" air blocks inside your mine. Set to true or false.",
					"fillMode: An alternate reset algorithm that will only \"reset\" air blocks inside your mine. Set to true or false.",
					"isSilent: A boolean (true or false) of whether or not this mine should broadcast a reset notification when it is reset *automatically*",
					"tpAtReset: A boolean (true or false) of whether or not players in the mine should be teleported at the reset."
			},
			usage = "<mine name> <setting> <value>",
			permissions = {"mineresetlite.mine.flag"},
			min = 3, max = -1, onlyPlayers = false)
	public void flag(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 2));
		//if (invalidMines(sender, mines)) return;
		String setting = args[args.length - 2];
		String value = args[args.length - 1];
		for (Mine mine : mines) {
			if (setting.equalsIgnoreCase("resetEvery") || setting.equalsIgnoreCase("resetDelay")) {
				int delay;
				try {
					delay = Integer.valueOf(value);
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("badResetDelay"));
					return;
				}
				if (delay < 0) {
					sender.sendMessage(phrase("badResetDelay"));
					return;
				}
				mine.setResetDelay(delay);
				if (delay == 0) {
					sender.sendMessage(phrase("resetDelayCleared", mine));
				} else {
					sender.sendMessage(phrase("resetDelaySet", mine, delay));
				}
				plugin.buffSave();
				return;
			} else if (setting.equalsIgnoreCase("resetWarnings") || setting.equalsIgnoreCase("resetWarning")) {
				String[] bits = value.split(",");
				List<Integer> warnings = mine.getResetWarnings();
				List<Integer> oldList = new LinkedList<>(warnings);

				List<Integer> warningsLastMinute = mine.getResetWarningsLastMinute();
				List<Integer> oldListLastMinute = new LinkedList<>(warningsLastMinute);
				warnings.clear();
				warningsLastMinute.clear();

				for (String bit : bits) {
					try {
						//System.out.println("bit : " + bit);
						if (bit.toLowerCase().endsWith("s")) {
							bit = bit.toLowerCase().replace("s", "");
							warningsLastMinute.add(Integer.valueOf(bit));
						} else {
							warnings.add(Integer.valueOf(bit));
						}
					} catch (NumberFormatException nfe) {
						sender.sendMessage(phrase("badWarningList"));
						return;
					}
				}
				//Validate warnings
				for (Integer warning : warnings) {
					if ((mine.getResetDelay() > 0 && warning >= mine.getResetDelay())
							|| (mine.getResetPercent() > 0 && warning < mine.getResetPercent() * 100)) {
						sender.sendMessage(phrase("badWarningList"));
						mine.setResetWarnings(oldList);
						mine.setResetWarnings(oldListLastMinute);
						return;
					}
				}

				for (Integer warning : warningsLastMinute) {
					if (warning >= 60) {
						sender.sendMessage(phrase("badWarningList"));
						mine.setResetWarnings(oldList);
						mine.setResetWarnings(oldListLastMinute);
						return;
					}
				}

				if (warnings.contains(0) && warnings.size() == 1) {
					warnings.clear();
					warningsLastMinute.clear();
					sender.sendMessage(phrase("warningListCleared", mine));
					return;
				} else if (warnings.contains(0) || warningsLastMinute.contains(0)) {
					sender.sendMessage(phrase("badWarningList"));
					mine.setResetWarnings(oldList);
					mine.setResetWarningsLastMinute(oldListLastMinute);
					return;
				}
				sender.sendMessage(phrase("warningListSet", mine));
				plugin.buffSave();
				return;
			} else if (setting.equalsIgnoreCase("surface")) {
				//Match material
				String[] bits = value.split(":");
				Material m = plugin.matchMaterial(bits[0]);
				if (!SerializableBlock.isBlock(bits[0], m)) {
					sender.sendMessage(phrase("notABlock"));
					return;
				}
				byte data = 0;
				if (bits.length == 2) {
					try {
						data = Byte.valueOf(bits[1]);
					} catch (NumberFormatException nfe) {
						sender.sendMessage(phrase("unknownBlock"));
						return;
					}
				}
				if (m.equals(Material.AIR)) {
					mine.setSurface(null);
					sender.sendMessage(phrase("surfaceBlockCleared", mine));
					plugin.buffSave();
					return;
				}
				SerializableBlock block = new SerializableBlock(bits[0], data);
				mine.setSurface(block);
				sender.sendMessage(phrase("surfaceBlockSet", mine));
				plugin.buffSave();
				return;
			} else if (setting.equalsIgnoreCase("fill") || setting.equalsIgnoreCase("fillMode")) {
				//Match true or false
				if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
					mine.setFillMode(true);
					sender.sendMessage(phrase("fillModeEnabled"));
					plugin.buffSave();
					return;
				} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
					mine.setFillMode(false);
					sender.sendMessage(phrase("fillModeDisabled"));
					plugin.buffSave();
					return;
				}
				sender.sendMessage(phrase("invalidFillMode"));
			} else if (setting.equalsIgnoreCase("isSilent") || setting.equalsIgnoreCase("silent") || setting.equalsIgnoreCase("silence")) {
				if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
					mine.setSilence(true);
					sender.sendMessage(phrase("mineIsNowSilent", mine));
					plugin.buffSave();
					return;
				} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
					mine.setSilence(false);
					sender.sendMessage(phrase("mineIsNoLongerSilent", mine));
					plugin.buffSave();
					return;
				}
				sender.sendMessage(phrase("badBoolean"));
			} else if (setting.equalsIgnoreCase("resetPercent")) {
				StringBuilder psb = new StringBuilder(value);
				psb.deleteCharAt(psb.length() - 1);
				double percentage;
				try {
					percentage = Double.valueOf(psb.toString());
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("badPercentage"));
					return;
				}
				// < 0 is used to cancel percent reset setting.
				if (percentage > 100) {// || percentage <= 0) {
					sender.sendMessage(phrase("badPercentage"));
					return;
				}
				percentage = percentage / 100; //Make it a programmatic percentage
				mine.setResetPercent(percentage);

				if (percentage < 0) {
					sender.sendMessage(phrase("resetDelayCleared", mine));
				} else {
					sender.sendMessage(phrase("resetPercentageSet", mine, (int) (percentage * 100)));
				}
				plugin.buffSave();
				return;
			} else if (setting.equalsIgnoreCase("tpAtReset")) {
				if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
					mine.setTpAtReset(true);
					sender.sendMessage(phrase("mineTpAtReset", mine));
					plugin.buffSave();
					return;
				} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
					mine.setTpAtReset(false);
					sender.sendMessage(phrase("mineNoTpAtReset", mine));
					plugin.buffSave();
					return;
				}
				sender.sendMessage(phrase("badBoolean"));
			}
		}
		sender.sendMessage(phrase("unknownFlag"));
	}

	@Command(aliases = {"erase"},
			description = "Completely erase a mine",
			help = {"Like most erasures of data, be sure you don't need to recover anything from this mine before you delete it."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.erase"},
			min = 1, max = -1, onlyPlayers = false)
	public void erase(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMines(sender, mines)) return;
		plugin.eraseMine(mines[0]);
		sender.sendMessage(phrase("mineErased", mines[0]));
	}

	@Command(aliases = {"reschedule"},
			description = "Synchronize all automatic mine resets",
			help = {"This command will set the 'start time' of the mine resets to the same point."},
			usage = "",
			permissions = {"mineresetlite.mine.flag"},
			min = 0, max = 0, onlyPlayers = false)
	public void reschedule(CommandSender sender, String[] args) {
		for (Mine mine : plugin.getMines()) {
			mine.setResetDelay(mine.getResetDelay());
		}
		plugin.buffSave();
		sender.sendMessage(phrase("rescheduled"));
	}

	@Command(aliases = {"tp", "teleport"}, description = "Teleport to the specified mine", help = {"This command will teleport you to the center of the specified mine or at the teleport point if it is specified."}, usage = "<mine name>", permissions = {"mineresetlite.mine.tp"}, min = 1, max = -1, onlyPlayers = true)
	public void teleport(CommandSender sender, String[] args) {
		Mine mine = null;

		for (Mine aMine : plugin.getMines()) {
			if (aMine.getName().equalsIgnoreCase(args[0])) {
				mine = aMine;
			}
		}

		if (mine == null) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}

		mine.teleport((Player) sender);
	}

	@Command(aliases = {"settp", "stp"}, description = "Sets the specified mine's spawn point", help = {"This command will set the specified mine's reset spawn point to where you're standing.", "Use /mrl removetp <mine name> to remove the mine's spawn point."}, usage = "<mine name>", permissions = {"mineresetlite.mine.settp"}, min = 1, max = -1, onlyPlayers = true)
	public void setTP(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMines(sender, mines)) return;
		mines[0].setTp(player.getLocation());
		plugin.buffSave();
		sender.sendMessage(phrase("tpSet", mines[0]));
	}

	@Command(aliases = {"removetp", "rtp"}, description = "Removes the specified mine's spawn point", help = {"This comamnd will remove the specified mine's reset spawn point.", "Use /mrl removetp to remove the spawn point.", "use /mrl settp to set it to where you're standing."}, usage = "<mine name>", permissions = {"mineresetlite.mine.removetp"}, min = 1, max = -1, onlyPlayers = true)
	public void removeTP(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMines(sender, mines)) return;
		mines[0].setTp(new Location(player.getWorld(), 0, -Integer.MAX_VALUE, 0));
		plugin.buffSave();
		sender.sendMessage(phrase("tpRemove", mines[0]));
	}

	@Command(aliases = {"addpotion", "addpot"}, description = "Adds the specified potion to the mine", help = {"This command will saddthe specified potion to the mine where you're standing.", "Use /mrl removepot <mine name> <potionname> to remove the specified potion effect from the mine."}, usage = "<mine name> <potionname:amplifier>", permissions = {"mineresetlite.mine.addpotion"}, min = 1, max = -1, onlyPlayers = true)
	public void addPot(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		//if (invalidMines(sender, mines)) return;
		for (Mine mine : mines) {
			if (mine.addPotion(args[args.length - 1]) != null) {
				plugin.buffSave();
				sender.sendMessage(phrase("potionAdded", args[args.length - 1], mine));
			}
		}
	}

	@Command(aliases = {"removepotion", "removepot"}, description = "Removes the specified potion from the mine", help = {"This comamnd will remove the specified potion from the mine.", "Use /mrl removepot <potionname> to remove the potion.", "Use /mrl addpot <mine name> <potionname:amplifier> to add the specified potion effect to the mine."}, usage = "<mine name> <potionname>", permissions = {"mineresetlite.mine.removepotion"}, min = 1, max = -1, onlyPlayers = true)
	public void removePot(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		//if (invalidMines(sender, mines)) return;
		for (Mine mine : mines) {
			mine.removePotion(args[args.length - 1]);
			plugin.buffSave();
			sender.sendMessage(phrase("potionRemoved", args[args.length - 1], mine));
		}
	}

	@Command(aliases = {"redefine", "rd"},
			description = "Redefine a mine's boundaries from either your WorldEdit selection or by manually specifying the points",
			help = {"Provided you have a selection made via either WorldEdit or selecting the points using MRL,"},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.redefine"},
			min = 1, max = -1, onlyPlayers = true)
	public void redefineMine(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		World world = null;
		Vector p1 = null;
		Vector p2 = null;

		if (this.point1.containsKey(player) && this.point2.containsKey(player)) {
			world = point1.get(player).getWorld();
			if (!world.equals(point2.get(player).getWorld())) {
				player.sendMessage(phrase("crossWorldSelection"));
				return;
			}
			p1 = point1.get(player).toVector();
			p2 = point2.get(player).toVector();
		}

		Object[] selections = MRLUtil.getSelection(player);
		if (selections != null) {
			if (selections[1] != null && selections[2] != null) {
				world = (World) selections[0];
				p1 = (Vector) selections[1];
				p2 = (Vector) selections[2];
			}
		}

		if (p1 == null) {
			player.sendMessage(phrase("emptySelection"));
			return;
		}
		//Construct mine name
		String name = StringTools.buildSpacedArgument(args);
		//Verify uniqueness of mine name
		Mine[] mines = plugin.matchMines(name);
		if (mines.length < 1) {
			player.sendMessage(phrase("noMinesMatched"));
			return;
		}

		//Sort coordinates
		sortCoordinates(p1, p2);
		MRLUtil.updateMine(mines[0], p1, p2, (MineType) selections[3], selections[4]);
		player.sendMessage(Phrases.phrase("mineRedefined"));
		plugin.buffSave();
	}

	@Command(aliases = {"setstruct", "addstruct", "+"},
			description = "Set the structure block for the mine",
			help = {"This command will add the specified block as the strcuture material types,",
					"and it will not be replaced by the mine blocks."},
			usage = "<mine name> <block>:(data)",
			permissions = {"mineresetlite.mine.composition"},
			min = 2, max = -1, onlyPlayers = false)
	public void setStructure(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		if (invalidMines(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 1].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (!SerializableBlock.isBlock(bits[0], m)) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.parseByte(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		SerializableBlock block = new SerializableBlock(bits[0], data);
		mines[0].getStructure().add(block);
		sender.sendMessage(phrase("mineStructureSet", mines[0], block));
		plugin.buffSave();
	}

	@Command(aliases = {"unsetstruct", "removestruct", "-"},
			description = "Remove a block from the strucutre material list.",
			usage = "<mine name> <block>:(data)",
			permissions = {"mineresetlite.mine.composition"},
			min = 2, max = -1, onlyPlayers = false)
	public void unsetStructureComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		if (invalidMines(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 1].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (!SerializableBlock.isBlock(bits[0], m)) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.parseByte(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Does the mine contain this block?
		SerializableBlock block = new SerializableBlock(bits[0], data);

		for (SerializableBlock entry : mines[0].getStructure()) {
			if (entry.equals(block)) {
				mines[0].getStructure().remove(entry);
				sender.sendMessage(phrase("blockRemovedFromStructure", mines[0], block));
				return;
			}
		}
		sender.sendMessage(phrase("blockNotInMine", mines[0], block));
		plugin.buffSave();
	}

	@Command(aliases = {"setlucky"}, description = "Sets the number of lucky blocks in the specified mine.", help = {"This command will set the number of lucky blocks in the specified mine."}, usage = "<mine name> <a_number_of_lucky_blocks>", permissions = {"mineresetlite.mine.setlucky"}, min = 1, max = -1, onlyPlayers = true)
	public void setLucky(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		if (invalidMines(sender, mines)) return;
		int num;
		try {
			num = Integer.parseInt(args[args.length - 1]);
			if (num > 0) {
				mines[0].setLuckyBlockNum(num);
				plugin.buffSave();
				sender.sendMessage(phrase("luckyBlocksSet", mines[0], args[args.length - 1]));
			}
		} catch (Throwable ignore) {
			//Args weren't empty or -feet, bad args
			throw new InvalidCommandArgumentsException();
		}
	}

	@Command(aliases = {"makelucky"}, description = "Makes the n-th mined block as a lucky block in the specified mine.", help = {"This command will make the n-th mined block in the specified mine as a lucky block."}, usage = "<mine name> <a_number_of_lucky_block>", permissions = {"mineresetlite.mine.makelucky"}, min = 1, max = -1, onlyPlayers = true)
	public void makeLucky(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args)); //, 1));
		if (invalidMines(sender, mines)) return;
		int num;
		try {
			num = Integer.parseInt(args[args.length - 1]);
			if (num > 0) {
				mines[0].makeLucky(num);
			}
		} catch (Throwable ignore) {
			//Args weren't empty or -feet, bad args
			throw new InvalidCommandArgumentsException();
		}
	}
}
