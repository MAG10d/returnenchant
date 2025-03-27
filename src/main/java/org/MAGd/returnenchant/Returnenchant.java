package org.MAGd.returnenchant;

import org.bukkit.plugin.java.JavaPlugin;

public class Returnenchant extends JavaPlugin {
    @Override
    public void onEnable() {
        // 註冊指令執行器
        this.getCommand("returnenchant").setExecutor(new ReturnEnchantCommand(this));
        getLogger().info("ReturnEnchant plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReturnEnchant plugin has been disabled!");
    }
}
