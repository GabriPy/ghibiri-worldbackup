package it.ghibiri.worldbackup.scheduler;

import it.ghibiri.worldbackup.backup.BackupService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class AutoBackupScheduler {

    private final JavaPlugin plugin;
    private final BackupService backupService;
    private BukkitTask task;

    public AutoBackupScheduler(JavaPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }

    public void startOrUpdateFromConfig() {
        stop();

        boolean autoEnabled = plugin.getConfig().getBoolean("autoEnabled", true);
        if (!autoEnabled) {
            plugin.getLogger().info("Autobackup OFF (config).");
            return;
        }

        String mode = plugin.getConfig().getString("mode", "daily").toLowerCase();
        int everyMinutes = plugin.getConfig().getInt("everyMinutes", 60);

        long periodTicks;
        switch (mode) {
            case "hourly" -> periodTicks = 20L * 60L * 60L;
            case "every" -> periodTicks = 20L * 60L * Math.max(1, everyMinutes);
            case "daily" -> periodTicks = 20L * 60L * 60L * 24L;
            default -> {
                plugin.getLogger().warning("mode non valido, uso daily.");
                periodTicks = 20L * 60L * 60L * 24L;
            }
        }

        // Parte dopo 60 secondi, poi ripete
        task = Bukkit.getScheduler().runTaskTimer(plugin, backupService::runBackup, 20L * 60L, periodTicks);

        plugin.getLogger().info("Autobackup ON: mode=" + mode + " periodTicks=" + periodTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
