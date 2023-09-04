package ink.ziip.hammer.parkourtag.command.sub;

import ink.ziip.hammer.parkourtag.api.command.BaseSubCommand;
import ink.ziip.hammer.parkourtag.api.object.QueueTeam;
import ink.ziip.hammer.parkourtag.api.object.area.Area;
import ink.ziip.hammer.parkourtag.api.object.team.TeamCard;
import ink.ziip.hammer.parkourtag.api.util.Utils;
import ink.ziip.hammer.parkourtag.task.AreaManagerTask;
import ink.ziip.hammer.teams.api.object.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand extends BaseSubCommand {

    private final String[] commandList = new String[]{
            "start",
            "end"
    };

    public AdminCommand() {
        super("admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            return true;
        }

        if (args[0].equals("start") && args.length == 4) {

            Area area = Area.getArea(args[1]);
            TeamCard teamCard1 = TeamCard.getTeamCard(args[2]);
            TeamCard teamCard2 = TeamCard.getTeamCard(args[3]);

            if (area == null) {
                sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b错误的场地。"));
                return true;
            }

            if (teamCard1 == null || teamCard2 == null || teamCard1.equals(teamCard2)) {
                sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b错误的队伍。"));
                return true;
            }

            QueueTeam queueTeam = QueueTeam.builder().teamCard1(teamCard1).teamCard2(teamCard2).area(area).build();
            if (AreaManagerTask.teamJoin(queueTeam, area)) {
                sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b调度成功。"));
            } else {
                sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b调度失败。"));
            }

        }
        if (args[0].equals("end") && args.length == 2) {
            Area area = Area.getArea(args[1]);
            if (area == null) {
                sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b错误的场地。"));
                return true;
            }
            area.endGame();
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            return Arrays.stream(commandList).toList();
        }
        if ((args[0].equals("start") || args[0].equals("end")) && args.length == 2) {
            List<String> returnList = Area.getAreaList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[1]));
            } catch (Exception ignored) {
            }
            return returnList;
        }

        if (args.length == 3 && args[0].equals("start")) {
            List<String> returnList = Team.getTeamNameList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[2]));
            } catch (Exception ignored) {
            }

            return returnList;
        }
        if (args.length == 4 && args[0].equals("start")) {
            List<String> returnList = Team.getTeamNameList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[3]) || s.equals(args[2]));
            } catch (Exception ignored) {
            }

            return returnList;
        }
        return Collections.emptyList();
    }
}
