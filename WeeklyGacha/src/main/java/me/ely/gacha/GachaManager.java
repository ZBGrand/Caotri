package me.ely.gacha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class GachaManager {
    private final WeeklyGacha plugin;
    private final File stateFile, playersFolder;
    private String activeBanner;
    private long startedAt;

    public GachaManager(WeeklyGacha plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        stateFile = new File(plugin.getDataFolder(), "rotation.yml");
        playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists()) playersFolder.mkdirs();
    }

    public List<String> bannerIds() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("banners");
        return s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false));
    }
    public ConfigurationSection banner(String id) { return plugin.getConfig().getConfigurationSection("banners." + id); }
    public String bannerName(String id) { return plugin.color(plugin.getConfig().getString("banners." + id + ".display-name", id)); }
    public String activeBanner() { updateRotationIfNeeded(); return activeBanner == null ? "" : activeBanner; }
    public long nextRotationAt() { return startedAt + Math.max(60_000L, plugin.getConfig().getLong("settings.rotation-duration-ms", 1_209_600_000L)); }

    public void loadState() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile);
        List<String> ids = bannerIds();
        activeBanner = y.getString("active-banner", plugin.getConfig().getString("settings.initial-banner", ""));
        startedAt = y.getLong("rotation-started-at", System.currentTimeMillis());
        if (ids.isEmpty()) activeBanner = "";
        else if (!ids.contains(activeBanner)) { activeBanner = ids.get(0); startedAt = System.currentTimeMillis(); }
        updateRotationIfNeeded(); saveState();
    }
    public void saveState() {
        YamlConfiguration y = new YamlConfiguration(); y.set("active-banner", activeBanner); y.set("rotation-started-at", startedAt);
        try { y.save(stateFile); } catch (IOException e) { plugin.getLogger().severe("Could not save rotation.yml: " + e.getMessage()); }
    }
    public boolean updateRotationIfNeeded() {
        List<String> ids = bannerIds(); if (ids.isEmpty()) return false;
        long duration = Math.max(60_000L, plugin.getConfig().getLong("settings.rotation-duration-ms", 1_209_600_000L));
        long now = System.currentTimeMillis();
        if (activeBanner == null || !ids.contains(activeBanner)) { activeBanner = ids.get(0); startedAt = now; saveState(); return true; }
        if (now < startedAt + duration) return false;
        long cycles = Math.max(1, (now - startedAt) / duration);
        int next = (int)((ids.indexOf(activeBanner) + cycles % ids.size()) % ids.size());
        activeBanner = ids.get(next); startedAt += cycles * duration; saveState();
        plugin.getLogger().info("Banner rotated to " + activeBanner); return true;
    }
    public boolean setBanner(String id) { if (!bannerIds().contains(id)) return false; activeBanner = id; startedAt = System.currentTimeMillis(); saveState(); return true; }
    public List<String> rewardIds(String bannerId) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("banners." + bannerId + ".rewards");
        return s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false));
    }
    public String rewardName(String banner, String reward) { return plugin.color(plugin.getConfig().getString("banners." + banner + ".rewards." + reward + ".display-name", reward)); }
    public ItemStack icon(String materialName, String displayName, List<String> lore) {
        Material material = Material.matchMaterial(materialName == null ? "PAPER" : materialName);
        if (material == null || material.isAir()) material = Material.PAPER;
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(plugin.color(displayName)); if (lore != null) meta.setLore(lore.stream().map(plugin::color).toList()); item.setItemMeta(meta); }
        return item;
    }
    public ItemStack rewardIcon(String banner, String reward) {
        String path = "banners." + banner + ".rewards." + reward;
        return icon(plugin.getConfig().getString(path + ".icon", "PAPER"), plugin.getConfig().getString(path + ".display-name", reward),
                List.of("&7Weight: &f" + plugin.getConfig().getDouble(path + ".weight", 0)));
    }
    private File playerFile(UUID uuid) { return new File(playersFolder, uuid + ".yml"); }
    private YamlConfiguration playerData(UUID uuid) { return YamlConfiguration.loadConfiguration(playerFile(uuid)); }
    private void savePlayer(UUID uuid, YamlConfiguration y) { try { y.save(playerFile(uuid)); } catch (IOException e) { plugin.getLogger().severe("Could not save player data: " + e.getMessage()); } }
    public int pulls(UUID uuid, String banner) { return playerData(uuid).getInt("banners." + banner + ".pulls", 0); }
    public int pity(UUID uuid, String banner) { return playerData(uuid).getInt("banners." + banner + ".pity-counter", 0); }

    public boolean pull(Player player) {
        String bannerId = activeBanner();
        if (banner(bannerId) == null) { player.sendMessage(plugin.msg("no-rewards")); return false; }
        List<String> rewards = rewardIds(bannerId);
        if (rewards.isEmpty()) { player.sendMessage(plugin.msg("no-rewards")); return false; }
        double cost = Math.max(0, plugin.getConfig().getDouble("settings.pull-cost", 0));
        if (cost > 0) {
            if (plugin.economy() == null) { player.sendMessage(plugin.msg("economy-missing")); return false; }
            if (!plugin.economy().has(player, cost)) { player.sendMessage(plugin.msg("not-enough-money").replace("%cost%", plugin.getConfig().getString("settings.currency-symbol", "$") + String.format(Locale.US, "%.2f", cost))); return false; }
        }
        YamlConfiguration data = playerData(player.getUniqueId());
        String base = "banners." + bannerId;
        int pityCount = data.getInt(base + ".pity-counter", 0);
        int pityEvery = Math.max(1, plugin.getConfig().getInt("settings.pity.every-pulls", 50));
        boolean pityTriggered = plugin.getConfig().getBoolean("settings.pity.enabled", true) && pityCount + 1 >= pityEvery;
        List<String> eligible = new ArrayList<>();
        for (String id : rewards) {
            String path = "banners." + bannerId + ".rewards." + id;
            if (plugin.getConfig().getDouble(path + ".weight", 0) > 0 && (!pityTriggered || plugin.getConfig().getBoolean(path + ".pity", false))) eligible.add(id);
        }
        if (eligible.isEmpty() && pityTriggered) for (String id : rewards) if (plugin.getConfig().getDouble("banners." + bannerId + ".rewards." + id + ".weight", 0) > 0) eligible.add(id);
        if (eligible.isEmpty()) { player.sendMessage(plugin.msg("no-rewards")); return false; }
        if (cost > 0 && !plugin.economy().withdrawPlayer(player, cost).transactionSuccess()) { player.sendMessage(plugin.msg("pull-failed")); return false; }
        String reward = weightedChoice(bannerId, eligible);
        List<String> commands = plugin.getConfig().getStringList("banners." + bannerId + ".rewards." + reward + ".commands");
        if (commands.isEmpty()) {
            if (cost > 0) plugin.economy().depositPlayer(player, cost);
            player.sendMessage(plugin.msg("pull-failed")); plugin.getLogger().warning("Reward " + bannerId + "." + reward + " has no commands; refunded."); return false;
        }
        for (String command : commands) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()).replace("%banner%", bannerId).replace("%reward%", reward));
        data.set(base + ".pulls", data.getInt(base + ".pulls", 0) + 1);
        boolean pityReward = plugin.getConfig().getBoolean("banners." + bannerId + ".rewards." + reward + ".pity", false);
        data.set(base + ".pity-counter", pityTriggered && pityReward ? 0 : pityCount + 1);
        data.set("total-pulls", data.getInt("total-pulls", 0) + 1);
        List<Map<?, ?>> history = data.getMapList("history"); Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("date", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now()));
        entry.put("banner", bannerId); entry.put("reward", rewardName(bannerId, reward)); history.add(entry);
        while (history.size() > 100) history.remove(0); data.set("history", history); savePlayer(player.getUniqueId(), data);
        if (pityTriggered) player.sendMessage(plugin.msg("pity-notice"));
        player.sendMessage(plugin.msg("pull-success").replace("%reward%", rewardName(bannerId, reward)));
        return true;
    }
    private String weightedChoice(String banner, List<String> ids) {
        double total = 0; for (String id : ids) total += plugin.getConfig().getDouble("banners." + banner + ".rewards." + id + ".weight", 0);
        if (total <= 0) return null; double roll = ThreadLocalRandom.current().nextDouble(total);
        for (String id : ids) { roll -= plugin.getConfig().getDouble("banners." + banner + ".rewards." + id + ".weight", 0); if (roll < 0) return id; }
        return ids.get(ids.size() - 1);
    }
    public void sendHistory(Player player) {
        List<Map<?, ?>> history = playerData(player.getUniqueId()).getMapList("history");
        if (history.isEmpty()) { player.sendMessage(plugin.msg("no-history")); return; }
        player.sendMessage(plugin.msg("history-header")); int start = Math.max(0, history.size() - 10);
        for (int i = history.size() - 1; i >= start; i--) {
            Map<?, ?> e = history.get(i);
            player.sendMessage(plugin.msg("history-line").replace("%date%", Objects.toString(e.get("date"), "")).replace("%banner%", Objects.toString(e.get("banner"), "")).replace("%reward%", Objects.toString(e.get("reward"), "")));
        }
    }
}
