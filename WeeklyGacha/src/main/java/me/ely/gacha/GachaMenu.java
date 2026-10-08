package me.ely.gacha;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GachaMenu implements Listener {
    private final WeeklyGacha plugin; private final GachaManager manager;
    public GachaMenu(WeeklyGacha plugin, GachaManager manager) { this.plugin = plugin; this.manager = manager; Bukkit.getPluginManager().registerEvents(this, plugin); }
    public void open(Player player) {
        String id = manager.activeBanner(); ConfigurationSection banner = manager.banner(id);
        if (banner == null) { player.sendMessage(plugin.msg("no-rewards")); return; }
        int size = plugin.getConfig().getInt("settings.gui.size", 27); if (size < 9 || size > 54 || size % 9 != 0) size = 27;
        String title = plugin.color(plugin.getConfig().getString("settings.gui.title", "&8Gacha - %banner%").replace("%banner%", banner.getString("display-name", id)));
        Inventory inv = Bukkit.createInventory(new GachaHolder(id), size, title);
        int bannerSlot = clamp(plugin.getConfig().getInt("settings.gui.banner-slot", 4), size);
        List<String> lore = new ArrayList<>(banner.getStringList("description")); lore.add("");
        lore.add("&7Price: &e" + plugin.getConfig().getString("settings.currency-symbol", "$") + String.format(Locale.US, "%.2f", plugin.getConfig().getDouble("settings.pull-cost", 0)));
        long left = Math.max(0, manager.nextRotationAt() - System.currentTimeMillis()); lore.add("&7Changes in: &f" + duration(left));
        inv.setItem(bannerSlot, manager.icon(banner.getString("icon", "CHEST"), banner.getString("display-name", id), lore));
        int slot = Math.max(0, plugin.getConfig().getInt("settings.gui.rewards-start-slot", 9));
        for (String reward : manager.rewardIds(id)) { if (slot >= size) break; inv.setItem(slot++, manager.rewardIcon(id, reward)); }
        int pityEvery = Math.max(1, plugin.getConfig().getInt("settings.pity.every-pulls", 50));
        inv.setItem(clamp(plugin.getConfig().getInt("settings.gui.info-slot", 22), size), manager.icon("BOOK", "&bYour Gacha Stats", List.of("&7Pulls: &f" + manager.pulls(player.getUniqueId(), id), "&7Pity: &f" + manager.pity(player.getUniqueId(), id) + "/" + pityEvery)));
        inv.setItem(pullSlot(size), manager.icon("SUNFLOWER", "&aPull x1", List.of("&7Click to pull once.", "&7Cost: &e" + plugin.getConfig().getString("settings.currency-symbol", "$") + String.format(Locale.US, "%.2f", plugin.getConfig().getDouble("settings.pull-cost", 0)))));
        player.openInventory(inv);
    }
    private int pullSlot(int size) { return clamp(plugin.getConfig().getInt("settings.gui.pull-slot", 13), size); }
    private int clamp(int n, int size) { return Math.max(0, Math.min(size - 1, n)); }
    private String duration(long ms) { long s=ms/1000; return (s/86400)+"d "+((s%86400)/3600)+"h "+((s%3600)/60)+"m"; }
    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof GachaHolder)) return;
        e.setCancelled(true); if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (e.getRawSlot() == pullSlot(e.getView().getTopInventory().getSize())) { manager.pull(p); Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) open(p); }); }
    }
    @EventHandler public void drag(InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder() instanceof GachaHolder) e.setCancelled(true); }
    private record GachaHolder(String bannerId) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
}
