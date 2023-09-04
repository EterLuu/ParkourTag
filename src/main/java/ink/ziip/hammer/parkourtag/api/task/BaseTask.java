package ink.ziip.hammer.parkourtag.api.task;

import ink.ziip.hammer.parkourtag.ParkourTag;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class BaseTask extends BukkitRunnable {

    protected boolean started;
    protected int period;

    public BaseTask(int period) {
        this.started = false;
        this.period = period;
    }

    public void start() {
        this.runTaskTimerAsynchronously(ParkourTag.getInstance(), 1, period);
        started = true;
    }

    public abstract void stop();
}
