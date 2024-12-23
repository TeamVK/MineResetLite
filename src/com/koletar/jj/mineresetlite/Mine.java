package com.koletar.jj.mineresetlite;

import com.vk2gpz.mineresetlite.event.MineResetEvent;
import com.vk2gpz.mineresetlite.event.MineUpdatedEvent;
import com.vk2gpz.vklib.math.MathUtil;
import com.vk2gpz.vklib.reflection.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * @author jjkoletar
 * @author vk2gpz
 */
public class Mine implements ConfigurationSerializable {
	protected static final Random RAND = new Random();

	protected int minX;
	protected int minY;
	protected int minZ;
	protected int maxX;
	protected int maxY;
	protected int maxZ;
	protected World world;
	protected Map<SerializableBlock, Double> composition;
	protected Set<SerializableBlock> structure; // structure material defining the mine walls, radder, etc.
	protected int resetDelay;
	protected List<Integer> resetWarnings;
	protected List<Integer> resetWarningsLastMinute;
	protected String name;
	protected SerializableBlock surface;
	protected boolean fillMode;
	protected int resetClock;
	protected boolean isSilent;
	protected int tpX = 0;
	protected int tpY = -Integer.MAX_VALUE;
	protected int tpZ = 0;
	protected int tpYaw = 0;
	protected int tpPitch = 0;

	// from MineResetLitePlus
	protected double resetPercent = -1.0;
	protected transient int maxCount = 0;
	private transient int currentBroken = 0;

	protected List<PotionEffect> potions = new ArrayList<>();

	protected int luckyBlocks = 0;
	List<Integer> luckyNum;
	protected List<String> luckyCommands;

	protected boolean tpAtReset;

	protected Mine(String name) {
		this.name = name;
		composition = new HashMap<>();
		resetWarnings = new LinkedList<>();
		resetWarningsLastMinute = new LinkedList<>();
		structure = new HashSet<>();
		luckyNum = new ArrayList<>();
		luckyCommands = new ArrayList<>();
	}

