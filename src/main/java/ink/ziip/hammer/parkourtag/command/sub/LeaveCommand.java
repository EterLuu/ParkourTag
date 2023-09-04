package ink.ziip.hammer.parkourtag.command.sub;

import ink.ziip.hammer.parkourtag.api.command.BaseSubCommand;
import ink.ziip.hammer.parkourtag.api.object.area.Area;
import ink.ziip.hammer.parkourtag.api.object.user.User;
import ink.ziip.hammer.parkourtag.api.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LeaveCommand extends BaseSubCommand {

    public LeaveCommand() {
        super("leave");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            User user = User.getUser(sender);
            if (user == null)
                return true;

            for (String name : Area.getAreaList()) {
                if (Area.getArea(name).spectatorLeave(user)) {
                    sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b退出旁观模式。"));
                }
            }
            sender.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b你现在没有旁观任何游戏。"));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.singletonList("");
    }
}
