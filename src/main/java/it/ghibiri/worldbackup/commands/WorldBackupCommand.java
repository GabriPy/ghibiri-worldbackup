package it.ghibiri.worldbackup.commands;

import it.ghibiri.worldbackup.scheduler.AutoBackupScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import it.ghibiri.worldbackup.lang.LangManager;

public class WorldBackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final AutoBackupScheduler scheduler;
    private final LangManager lang;

    public WorldBackupCommand(JavaPlugin plugin, AutoBackupScheduler scheduler, LangManager lang) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(lang.msg("worldbackup.reloadUsage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            lang.reload();
            scheduler.startOrUpdateFromConfig();
            sender.sendMessage(lang.msg("worldbackup.reloaded"));
            return true;
        }

        sender.sendMessage(lang.msg("worldbackup.reloadUsage "));
        return true;
    }
}
