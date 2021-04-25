package net.novauniverse.games.uhcv2.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.games.uhcv2.NovaUHCv2;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;

public class UHCCommand extends NovaCommand {

	public UHCCommand() {
		super("uhc", NovaUHCv2.getInstance());

		setAllowedSenders(AllowedSenders.ALL);
		setPermission("novauniverse.games.uhc.commands.uhc");
		setPermissionDefaultValue(PermissionDefault.OP);
		setDescription("Main command for uhc");
		setEmptyTabMode(true);

		
		
		addHelpSubCommand();
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		sender.sendMessage(ChatColor.GOLD + "Use " + ChatColor.AQUA + "/uhc help " + ChatColor.GOLD + "for help");
		return true;
	}

}
