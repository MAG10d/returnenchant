package org.MAGd.returnenchant;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ReturnEnchantCommand implements CommandExecutor {
    
    private final Returnenchant plugin;
    
    public ReturnEnchantCommand(Returnenchant plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.player-only", "&c此指令只能由玩家執行！")));
            return true;
        }

        Player player = (Player) sender;
        
        // 檢查權限
        if (!player.hasPermission("returnenchant.use")) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
            return true;
        }
        
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

        // 獲取原版附魔
        Map<Enchantment, Integer> vanillaEnchants = meta.getEnchants();
        int totalEnchants = 0;
        
        // 檢查是否有任何附魔
        if (vanillaEnchants.isEmpty()) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-enchant", "&c此物品沒有任何附魔！")));
            return true;
        }
        
        // 處理原版附魔
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
        
        // 更新物品Meta
        item.setItemMeta(meta);
        
        // 顯示結果訊息
        String successMessage = plugin.getConfig().getString("messages.success", "&a已成功移除 %count% 個附魔並轉換為附魔書！");
        successMessage = successMessage.replace("%count%", String.valueOf(totalEnchants));
        player.sendMessage(formatMessage(successMessage));
        
        return true;
    }
    
    private void giveItemToPlayer(Player player, ItemStack item) {
        // 如果玩家背包滿了，物品會掉在地上
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