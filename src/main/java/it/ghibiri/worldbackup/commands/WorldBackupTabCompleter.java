package it.ghibiri.worldbackup.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldBackupTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {

            // /backupnow [force]
            case "backupnow":
                if (args.length == 1) {
                    List<String> opts = new ArrayList<>();
                    if (sender.hasPermission("worldbackup.force")) {
                        opts.add("force");
                    }
                    return filter(opts, args[0]);
                }
                return Collections.emptyList();

            // /autobackup <daily|hourly|every|now|on|off> [minuti]
            case "autobackup":
                if (args.length == 1) {
                    return filter(List.of("now", "on", "off", "daily", "hourly", "every"), args[0]);
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("every")) {
                    return filter(List.of("5", "10", "15", "30", "60", "120", "240"), args[1]);
                }
                return Collections.emptyList();

            // /worldbackup reload
            case "worldbackup":
                if (args.length == 1) {
                    if (sender.hasPermission("worldbackup.admin")) {
                        return filter(List.of("reload"), args[0]);
                    }
                }
                return Collections.emptyList();

            default:
                return Collections.emptyList();
        }
    }

    private List<String> filter(List<String> options, String typed) {
        if (typed == null || typed.isEmpty()) return options;

        String t = typed.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(t)) out.add(o);
        }
        return out;
    }
}
