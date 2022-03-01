package net.novauniverse.games.uhcv2.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.novauniverse.games.uhcv2.NovaUHCv2;
import net.novauniverse.games.uhcv2.tracker.UHCCompassTracker;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.timers.TickCallback;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.commons.utils.ListUtils;
import net.zeeraa.novacore.commons.utils.RandomGenerator;
import net.zeeraa.novacore.commons.utils.TextUtils;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependantUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependantSound;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker;
import net.zeeraa.novacore.spigot.module.modules.game.Game;
import net.zeeraa.novacore.spigot.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.DelayedGameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.GameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.ScheduledGameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.TriggerCallback;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.TriggerFlag;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect;
import net.zeeraa.novacore.spigot.world.worldgenerator.worldpregenerator.WorldPreGenerator;

public class UHCv2 extends Game implements Listener {
	private boolean ended;
	private boolean started;

	private World mainWorld;
	private World netherWorld;

	private int mainWorldSize;

	private int spawnLocationsToFind;

	private WorldPreGenerator mainWorldGenerator;
	private WorldPreGenerator netherWorldGenerator;

	private boolean gracePeriodActive;
	private DelayedGameTrigger endGracePeriodTrigger;
	private DelayedGameTrigger meetupTrigger;
	private DelayedGameTrigger finalHealTrigger;

	private long meetupTime;

	private long finalHealTime;

	private List<Location> spawnLocations;
	private List<Location> spawnLocationsToUse;

	private boolean spawnLocationsFound;

	private boolean preventAllDamage;

	private long gracePeriodTime;

	private boolean meetupStarted;

	private Task borderEscapePreventionTask;

	private Task fireResTask;

	private List<String> hasSpawned;
	private List<Player> toTeleport;
	private boolean teleportRunning;

	public List<Location> getSpawnLocations() {
		return spawnLocations;
	}

	public int getSpawnLocationsToFind() {
		return spawnLocationsToFind;
	}

	public long getGracePeriodTime() {
		return gracePeriodTime;
	}

	public UHCv2(World mainWorld, World netherWorld, int mainWorldSize, int spawnLocationsToFind, long meetupTime, long gracePeriodTime, long finalHealTime) {
		super(NovaUHCv2.getInstance());

		this.world = mainWorld;

		this.mainWorld = mainWorld;
		this.netherWorld = netherWorld;
		this.ended = false;
		this.started = false;

		this.mainWorldSize = mainWorldSize;

		this.spawnLocationsToFind = spawnLocationsToFind;

		this.spawnLocations = new ArrayList<Location>();
		this.spawnLocationsToUse = new ArrayList<Location>();

		this.endGracePeriodTrigger = null;
		this.meetupTrigger = null;

		this.mainWorldGenerator = null;
		this.spawnLocationsFound = false;

		this.preventAllDamage = false;

		this.meetupTime = meetupTime;

		this.gracePeriodTime = gracePeriodTime;

		this.meetupStarted = false;

		this.finalHealTime = finalHealTime;

		this.borderEscapePreventionTask = null;

		this.hasSpawned = new ArrayList<String>();
		this.toTeleport = new ArrayList<Player>();
		this.teleportRunning = false;

		this.fireResTask = null;
	}

