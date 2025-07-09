package org.MAGd.returnenchant;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ReturnEnchantCommand implements CommandExecutor {
    
    private final Returnenchant plugin;
    private final Economy economy;
    
    public ReturnEnchantCommand(Returnenchant plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 檢查是否是 reload 指令
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("returnenchant.reload")) {
                sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.reload-no-permission", "&c你沒有權限重新載入配置！")));
                return true;
            }
            
            plugin.reloadPluginConfig();
            sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.reload-success", "&a插件配置已重新載入！")));
            return true;
        }
        
        // 檢查是否是 armor 指令
        if (args.length > 0 && args[0].equalsIgnoreCase("armor")) {
            return handleArmorCommand(sender);
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.player-only", "&c此指令只能由玩家執行！")));
            return true;
        }

        Player player = (Player) sender;
        
        // 檢查基本權限
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
        
        // 檢查是否有任何附魔
        if (vanillaEnchants.isEmpty()) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.no-enchant", "&c此物品沒有任何附魔！")));
            return true;
        }
        
        // 檢查 VIP 系統並計算費用
        if (!plugin.getConfig().getBoolean("vip-system.enabled", true)) {
            // VIP 系統未啟用，直接執行
            return executeEnchantReturn(player, item, meta, vanillaEnchants, 0.0);
        }
        
        VipTier playerTier = getPlayerVipTier(player);
        double cost = playerTier.getCost();
        
        // 檢查玩家是否有足夠的錢
        if (economy != null && cost > 0) {
            double balance = economy.getBalance(player);
            if (balance < cost) {
                String message = plugin.getConfig().getString("messages.insufficient-money", "&c你的錢不夠！需要 $%cost%，但你只有 $%balance%");
                message = message.replace("%cost%", String.format("%.2f", cost))
                               .replace("%balance%", String.format("%.2f", balance));
                player.sendMessage(formatMessage(message));
                return true;
            }
            
            // 扣錢
            EconomyResponse response = economy.withdrawPlayer(player, cost);
            if (!response.transactionSuccess()) {
                player.sendMessage(formatMessage(plugin.getConfig().getString("messages.economy-error", "&c經濟系統出現錯誤，請聯繫管理員！")));
                return true;
            }
        }
        
        return executeEnchantReturn(player, item, meta, vanillaEnchants, cost);
    }
    
    private boolean executeEnchantReturn(Player player, ItemStack item, ItemMeta meta, Map<Enchantment, Integer> vanillaEnchants, double cost) {
        int totalEnchants = 0;
        
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
        String successMessage = plugin.getConfig().getString("messages.success", "&a已成功移除 %count% 個附魔並轉換為附魔書！花費了 $%cost%");
        successMessage = successMessage.replace("%count%", String.valueOf(totalEnchants))
                                     .replace("%cost%", String.format("%.2f", cost));
        player.sendMessage(formatMessage(successMessage));
        
        return true;
    }
    
    private boolean handleArmorCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.player-only", "&c此指令只能由玩家執行！")));
            return true;
        }

        Player player = (Player) sender;
        
        // 檢查護甲附魔移除權限
        if (!player.hasPermission("returnenchant.armor.use")) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.armor-no-permission", "&c你沒有權限使用護甲附魔移除功能！")));
            return true;
        }
        
        // 檢查護甲系統是否啟用
        if (!plugin.getConfig().getBoolean("armor-system.enabled", true)) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.armor-no-permission", "&c護甲附魔移除功能已被停用！")));
            return true;
        }
        
        // 獲取玩家穿戴的護甲
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        Map<Integer, Map<Enchantment, Integer>> armorEnchants = new java.util.HashMap<>();
        int totalEnchantCount = 0;
        
        // 檢查每個護甲槽位的附魔
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack armor = armorContents[i];
            if (armor != null && armor.getType() != Material.AIR) {
                ItemMeta meta = armor.getItemMeta();
                if (meta != null && !meta.getEnchants().isEmpty()) {
                    armorEnchants.put(i, meta.getEnchants());
                    totalEnchantCount += meta.getEnchants().size();
                }
            }
        }
        
        // 檢查是否有任何附魔的護甲
        if (armorEnchants.isEmpty()) {
            player.sendMessage(formatMessage(plugin.getConfig().getString("messages.armor-no-enchanted-armor", "&c你身上沒有任何附魔的護甲！")));
            return true;
        }
        
        // 獲取護甲 VIP 層級並計算費用
        VipTier armorTier = getArmorVipTier(player);
        double cost = armorTier.getCost();
        
        // 檢查玩家是否有足夠的錢
        if (economy != null && cost > 0) {
            double balance = economy.getBalance(player);
            if (balance < cost) {
                String message = plugin.getConfig().getString("messages.armor-insufficient-money", "&c你的錢不夠移除護甲附魔！需要 $%cost%，但你只有 $%balance%");
                message = message.replace("%cost%", String.format("%.2f", cost))
                               .replace("%balance%", String.format("%.2f", balance));
                player.sendMessage(formatMessage(message));
                return true;
            }
            
            // 扣錢
            EconomyResponse response = economy.withdrawPlayer(player, cost);
            if (!response.transactionSuccess()) {
                player.sendMessage(formatMessage(plugin.getConfig().getString("messages.economy-error", "&c經濟系統出現錯誤，請聯繫管理員！")));
                return true;
            }
        }
        
        return executeArmorEnchantReturn(player, armorContents, armorEnchants, totalEnchantCount, cost);
    }
    
    private boolean executeArmorEnchantReturn(Player player, ItemStack[] armorContents, Map<Integer, Map<Enchantment, Integer>> armorEnchants, int totalEnchantCount, double cost) {
        int actualRemovedCount = 0;
        
        // 處理每個護甲槽位的附魔
        for (Map.Entry<Integer, Map<Enchantment, Integer>> slotEntry : armorEnchants.entrySet()) {
            int slotIndex = slotEntry.getKey();
            Map<Enchantment, Integer> enchants = slotEntry.getValue();
            ItemStack armor = armorContents[slotIndex];
            ItemMeta meta = armor.getItemMeta();
            
            if (meta != null) {
                // 為每個附魔創建附魔書
                for (Map.Entry<Enchantment, Integer> enchantEntry : enchants.entrySet()) {
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
                    
                    if (bookMeta != null) {
                        bookMeta.addStoredEnchant(enchantEntry.getKey(), enchantEntry.getValue(), true);
                        book.setItemMeta(bookMeta);
                        
                        giveItemToPlayer(player, book);
                        actualRemovedCount++;
                    }
                    
                    // 從護甲中移除附魔
                    meta.removeEnchant(enchantEntry.getKey());
                }
                
                // 更新護甲的 meta
                armor.setItemMeta(meta);
            }
        }
        
        // 更新玩家的護甲
        player.getInventory().setArmorContents(armorContents);
        
        // 顯示結果訊息
        String successMessage = plugin.getConfig().getString("messages.armor-success", "&a已成功移除身上護甲的 %count% 個附魔並轉換為附魔書！花費了 $%cost%");
        successMessage = successMessage.replace("%count%", String.valueOf(actualRemovedCount))
                                     .replace("%cost%", String.format("%.2f", cost));
        player.sendMessage(formatMessage(successMessage));
        
        return true;
    }
    
    private VipTier getPlayerVipTier(Player player) {
        ConfigurationSection vipSection = plugin.getConfig().getConfigurationSection("vip-system.tiers");
        if (vipSection == null) {
            return new VipTier("default", "returnenchant.use", "&7普通玩家", 100.0);
        }
        
        // 按照配置文件中的順序檢查權限（從上到下，優先級從高到低）
        for (String tierName : vipSection.getKeys(false)) {
            ConfigurationSection tier = vipSection.getConfigurationSection(tierName);
            if (tier != null) {
                String permission = tier.getString("permission", "returnenchant.use");
                if (player.hasPermission(permission)) {
                    String displayName = tier.getString("display_name", "&7" + tierName);
                    double cost = tier.getDouble("cost", 100.0);
                    return new VipTier(tierName, permission, displayName, cost);
                }
            }
        }
        
        // 如果都沒有，返回默認
        return new VipTier("default", "returnenchant.use", "&7普通玩家", 100.0);
    }
    
    private VipTier getArmorVipTier(Player player) {
        ConfigurationSection armorSection = plugin.getConfig().getConfigurationSection("armor-system.tiers");
        if (armorSection == null) {
            return new VipTier("default", "returnenchant.armor.use", "&7普通玩家", 200.0);
        }
        
        // 按照配置文件中的順序檢查權限（從上到下，優先級從高到低）
        for (String tierName : armorSection.getKeys(false)) {
            ConfigurationSection tier = armorSection.getConfigurationSection(tierName);
            if (tier != null) {
                String permission = tier.getString("permission", "returnenchant.armor.use");
                if (player.hasPermission(permission)) {
                    String displayName = tier.getString("display_name", "&7" + tierName);
                    double cost = tier.getDouble("cost", 200.0);
                    return new VipTier(tierName, permission, displayName, cost);
                }
            }
        }
        
        // 如果都沒有，返回默認
        return new VipTier("default", "returnenchant.armor.use", "&7普通玩家", 200.0);
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
    
    // VIP 層級類別
    private static class VipTier {
        private final String name;
        private final String permission;
        private final String displayName;
        private final double cost;
        
        public VipTier(String name, String permission, String displayName, double cost) {
            this.name = name;
            this.permission = permission;
            this.displayName = displayName;
            this.cost = cost;
        }
        
        public String getName() { return name; }
        public String getPermission() { return permission; }
        public String getDisplayName() { return displayName; }
        public double getCost() { return cost; }
    }
}