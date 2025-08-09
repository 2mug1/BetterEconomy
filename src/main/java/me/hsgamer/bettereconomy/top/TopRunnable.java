package me.hsgamer.bettereconomy.top;

import io.github.projectunified.minelib.plugin.base.Loadable;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.common.task.Task;
import me.hsgamer.bettereconomy.BetterEconomy;
import me.hsgamer.bettereconomy.Utils;
import me.hsgamer.bettereconomy.api.EconomyHandler;
import me.hsgamer.bettereconomy.config.MainConfig;
import me.hsgamer.bettereconomy.provider.EconomyHandlerProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import com.maximde.hologramlib.HologramLib;
import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.custom.LeaderboardHologram;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TopRunnable implements Runnable, Loadable, Listener {
    private final BetterEconomy instance;
    private final AtomicReference<List<PlayerBalanceSnapshot>> topList = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<Map<UUID, Integer>> topIndex = new AtomicReference<>(Collections.emptyMap());
    private Task task;

    private LeaderboardHologram leaderboardHologram;

    public TopRunnable(BetterEconomy instance) {
        this.instance = instance;
    }

    @Override
    public void run() {
        EconomyHandler economyHandler = instance.get(EconomyHandlerProvider.class).getEconomyHandler();
        List<PlayerBalanceSnapshot> list = Arrays.stream(Bukkit.getOfflinePlayers())
                .parallel()
                .map(Utils::getUniqueId)
                .filter(economyHandler::hasAccount)
                .map(uuid -> new PlayerBalanceSnapshot(uuid, economyHandler.get(uuid)))
                .sorted(Comparator.comparingDouble(PlayerBalanceSnapshot::getBalance).reversed())
                .collect(Collectors.toList());
        topList.lazySet(list);

        Map<UUID, Integer> position = IntStream.range(0, list.size())
                .boxed()
                .collect(Collectors.toMap(i -> list.get(i).getUuid(), i -> i, (a, b) -> b));
        topIndex.lazySet(position);

        if (instance.get(MainConfig.class).isUseTopHologram()) {
            Map<UUID, LeaderboardHologram.PlayerScore> playerData = new HashMap<>();

            for (PlayerBalanceSnapshot s : getTopList()) {
                playerData.put(s.getUuid(), new LeaderboardHologram.PlayerScore(Bukkit.getOfflinePlayer(s.getUuid()).getName(), s.getBalance()));
            }

            leaderboardHologram.setAllScores(playerData);
            leaderboardHologram.update();
        }
    }

    @Override
    public void enable() {
        if (instance.get(MainConfig.class).isUseTopHologram()) {

            HologramManager hologramManager = HologramLib.getManager().get();

            hologramManager.removeAll();

             LeaderboardHologram.LeaderboardOptions options = LeaderboardHologram.LeaderboardOptions.builder()
                .title("所持金ランキング")
                .maxDisplayEntries(10)
                .leaderboardType(LeaderboardHologram.LeaderboardType.SIMPLE_TEXT)
                .showEmptyPlaces(true)
                .build();

            String locationData[] = instance.get(MainConfig.class).getTopHologramLocation().split(":");
            Location location = new Location(Bukkit.getWorld(locationData[0]), new Double(locationData[1]), new Double(locationData[2]), new Double(locationData[3]), new Float(locationData[4]), new Float(locationData[5]));

            leaderboardHologram = new LeaderboardHologram(options, "top_balance");

            Map<UUID, LeaderboardHologram.PlayerScore> playerData = new HashMap<>();

            for (PlayerBalanceSnapshot s : getTopList()) {
                playerData.put(s.getUuid(), new LeaderboardHologram.PlayerScore(Bukkit.getOfflinePlayer(s.getUuid()).getName(), s.getBalance()));
            }

            leaderboardHologram.setAllScores(playerData);
            leaderboardHologram.update();

            hologramManager.spawn(leaderboardHologram, location);
        }

        task = AsyncScheduler.get(instance).runTimer(this, 0, instance.get(MainConfig.class).getUpdateBalanceTopPeriod());
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
        }
        HologramManager hologramManager = HologramLib.getManager().get();

        hologramManager.removeAll();
    }

    public List<PlayerBalanceSnapshot> getTopList() {
        return topList.get();
    }

    public int getTopIndex(UUID uuid) {
        return topIndex.get().getOrDefault(uuid, -1);
    }

    public LeaderboardHologram getLeaderboardHologram() {
        return this.leaderboardHologram;
    }
}
