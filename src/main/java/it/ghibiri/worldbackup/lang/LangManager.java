package it.ghibiri.worldbackup.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class LangManager {

    private final JavaPlugin plugin;

    private YamlConfiguration lang;
    private String langCode;

    // fallback statico: MAI chiamare msg() per costruire il prefix
    private static final String DEFAULT_PREFIX = "&7[&bWorldBackup&7]&r ";

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.langCode = plugin.getConfig().getString("lang", "it_IT");

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Impossibile creare cartella lang/");
        }

        File langFile = new File(langDir, langCode + ".yml");

        // Se non esiste, prova a copiarlo dal jar (resources)
        if (!langFile.exists()) {
            if (!copyDefaultLang(langCode, langFile)) {
                // fallback su it_IT
                if (!"it_IT".equalsIgnoreCase(langCode)) {
                    plugin.getLogger().warning("Fallback lingua su it_IT.");
                    this.langCode = "it_IT";
                    langFile = new File(langDir, "it_IT.yml");
                    if (!langFile.exists()) {
                        copyDefaultLang("it_IT", langFile);
                    }
                }
            }
        }

        this.lang = YamlConfiguration.loadConfiguration(langFile);
        plugin.getLogger().info("Lingua caricata: " + this.langCode + " (" + langFile.getName() + ")");
    }

    /**
     * Copia lang/<code>.yml dalle resources al disco.
     * Ritorna true se copiato, false se non trovato o errore.
     */
    private boolean copyDefaultLang(String code, File outFile) {
        String resPath = "lang/" + code + ".yml";

        try (InputStream in = plugin.getResource(resPath)) {
            if (in == null) {
                plugin.getLogger().warning("Resource non trovata: " + resPath);
                return false;
            }

            // assicura che la cartella esista
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Impossibile creare cartella: " + parent.getAbsolutePath());
            }

            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Creato file lingua: " + outFile.getName());
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Impossibile copiare file lingua: " + code);
            e.printStackTrace();
            return false;
        }
    }

    public String msg(String path) {
        ensureLoaded();

        // Leggi prefix dal file lingua, fallback statico
        String prefix = color(lang.getString("prefix", DEFAULT_PREFIX));

        // Leggi messaggio
        String raw = lang.getString(path, "&cMissing lang key: " + path);

        // Sostituzione {prefix} ovunque TRANNE se stai chiedendo la key "prefix"
        if (!"prefix".equalsIgnoreCase(path)) {
            raw = raw.replace("{prefix}", prefix);
        }

        return color(raw);
    }

    public String msg(String path, Map<String, String> placeholders) {
        String s = msg(path);
        if (placeholders == null || placeholders.isEmpty()) return s;

        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (key == null) continue;
            if (val == null) val = "";

            s = s.replace("{" + key + "}", val);
        }
        return s;
    }

    private void ensureLoaded() {
        if (lang == null) {
            reload();
        }
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String getLangCode() {
        return langCode;
    }
}
