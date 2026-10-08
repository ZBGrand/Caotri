package me.ely.gacha;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeeklyGacha extends JavaPlugin {
    private Economy economy;
    private GachaManager manager;
    private GachaMenu menu;

    @Override public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        manager = new GachaManager(this);
        manager.loadState();
        menu = new GachaMenu(this, manager);
        PluginCommand cmd = getCommand("gacha");
        if (cmd != null) { GachaCommand executor = new GachaCommand(this, manager, menu); cmd.setExecutor(executor); cmd.setTabCompleter(executor); }
        Bukkit.getScheduler().runTaskTimer(this, manager::updateRotationIfNeeded, 1200L, 1200L);
        getLogger().info("WeeklyGacha enabled. Vault economy: " + (economy != null));
    }
    @Override public void onDisable() { if (manager != null) manager.saveState(); }
    public Economy economy() { return economy; }
    public GachaManager manager() { return manager; }
    public String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    public String msg(String key) { return color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, "&cMissing message: " + key)); }
}
