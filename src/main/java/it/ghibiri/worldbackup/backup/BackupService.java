package it.ghibiri.worldbackup.backup;

import it.ghibiri.worldbackup.github.GitHubReleaseUploader;
import it.ghibiri.worldbackup.lang.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final GitHubReleaseUploader uploader;

    private boolean running = false;
    private volatile long lastBackupStartMillis = 0;

    public BackupService(JavaPlugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.uploader = new GitHubReleaseUploader(plugin);
    }

    public synchronized void runBackup() {
        if (running) {
            plugin.getLogger().warning("Backup già in corso, salto.");
            broadcastToAdmins(lang.msg("backup.alreadyRunning"));
            return;
        }

        running = true;
        lastBackupStartMillis = System.currentTimeMillis();

        // Salvataggio "sicuro" sul main thread, poi zip in async
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.savePlayers();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::doBackup);
        });
    }

    private void doBackup() {
        long start = System.currentTimeMillis();
        File zipFile = null;

        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            // Nome file locale: yyyy-MM-dd_HH-mm.zip
            String baseName = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
            zipFile = new File(backupDir, baseName + ".zip");

            int i = 2;
            while (zipFile.exists()) {
                zipFile = new File(backupDir, baseName + "-" + i + ".zip");
                i++;
            }

            // Crea ZIP con tutti i mondi
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                List<World> worlds = Bukkit.getWorlds();
                for (World world : worlds) {
                    File worldFolder = world.getWorldFolder();
                    zipFolder(worldFolder, worldFolder.getName(), zos);
                }
            }

            long ms = System.currentTimeMillis() - start;
            double mb = zipFile.length() / 1024.0 / 1024.0;

            Map<String, String> placeholders = Map.of(
                    "file", zipFile.getName(),
                    "mb", String.format("%.2f", mb),
                    "sec", String.valueOf(ms / 1000)
            );

            broadcastToAdmins(lang.msg("backup.done", placeholders));

            cleanupOldBackups(backupDir);

            // Upload su GitHub Releases (se attivo)
            if (plugin.getConfig().getBoolean("uploadToGithub", false)) {
                uploader.uploadBackup(zipFile);
            } else {
                // opzionale: messaggio che manca creds / disattivato
                broadcastToAdmins(lang.msg("backup.missingGithubCreds"));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Errore durante il backup!");
            e.printStackTrace();
            broadcastToAdmins(lang.msg("backup.failed"));
        } finally {
            running = false;
        }
    }

    private void zipFolder(File folder, String entryName, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {

            // session.lock spesso bloccato su Windows -> skip
            if (file.isFile() && file.getName().equalsIgnoreCase("session.lock")) {
                continue;
            }

            String zipEntryName = entryName + "/" + file.getName();

            if (file.isDirectory()) {
                zipFolder(file, zipEntryName, zos);
            } else {
                zipFileWithRetry(file, zipEntryName, zos);
            }
        }
    }

    private void zipFileWithRetry(File file, String zipEntryName, ZipOutputStream zos) throws IOException {
        int attempts = 3;

        for (int a = 1; a <= attempts; a++) {
            try (FileInputStream fis = new FileInputStream(file)) {
                zos.putNextEntry(new ZipEntry(zipEntryName));
                fis.transferTo(zos);
                zos.closeEntry();
                return;
            } catch (IOException ex) {
                if (a == attempts) {
                    plugin.getLogger().warning("SKIP file bloccato: " + file.getAbsolutePath());
                    return;
                }
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void cleanupOldBackups(File backupDir) {
        int keep = plugin.getConfig().getInt("keepLastLocal", 10);
        if (keep < 1) return;

        File[] zips = backupDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (zips == null) return;

        // Ordina dal più nuovo al più vecchio
        Arrays.sort(zips, Comparator.comparingLong(File::lastModified).reversed());

        // Cancella dal keep in poi
        for (int i = keep; i < zips.length; i++) {
            File f = zips[i];
            boolean ok = f.delete();
            if (ok) plugin.getLogger().info("Backup vecchio eliminato: " + f.getName());
            else plugin.getLogger().warning("Non riesco a eliminare: " + f.getName());
        }
    }

    public long getLastBackupStartMillis() {
        return lastBackupStartMillis;
    }

    public boolean isRunning() {
        return running;
    }

    private void broadcastToAdmins(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.isOp() || p.hasPermission("worldbackup.admin") || p.hasPermission("worldbackup.backupnow")) {
                p.sendMessage(msg);
            }
        });
    }
}
