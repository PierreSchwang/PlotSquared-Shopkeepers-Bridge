package de.pierreschwang.psskbridge;

import com.plotsquared.core.PlotAPI;
import de.pierreschwang.psskbridge.listener.PlotListener;
import org.bukkit.plugin.java.JavaPlugin;

public class PsskBridgePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        new PlotAPI().registerListener(new PlotListener());
    }

}
