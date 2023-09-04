package ink.ziip.hammer.parkourtag.command.sub;

import ink.ziip.hammer.parkourtag.api.command.BaseSubCommand;
import ink.ziip.hammer.parkourtag.api.object.area.Area;
import ink.ziip.hammer.parkourtag.api.object.team.TeamCard;
import ink.ziip.hammer.parkourtag.api.object.user.User;
import ink.ziip.hammer.teams.api.object.Team;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SpectatorCommand extends BaseSubCommand {

    public SpectatorCommand() {
        super("spectator");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return true;
        }
        User user = User.getUser(sender);
        if (user == null)
            return true;

        Area area = Area.getArea(args[0]);

        if (area == null) {
            user.sendMessage("&b[Parkour Tag] &c错误的场地名。");
            return true;
        }

        if (user.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            user.sendMessage("&b[Parkour Tag] &c你已经在旁观一个游戏了，使用“/pkt leave”退出旁观。");
            return true;
        }

        Team team = Team.getTeamByPlayer((Player) sender);

        if (team != null) {
            TeamCard teamCard = TeamCard.getTeamCard(team.getName());
            if (teamCard != null && teamCard.getArea() != null) {
                user.sendMessage("&b[Parkour Tag] &c你加入一个游戏了，不能旁观。");
                return true;
            }
        }

        if (area.joinAsSpectator(user)) {
            user.sendMessage("&b[Parkour Tag] &c旁观场地中。");
        } else {
            user.sendMessage("&b[Parkour Tag] &c旁观场地失败。");
        }

        return true;


    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            List<String> returnList = Area.getAreaList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[0]));
            } catch (Exception ignored) {
            }
            return returnList;
        }

        return Collections.singletonList("");
    }
}
