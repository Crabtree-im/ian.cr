package com.btree.hungergames;

import com.btree.hungergames.command.HGCommand;
import com.btree.hungergames.config.PluginConfig;
import com.btree.hungergames.listener.MatchListener;
import com.btree.hungergames.match.MatchManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HungerGamesPlugin extends JavaPlugin {
    private MatchManager matchManager;

    @Override
    public void onEnable() {
        PluginConfig config = PluginConfig.load(this);
        this.matchManager = new MatchManager(this, config);

        PluginCommand command = getCommand("hg");
        if (command != null) {
            HGCommand hgCommand = new HGCommand(matchManager);
            command.setExecutor(hgCommand);
            command.setTabCompleter(hgCommand);
        }

        getServer().getPluginManager().registerEvents(new MatchListener(matchManager), this);
        getLogger().info("HungerGamesCore enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HungerGamesCore disabled.");
    }
}
