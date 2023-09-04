package ink.ziip.hammer.parkourtag.task;

import ink.ziip.hammer.parkourtag.ParkourTag;
import ink.ziip.hammer.parkourtag.api.object.QueueTeam;
import ink.ziip.hammer.parkourtag.api.object.area.Area;
import ink.ziip.hammer.parkourtag.api.task.BaseTask;
import ink.ziip.hammer.parkourtag.api.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AreaManagerTask extends BaseTask {

    private static Queue<QueueTeam> queue = new ConcurrentLinkedQueue<>();

    public AreaManagerTask() {
        super(10);
    }

    @Override
    public void stop() {
        cancel();
    }

    @Override
    public void run() {
        if (started) {
            Area.getAreaList().forEach(area -> {
                Objects.requireNonNull(Area.getArea(area)).checkStart();
            });
            if (!queue.isEmpty()) {
                QueueTeam queueTeam = queue.poll();
                if (!queueTeam.getArea().joinGame(queueTeam.getTeamCard1(), queueTeam.getTeamCard2())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                player.sendMessage(Utils.translateColorCodes("&6[Parkour Tag 调度] &c队伍 " +
                                        queueTeam.getTeamCard1().getTeam().getColoredName() +
                                        " &c和队伍 " + queueTeam.getTeamCard2().getTeam().getColoredName() + " &c无法满足条件，加入场地 " + queueTeam.getArea().getAreaName() + " 失败。"));
                            });
                        }
                    }.runTaskAsynchronously(ParkourTag.getInstance());
                    queueTeam.getTeamCard1().setArea(null);
                    queueTeam.getTeamCard2().setArea(null);
                }
            }
        }
    }

    public static boolean teamJoin(QueueTeam queueTeam, Area area) {
        if (queueTeam.getTeamCard1().getArea() == null && queueTeam.getTeamCard2().getArea() == null) {
            for (String name : Area.getAreaList()) {
                queueTeam.getTeamCard1().getOnlineUsers().forEach(user -> {
                    Area.getArea(name).spectatorLeave(user);
                });
                queueTeam.getTeamCard2().getOnlineUsers().forEach(user -> {
                    Area.getArea(name).spectatorLeave(user);
                });
            }
            queueTeam.getTeamCard1().setArea(area);
            queueTeam.getTeamCard2().setArea(area);
            queue.add(queueTeam);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendMessage(Utils.translateColorCodes("&6[Parkour Tag 调度] &c队伍 " +
                                queueTeam.getTeamCard1().getTeam().getColoredName() +
                                " &c和队伍 " + queueTeam.getTeamCard2().getTeam().getColoredName() + " &c即将在场地 " + queueTeam.getArea().getAreaName() + " 展开比赛。使用命令&f“/pkt spectator " + queueTeam.getArea().getAreaName() + "”&b前往旁观吧！"));
                    });
                }
            }.runTaskAsynchronously(ParkourTag.getInstance());
            return true;
        }
        return false;
    }
}
