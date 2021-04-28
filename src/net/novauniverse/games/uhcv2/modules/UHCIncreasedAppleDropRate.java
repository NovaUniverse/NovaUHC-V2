package net.novauniverse.games.uhcv2.modules;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import net.zeeraa.novacore.commons.utils.RandomGenerator;
import net.zeeraa.novacore.spigot.module.NovaModule;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.LocationUtils;

public class UHCIncreasedAppleDropRate extends NovaModule implements Listener {
	@Override
	public String getName() {
		return "UHCIncreasedAppleDropRate";
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getBlock().getType().name().toUpperCase().contains("LEAVES")) {
			if (RandomGenerator.generate(1, 20) == 19) {
				Location location = LocationUtils.centerLocation(e.getBlock().getLocation());

				location.getWorld().dropItemNaturally(location, ItemBuilder.materialToItemStack(Material.APPLE));
			}
		}
	}
}