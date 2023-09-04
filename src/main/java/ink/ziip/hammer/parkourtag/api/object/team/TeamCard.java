package ink.ziip.hammer.parkourtag.api.object.team;

import ink.ziip.hammer.parkourtag.api.object.area.Area;
import ink.ziip.hammer.parkourtag.api.object.user.User;
import ink.ziip.hammer.teams.api.object.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TeamCard {
    private static final Map<String, TeamCard> teamCards = new ConcurrentHashMap<>();

    private final Team team;
    private Area area = null;

    public void clear() {
        area = null;
    }

    private TeamCard(Team team) {
        this.team = team;
        teamCards.put(team.getName(), this);
    }

    public static TeamCard getTeamCard(String name) {
        if (teamCards.containsKey(name))
            return teamCards.get(name);

        Team team = Team.getTeam(name);
        if (team == null)
            return null;

        return new TeamCard(team);
    }

    public static TeamCard getPlayerTeamCard(Player player) {
        Team team = Team.getTeamByPlayer(player);
        return getTeamCard(team.getName());
    }

    public static TeamCard getPlayerTeamCard(OfflinePlayer offlinePlayer) {
        Team team = Team.getTeamByPlayer(offlinePlayer);
        return getTeamCard(team.getName());
    }

    public static TeamCard getPlayerTeamCard(String player) {
        Team team = Team.getTeamByPlayer(player);
        return getTeamCard(team.getName());
    }

    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();

        for (String name : team.getMemberNames()) {
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    public List<User> getOnlineUsers() {
        List<User> users = new ArrayList<>();

        for (Player player : getOnlinePlayers()) {
            User user = User.getUser(player);
            if (user != null) {
                users.add(user);
            }
        }

        return users;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Team getTeam() {
        return team;
    }

    public void teleport(Location location) {
        getOnlineUsers().forEach(user -> {
            user.teleport(location);
        });
    }

    public void setGameMode(GameMode gameMode) {
        getOnlineUsers().forEach(user -> {
            user.setGameMode(gameMode);
        });
    }

    public void sendMessage(String content) {
        getOnlineUsers().forEach(user -> {
            user.sendMessage(content);
        });
    }
}
