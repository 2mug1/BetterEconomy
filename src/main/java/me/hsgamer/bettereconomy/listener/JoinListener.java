package me.hsgamer.bettereconomy.listener;

import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.bettereconomy.BetterEconomy;
import me.hsgamer.bettereconomy.api.EconomyHandler;
import me.hsgamer.bettereconomy.provider.EconomyHandlerProvider;
import me.hsgamer.bettereconomy.top.TopRunnable;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.maximde.hologramlib.hologram.custom.LeaderboardHologram;

import java.util.UUID;

public class JoinListener implements ListenerComponent {
    private final BetterEconomy instance;

    public JoinListener(BetterEconomy instance) {
        this.instance = instance;
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        EconomyHandler economyHandler = instance.get(EconomyHandlerProvider.class).getEconomyHandler();
        if (!economyHandler.hasAccount(uuid)) {
            economyHandler.createAccount(uuid);
        }
        LeaderboardHologram leaderboardHologram = instance.get(TopRunnable.class).getLeaderboardHologram();
        if (leaderboardHologram != null) {
            leaderboardHologram.show(event.getPlayer());
        }
    }
}
