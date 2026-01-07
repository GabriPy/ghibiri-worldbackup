package it.ghibiri.worldbackup.commands;

import it.ghibiri.worldbackup.scheduler.AutoBackupScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import it.ghibiri.worldbackup.lang.LangManager;


public class AutoBackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final AutoBackupScheduler scheduler;
    private final LangManager lang;

    public AutoBackupCommand(JavaPlugin plugin, AutoBackupScheduler scheduler, LangManager lang) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(lang.msg("autobackup.everyUsage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "now" -> {
                boolean autoEnabled = plugin.getConfig().getBoolean("autoEnabled", true);
                String mode = plugin.getConfig().getString("mode", "daily");
                int every = plugin.getConfig().getInt("everyMinutes", 60);
                sender.sendMessage(lang.msg("autobackup.now") + autoEnabled + " " + lang.msg("mode") + " " + mode);
                return true;
            }
            case "on" -> {
                plugin.getConfig().set("autoEnabled", true);
                plugin.saveConfig();
                scheduler.startOrUpdateFromConfig();
                sender.sendMessage(lang.msg("autobackup.on"));
                return true;
            }
            case "off" -> {
                plugin.getConfig().set("autoEnabled", false);
                plugin.saveConfig();
                scheduler.stop();
                sender.sendMessage(lang.msg("autobackup.off"));
                return true;
            }
            case "daily", "hourly" -> {
                plugin.getConfig().set("mode", sub);
                plugin.saveConfig();
                scheduler.startOrUpdateFromConfig();
                sender.sendMessage(lang.msg("autobackup.set") + sub);
                return true;
            }
            case "every" -> {
                if (args.length < 2) {
                    sender.sendMessage(lang.msg("autobackup.everyUsage"));
                    return true;
                }
                int minutes;
                try {
                    minutes = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("autobackup.minutesErr");
                    return true;
                }
                if (minutes < 1) {
                    sender.sendMessage(lang.msg("autobackup.minutesMust"));
                    return true;
                }

                plugin.getConfig().set("mode", "every");
                plugin.getConfig().set("everyMinutes", minutes);
                plugin.saveConfig();
                scheduler.startOrUpdateFromConfig();
                sender.sendMessage(lang.msg("autobackup.everySetted") + minutes + " min");
                return true;
            }
            default -> {
                sender.sendMessage(lang.msg("everyUsage"));
                return true;
            }
        }
    }
}