	@Override
	public void onLoad() {
		this.fireResTask = new SimpleTask(new Runnable() {
			@Override
			public void run() {
				if (hasStarted()) {
					if (gracePeriodActive) {
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							if (players.contains(player.getUniqueId())) {
								boolean shouldGive = true;

								for (PotionEffect potionEffect : player.getActivePotionEffects()) {
									if (potionEffect.getType().getName().toLowerCase().contains("fire")) {
										if (potionEffect.getDuration() > 40) {
											shouldGive = false;
											break;
										}
									}
								}

								if (shouldGive) {
									player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0), true);
								}
							}
						}
					}
				}
			}
		}, 20L);

		this.mainWorldGenerator = new WorldPreGenerator(mainWorld, mainWorldSize + 10, 60, 1, new Callback() {
			@Override
			public void execute() {
				Log.info("UHC", "Main world generation complete");
				Log.info("UHC", "Starting spawn location scanner");
				runSpawnLocationScan();
			}
		});

		this.netherWorldGenerator = new WorldPreGenerator(netherWorld, mainWorldSize + 10, 60, 1, new Callback() {
			@Override
			public void execute() {
				Log.info("UHC", "Nether world generation complete");
			}
		});

		this.endGracePeriodTrigger = new DelayedGameTrigger("novauniverse.uhc.endgraceperiod", 1337L /* placeholder */, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger, TriggerFlag reason) {
				((ScheduledGameTrigger) trigger).stop();

				Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PVP is now enabled");

				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
				}

				gracePeriodActive = false;
			}
		});

		this.meetupTrigger = new DelayedGameTrigger("novauniverse.uhc.meetup", meetupTime * 20 /* in ticks */, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger, TriggerFlag reason) {
				((ScheduledGameTrigger) trigger).stop();

				Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Teleporting to meetup area");

				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
				}

				triggerMeetup();
			}
		});

		endGracePeriodTrigger.addFlag(TriggerFlag.STOP_ON_GAME_END);

		meetupTrigger.addFlag(TriggerFlag.START_ON_GAME_START);
		meetupTrigger.addFlag(TriggerFlag.RUN_ONLY_ONCE);
		meetupTrigger.addFlag(TriggerFlag.STOP_ON_GAME_END);

		meetupTrigger.addTickCallback(new TickCallback() {
			@Override
			public void execute(long timeLeft) {
				if (timeLeft % 20 != 19) {
					return;
				}
				int timeLeftReal = (int) timeLeft / 20;

				switch (timeLeftReal) {
				case 60:
					Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Meetup starts in" + ChatColor.AQUA + "" + ChatColor.BOLD + " 1 minute");
					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
					}
					break;

				case 600:
					Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Meetup starts in " + ChatColor.AQUA + "" + ChatColor.BOLD + "10 minutes");
					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
					}
					break;

				default:
					if (timeLeftReal < 6 && timeLeftReal > 0) {
						Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Meetup starts in " + ChatColor.AQUA + "" + ChatColor.BOLD + "" + timeLeftReal);
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
						}
					}
					break;
				}
			}
		});

		endGracePeriodTrigger.addTickCallback(new TickCallback() {
			@Override
			public void execute(long timeLeft) {
				if (timeLeft % 20 != 19) {
					return;
				}
				int timeLeftReal = (int) timeLeft / 20;

				switch ((int) timeLeftReal) {
				case 60:
					Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Grace period ends in" + ChatColor.AQUA + "" + ChatColor.BOLD + " 1 minute");
					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
					}
					break;

				case 600:
					Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Grace period ends in " + ChatColor.AQUA + "" + ChatColor.BOLD + "10 minutes");
					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
					}
					break;

				default:
					if (timeLeftReal < 6 && timeLeftReal > 0) {
						Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Grace period ends in " + ChatColor.AQUA + "" + ChatColor.BOLD + "" + timeLeftReal);
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
						}
					}
					break;
				}
			}
		});

		this.addTrigger(endGracePeriodTrigger);
		this.addTrigger(meetupTrigger);

		if (finalHealTime > 0) {
			finalHealTrigger = new DelayedGameTrigger("novauniverse.uhc.finalheal", finalHealTime * 20 /* in ticks */, new TriggerCallback() {
				@Override
				public void run(GameTrigger trigger, TriggerFlag reason) {
					((DelayedGameTrigger) trigger).stop();

					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						if (player.getGameMode() != GameMode.SPECTATOR) {
							player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have been healed");

							player.setSaturation(20F);
							player.setFoodLevel(20);
							VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING, 1F, 1.5F);

							PlayerUtils.fullyHealPlayer(player);
						}
					}
				}
			});

			finalHealTrigger.addTickCallback(new TickCallback() {
				@Override
				public void execute(long timeLeft) {
					if (timeLeft % 20 != 19) {
						return;
					}
					int timeLeftReal = (int) timeLeft / 20;

					switch ((int) timeLeftReal) {
					case 60:
						Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Final heal in" + ChatColor.AQUA + "" + ChatColor.BOLD + " 1 minute");
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
						}
						break;

					case 600:
						Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Final heal in " + ChatColor.AQUA + "" + ChatColor.BOLD + "10 minutes");
						for (Player player : Bukkit.getServer().getOnlinePlayers()) {
							VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING);
						}
						break;

					default:
						break;
					}
				}
			});

			finalHealTrigger.addFlag(TriggerFlag.START_ON_GAME_START);
			finalHealTrigger.addFlag(TriggerFlag.STOP_ON_GAME_END);
			finalHealTrigger.addFlag(TriggerFlag.RUN_ONLY_ONCE);

			this.addTrigger(finalHealTrigger);
		}

		mainWorldGenerator.start();
		netherWorldGenerator.start();

		mainWorld.getWorldBorder().setSize((mainWorldSize * 2) * 16);
		netherWorld.getWorldBorder().setSize((mainWorldSize * 2) * 16);
	}

	public World getMainWorld() {
		return mainWorld;
	}

	public World getNetherWorld() {
		return netherWorld;
	}

	public int getMainWorldSize() {
		return mainWorldSize;
	}

	@Override
	public String getName() {
		return "uhcv2";
	}

	@Override
	public String getDisplayName() {
		return "UHC";
	}

	@Override
	public PlayerQuitEliminationAction getPlayerQuitEliminationAction() {
		return NovaUHCv2.getInstance().isAllowReconnect() ? PlayerQuitEliminationAction.DELAYED : PlayerQuitEliminationAction.INSTANT;
	}

	@Override
	public boolean eliminatePlayerOnDeath(Player player) {
		return true;
	}

	@Override
	public boolean isPVPEnabled() {
		return !gracePeriodActive;
	}

	@Override
	public boolean autoEndGame() {
		return true;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity attacker, LivingEntity target) {
		// TODO:
		return true;
	}

	@Override
	public boolean canStart() {
		return mainWorldGenerator.isFinished() && netherWorldGenerator.isFinished() && spawnLocationsFound;
	}

	@Override
	public void onStart() {
		if (started) {
			return;
		}

		spawnLocationsToUse = ListUtils.cloneList(spawnLocations);
		toTeleport = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());

		borderEscapePreventionTask = new SimpleTask(new Runnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					if (player.getGameMode() == GameMode.SPECTATOR) {
						if (player.getWorld().getUID().toString().equalsIgnoreCase(mainWorld.getUID().toString()) || player.getWorld().getUID().toString().equalsIgnoreCase(netherWorld.getUID().toString())) {
							double borderSize = player.getWorld().getWorldBorder().getSize() / 2;

							// Log.trace("Border size: " + borderSize);

							double px = player.getLocation().getX();
							double pz = player.getLocation().getZ();

							if (px > borderSize || (px < 0 && (px * -1) > borderSize)) {
								Location newLocation = player.getLocation().clone();

								newLocation.setX(px < 0 ? borderSize * -1 : borderSize);

								player.teleport(newLocation);
							}

							if (pz > borderSize || (pz < 0 && (pz * -1) > borderSize)) {
								Location newLocation = player.getLocation().clone();

								newLocation.setZ(pz < 0 ? borderSize * -1 : borderSize);

								player.teleport(newLocation);
							}
						}
					}
				}
			}
		}, 5L);

		Task.tryStartTask(borderEscapePreventionTask);
		Task.tryStartTask(fireResTask);

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 137420, 0, true, false), true);
		}

		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Please wait for all players to teleport");

		tpPlayersToArena();

		started = true;
	}

	public void tpPlayersToArena() {
		teleportRunning = true;
		preventAllDamage = true;

		if (toTeleport.size() == 0) {
			Log.info("Teleport completed");
			new BukkitRunnable() {
				@Override
				public void run() {
					preventAllDamage = false;
				}
			}.runTaskLater(NovaUHCv2.getInstance(), 60L);

			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
				PlayerUtils.clearPotionEffects(player);
				player.setFireTicks(0);
				player.setSaturation(20F);
			}

			teleportRunning = false;
			startGracePeriod(gracePeriodTime);
			this.sendBeginEvent();
			return;
		}

		Player player = toTeleport.remove(0);

		if (player.isOnline()) {
			Log.info("Spwning player" + player.getName() + ". " + toTeleport.size() + " left");
			spawnPlayer(player);
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				tpPlayersToArena();
			}

		}.runTaskLater(NovaUHCv2.getInstance(), 1L);
	}

	private void spawnPlayer(Player player) {
		boolean isPlaying = players.contains(player.getUniqueId());

		PlayerUtils.clearPlayerInventory(player);
		PlayerUtils.resetPlayerXP(player);
		PlayerUtils.setMaxHealth(player, 40);
		PlayerUtils.fullyHealPlayer(player);

		player.setGameMode(isPlaying ? GameMode.SURVIVAL : GameMode.SPECTATOR);
		player.setFireTicks(0);
		player.setSaturation(20F);

		world.setTime(1000);

		if (spawnLocationsToUse.size() == 0) {
			spawnLocationsToUse = ListUtils.cloneList(spawnLocations);
		}

		Location location = spawnLocationsToUse.remove(0);

		safeTeleport(player, location);

		new BukkitRunnable() {
			@Override
			public void run() {
				safeTeleport(player, location);
			}
		}.runTaskLater(NovaUHCv2.getInstance(), 5L);

		hasSpawned.add(player.getUniqueId().toString());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (teleportRunning) {
			double fromX = event.getFrom().getX();
			double fromZ = event.getFrom().getZ();
			double toX = event.getTo().getX();
			double toZ = event.getTo().getZ();
			if (!(fromX == toX)) {
				event.getTo().setX(fromX);
			}

			if (!(fromZ == toZ)) {
				event.getTo().setZ(fromZ);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (hasStarted()) {
			if (!hasSpawned.contains(e.getPlayer().getUniqueId().toString())) {
				if (teleportRunning) {
					toTeleport.add(e.getPlayer());
				} else {
					spawnPlayer(e.getPlayer());
				}
			}
		}
	}

	@Override
	public void onEnd(GameEndReason reason) {
		if (ended) {
			return;
		}

		try {
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				p.setHealth(p.getMaxHealth());
				p.setFoodLevel(20);
				PlayerUtils.clearPlayerInventory(p);
				PlayerUtils.resetPlayerXP(p);
				p.setGameMode(GameMode.SPECTATOR);
				VersionIndependantUtils.get().playSound(p, p.getLocation(), VersionIndependantSound.WITHER_DEATH, 1F, 1F);

				Firework fw = (Firework) p.getLocation().getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
				FireworkMeta fwm = fw.getFireworkMeta();

				fwm.setPower(2);
				fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

				fw.setFireworkMeta(fwm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Task.tryStopTask(borderEscapePreventionTask);
		Task.tryStopTask(fireResTask);

		ended = true;
	}

	private void startNoTpDamageTime() {
		preventAllDamage = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				preventAllDamage = false;
			}
		}.runTaskLater(NovaUHCv2.getInstance(), 60L);
	}

	public void triggerMeetup() {
		startGracePeriod(20);

		startNoTpDamageTime();

		ModuleManager.enable(CompassTracker.class);

		CompassTracker.getInstance().setStrictMode(true);
		CompassTracker.getInstance().setCompassTrackerTarget(new UHCCompassTracker());

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			player.getInventory().addItem(new ItemBuilder(Material.COMPASS).setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Player tracker").addLore(ChatColor.AQUA + "Points towards the closest player").build());

			Location location = LocationUtils.centerLocation(mainWorld.getHighestBlockAt(new Location(mainWorld, RandomGenerator.generateDouble(-31, 31), 0, RandomGenerator.generateDouble(-31, 31))).getLocation().clone().add(0, 3, 0));

			safeTeleport(player, location);
			new BukkitRunnable() {
				@Override
				public void run() {
					safeTeleport(player, location);
				}
			}.runTaskLater(NovaUHCv2.getInstance(), 5L);
		}

		mainWorld.getWorldBorder().setCenter(0.5, 0.5);
		mainWorld.getWorldBorder().setSize(66);

		meetupStarted = true;
	}

	public void startGracePeriod(long time) {
		if (endGracePeriodTrigger.isRunning()) {
			endGracePeriodTrigger.stop();
		}

		Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Grace period will end in " + ChatColor.AQUA + "" + ChatColor.BOLD + TextUtils.formatTimeToText(time));

		gracePeriodActive = true;

		endGracePeriodTrigger.setDelay(time * 20); // in ticks

		endGracePeriodTrigger.start();
	}

	public void safeTeleport(Player player, Location location) {
		player.teleport(LocationUtils.centerLocation(location));
		player.setFallDistance(0);
	}

	private int scanFails = 0;

	private void runSpawnLocationScan() {
		if (spawnLocations.size() >= spawnLocationsToFind) {
			spawnLocationsFound = true;
			return;
		}

		if (scanFails >= 100) {
			Log.fatal("UHC", "Players spawn location scan failed 100 times, this may indicate that the world does not have any valid spawn locations. The server will now restart to resolve this softlock");
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "The world does not seem to have any valid spawn location. The server will now restart because of this");

			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.getServer().shutdown();
				}
			}.runTaskLater(NovaUHCv2.getInstance(), 100L);

			return;
		}

		int tries = 0;
		while (true) {
			if (tries > 100) {
				Log.warn("UHC", "Failed to find spawn location within 100 tries");
				scanFails++;
				break;
			}

			// We want the minimum distance to the border to be 1 chunk
			int size = (mainWorldSize - 1) * 16;

			int x = RandomGenerator.generate(size * -1, size);
			int z = RandomGenerator.generate(size * -1, size);

			Location location = new Location(mainWorld, x, 256, z);

			boolean success = false;

			while (location.getBlockY() > 0) {
				Block block = location.getBlock();

				if (block.isLiquid()) {
					// Do now spawn player in liquids
					break;
				}

				// Seems like the plugin dont think leaves are solid
				if (block.getType().isSolid() || block.getType().name().toUpperCase().contains("LEAVES")) {
					location.add(0, 2, 0);
					spawnLocations.add(location);
					success = true;

					Log.trace("UHC", "Found valid spawn location at X: " + location.getBlockX() + " Y: " + location.getBlockY() + " Z: " + location.getBlockZ() + " (" + spawnLocations.size() + " / " + spawnLocationsToFind + ")");

					break;
				}

				location.add(0, -1, 0);
			}

			if (success) {
				break;
			}

			tries++;
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				runSpawnLocationScan();
			}

		}.runTaskLater(NovaUHCv2.getInstance(), 1L);
	}

	public void stopGracePeriod() {
		gracePeriodActive = false;

		if (this.endGracePeriodTrigger.isRunning()) {
			this.endGracePeriodTrigger.stop();
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (hasStarted()) {
			e.getEntity().setGameMode(GameMode.SPECTATOR);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent e) {
		if (teleportRunning) {
			e.setCancelled(true);
		}

		if (meetupStarted) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityPortal(EntityPortalEvent e) {
		if (teleportRunning) {
			e.setCancelled(true);
		}

		if (meetupStarted) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (teleportRunning) {
			e.setCancelled(true);
		}

		if (e.getEntity() instanceof Player) {
			if (preventAllDamage) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity() instanceof Player) {
			if (players.contains(e.getEntity().getUniqueId())) {
				if (e.getRegainReason() == RegainReason.SATIATED || e.getRegainReason() == RegainReason.REGEN) {
					e.setCancelled(true);
				}
			}
		}
	}
}