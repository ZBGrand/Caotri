package me.ely.gacha;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GachaCommand implements CommandExecutor, TabCompleter {
    private final WeeklyGacha plugin; private final GachaManager manager; private final GachaMenu menu;
    public GachaCommand(WeeklyGacha plugin, GachaManager manager, GachaMenu menu) { this.plugin=plugin; this.manager=manager; this.menu=menu; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Only players can open the gacha menu."); return true; }
            if (!sender.hasPermission("weeklygacha.use")) { sender.sendMessage(plugin.msg("no-permission")); return true; }
            menu.open(p); return true;
        }
        if (args[0].equalsIgnoreCase("history")) { if (sender instanceof Player p) manager.sendHistory(p); else sender.sendMessage("Only players can view history."); return true; }
        if (!args[0].equalsIgnoreCase("admin") || !sender.hasPermission("weeklygacha.admin")) { sender.sendMessage(plugin.msg("no-permission")); return true; }
        if (args.length < 2) { sender.sendMessage(plugin.color("&eUsage: /gacha admin <set|reload|list>")); return true; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 3) { sender.sendMessage(plugin.color("&cUsage: /gacha admin set <bannerId>")); return true; }
                if (!manager.setBanner(args[2])) sender.sendMessage(plugin.msg("banner-not-found"));
                else sender.sendMessage(plugin.msg("banner-changed").replace("%banner%", manager.bannerName(args[2])));
            }
            case "reload" -> { plugin.reloadConfig(); manager.loadState(); sender.sendMessage(plugin.msg("reloaded")); }
            case "list" -> {
                sender.sendMessage(plugin.color("&bConfigured banners:"));
                for (String id : manager.bannerIds()) sender.sendMessage(plugin.color("&7- &f" + id + (id.equals(manager.activeBanner()) ? " &a(active)" : "")));
            }
            default -> sender.sendMessage(plugin.color("&eUsage: /gacha admin <set|reload|list>"));
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("history", "admin"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) return filter(List.of("set", "reload", "list"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("set")) return filter(manager.bannerIds(), args[2]);
        return List.of();
    }
    private List<String> filter(List<String> options, String input) { String s=input.toLowerCase(Locale.ROOT); return options.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(s)).collect(Collectors.toList()); }
}