	public Mine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String name, World world) {
		this(name);
		redefine(minX, minY, minZ, maxX, maxY, maxZ, world);
		setMaxCount();
	}

	public Mine(Map<String, Object> me) {
		this((String) me.get("name"));
		try {
			redefine((Integer) me.get("minX"), (Integer) me.get("minY"), (Integer) me.get("minZ"),
					(Integer) me.get("maxX"), (Integer) me.get("maxY"), (Integer) me.get("maxZ"),
					Bukkit.getServer().getWorld((String) me.get("world")));

			setMaxCount();
		} catch (Throwable t) {
			throw new IllegalArgumentException("Error deserializing coordinate pairs");
		}

		if (world == null) {
			Logger l = MineResetLite.getInstance().getLogger();
			l.severe("[MineResetLite] Unable to find a world! Please include these logger lines along with the stack trace when reporting this bug!");
			l.severe("[MineResetLite] Attempted to load world named: " + me.get("world"));
			l.severe("[MineResetLite] Worlds listed: " + StringTools.buildList(Bukkit.getWorlds(), "", ", "));
			throw new IllegalArgumentException("World was null!");
		}
		try {
			Map<String, Double> sComposition = (Map<String, Double>) me.get("composition");
			composition = new HashMap<>();
			for (Map.Entry<String, Double> entry : sComposition.entrySet()) {
				composition.put(new SerializableBlock(entry.getKey()), MathUtil.round(entry.getValue(), 4));
			}
		} catch (Throwable t) {
			throw new IllegalArgumentException("Error deserializing composition");
		}

		try {
			List<String> sStructure = (List<String>) me.get("structure");
			structure = new HashSet<>();
			for (String entry : sStructure) {
				structure.add(new SerializableBlock(entry));
			}
		} catch (Throwable t) {
			//throw new IllegalArgumentException("Error deserializing structure");
		}

		resetDelay = (Integer) me.get("resetDelay");
		List<String> warnings = (List<String>) me.get("resetWarnings");
		resetWarnings = new LinkedList<>();
		resetWarningsLastMinute = new LinkedList<>();
		for (String warning : warnings) {
			try {
				if (warning.toLowerCase().endsWith("s")) {
					warning = warning.toLowerCase().replace("s", "");
					resetWarningsLastMinute.add(Integer.valueOf(warning));
				} else {
					resetWarnings.add(Integer.valueOf(warning));
				}
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Non-numeric reset warnings supplied");
			}
		}
		if (me.containsKey("surface")) {
			if (!me.get("surface").equals("")) {
				surface = new SerializableBlock((String) me.get("surface"));
			}
		}
		if (me.containsKey("fillMode")) {
			fillMode = (Boolean) me.get("fillMode");
		}
		if (me.containsKey("resetClock")) {
			resetClock = (Integer) me.get("resetClock");
		}
		//Compat for the clock
		if (resetDelay > 0 && resetClock == 0) {
			resetClock = resetDelay;
		}
		if (me.containsKey("isSilent")) {
			isSilent = (Boolean) me.get("isSilent");
		}
		if (me.containsKey("tpY")) { // Should contain all three if it contains this one
			tpX = (int) me.get("tpX");
			tpY = (int) me.get("tpY");
			tpZ = (int) me.get("tpZ");
		}

		if (me.containsKey("tpYaw")) {
			tpYaw = (int) me.get("tpYaw");
			tpPitch = (int) me.get("tpPitch");
		}

		if (me.containsKey("resetPercent")) {
			resetPercent = (double) me.get("resetPercent");
		}

		if (me.containsKey("potions")) {
			potions = new ArrayList<>();
			Map<String, Integer> potionpairs = (Map<String, Integer>) me.get("potions");
			for (Map.Entry<String, Integer> entry : potionpairs.entrySet()) {
				String name = entry.getKey();
				int amp = entry.getValue();
				PotionEffect pot = new PotionEffect(
						PotionEffectType.getByName(name),
						Integer.MAX_VALUE,
						amp);
				potions.add(pot);
			}
		}

		if (me.containsKey("lucky_blocks")) {
			setLuckyBlockNum((int) me.get("lucky_blocks"));
		}
		if (me.containsKey("lucky_commands")) {
			setLuckyCommands((List<String>) me.get("lucky_commands"));
		}

		tpAtReset = !me.containsKey("tpAtReset") || (boolean) me.get("tpAtReset");
	}

	@NotNull
	public Map<String, Object> serialize() {
		Map<String, Object> me = new HashMap<>();
		me.put("minX", minX);
		me.put("minY", minY);
		me.put("minZ", minZ);
		me.put("maxX", maxX);
		me.put("maxY", maxY);
		me.put("maxZ", maxZ);
		me.put("world", world.getName());
		//Make string form of composition
		Map<String, Double> sComposition = new HashMap<>();
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			sComposition.put(entry.getKey().toString(), entry.getValue());
		}
		me.put("composition", sComposition);
		List<String> sStructure = new ArrayList<>();
		for (SerializableBlock entry : structure) {
			sStructure.add(entry.toString());
		}
		me.put("structure", sStructure);
		me.put("name", name);
		me.put("resetDelay", resetDelay);
		List<String> warnings = new LinkedList<>();
		for (Integer warning : resetWarnings) {
			warnings.add(warning.toString());
		}
		for (Integer warning : resetWarningsLastMinute) {
			warnings.add(warning.toString() + 's');
		}

		me.put("resetWarnings", warnings);
		if (surface != null) {
			me.put("surface", surface.toString());
		} else {
			me.put("surface", "");
		}
		me.put("fillMode", fillMode);
		me.put("resetClock", resetClock);
		me.put("isSilent", isSilent);
		me.put("tpX", tpX);
		me.put("tpY", tpY);
		me.put("tpZ", tpZ);
		me.put("tpYaw", tpYaw);
		me.put("tpPitch", tpPitch);

		me.put("resetPercent", resetPercent);

		Map<String, Integer> potionpairs = new HashMap<>();
		for (PotionEffect pe : this.potions) {
			potionpairs.put(pe.getType().getName(), pe.getAmplifier());
		}
		me.put("potions", potionpairs);

		me.put("lucky_blocks", luckyBlocks);
		me.put("lucky_commands", luckyCommands);

		me.put("tpAtReset", tpAtReset);

		return me;
	}

	public boolean getFillMode() {
		return fillMode;
	}

	public void setFillMode(boolean fillMode) {
		this.fillMode = fillMode;
	}

	public void setResetDelay(int minutes) {
		resetDelay = minutes;
		resetClock = minutes;
	}

	public void setResetWarnings(List<Integer> warnings) {
		resetWarnings = warnings;
	}

	public List<Integer> getResetWarnings() {
		return resetWarnings;
	}

	public void setResetWarningsLastMinute(List<Integer> warnings) {
		resetWarningsLastMinute = warnings;
	}

	public List<Integer> getResetWarningsLastMinute() {
		return resetWarningsLastMinute;
	}

	public int getResetDelay() {
		return resetDelay;
	}

	/**
	 * Return the length of time until the next automatic reset.
	 * The actual length of time is anywhere between n and n-1 minutes.
	 *
	 * @return clock ticks left until reset
	 */
	public int getTimeUntilReset() {
		return resetClock;
	}

	public int getSecondsUntilReset() {
		int ret = resetClock * 60;

		if (resetClock == 1 && lastMinueCounter != null && !lastMinueCounter.isCancelled() && secondsCounter != null) {
			ret = secondsCounter.getCount();
		}

		return ret;
	}

	public SerializableBlock getSurface() {
		return surface;
	}

	public void setSurface(SerializableBlock surface) {
		this.surface = surface;
	}

	public World getWorld() {
		return world;
	}

	public String getName() {
		return name;
	}

	public Map<SerializableBlock, Double> getComposition() {
		return composition;
	}

	public Set<SerializableBlock> getStructure() {
		return structure;
	}

	public boolean isSilent() {
		return isSilent;
	}

	public void setSilence(boolean isSilent) {
		this.isSilent = isSilent;
	}

	public double getCompositionTotal() {
		return MathUtil.round(composition.values().stream()
				.mapToDouble(Double::doubleValue)
				.sum(), 4);
	}

	public boolean isInside(Player p) {
		return isInside(p.getLocation());
	}

	public boolean isInside(Location l) {
		return Objects.equals(l.getWorld(), world)
				&& (l.getBlockX() >= minX && l.getBlockX() <= maxX)
				&& (l.getBlockY() >= minY && l.getBlockY() <= maxY)
				&& (l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ);
	}

	public boolean tpAtReset() {
		return tpAtReset;
	}

	public void setTpAtReset(boolean tpatreset) {
		this.tpAtReset = tpatreset;
	}

	public void setTp(Location l) {
		tpX = l.getBlockX();
		tpY = l.getBlockY();
		tpZ = l.getBlockZ();
		tpYaw = (int) l.getYaw();
		tpPitch = (int) l.getPitch();
	}

	private Location getTp() {
		return new Location(getWorld(), tpX, tpY, tpZ, tpYaw, tpPitch);
	}

	protected transient boolean resetting = false;

	protected transient List<CompositionEntry> probabilityMap;

	public boolean reset() {
		if (resetting)
			return false;

		resetting = true;
		final Mine mine = this;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (probabilityMap == null || probabilityMap.isEmpty()) {
					probabilityMap = mapComposition(composition);
				}

				if (tpAtReset) {
					Bukkit.getServer().getOnlinePlayers().stream()
							.filter(p -> isInside(p))
							.forEach(p -> teleport(p, true));
				}

				for (int x = minX; x <= maxX; ++x) {
					for (int y = minY; y <= maxY; ++y) {
						for (int z = minZ; z <= maxZ; ++z) {
							if (!fillMode || shoulBeFilled(world.getBlockAt(x, y, z).getType())) {
								if (y == maxY && surface != null) {
									Block b = world.getBlockAt(x, y, z);
									surface.setBlockTypeFor(b);
									continue;
								}
								double r = RAND.nextDouble();
								int finalX = x;
								int finalY = y;
								int finalZ = z;

								probabilityMap.stream()
										.filter(ce -> r <= ce.getChance())
										.findFirst()
										.ifPresent(ce -> ce.getBlock().setBlockTypeFor(world.getBlockAt(finalX, finalY, finalZ)));
							}
						}
					}
				}
				resetMRLP();
				resetting = false;
				MineResetEvent mre = new MineResetEvent(mine);
				Bukkit.getServer().getPluginManager().callEvent(mre);
			}
		}.runTaskLater(MineResetLite.getInstance(), Config.getResetDelay());

		return true;
	}

	private transient Set<Material> mineMaterials;
	private transient Set<Material> exceptions;  // these material won't be replaced.

	private void setMineMaterials() {
		if (mineMaterials == null) {
			mineMaterials = new HashSet<>();
		}

		for (SerializableBlock sb : this.composition.keySet())
			mineMaterials.add(sb.getBlockType());
	}

	private void setStructureMaterials() {
		if (exceptions == null) {
			exceptions = new HashSet<>();
		}

		for (SerializableBlock sb : this.structure)
			exceptions.add(sb.getBlockType());
	}

	protected boolean shoulBeFilled(Material mat) {
		if (mineMaterials == null || mineMaterials.size() == 0)
			setMineMaterials();
		if (exceptions == null)
			setStructureMaterials();

		return (mat == Material.AIR || (!mineMaterials.contains(mat) && !exceptions.contains(mat)));
	}

	transient SecondsCounter secondsCounter;
	transient BukkitTask lastMinueCounter;

	public void cron() {
		if (resetDelay == 0) {
			return;
		}
		if (resetClock > 0) {
			resetClock--; //Tick down to the reset
		}
		if (resetClock == 0) {
			if (!isSilent && !resetting) {
				MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", this), this);
			}
			reset();
			resetClock = resetDelay;
			return;
		}

		//if (!isSilent) {
		for (Integer warning : resetWarnings) {
			if (warning == resetClock) {
				MineResetLite.broadcast(Phrases.phrase("mineWarningBroadcast", this, warning), this);
			}

			if (resetClock == 1 && resetWarningsLastMinute.size() > 0 && isCancelled(lastMinueCounter)) {
				secondsCounter = new SecondsCounter();
				lastMinueCounter = secondsCounter.runTaskTimerAsynchronously(MineResetLite.getInstance(), 0L, 20L);
			}
		}
		//}
	}

	class SecondsCounter extends BukkitRunnable {
		int count = 60;

		@Override
		public void run() {
			if (resetWarningsLastMinute.contains(count)) {
				MineResetLite.broadcast(Phrases.phrase("mineWarningLastMinuteBroadcast", Mine.this, count), Mine.this);
			}
			count--;
			if (count < 1) {
				lastMinueCounter = null;
				cancel();
			}
		}

		public int getCount() {
			return count;
		}
	}

	private boolean isCancelled(BukkitTask task) {
		if (task == null) {
			return true;
		}

		try {
			return task.isCancelled();
		} catch (IllegalStateException e) {
			try {
				long period = (long) ReflectionUtil.getValue(task, "period", true);
				return (period == -2L);
			} catch (Exception ignored) {
				task.cancel();
				return true;
			}
		}
	}

	public static class CompositionEntry {
		private SerializableBlock block;
		private double chance;

		public CompositionEntry(SerializableBlock block, double chance) {
			this.block = block;
			this.chance = chance;
		}

		public SerializableBlock getBlock() {
			return block;
		}

		public double getChance() {
			return chance;
		}
	}

	protected static ArrayList<CompositionEntry> mapComposition(Map<SerializableBlock, Double> compositionIn) {
		ArrayList<CompositionEntry> probabilityMap = new ArrayList<>();
		Map<SerializableBlock, Double> composition = new HashMap<>(compositionIn);
		double max = 0;
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			max += entry.getValue();
		}
		//Pad the remaining percentages with air
		// use material name instead of int id.
		if (max < 1) {
			composition.put(new SerializableBlock("AIR"), MathUtil.round(1 - max, 4));
			max = 1;
		}
		double i = 0;
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			double v = entry.getValue() / max;
			i += v;
			probabilityMap.add(new CompositionEntry(entry.getKey(), i));
		}
		return probabilityMap;
	}

	public void teleport(Player player) {
		teleport(player, false);
	}

	protected void teleport(Player player, boolean straight_up) {
		Location destination;

		if (tpY != -Integer.MAX_VALUE) {
			destination = getTp();
		} else {
			if (straight_up) {
				Location playerLocation = player.getLocation();
				destination = new Location(world, playerLocation.getX(), maxY + 1D, playerLocation.getZ());
			} else {
				destination = this.centerOfGravity;
			}
			Block block = destination.getBlock();

			if (block.getType() != Material.AIR || block.getRelative(BlockFace.UP).getType() != Material.AIR) {
				destination = new Location(world, destination.getX(), destination.getWorld().getHighestBlockYAt(destination.getBlockX(), destination.getBlockZ()), destination.getZ());
			}
		}

		player.teleport(destination);
	}

	private void setMaxCount() {
		int dx = maxX - minX + 1;
		int dy = maxY - minY + 1;
		int dz = maxZ - minZ + 1;

		this.maxCount = dx * dy * dz;
	}

	public int getMaxCount() {
		return this.maxCount;
	}

	public void setResetPercent(double per) {
		this.resetPercent = per;
	}

	public double getResetPercent() {
		return this.resetPercent;
	}

	public void setBrokenBlocks(int broken) {
		int previous = this.currentBroken;
		this.currentBroken = broken;

		MineUpdatedEvent mue = new MineUpdatedEvent(this);
		Bukkit.getServer().getPluginManager().callEvent(mue);

		if (this.resetPercent > 0) {
			resetWarnings.stream()
					.map(warning -> (int) (this.maxCount * (1.0 - (warning * 0.01))))
					.filter(threshold -> previous < threshold && this.currentBroken >= threshold)
					.forEach(threshold -> MineResetLite.broadcast(Phrases.phrase("mineWarningPercentageBroadcast", this, threshold), this));

			if (this.currentBroken >= (this.maxCount * (1.0 - this.resetPercent))) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!isSilent && !resetting) {
							MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", Mine.this), Mine.this);
						}
						reset();
					}
				}.runTask(MineResetLite.getInstance());
			}
		}
	}

	public int getBrokenBlocks() {
		return this.currentBroken;
	}

	protected void resetMRLP() {
		this.currentBroken = 0;
		resetLuckyNumbers();
	}

	public List<PotionEffect> getPotions() {
		return this.potions;
	}

	public PotionEffect addPotion(@NotNull String potstr) {
		String[] tokens = potstr.split(":");
		int amp = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 1;

		removePotion(tokens[0]);

		PotionEffect pot = createPotionEffect(tokens[0], amp);
		if (pot != null) {
			potions.add(pot);
		}

		return pot;
	}

	private @Nullable PotionEffect createPotionEffect(String potionName, int amplifier) {
		try {
			return new PotionEffect(
					PotionEffectType.getByName(potionName),
					Integer.MAX_VALUE,
					amplifier);
		} catch (Throwable ignore) {
			return null;
		}
	}


	public void removePotion(final String pot) {
		potions.removeIf(pe -> pe.getType().getName().equalsIgnoreCase(pot));
	}

	public void redefine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.world = world;

		computeCenterOfGravity(this.world, this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ);
	}

	protected Location centerOfGravity;

	protected Location computeCenterOfGravity(World w, int min_x, int max_x, int min_y, int max_y, int min_z, int max_z) {
		Location max = new Location(w, Math.max(max_x, min_x), max_y, Math.max(max_z, min_z));
		Location min = new Location(w, Math.min(max_x, min_x), min_y, Math.min(max_z, min_z));

		centerOfGravity = max.add(min).multiply(0.5);
		return centerOfGravity;
	}

	public void setLuckyBlockNum(int num) {
		this.luckyBlocks = num;
		resetLuckyNumbers();
	}

	private void resetLuckyNumbers() {
		this.luckyNum = new ArrayList<>();

		IntStream.range(0, this.luckyBlocks)
				.map(i -> RAND.nextInt(this.maxCount) + 1)
				.forEach(this.luckyNum::add);
	}

	private boolean isLuckyNumber(int num) {
		return this.luckyNum != null && this.luckyNum.contains(num);
	}

	public boolean executeLuckyCommand(Player p) {
		if (isLuckyNumber(this.currentBroken) && !this.luckyCommands.isEmpty()) {
			int id = RAND.nextInt(this.luckyCommands.size());
			String cmd = this.luckyCommands.get(id).replace("%player%", p.getName());
			String[] cmds = cmd.split(";");

			try {
				Arrays.stream(cmds)
						.map(String::trim)
						.forEach(c -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), c));

				// play effect
				Optional.ofNullable(Config.getLuckyEffect()).ifPresent(effect -> p.getWorld().playEffect(p.getLocation(), effect, 0, 20));
				Optional.ofNullable(Config.getLuckySound()).ifPresent(sound -> p.getWorld().playSound(p.getLocation(), sound, 1F, 1F));

				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	protected void setLuckyCommands(List<String> list) {
		this.luckyCommands = list;
	}

	public void makeLucky(int num) {
		this.luckyNum.set(0, num);
	}
}
