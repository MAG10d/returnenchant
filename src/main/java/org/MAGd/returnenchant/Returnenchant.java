package org.MAGd.returnenchant;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class Returnenchant extends JavaPlugin {
    
    private Economy economy = null;
    private Set<String> registeredPermissions = new HashSet<>();
    
    @Override
    public void onEnable() {
        // 保存默認配置
        saveDefaultConfig();
        
        // 動態註冊權限
        registerDynamicPermissions();
        
        // 設置經濟系統
        if (!setupEconomy()) {
            getLogger().severe("找不到經濟插件！插件將被禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 註冊指令執行器
        this.getCommand("returnenchant").setExecutor(new ReturnEnchantCommand(this));
        getLogger().info("ReturnEnchant plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // 清理動態註冊的權限
        unregisterDynamicPermissions();
        getLogger().info("ReturnEnchant plugin has been disabled!");
    }
    
    /**
     * 重新載入插件配置
     */
    public void reloadPluginConfig() {
        // 重新載入配置文件
        reloadConfig();
        
        // 清理舊的動態權限
        unregisterDynamicPermissions();
        
        // 重新註冊動態權限
        registerDynamicPermissions();
        
        getLogger().info("Plugin configuration has been reloaded!");
    }
    
    private void registerDynamicPermissions() {
        ConfigurationSection vipSection = getConfig().getConfigurationSection("vip-system.tiers");
        if (vipSection == null) {
            getLogger().warning("VIP system configuration not found, skipping dynamic permission registration.");
            return;
        }
        
        for (String tierName : vipSection.getKeys(false)) {
            ConfigurationSection tier = vipSection.getConfigurationSection(tierName);
            if (tier != null) {
                String permissionNode = tier.getString("permission");
                String displayName = tier.getString("display_name", tierName);
                double cost = tier.getDouble("cost", 0.0);
                
                if (permissionNode != null && !permissionNode.equals("returnenchant.use")) {
                    try {
                        // 檢查權限是否已經存在
                        if (getServer().getPluginManager().getPermission(permissionNode) == null) {
                            Permission permission = new Permission(
                                permissionNode,
                                String.format("VIP tier: %s (Cost: $%.2f)", displayName, cost),
                                PermissionDefault.FALSE
                            );
                            
                            getServer().getPluginManager().addPermission(permission);
                            registeredPermissions.add(permissionNode);
                            getLogger().info("Dynamic permission registered: " + permissionNode);
                        } else {
                            getLogger().info("Permission " + permissionNode + " already exists, skipping...");
                        }
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Failed to register permission " + permissionNode + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // 註冊護甲系統權限
        ConfigurationSection armorSection = getConfig().getConfigurationSection("armor-system.tiers");
        if (armorSection != null) {
            for (String tierName : armorSection.getKeys(false)) {
                ConfigurationSection tier = armorSection.getConfigurationSection(tierName);
                if (tier != null) {
                    String permissionNode = tier.getString("permission");
                    String displayName = tier.getString("display_name", tierName);
                    double cost = tier.getDouble("cost", 0.0);
                    
                    if (permissionNode != null && !permissionNode.equals("returnenchant.armor.use")) {
                        try {
                            // 檢查權限是否已經存在
                            if (getServer().getPluginManager().getPermission(permissionNode) == null) {
                                Permission permission = new Permission(
                                    permissionNode,
                                    String.format("Armor VIP tier: %s (Cost: $%.2f)", displayName, cost),
                                    PermissionDefault.FALSE
                                );
                                
                                getServer().getPluginManager().addPermission(permission);
                                registeredPermissions.add(permissionNode);
                                getLogger().info("Dynamic armor permission registered: " + permissionNode);
                            } else {
                                getLogger().info("Permission " + permissionNode + " already exists, skipping...");
                            }
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Failed to register armor permission " + permissionNode + ": " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            getLogger().warning("Armor system configuration not found, skipping armor permission registration.");
        }
    }
    
    private void unregisterDynamicPermissions() {
        for (String permissionNode : registeredPermissions) {
            try {
                Permission permission = getServer().getPluginManager().getPermission(permissionNode);
                if (permission != null) {
                    getServer().getPluginManager().removePermission(permission);
                    getLogger().info("Dynamic permission unregistered: " + permissionNode);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to unregister permission " + permissionNode + ": " + e.getMessage());
            }
        }
        registeredPermissions.clear();
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public Economy getEconomy() {
        return economy;
    }
}