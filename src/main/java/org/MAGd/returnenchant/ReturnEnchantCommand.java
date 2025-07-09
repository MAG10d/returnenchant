package org.MAGd.returnenchant;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;

public class ReturnEnchantCommand implements CommandExecutor {
    private final Returnenchant plugin;
    private Economy economy;

    public ReturnEnchantCommand(Returnenchant plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.player-only", "&c此指令只能由玩家執行！")));
            return true;
        }
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("armor")) {
            if (!player.hasPermission("returnenchant.armor")) {
                player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
                return true;
            }
            // 收費檢查
            if (!handleFee(player)) return true;
            int totalEnchants = 0;
            PlayerInventory inv = player.getInventory();
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                ItemStack piece = armor[i];
                if (piece == null || piece.getType() == Material.AIR) continue;
                ItemMeta meta = piece.getItemMeta();
                if (meta == null) continue;
                Map<Enchantment, Integer> enchants = meta.getEnchants();
                if (enchants.isEmpty()) continue;
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
                    if (bookMeta != null) {
                        bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                        book.setItemMeta(bookMeta);
                        giveItemToPlayer(player, book);
                        totalEnchants++;
                    }
                    meta.removeEnchant(entry.getKey());
                }
                piece.setItemMeta(meta);
                armor[i] = piece;
            }
            inv.setArmorContents(armor);
            String successMessage = plugin.getConfig().getString("messages.success", "&a已成功移除 %count% 個附魔並轉換為附魔書！");
            successMessage = successMessage.replace("%count%", String.valueOf(totalEnchants));
            player.sendMessage(formatMessage(successMessage));
            return true;
        }

        // 檢查權限
        if (!player.hasPermission("returnenchant.use")) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
            return true;
        }
        // 收費檢查
        if (!handleFee(player)) return true;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-item", "&c請手持要移除附魔的物品！")));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-enchant", "&c此物品沒有任何附魔！")));
            return true;
        }
        Map<Enchantment, Integer> vanillaEnchants = meta.getEnchants();
        int totalEnchants = 0;
        if (vanillaEnchants.isEmpty()) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-enchant", "&c此物品沒有任何附魔！")));
            return true;
        }
        for (Map.Entry<Enchantment, Integer> entry : vanillaEnchants.entrySet()) {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
            if (bookMeta != null) {
                bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                book.setItemMeta(bookMeta);
                giveItemToPlayer(player, book);
                totalEnchants++;
            }
            meta.removeEnchant(entry.getKey());
        }
        item.setItemMeta(meta);
        String successMessage = plugin.getConfig().getString("messages.success", "&a已成功移除 %count% 個附魔並轉換為附魔書！");
        successMessage = successMessage.replace("%count%", String.valueOf(totalEnchants));
        player.sendMessage(formatMessage(successMessage));
        return true;
    }

    private boolean handleFee(Player player) {
        boolean feeEnabled = plugin.getConfig().getBoolean("fee.enabled", false);
        double price = plugin.getConfig().getDouble("fee.price", 0.0);
        if (!feeEnabled || price <= 0) return true;
        if (economy == null) {
            player.sendMessage(formatMessage("&c經濟系統未安裝或未正確設置，無法收費。"));
            return false;
        }
        if (!economy.has(player, price)) {
            player.sendMessage(formatMessage("&c你的金錢不足，無法使用此功能！"));
            return false;
        }
        economy.withdrawPlayer(player, price);
        return true;
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.inventory-full", "&e背包已滿，附魔書已掉落在地上！")));
        } else {
            player.getInventory().addItem(item);
        }
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}