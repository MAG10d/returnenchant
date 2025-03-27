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
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReturnEnchantCommand implements CommandExecutor {
    
    private final Returnenchant plugin;
    
    public ReturnEnchantCommand(Returnenchant plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家執行！");
            return true;
        }

        Player player = (Player) sender;
        
        // 檢查權限
        if (!player.hasPermission("returnenchant.use")) {
            player.sendMessage(ChatColor.RED + "你沒有權限使用此指令！");
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "請手持要移除附魔的物品！");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "此物品沒有任何附魔！");
            return true;
        }

        // 獲取原版附魔
        Map<Enchantment, Integer> vanillaEnchants = meta.getEnchants();
        int totalEnchants = 0;
        
        // 檢查並處理ExcellentEnchants附魔
        Map<Object, Integer> excellentEnchants = new HashMap<>();
        boolean hasExcellentEnchants = false;
        
        // 檢查ExcellentEnchants插件是否存在並啟用
        Plugin excellentPlugin = player.getServer().getPluginManager().getPlugin("ExcellentEnchants");
        if (excellentPlugin != null && excellentPlugin.isEnabled()) {
            try {
                // 使用反射獲取ExcellentEnchants的API方法
                Class<?> apiClass = Class.forName("su.nightexpress.excellentenchants.api.ExcellentEnchantsAPI");
                Method getItemEnchantsMethod = apiClass.getMethod("getItemEnchants", ItemStack.class);
                
                // 獲取物品上的ExcellentEnchants附魔
                @SuppressWarnings("unchecked")
                Map<Object, Integer> customEnchants = (Map<Object, Integer>) getItemEnchantsMethod.invoke(null, item);
                if (customEnchants != null && !customEnchants.isEmpty()) {
                    excellentEnchants = customEnchants;
                    hasExcellentEnchants = true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("無法獲取ExcellentEnchants附魔: " + e.getMessage());
            }
        }
        
        // 檢查是否有任何附魔
        if (vanillaEnchants.isEmpty() && !hasExcellentEnchants) {
            player.sendMessage(ChatColor.RED + "此物品沒有任何附魔！");
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
        
        // 處理ExcellentEnchants附魔
        if (hasExcellentEnchants) {
            try {
                Class<?> apiClass = Class.forName("su.nightexpress.excellentenchants.api.ExcellentEnchantsAPI");
                Method addEnchantMethod = apiClass.getMethod("addEnchant", ItemStack.class, Object.class, int.class, boolean.class);
                Method removeEnchantMethod = apiClass.getMethod("removeEnchant", ItemStack.class, Object.class);

                for (Map.Entry<Object, Integer> entry : excellentEnchants.entrySet()) {
                    // 創建附魔書
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
                    book.setItemMeta(bookMeta);
                    
                    // 添加ExcellentEnchants附魔到書本
                    addEnchantMethod.invoke(null, book, entry.getKey(), entry.getValue(), true);
                    
                    // 給予玩家
                    giveItemToPlayer(player, book);
                    
                    // 從原物品移除附魔
                    removeEnchantMethod.invoke(null, item, entry.getKey());
                    
                    totalEnchants++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("處理ExcellentEnchants附魔時出錯: " + e.getMessage());
            }
        }
        
        // 更新物品Meta
        item.setItemMeta(meta);
        
        // 顯示結果訊息
        player.sendMessage(ChatColor.GREEN + "已成功移除 " + totalEnchants + " 個附魔並轉換為附魔書！");
        return true;
    }
    
    private void giveItemToPlayer(Player player, ItemStack item) {
        // 如果玩家背包滿了，物品會掉在地上
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(ChatColor.YELLOW + "背包已滿，附魔書已掉落在地上！");
        } else {
            player.getInventory().addItem(item);
        }
    }
} 