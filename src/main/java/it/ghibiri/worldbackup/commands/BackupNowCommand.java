package it.ghibiri.worldbackup.commands;

import it.ghibiri.worldbackup.backup.BackupService;
import it.ghibiri.worldbackup.lang.LangManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BackupNowCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BackupService backupService;
    private final LangManager lang;

    public BackupNowCommand(JavaPlugin plugin, BackupService backupService, LangManager lang) {
        this.plugin = plugin;
        this.backupService = backupService;
        this.lang = lang; // ✅ ora è corretto
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        boolean force = args.length > 0 && args[0].equalsIgnoreCase("force");

        // Se backup in corso, stop immediato
        if (backupService.isRunning()) {
            sender.sendMessage(lang.msg("backup.alreadyRunning"));
            return true;
        }

        // Cooldown
        int cooldownMin = plugin.getConfig().getInt("cooldownMinutes", 30);
        long cooldownMs = Math.max(0, cooldownMin) * 60_000L;

        long last = backupService.getLastBackupStartMillis();
        long now = System.currentTimeMillis();
        long remaining = (last + cooldownMs) - now;

        // Se vuoi forzare, controlliamo permesso
        if (force) {
            if (!sender.hasPermission("worldbackup.force")) {
                sender.sendMessage(lang.msg("cooldown.noForcePerm")); // ✅ chiave giusta dal tuo yml
                return true;
            }
        } else {
            if (cooldownMs > 0 && last > 0 && remaining > 0) {
                long sec = (remaining + 999) / 1000;
                long min = sec / 60;
                long s = sec % 60;

                Map<String, String> ph = new HashMap<>();
                ph.put("min", String.valueOf(min));
                ph.put("sec", String.valueOf(s));

                sender.sendMessage(lang.msg("backup.cooldownBackup", ph)); // ✅ con placeholder
                sender.sendMessage(lang.msg("backup.wannaForce"));
                return true;
            }
        }

        // Avvio
        sender.sendMessage(lang.msg("backup.started"));
        backupService.runBackup();
        return true;
    }
}
