package ink.ziip.hammer.parkourtag.manager;

import ink.ziip.hammer.parkourtag.ParkourTag;
import ink.ziip.hammer.parkourtag.api.listener.BaseListener;
import ink.ziip.hammer.parkourtag.api.manager.BaseManager;
import ink.ziip.hammer.parkourtag.listener.OtherListener;
import org.bukkit.event.HandlerList;

public class ListenerManager extends BaseManager {

    private final BaseListener otherListener;

    public ListenerManager() {
        otherListener = new OtherListener();
    }

    @Override
    public void load() {

        otherListener.register();

    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(ParkourTag.getInstance());
    }

    @Override
    public void reload() {

    }
}
