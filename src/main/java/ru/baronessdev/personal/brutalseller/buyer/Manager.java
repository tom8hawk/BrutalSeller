package ru.baronessdev.personal.brutalseller.buyer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import ru.baronessdev.personal.brutalseller.Config;
import ru.baronessdev.personal.brutalseller.Gui;
import ru.baronessdev.personal.brutalseller.Main;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Manager {
    private static final List<Product> slots = new ArrayList<>();
    private static final Random random = new Random();
    private static final Map<UUID, Manager> specials = new HashMap<>();
    private static Timer task = new Timer();
    private static boolean first = true;
    private final List<Product> prices = new ArrayList<>();
    private final HashMap<Product, Integer> buy = new HashMap<>();

    private Manager(OfflinePlayer player) {
        prices.addAll(slots);
        specials.put(player.getUniqueId(), this);
    }

    public static void read() {
        task.cancel();
        task = new Timer();

        task.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                specials.clear();
                slots.clear();

                for (int now = 1; now < 10; now++) {
                    String path = "slots." + now;

                    HashMap<String, Integer> randomMap = new HashMap<>();
                    ConfigurationSection config = Config.inst.getSection(path);
                    config.getKeys(false).parallelStream().forEach(key -> randomMap.put(key, config.getInt(path + "." + key + ".chance")));

                    String item;
                    while (true) {
                        double chance = random.nextDouble() * randomMap.values().stream().mapToInt(Number::intValue).sum();

                        AtomicInteger needle = new AtomicInteger();
                        List<String> result = randomMap.entrySet().stream()
                                .filter(ent -> needle.addAndGet(ent.getValue()) >= chance)
                                .map(Map.Entry::getKey).collect(Collectors.toList());

                        if (!result.isEmpty()) {
                            item = result.get(random.nextInt(result.size()));

                            if (Manager.getSlots().size() <= 1 || !slots.get(now - 2).getItem().getType().toString().equalsIgnoreCase(item))
                                break;
                        }
                    }

                    Material material = Material.getMaterial(item.toUpperCase());

                    if (material != null)
                        slots.add(new Product(Config.inst.getDouble(path + "." + item + ".price"), new ItemStack(material)));
                    else
                        System.out.println("BrutalSeller | Предмет " + item + " не найден!");
                }

                Bukkit.getOnlinePlayers().parallelStream().forEach(player -> Main.inventoryManager.getInventory(player).ifPresent(inv -> new Gui(player)));

                if (!first) {
                    String msg = Config.inst.getMessage("Messages.update-broadcast");

                    Bukkit.getOnlinePlayers().parallelStream()
                            .forEach(player -> player.sendMessage(msg));
                }
                first = false;
            }
        }, 0L, TimeUnit.HOURS.toMillis(Config.inst.getUpdate()));
    }

    public static List<Product> getSlots() {
        return slots;
    }

    public static List<Product> getPrices(OfflinePlayer player) {
        return get(player).prices;
    }

    public static void updatePrice(OfflinePlayer player, int slot, double price) {
        Manager manager = get(player);
        manager.prices.set(slot, manager.prices.get(slot).setPrice(price));
    }

    public static int getProductBuy(OfflinePlayer player, int slot) {
        Manager manager = get(player);
        return manager.buy.getOrDefault(manager.prices.get(slot), 0);
    }

    public static void setProductBuy(OfflinePlayer player, int slot, int value) {
        Manager manager = get(player);
        manager.buy.put(manager.prices.get(slot), value);
    }

    public static Manager get(OfflinePlayer player) {
        return specials.containsKey(player.getUniqueId()) ? specials.get(player.getUniqueId()) : new Manager(player);
    }
}
