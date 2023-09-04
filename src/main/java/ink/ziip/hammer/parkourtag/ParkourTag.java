package ink.ziip.hammer.parkourtag;

import ink.ziip.hammer.parkourtag.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkourTag extends JavaPlugin {

    private static ParkourTag instance;

    private AreaManager areaManager;
    private ConfigManager configManager;
    private TaskManager taskManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;


    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;


        configManager = new ConfigManager();
        areaManager = new AreaManager();
        taskManager = new TaskManager();
        commandManager = new CommandManager();
        listenerManager = new ListenerManager();


        configManager.load();
        areaManager.load();
        taskManager.load();
        commandManager.load();
        listenerManager.load();


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        configManager.unload();
        areaManager.unload();
        taskManager.unload();
        commandManager.unload();
        listenerManager.unload();
    }

    public static ParkourTag getInstance() {
        return instance;
    }
}
