package de.pierreschwang.psskbridge.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.plotsquared.core.events.PlotUnlinkEvent;
import com.plotsquared.core.events.post.PostPlotDeleteEvent;
import com.plotsquared.core.events.post.PostPlotUnlinkEvent;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import it.unimi.dsi.fastutil.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class PlotListener {

    private static final long PENDING_THRESHOLD = 1000 * 5; // the server has 5 seconds to clear and unlink the plots

    private final Map<Plot, Pair<Long, Set<ChunkCoords>>> pendingUnlinks = Collections.synchronizedMap(new HashMap<>());

    public PlotListener() {
        // clean up pending unlink operations
        TaskManager.getPlatformImplementation().taskRepeat(() -> {
            synchronized (pendingUnlinks) {
                pendingUnlinks.entrySet().removeIf(longPairEntry -> longPairEntry
                        .getValue().left() + PENDING_THRESHOLD > System.currentTimeMillis());
            }
        }, TaskTime.seconds(1));
    }

    @Subscribe
    public void onPlotDelete(PostPlotDeleteEvent event) {
        String worldName = Objects.requireNonNull(event.getPlot().getArea()).getWorldName();
        var shopkeepersByChunks = ImmutableMap.copyOf(ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersByChunks(worldName));

        Set<ChunkCoords> chunkCoords = event.getPlot().getLargestRegion().getChunks().stream()
                .map(chunkXZ -> new ChunkCoords(Objects.requireNonNull(event.getPlot().getWorldName()), chunkXZ.getX(), chunkXZ.getZ()))
                .collect(Collectors.toSet());
        TaskManager.getPlatformImplementation().task(() -> {
            chunkCoords.forEach(coords -> {
                Collection<? extends Shopkeeper> shopkeepers = shopkeepersByChunks.get(coords);
                if (shopkeepers == null) {
                    return;
                }
                ImmutableSet.copyOf(shopkeepers).forEach(shopkeeper -> {
                    shopkeeper.delete();
                    shopkeeper.save();
                });
            });
        });
    }

    @Subscribe
    public void onPlotUnlink(PlotUnlinkEvent event) {
        Set<ChunkCoords> chunkCoords = event.getPlot().getLargestRegion().getChunks().stream()
                .map(chunkXZ -> new ChunkCoords(Objects.requireNonNull(event.getPlot().getWorldName()), chunkXZ.getX(), chunkXZ.getZ()))
                .collect(Collectors.toSet());
        this.pendingUnlinks.put(event.getPlot(), Pair.of(System.currentTimeMillis(), chunkCoords));
    }

    @Subscribe
    public void onPostPlotUnlink(PostPlotUnlinkEvent event) {
        PlotArea area = event.getPlot().getArea();
        synchronized (pendingUnlinks) {
            final Pair<Long, Set<ChunkCoords>> result = pendingUnlinks.remove(event.getPlot());
            if (result == null) {
                return;
            }
            var shopkeepersByChunks = ImmutableMap.copyOf(ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersByChunks(Objects.requireNonNull(event.getPlot().getWorldName())));
            TaskManager.getPlatformImplementation().task(() -> {
                result.right().forEach(coords -> {
                    Collection<? extends Shopkeeper> shopkeepers = shopkeepersByChunks.get(coords);
                    if (shopkeepers == null) {
                        return;
                    }
                    ImmutableSet.copyOf(shopkeepers).forEach(shopkeeper -> {
                        org.bukkit.Location skLocation = shopkeeper.getLocation();
                        if (area.getPlot(Location.at(area.getWorldName(), skLocation.getBlockX(), skLocation.getBlockY(), skLocation.getBlockZ())) == null) {
                            shopkeeper.delete();
                            shopkeeper.save();
                        }
                    });
                });
            });
        }
    }

}
