package ru.baronessdev.personal.brutalseller;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class Config {
    public static Config inst;
    private final YamlConfiguration configuration = new YamlConfiguration();

    public Config() {
        File file = new File(Main.inst.getDataFolder() + "/config.yml");

        if (!file.exists())
            Main.inst.saveResource("config.yml", true);

        try {
            configuration.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }

        inst = this;
    }

    public String getMessage(String path) {
        String result = configuration.getString(path);
        return ChatColor.translateAlternateColorCodes('&', result != null ? result : " ");
    }

    public List<String> getList(String path) {
        return configuration.getStringList(path)
                .stream().map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public ConfigurationSection getSection(String path) {
        return configuration.getConfigurationSection(path);
    }

    public Double getDouble(String path) {
        return configuration.getDouble(path);
    }

    public long getUpdate() {
        return configuration.getLong("update");
    }

    public int getPriceReduction() {
        return configuration.getInt("price-reduction-every");
    }

    public int purchasesToDelete() {
        return configuration.getInt("purchases-to-delete");
    }
}
