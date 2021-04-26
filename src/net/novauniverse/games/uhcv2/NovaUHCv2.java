package net.novauniverse.games.uhcv2;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.novauniverse.games.uhcv2.debug.DebugCommands;
import net.novauniverse.games.uhcv2.game.UHCv2;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.module.modules.gamelobby.GameLobby;

public class NovaUHCv2 extends JavaPlugin implements Listener {
	private static NovaUHCv2 instance;
	
	private boolean allowReconnect;
	private boolean combatTagging;
	private int reconnectTime;
	
	private UHCv2 game;
	
	public static NovaUHCv2 getInstance() {
		return instance;
	}
	
	public UHCv2 getGame() {
		return game;
	}
	
	public boolean isAllowReconnect() {
		return allowReconnect;
	}

	public boolean isCombatTagging() {
		return combatTagging;
	}

	public int getReconnectTime() {
		return reconnectTime;
	}
	
	@Override
	public void onEnable() {
		NovaUHCv2.instance = this;

		saveDefaultConfig();

		allowReconnect = getConfig().getBoolean("allow_reconnect");
		combatTagging = getConfig().getBoolean("combat_tagging");
		reconnectTime = getConfig().getInt("player_elimination_delay");
		
		int mainWorldSize = getConfig().getInt("main_world_size");
		int spawnLocationsToFind = getConfig().getInt("spawn_locations_to_find");

		long meetupTime = getConfig().getLong("meetup_time");
		
		long finalHealTime = getConfig().getLong("final_heal_time");
		
		long gracePeriodTime =  getConfig().getLong("grace_period_time");
		
		GameManager.getInstance().setUseCombatTagging(combatTagging);
		
		ModuleManager.require(GameManager.class);
		ModuleManager.require(GameLobby.class);
		
		World mainWorld = Bukkit.getServer().getWorld("world");
		World netherWorld = Bukkit.getServer().getWorld("world_nether");
		
		this.game = new UHCv2(mainWorld, netherWorld, mainWorldSize, spawnLocationsToFind, meetupTime, gracePeriodTime, finalHealTime);
		
		GameManager.getInstance().loadGame(game);
		
		GameManager.getInstance().setAutoRespawn(true);
		GameManager.getInstance().setCombatTaggingTime(5);
		GameManager.getInstance().setUseCombatTagging(combatTagging);
		
		new DebugCommands();
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin)this);
		Bukkit.getScheduler().cancelTasks(this);
	}
}