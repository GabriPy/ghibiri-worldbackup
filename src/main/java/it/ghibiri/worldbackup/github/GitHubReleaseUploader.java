package it.ghibiri.worldbackup.github;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;


public class GitHubReleaseUploader {

    private final JavaPlugin plugin;
    private final HttpClient client;

    public GitHubReleaseUploader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newHttpClient();
    }

    public void uploadBackup(File zipFile) {
        try {
            String repo = plugin.getConfig().getString("githubRepo", "");
            String token = plugin.getConfig().getString("githubToken", "");

            if (repo.isEmpty() || token.isEmpty()) {
                plugin.getLogger().warning("GitHub upload SKIP: repo o token mancanti");
                return;
            }

            String[] parts = repo.split("/");
            if (parts.length != 2) {
                plugin.getLogger().warning("GitHub upload SKIP: githubRepo deve essere owner/repo");
                return;
            }

            String owner = parts[0];
            String repoName = parts[1];

            String tag = "backup-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            String releaseTitle = new SimpleDateFormat("HH:mm dd/MM/yyyy").format(new Date());

            // === CREA RELEASE ===
            String json = """
                    {
                      "tag_name": "%s",
                      "name": "%s",
                      "body": "Backup automatico del server Minecraft",
                      "draft": false,
                      "prerelease": false
                    }
                    """.formatted(tag, releaseTitle);

            HttpRequest createRelease = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repoName + "/releases"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WorldBackup")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> createResp =
                    client.send(createRelease, HttpResponse.BodyHandlers.ofString());

            if (createResp.statusCode() != 201) {
                plugin.getLogger().warning("GitHub release FALLITA: " + createResp.statusCode());
                plugin.getLogger().warning(createResp.body());
                return;
            }

            String uploadUrl = extractUploadUrl(createResp.body());
            if (uploadUrl == null) {
                plugin.getLogger().warning("Upload URL non trovato");
                return;
            }

            // === UPLOAD ASSET ===
            byte[] data = Files.readAllBytes(zipFile.toPath());
            String encodedName = URLEncoder.encode(zipFile.getName(), StandardCharsets.UTF_8);

            HttpRequest upload = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl + "?name=" + encodedName))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WorldBackup")
                    .header("Content-Type", "application/zip")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();

            HttpResponse<String> uploadResp =
                    client.send(upload, HttpResponse.BodyHandlers.ofString());

            if (uploadResp.statusCode() == 201) {
                plugin.getLogger().info("GitHub upload OK: " + zipFile.getName());
                cleanupOldBackupReleases(owner, repoName, token);
            } else {
                plugin.getLogger().warning("GitHub upload asset FALLITO: " + uploadResp.statusCode());
                plugin.getLogger().warning(uploadResp.body());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Errore upload GitHub");
            e.printStackTrace();
        }
    }

    private String extractUploadUrl(String json) {
        String key = "\"upload_url\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("{", start);
        return json.substring(start, end);
    }

    private void cleanupOldBackupReleases(String owner, String repoName, String token) {
        int keep = plugin.getConfig().getInt("keepLastGithub", 10);
        if (keep < 1) return;

        try {
            // GitHub restituisce le release dalla più recente alla più vecchia
            HttpRequest listReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repoName + "/releases?per_page=100"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WorldBackup")
                    .GET()
                    .build();

            HttpResponse<String> listResp = client.send(listReq, HttpResponse.BodyHandlers.ofString());
            if (listResp.statusCode() != 200) {
                plugin.getLogger().warning("GitHub cleanup: list releases FALLITA: " + listResp.statusCode());
                return;
            }

            List<ReleaseInfo> backups = parseBackupReleases(listResp.body());

            // Tieni le prime "keep", elimina il resto
            for (int i = keep; i < backups.size(); i++) {
                ReleaseInfo r = backups.get(i);

                // 1) elimina release
                HttpRequest delRelease = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repoName + "/releases/" + r.id))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "WorldBackup")
                        .DELETE()
                        .build();

                HttpResponse<String> delResp = client.send(delRelease, HttpResponse.BodyHandlers.ofString());
                if (delResp.statusCode() == 204 || delResp.statusCode() == 404) {
                    plugin.getLogger().info("GitHub cleanup: release eliminata (id=" + r.id + ", tag=" + r.tag + ")");
                } else {
                    plugin.getLogger().warning("GitHub cleanup: delete release FALLITA: " + delResp.statusCode() + " (id=" + r.id + ")");
                }

                // 2) elimina anche il tag (opzionale ma pulito)
                deleteTagRef(owner, repoName, token, r.tag);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("GitHub cleanup: errore");
            e.printStackTrace();
        }
    }

    private void deleteTagRef(String owner, String repoName, String token, String tag) {
        try {
            // GitHub API: DELETE /repos/{owner}/{repo}/git/refs/tags/{tag}
            String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8);
            HttpRequest delTag = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repoName + "/git/refs/tags/" + encodedTag))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WorldBackup")
                    .DELETE()
                    .build();

            HttpResponse<String> resp = client.send(delTag, HttpResponse.BodyHandlers.ofString());

            // 204 = ok, 422/404 = tag non trovato (non è un problema)
            if (resp.statusCode() == 204) {
                plugin.getLogger().info("GitHub cleanup: tag eliminato: " + tag);
            }
        } catch (Exception ignored) {
            // non blocchiamo mai per il tag
        }
    }

    private static class ReleaseInfo {
        long id;
        String tag;
        ReleaseInfo(long id, String tag) { this.id = id; this.tag = tag; }
    }

    /**
     * Estrae solo le release del plugin: tag_name che inizia con "backup-"
     * Parser semplice (senza librerie).
     */
    private List<ReleaseInfo> parseBackupReleases(String json) {
        List<ReleaseInfo> out = new ArrayList<>();

        // Split robusto: prende ogni oggetto release { ... } nel JSON array
        List<String> objects = splitTopLevelJsonObjects(json);

        for (String obj : objects) {
            String tag = extractJsonString(obj, "\"tag_name\":\"");
            if (tag == null || !tag.startsWith("backup-")) continue;

            Long releaseId = extractReleaseIdFromUrl(obj);
            if (releaseId == null) continue;

            out.add(new ReleaseInfo(releaseId, tag));
        }

        return out;
    }

    private List<String> splitTopLevelJsonObjects(String json) {
        List<String> objs = new ArrayList<>();

        int depth = 0;
        int start = -1;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // gestisci stringhe "..." (ignora braces dentro stringhe)
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) continue;

            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objs.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objs;
    }

    private String extractJsonString(String obj, String keyStart) {
        int p = obj.indexOf(keyStart);
        if (p == -1) return null;
        int s = p + keyStart.length();
        int e = obj.indexOf("\"", s);
        if (e == -1) return null;
        return obj.substring(s, e);
    }

    private Long extractReleaseIdFromUrl(String obj) {
        // url top-level: "url":"https://api.github.com/repos/OWNER/REPO/releases/123456"
        String url = extractJsonString(obj, "\"url\":\"");
        if (url == null) return null;

        int k = url.indexOf("/releases/");
        if (k == -1) return null;

        String tail = url.substring(k + "/releases/".length());
        // tail dovrebbe essere solo numero
        try {
            return Long.parseLong(tail);
        } catch (Exception e) {
            return null;
        }
    }


}
