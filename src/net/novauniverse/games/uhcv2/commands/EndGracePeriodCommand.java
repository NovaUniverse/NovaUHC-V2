package net.novauniverse.games.uhcv2.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.games.uhcv2.NovaUHCv2;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaSubCommand;

public class EndGracePeriodCommand extends NovaSubCommand {
	public EndGracePeriodCommand() {
		super("endgraceperiod");
		
		setAllowedSenders(AllowedSenders.ALL);
		setPermission("novauniverse.games.uhc.commands.uhc.endgraceperiod");
		setPermissionDefaultValue(PermissionDefault.OP);
		setDescription("Main command for uhc");
		setEmptyTabMode(true);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		NovaUHCv2.getInstance().getGame().stopGracePeriod();
		
		sender.sendMessage(ChatColor.GREEN + "Ended grace period and timers");
		
		return true;
	}

}
