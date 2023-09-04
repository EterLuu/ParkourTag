package ink.ziip.hammer.parkourtag.api.object.user;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import ink.ziip.hammer.parkourtag.ParkourTag;
import ink.ziip.hammer.parkourtag.api.util.Utils;
import lombok.Data;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class User {

    private static final Map<UUID, User> users = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> chaserTimes = new ConcurrentHashMap<>();
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private final Player player;
    private final OfflinePlayer offlinePlayer;
    private final UUID playerUUID;
    private final CommandSender commandSender;

    private User(@NonNull Player player) {
        this.player = player;
        this.offlinePlayer = player;
        this.playerUUID = player.getUniqueId();
        this.commandSender = player;

        if (!chaserTimes.containsKey(playerUUID))
            chaserTimes.put(playerUUID, 0);

        users.put(this.playerUUID, this);
    }

    public static User getUser(@NonNull Player player) {
        if (users.containsKey(player.getUniqueId())) {
            return users.get(player.getUniqueId());
        }
        return new User(player);
    }

    public static User getUser(@NonNull CommandSender commandSender) {
        if (commandSender instanceof Player)
            return getUser((Player) commandSender);

        return null;
    }

    public static void removeUser(@NonNull Player player) {
        users.remove(player.getUniqueId());
    }

    public static void removeUser(@NonNull OfflinePlayer offlinePlayer) {
        users.remove(offlinePlayer.getUniqueId());
    }

    public static void removeUser(@NonNull UUID uuid) {
        users.remove(uuid);
    }

    public static void cleanUsers() {
        users.clear();
    }

    public int getChaserTimes() {
        return chaserTimes.get(playerUUID);
    }

    public void addChaserTimes() {
        chaserTimes.put(playerUUID, chaserTimes.get(playerUUID) + 1);
    }

    public void sendActionBar(String content, boolean filtered) {
        WrappedChatComponent wrappedChatComponent = WrappedChatComponent.fromLegacyText(setPlaceholders(content));
        PacketContainer packetContainer = protocolManager.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
        StructureModifier<Integer> integers = packetContainer.getIntegers();
        if (integers.size() == 1) {
            integers.write(0, (int) EnumWrappers.ChatType.GAME_INFO.getId());
        } else {
            packetContainer.getBooleans().write(0, true);
        }
        packetContainer.getStrings().write(0, wrappedChatComponent.getJson());
        if (filtered) {
            packetContainer.setMeta("signed", true);
        }
        protocolManager.sendServerPacket(player, packetContainer);
    }

    public void sendMessage(String content) {
        if (commandSender != null) {
            commandSender.sendMessage(Utils.translateColorCodes(setPlaceholders(content)));
            return;
        }

        if (player != null) {
            player.sendMessage(Utils.translateColorCodes(setPlaceholders(content)));
        }
    }

    public String setPlaceholders(String content) {
        // Using offlinePlayer to avoid issues
        return Utils.translateColorCodes(PlaceholderAPI.setPlaceholders(offlinePlayer, content));
    }

    public void setLevel(int level) {
        if (player != null) {
            player.setLevel(level);
        }
    }

    public void playSound(Sound sound, float volume, float pitch) {
        if (player != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public void sendTitle(String title, String subTitle) {
        if (player != null) {
            player.sendTitle(Utils.translateColorCodes(setPlaceholders(title)), Utils.translateColorCodes(setPlaceholders(subTitle)), 1, 20, 1);
        }
    }

    public void setGameMode(GameMode gameMode) {
        new BukkitRunnable() {

            @Override
            public void run() {
                player.setGameMode(gameMode);
            }
        }.runTaskLater(ParkourTag.getInstance(), 0L);
    }

    public void teleport(Location location) {
        new BukkitRunnable() {

            @Override
            public void run() {
                player.teleport(location);
            }
        }.runTaskLater(ParkourTag.getInstance(), 0L);
    }
}
