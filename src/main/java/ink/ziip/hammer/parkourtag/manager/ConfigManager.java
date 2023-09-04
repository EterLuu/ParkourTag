package ink.ziip.hammer.parkourtag.manager;

import ink.ziip.hammer.parkourtag.ParkourTag;
import ink.ziip.hammer.parkourtag.api.manager.BaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class ConfigManager extends BaseManager {

    private final FileConfiguration config;

    public static Location spawnLocation;

    public ConfigManager() {
        config = ParkourTag.getInstance().getConfig();
    }

    @Override
    public void load() {
        loadConfig();
    }

    @Override
    public void unload() {

    }

    @Override
    public void reload() {
        ParkourTag.getInstance().reloadConfig();
        load();
    }

    private void loadConfig() {


        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString("spawn.world")));
        double x, y, z;
        float yaw, pitch;
        x = Double.parseDouble(Objects.requireNonNull(config.getString("spawn.x")));
        y = Double.parseDouble(Objects.requireNonNull(config.getString("spawn.y")));
        z = Double.parseDouble(Objects.requireNonNull(config.getString("spawn.z")));
        yaw = Float.parseFloat(Objects.requireNonNull(config.getString("spawn.yaw")));
        pitch = Float.parseFloat(Objects.requireNonNull(config.getString("spawn.pitch")));

        spawnLocation = new Location(world, x, y, z, yaw, pitch);
    }
}
