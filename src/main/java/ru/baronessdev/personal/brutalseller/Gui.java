package ru.baronessdev.personal.brutalseller;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.baronessdev.personal.brutalseller.buyer.Manager;
import ru.baronessdev.personal.brutalseller.buyer.Product;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.baronessdev.personal.brutalseller.Main.executor;

public class Gui implements InventoryProvider {
    public Gui(Player player) {
        SmartInventory inventory = SmartInventory.builder()
                .provider(this)
                .manager(Main.inventoryManager)
                .size(3, 9)
                .title(Config.inst.getMessage("gui.title"))
                .build();

        Bukkit.getScheduler().runTask(Main.inst, () -> inventory.open(player));
    }

    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        for (int slot = 0; slot < 9; slot++) {
            int finalSlot = slot;
            Product product = Manager.getPrices(player).get(slot);

            if (Config.inst.purchasesToDelete() > Manager.getProductBuy(player, slot)) {
                inventoryContents.set(1, slot,
                        ClickableItem.of(createProduct(product.getItem(), product),
                                e -> {
                                    if (e.getCurrentItem() != null)
                                        buy(player, finalSlot, e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                                                ? Arrays.stream(e.getWhoClicked().getInventory().getStorageContents()).parallel()
                                                .filter(Objects::nonNull).filter(item -> item.getType().equals(product.getItem().getType()))
                                                .mapToInt(ItemStack::getAmount).sum() : 1);
                                }));
            } else {
                inventoryContents.set(1, slot, ClickableItem.empty(createItem(Material.LIME_STAINED_GLASS_PANE, "gui.Filled")));
            }
        }

        inventoryContents.fillBorders(ClickableItem.empty(createItem(Material.BLUE_STAINED_GLASS_PANE, "")));
        inventoryContents.set(2, 8, ClickableItem.empty(createItem(Material.BOOK, "gui.Info")));
    }

    private void buy(Player player, int slot, int amount) {
        executor.execute(() -> {
            Product product = Manager.getPrices(player).get(slot);
            Inventory inventory = player.getInventory();

            int toWithdraw = Math.min(Math.min(amount, Arrays.stream(inventory.getStorageContents()).parallel()
                    .filter(Objects::nonNull).filter(item -> item.getType().equals(product.getItem().getType()))
                    .mapToInt(ItemStack::getAmount).sum()), Config.inst.purchasesToDelete() - Manager.getProductBuy(player, slot));

            if (toWithdraw > 0) {
                int withdraw = toWithdraw;

                for (int num = 0; num < inventory.getSize(); num++) {
                    ItemStack is = inventory.getItem(num);

                    if (is != null && is.getType().equals(product.getItem().getType())) {
                        int newAmount = is.getAmount() - withdraw;

                        if (newAmount > 0) {
                            is.setAmount(newAmount);
                            break;
                        } else {
                            inventory.clear(num);
                            withdraw = -newAmount;
                            if (withdraw == 0) break;
                        }
                    }
                }

                Manager.setProductBuy(player, slot, Manager.getProductBuy(player, slot) + toWithdraw);
                int priceReduction = Manager.getProductBuy(player, slot) / Config.inst.getPriceReduction();

                if (priceReduction > 0)
                    Manager.updatePrice(player, slot, product.getPrice() * Math.pow(0.93, (0.2 * priceReduction)));

                double price = toWithdraw * product.getPrice();
                Main.econ.depositPlayer(player, price);

                new Gui(player);
                player.sendMessage(Config.inst.getMessage("Messages.successfully")
                        .replace("%amount", String.valueOf(toWithdraw))
                        .replace("%price", String.valueOf(Math.round(price))));
            } else {
                player.sendMessage(Config.inst.getMessage("Messages.not-in-inventory"));
            }
        });
    }

    private ItemStack createProduct(ItemStack stack, Product product) {
        ItemMeta meta = stack.getItemMeta();

        meta.setLore(Config.inst.getList("gui.Product.lore").stream()
                .map(line -> line.replace("%price", String.valueOf(Math.round(product.getPrice() * stack.getMaxStackSize()))))
                .collect(Collectors.toList()));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createItem(Material material, String path) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(Config.inst.getMessage(path + ".name"));
        meta.setLore(Config.inst.getList(path + ".lore"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {
    }
}
