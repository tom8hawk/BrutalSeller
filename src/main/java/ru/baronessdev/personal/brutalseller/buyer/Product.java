package ru.baronessdev.personal.brutalseller.buyer;

import org.bukkit.inventory.ItemStack;

public class Product {
    private final ItemStack item;
    private double price;

    public Product(double price, ItemStack item) {
        this.price = price;
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public Product setPrice(double price) {
        this.price = price;

        return this;
    }
}
