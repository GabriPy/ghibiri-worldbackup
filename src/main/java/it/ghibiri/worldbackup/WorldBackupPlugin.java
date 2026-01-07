package it.ghibiri.worldbackup;

import it.ghibiri.worldbackup.backup.BackupService;
import it.ghibiri.worldbackup.commands.AutoBackupCommand;
import it.ghibiri.worldbackup.commands.BackupNowCommand;
import it.ghibiri.worldbackup.scheduler.AutoBackupScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import it.ghibiri.worldbackup.commands.WorldBackupCommand;
import it.ghibiri.worldbackup.commands.WorldBackupTabCompleter;
import it.ghibiri.worldbackup.lang.LangManager;



import java.io.File;

public class WorldBackupPlugin extends JavaPlugin {

    private BackupService backupService;
    private AutoBackupScheduler scheduler;

    private LangManager langManager;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        // LANG
        this.langManager = new LangManager(this);
        langManager.reload();

        // Cartelle plugin
        getDataFolder().mkdirs();
        new File(getDataFolder(), "backups").mkdirs();

        // Servizi
        backupService = new BackupService(this, langManager);
        scheduler = new AutoBackupScheduler(this, backupService);

        // Comandi
        if (getCommand("backupnow") != null) {
            getCommand("backupnow").setExecutor(new BackupNowCommand(this, backupService, langManager));
        }

        if (getCommand("autobackup") != null) {
            getCommand("autobackup").setExecutor(new AutoBackupCommand(this, scheduler, langManager));
        }

        if (getCommand("worldbackup") != null) {
            getCommand("worldbackup").setExecutor(new WorldBackupCommand(this, scheduler, langManager));
        }

        WorldBackupTabCompleter tab = new WorldBackupTabCompleter();

        if (getCommand("backupnow") != null) {
            getCommand("backupnow").setTabCompleter(tab);
        }
        if (getCommand("autobackup") != null) {
            getCommand("autobackup").setTabCompleter(tab);
        }
        if (getCommand("worldbackup") != null) {
            getCommand("worldbackup").setTabCompleter(tab);
        }


        // Avvia autobackup da config
        scheduler.startOrUpdateFromConfig();


        getLogger().info("WorldBackup acceso.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        getLogger().info("WorldBackup spento.");
    }
}
