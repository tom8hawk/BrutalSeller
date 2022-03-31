package ru.baronessdev.personal.brutalseller;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.baronessdev.personal.brutalseller.buyer.Manager;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class Main extends JavaPlugin implements Listener {
    public static final Executor executor = Executors.newCachedThreadPool();
    public static InventoryManager inventoryManager;
    public static Economy econ;

    public static Main inst;

    public Main() {
        inst = this;
    }

    @Override
    public void onEnable() {
        if (!setup()) {
            System.out.println("Vault not found :<");
            setEnabled(false);
        }

        inventoryManager = new InventoryManager(this);
        inventoryManager.init();
        Bukkit.getPluginManager().registerEvents(this, this);

        new Config();
        Manager.read();

        getCommand("bwseller").setExecutor((CommandSender sender, Command command, String label, String[] args) -> {
            if (sender instanceof Player)
                new Gui((Player) sender);

            return true;
        });
        getCommand("bw").setExecutor((CommandSender sender, Command command, String label, String[] args) -> {
            if (sender.hasPermission("bw.admin")) {
                if (args.length >= 1) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        new Config();
                        sender.sendMessage(Config.inst.getMessage("Messages.successfully-reload"));
                    } else if (args[0].equalsIgnoreCase("update")) {
                        Manager.read();
                        sender.sendMessage(Config.inst.getMessage("Messages.successfully-update"));
                    }
                }
            }

            return true;
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            Optional<InventoryContents> clickedInv = inventoryManager.getContents((Player) e.getWhoClicked());

            if (clickedInv.isPresent()) {
                InventoryContents inv = clickedInv.get();

                int row = e.getSlot() / 9;
                int column = e.getSlot() % 9;
                if (row < 0 || column < 0) {
                    return;
                }

                SmartInventory smartInv = inv.inventory();
                if (row >= smartInv.getRows() || column >= smartInv.getColumns()) {
                    return;
                }

                inv.get(row, column).ifPresent(item -> item.run(e));
            }
        }
    }

    private boolean setup() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;

        econ = rsp.getProvider();
        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
