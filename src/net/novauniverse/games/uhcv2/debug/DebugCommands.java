package net.novauniverse.games.uhcv2.debug;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import net.novauniverse.games.uhcv2.NovaUHCv2;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.utils.ListUtils;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.debug.DebugCommandRegistrator;
import net.zeeraa.novacore.spigot.debug.DebugTrigger;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;

public class DebugCommands {
	private SimpleTask task;
	private List<Location> testLocations;

	public DebugCommands() {
		testLocations = new ArrayList<Location>();
		task = new SimpleTask(new Runnable() {
			@Override
			public void run() {
				if (testLocations.size() > 0) {
					Location location = testLocations.remove(0);

					for (Player player : Bukkit.getServer().getOnlinePlayers()) {
						NovaUHCv2.getInstance().getGame().safeTeleport(player, location);

						player.sendMessage(ChatColor.LIGHT_PURPLE + "Testing location " + ChatColor.AQUA + (testLocations.size() + 1) + ChatColor.LIGHT_PURPLE + " / " + ChatColor.AQUA + NovaUHCv2.getInstance().getGame().getSpawnLocationsToFind());

						new BukkitRunnable() {
							@Override
							public void run() {
								NovaUHCv2.getInstance().getGame().safeTeleport(player, location);
							}
						}.runTaskLater(NovaUHCv2.getInstance(), 5L);
					}
				} else {
					Task.tryStopTask(task);
				}
			}
		}, 40);

		DebugCommandRegistrator.getInstance().addDebugTrigger(new DebugTrigger() {
			@Override
			public void onExecute(CommandSender sender, String commandLabel, String[] args) {
				task.start();
				testLocations = ListUtils.cloneList(NovaUHCv2.getInstance().getGame().getSpawnLocations());
				sender.sendMessage(ChatColor.GREEN + "Test started");
			}

			@Override
			public PermissionDefault getPermissionDefault() {
				return PermissionDefault.OP;
			}

			@Override
			public String getPermission() {
				return "novauniverse.debug.uhc.testuhcspawnlocations";
			}

			@Override
			public String getName() {
				return "testuhcspawnlocations";
			}

			@Override
			public AllowedSenders getAllowedSenders() {
				return AllowedSenders.ALL;
			}
		});
	}
}