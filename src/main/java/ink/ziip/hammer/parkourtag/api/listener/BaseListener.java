package ink.ziip.hammer.parkourtag.api.listener;

import ink.ziip.hammer.parkourtag.ParkourTag;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class BaseListener implements Listener {

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, ParkourTag.getInstance());
    }

    public void unRegister() {
        HandlerList.unregisterAll(this);
    }
}
