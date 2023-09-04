package ink.ziip.hammer.parkourtag.api.object.area;

import ink.ziip.hammer.parkourtag.ParkourTag;
import ink.ziip.hammer.parkourtag.api.object.team.TeamCard;
import ink.ziip.hammer.parkourtag.api.object.user.User;
import ink.ziip.hammer.parkourtag.api.util.Utils;
import ink.ziip.hammer.parkourtag.manager.ConfigManager;
import ink.ziip.hammer.teams.api.object.GameTypeEnum;
import ink.ziip.hammer.teams.api.object.Team;
import ink.ziip.hammer.teams.manager.TeamRecordManager;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Area implements Listener {

    private static final Map<String, Area> areas = new ConcurrentHashMap<>();

    private String areaName;
    private int areaTimer;
    private int areaDefaultTimer;
    private BoundingBox areaBoundingBox;
    private Location team1SpawnPoint;
    private Location team2SpawnPoint;
    private Location chaserSpawnPoint;
    private Location escapeeSpawnPoint;

    private Boolean status = false;
    private Boolean started = false;

    private final List<TeamCard> teamCards = new ArrayList<>();
    private final List<User> spectators = new ArrayList<>();
    private final Map<String, Integer> userAliveTime = new ConcurrentHashMap<>();
    private final Map<Team, Integer> teamFailedTime = new ConcurrentHashMap<>();
    private final Map<Team, Integer> teamPoints = new ConcurrentHashMap<>();
    private User chaser1;
    private User chaser2;
    private final List<String> chasers = new ArrayList<>();
    private Long itemUsingTimestamp;
    private final Stack<BlockState> blockStates = new Stack<>();
    private final Stack<BlockData> blockData = new Stack<>();

    private int runTaskId;
    private int round = -1;

    private void resetData() {
        while (!blockStates.isEmpty()) {
            BlockState blockState = blockStates.pop();
            blockState.setType(Material.BIRCH_WALL_SIGN);
            blockState.setBlockData(blockData.pop());
            blockState.update(true);
        }
        teamCards.clear();
        chaser1 = null;
        chaser2 = null;
        round = -1;
        areaTimer = 0;
        started = false;
        itemUsingTimestamp = 0L;
        userAliveTime.clear();
        teamFailedTime.clear();
        teamPoints.clear();
        blockStates.clear();
        blockData.clear();
        chasers.clear();
    }

    public String getAreaName() {
        return areaName;
    }

    public void loadFromConfig(String name) {
        File file = new File(ParkourTag.getInstance().getDataFolder() + File.separator + "areas" + File.separator + name + ".yml");

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        areaName = config.getString("area.name");
        areaTimer = 0;
        areaDefaultTimer = config.getInt("area.timer");
        Location areaPos1 = Utils.getLocation(config.getString("area.pos1"));
        Location areaPos2 = Utils.getLocation(config.getString("area.pos2"));
        areaBoundingBox = new BoundingBox(
                areaPos1.getX(), areaPos1.getY(), areaPos1.getZ(),
                areaPos2.getX(), areaPos2.getY(), areaPos2.getZ());

        team1SpawnPoint = Utils.getLocation(config.getString("area.team1-spawn-point"));
        team2SpawnPoint = Utils.getLocation(config.getString("area.team2-spawn-point"));
        chaserSpawnPoint = Utils.getLocation(config.getString("area.chaser-spawn-point"));
        escapeeSpawnPoint = Utils.getLocation(config.getString("area.escapee-spawn-point"));

        resetData();

        status = true;
        Bukkit.getPluginManager().registerEvents(this, ParkourTag.getInstance());
    }

    public static Area getArea(String name) {
        if (areas.containsKey(name)) {
            return areas.get(name);
        }
        return null;
    }

    public static void createArea(String name) {
        if (!areas.containsKey(name)) {
            Area area = new Area();
            area.loadFromConfig(name);
            areas.put(name, area);
        }
    }

    public static List<String> getAreaList() {
        return new ArrayList<>(areas.keySet().stream().toList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!isAreaPlayer((Player) event.getEntity())) {
            return;
        }

        Location location = event.getEntity().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            User damager = User.getUser(event.getDamager());
            User user = User.getUser((Player) event.getEntity());

            if (damager != null) {
                if (round == 2) {
                    if (damager.equals(chaser1)) {
                        captureUser(user);
                    } else {
                        event.setCancelled(true);
                    }
                }
                if (round == 4) {
                    if (damager.equals(chaser2)) {
                        captureUser(user);
                    } else {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private void captureUser(User user) {
        if (userAliveTime.containsKey(user.getPlayer().getName())) {
            return;
        }

        if (round == 2 || round == 4) {
            TeamCard escapeTeam = getEscapeeTeam();
            if (escapeTeam.getOnlineUsers().contains(user)) {
                user.setGameMode(GameMode.SPECTATOR);
                userAliveTime.put(user.getPlayer().getName(), areaDefaultTimer - areaTimer);
                sendMessageToAllUsers("&c[Parkour Tag] &b玩家 " + user.getPlayer().getName() + " &b被抓住了。" + "&f（存活时间：" + String.valueOf(areaDefaultTimer - areaTimer + "秒）"));

                int escapeTeamSize = 0;
                if (round == 2) {
                    if (chaser2 != null) {
                        escapeTeamSize = escapeTeam.getTeam().getMemberNames().size() - 1;
                    } else {
                        escapeTeamSize = escapeTeam.getTeam().getMemberNames().size();
                    }
                }
                if (round == 4) {
                    if (chaser1 != null) {
                        escapeTeamSize = escapeTeam.getTeam().getMemberNames().size() - 1;
                    } else {
                        escapeTeamSize = escapeTeam.getTeam().getMemberNames().size();
                    }
                }

                if (userAliveTime.size() == escapeTeamSize) {
                    teamFailedTime.put(escapeTeam.getTeam(), areaDefaultTimer - areaTimer);
                    sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + escapeTeam.getTeam().getOriginColoredName() + " &b的存活时间为：&f" + String.valueOf(areaDefaultTimer - areaTimer));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            if (areaTimer >= areaDefaultTimer) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteraction(PlayerInteractEvent event) {
        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {

            if (areaTimer > areaDefaultTimer) {
                event.setCancelled(true);
            }

            if (round == 1 || round == 3) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (event.getClickedBlock() != null)
                        if (event.getClickedBlock().getType() == Material.BIRCH_WALL_SIGN) {
                            if (setChaser(event.getPlayer())) {
                                BlockState blockState = event.getClickedBlock().getState();
                                blockStates.add(blockState);
                                blockData.add(event.getClickedBlock().getBlockData().clone());
                                blockState.setType(Material.AIR);
                                blockState.update(true);
                            }
                        }

                }
            }

            if (round == 2 || round == 4) {
                if (areaTimer <= areaDefaultTimer) {

                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        if (event.getMaterial() == Material.CLOCK) {
                            long time = System.currentTimeMillis();
                            User user = User.getUser(event.getPlayer());
                            if (time - itemUsingTimestamp >= 10000) {
                                PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 60, 1);
                                if (round == 2) {
                                    if (chaser1 != null)
                                        chaser1.getPlayer().addPotionEffect(glowing);
                                }
                                if (round == 4) {
                                    if (chaser2 != null)
                                        chaser2.getPlayer().addPotionEffect(glowing);
                                }
                                itemUsingTimestamp = time;
                                getEscapeeTeam().getOnlineUsers().forEach(user1 -> {
                                    user1.sendMessage("&c[Parkour Tag] &b让追击者发光三秒。");
                                });
                            } else {
                                user.sendMessage("&c[Parkour Tag] &b请等待 " + (10000 - time + itemUsingTimestamp) / 1000 + " 秒后再使用。");
                            }
                        }
                        if (event.getMaterial() == Material.FEATHER) {
                            User user = User.getUser(event.getPlayer());
                            user.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                            user.getPlayer().getInventory().clear();
                        }
                    }
                }
            }
        }

    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        User user = User.getUser(event.getPlayer());
        spectatorLeave(user);

        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }

        if (round == 1) {
            if (!chaser1.equals(user)) {
                return;
            }
        }
        if (round == 3) {
            if (!chaser2.equals(user)) {
                return;
            }
        }

        captureUser(user);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isAreaPlayer(event.getPlayer())) {
            return;
        }


        if (round == 1) {
            if (chaser1 != null) {
                if (chaser1.getPlayerUUID().equals(event.getPlayer().getUniqueId())) {
                    chaser1 = User.getUser(event.getPlayer());
                    return;
                }
            }
        }
        if (round == 3) {
            if (chaser2 != null) {
                if (chaser2.getPlayerUUID().equals(event.getPlayer().getUniqueId())) {
                    chaser2 = User.getUser(event.getPlayer());
                    return;
                }
            }
        }

        User user = User.getUser(event.getPlayer());
        user.setGameMode(GameMode.SPECTATOR);
    }

    private boolean checkJoin(TeamCard teamCard) {
        if (!teamCard.getArea().equals(this))
            return false;
        if (teamCards.contains(teamCard))
            return false;
        if (teamCard.getOnlinePlayers().isEmpty())
            return false;

        return true;
    }

    public boolean joinGame(TeamCard teamCard1, TeamCard teamCard2) {
        if (!status)
            return false;
        if (started)
            return false;
        if (!teamCards.isEmpty())
            return false;

        if (!checkJoin(teamCard1))
            return false;

        if (!checkJoin(teamCard2))
            return false;

        teamCards.add(teamCard1);
        teamCards.add(teamCard2);

        return true;
    }

    public boolean joinAsSpectator(User user) {
        if (!status) {
            return false;
        }

        if (spectators.contains(user)) {
            return false;
        }

        for (TeamCard teamCard : teamCards) {
            if (teamCard.getOnlineUsers().contains(user)) {
                return false;
            }
        }

        spectators.add(user);
        user.setGameMode(GameMode.SPECTATOR);
        user.teleport(team1SpawnPoint);

        return true;
    }

    public boolean spectatorLeave(User user) {
        for (TeamCard teamCard : teamCards) {
            if (teamCard.getOnlineUsers().contains(user)) {
                return false;
            }
        }

        if (spectators.contains(user)) {
            spectators.remove(user);
            user.setGameMode(GameMode.ADVENTURE);
            user.teleport(ConfigManager.spawnLocation);

            return true;
        }
        return false;
    }

    public boolean checkStart() {
        if (!status)
            return false;
        if (started)
            return false;
        if (teamCards.size() != 2)
            return false;
        started = true;
        teleportTeamsToSpawnPoint();
        chooseChaser(1);
        return true;
    }

    private void chooseChaser(int round) {
        this.round = round;
        if (round == 1) {

            sendMessageToAllUsers(
                    "&c[Parkour Tag] &b请&c&l&n双方队伍&b选择追击者。（超过时间将随机选择）"
            );
        }

        new BukkitRunnable() {

            private int timer = 12;

            @Override
            public void run() {

                if (!started) {
                    cancel();
                }

                if (timer >= 2) {
                    setAllPlayerLevel(timer - 2);
                }

                if (timer == 2) {
                    if (round == 1) {
                        if (chaser1 == null) {
                            TeamCard chaserTeam = getChaserTeam();
                            chaser1 = getRandomChaser(chaserTeam);
                            if (chaser1 != null) {
                                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                                        chaserTeam.getTeam().getOriginColoredName() +
                                        " &f的 &6" + chaser1.getPlayer().getName() + " &b成为了追击者。&f（剩余 " +
                                        (2 - chaser1.getChaserTimes()) + " 次）");
                            } else {
                                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                                        chaserTeam.getTeam().getOriginColoredName() +
                                        " &b没有人成为追击者。");
                            }
                        }
                        if (chaser2 == null) {
                            TeamCard chaserTeam = getEscapeeTeam();
                            chaser2 = getRandomChaser(chaserTeam);
                            if (chaser2 != null) {
                                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                                        chaserTeam.getTeam().getOriginColoredName() +
                                        " &f的 &6" + chaser2.getPlayer().getName() + " &b成为了追击者。&f（剩余 " +
                                        (2 - chaser2.getChaserTimes()) + " 次）");
                            } else {
                                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                                        chaserTeam.getTeam().getOriginColoredName() +
                                        " &b没有人成为追击者。");
                            }
                        }
                    }
                    setAllPlayerLevel(0);
                }

                if (timer == 0) {
                    startGame();
                    cancel();
                }

                timer--;
            }
        }.runTaskTimer(ParkourTag.getInstance(), 0L, 20L);
    }

    public boolean setChaser(Player player) {

        Team team = Team.getTeamByPlayer(player);

        if (team == null)
            return false;

        User user = User.getUser(player);
        if (round == 1) {
            TeamCard chaserTeam = getChaserTeam();
            if (chaserTeam.getTeam().equals(team)) {
                if (user.getChaserTimes() < 3) {
                    chaser1 = user;
                    sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                            chaserTeam.getTeam().getOriginColoredName() +
                            " &f的 &6" + player.getName() + " &b成为了追击者。&f（剩余 " +
                            (2 - chaser1.getChaserTimes()) + " 次）");
                    return true;
                }
                player.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b你成为追击者的次数已经到达上限。"));
                return false;
            } else {
                if (user.getChaserTimes() < 3) {
                    chaser2 = user;
                    sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " +
                            getEscapeeTeam().getTeam().getOriginColoredName() +
                            " &f的 &6" + player.getName() + " &b成为了追击者。&f（剩余 " +
                            (2 - chaser2.getChaserTimes()) + " 次）");
                    return true;
                }
                player.sendMessage(Utils.translateColorCodes("&c[Parkour Tag] &b你成为追击者的次数已经到达上限。"));
                return false;
            }
        }

        return false;
    }

    private void startGame() {

        if (round == 1 || round == 3) {
            if (round == 1) {
                chasers.add(chaser1.getPlayer().getName());
                chasers.add(chaser2.getPlayer().getName());
                round = 2;
                sendMessageToAllUsers("&c[Parkour Tag] &b追击方：" + getChaserTeam().getTeam().getColoredName() + " | " + chaser1.getPlayer().getName() + "&b，逃脱方：" + getEscapeeTeam().getTeam().getColoredName());
            }
            if (round == 3) {
                round = 4;
                sendMessageToAllUsers("&c[Parkour Tag] &b追击方：" + getChaserTeam().getTeam().getColoredName() + " | " + chaser2.getPlayer().getName() + "&b，逃脱方：" + getEscapeeTeam().getTeam().getColoredName());
            }
            TeamCard escapeeTeam = getEscapeeTeam();
            for (String name : escapeeTeam.getTeam().getMemberNames()) {
                if (!escapeeTeam.getOnlinePlayers().contains(Bukkit.getPlayer(name))) {
                    userAliveTime.put(name, 0);
                }
            }
            escapeeTeam.getOnlineUsers().forEach(user -> {
                if (!chasers.contains(user.getPlayer().getName())) {
                    user.teleport(escapeeSpawnPoint);
                } else {
                    user.setGameMode(GameMode.SPECTATOR);
                    user.teleport(chaserSpawnPoint);
                }
            });
            escapeeTeam.getOnlinePlayers().forEach(player -> {
                player.getInventory().addItem(new ItemStack(Material.CLOCK));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1400, 1));
            });
        }

        if (round == 2) {
            if (chaser1 != null) {
                chaser1.addChaserTimes();
                chaser1.teleport(chaserSpawnPoint);
                chaser1.getPlayer().getInventory().addItem(new ItemStack(Material.FEATHER));
            }
            getChaserTeam().getOnlineUsers().forEach(user -> {
                if (!user.equals(chaser1)) {
                    user.setGameMode(GameMode.SPECTATOR);
                    user.teleport(chaserSpawnPoint);
                }
            });
        }
        if (round == 4) {
            if (chaser2 != null) {
                chaser2.addChaserTimes();
                chaser2.teleport(chaserSpawnPoint);
                chaser2.getPlayer().getInventory().addItem(new ItemStack(Material.FEATHER));
            }
            getChaserTeam().getOnlineUsers().forEach(user -> {
                if (!user.equals(chaser2)) {
                    user.setGameMode(GameMode.SPECTATOR);
                    user.teleport(chaserSpawnPoint);
                }
            });
        }

        areaTimer = areaDefaultTimer + 10;

        runTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ParkourTag.getInstance(), new Runnable() {
            @Override
            public void run() {

                setAllPlayerLevel(areaTimer);
                sendActionBarToAllSpectators(" &6游戏剩余时间：&c" + areaTimer);

                if (areaTimer > areaDefaultTimer) {
                    sendTitleToAllUsers("&b游戏即将开始", "&c倒计时：&6" + String.valueOf(areaTimer - areaDefaultTimer));
                    playerSoundToAllUsers(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 0.5F);
                }

                if (Objects.equals(areaTimer, areaDefaultTimer)) {
                    sendTitleToAllUsers("&c[Parkour Tag]", "&b游戏开始！");
                    playerSoundToAllUsers(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1F);
                }

                if (areaTimer == 5) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.5F);
                } else if (areaTimer == 4) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.6F);
                } else if (areaTimer == 3) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.7F);
                } else if (areaTimer == 2) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.8F);
                } else if (areaTimer == 1) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.9F);
                }

                if (areaTimer == 0) {
                    endRound();
                    Bukkit.getScheduler().cancelTask(runTaskId);
                    return;
                }

                areaTimer = areaTimer - 1;
            }
        }, 0L, 20L);
    }

    public void endRound() {

        if (round == 2 || round == 4) {
            TeamCard escapeeTeam = getEscapeeTeam();
            TeamCard chaserTeam = getChaserTeam();

            if (!teamFailedTime.containsKey(escapeeTeam.getTeam())) {
                teamFailedTime.put(escapeeTeam.getTeam(), areaDefaultTimer);
            }

            // 每抓到一个逃脱者得6分
            int escapeNum = userAliveTime.size();
            addPointsToTeam(chaserTeam, escapeNum * 6);
            sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + chaserTeam.getTeam().getColoredName() + " &b抓到 " + escapeNum + " &b个逃脱者，积 " + escapeNum * 6 + " 分。");

            int escapeTeamSize = 0;
            if (round == 2) {
                if (chaser2 != null) {
                    escapeTeamSize = escapeeTeam.getTeam().getMemberNames().size() - 1;
                } else {
                    escapeTeamSize = escapeeTeam.getTeam().getMemberNames().size();
                }
            }
            if (round == 4) {
                if (chaser1 != null) {
                    escapeTeamSize = escapeeTeam.getTeam().getMemberNames().size() - 1;
                } else {
                    escapeTeamSize = escapeeTeam.getTeam().getMemberNames().size();
                }
            }

            if (userAliveTime.size() != escapeTeamSize) {
                // 有任何一位逃脱者存活，75
                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + escapeeTeam.getTeam().getColoredName() + " &b有逃脱者存活，积 75 分。");
                addPointsToTeam(escapeeTeam, 75);
                escapeeTeam.getTeam().getMemberNames().forEach(name -> {
                    if (!userAliveTime.containsKey(name))
                        if (!chasers.contains(name))
                            userAliveTime.put(name, areaDefaultTimer);
                });
            }
            // 逃脱者存活10秒得2分
            userAliveTime.values().forEach(userAliveTime -> {
                addPointsToTeam(escapeeTeam, userAliveTime / 10 * 2);
                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + escapeeTeam.getTeam().getColoredName() + " &b逃脱者存活 " + userAliveTime + " 秒，积 " + userAliveTime / 10 * 2 + " 分。");
            });

            userAliveTime.clear();

            if (round == 2) {
                teleportTeamsToSpawnPoint();
                chooseChaser(3);
            }
        }

        if (round == 4) {

            //一队先于另一队，150分
            if (teamFailedTime.get(teamCards.get(0).getTeam()) > teamFailedTime.get(teamCards.get(1).getTeam())) {
                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + teamCards.get(0).getTeam().getColoredName() + " &b存活时间较长，积 150 分。");
                addPointsToTeam(teamCards.get(0), 150);
            } else if (teamFailedTime.get(teamCards.get(0).getTeam()) < teamFailedTime.get(teamCards.get(1).getTeam())) {
                sendMessageToAllUsers("&c[Parkour Tag] &b队伍 " + teamCards.get(1).getTeam().getColoredName() + " &b存活时间较长，积 150 分。");
                addPointsToTeam(teamCards.get(1), 150);
            }

            StringBuilder content = new StringBuilder();
            content.append("&b==========")
                    .append(teamCards.get(0).getTeam().getOriginColoredName())
                    .append("&f vs ")
                    .append(teamCards.get(1).getTeam().getOriginColoredName())
                    .append("&b==========\n")
                    .append(teamCards.get(0).getTeam().getOriginColoredName()).append(": ").append(teamPoints.get(teamCards.get(0).getTeam())).append("\n")
                    .append(teamCards.get(1).getTeam().getOriginColoredName()).append(": ").append(teamPoints.get(teamCards.get(1).getTeam())).append("\n");

            TeamRecordManager.addTeamRecord(teamCards.get(0).getTeam(), teamCards.get(1).getTeam(), GameTypeEnum.ParkourTag.name(), areaName, teamPoints.get(teamCards.get(0).getTeam()), false);
            TeamRecordManager.addTeamRecord(teamCards.get(1).getTeam(), teamCards.get(0).getTeam(), GameTypeEnum.ParkourTag.name(), areaName, teamPoints.get(teamCards.get(1).getTeam()), false);
            sendMessageToAllUsers(content.toString());

            endGame();
        }
    }

    public void endGame() {
        Bukkit.getScheduler().cancelTask(runTaskId);
        teleportTeamsToLobby();
        resetData();
    }

    private void teleportTeamsToSpawnPoint() {
        teamCards.get(0).getOnlineUsers().forEach(user -> {
            user.teleport(team1SpawnPoint);
            user.setGameMode(GameMode.ADVENTURE);
            user.getPlayer().getInventory().clear();
        });
        teamCards.get(1).getOnlineUsers().forEach(user -> {
            user.teleport(team2SpawnPoint);
            user.setGameMode(GameMode.ADVENTURE);
            user.getPlayer().getInventory().clear();
        });
    }

    private void playerSoundToAllUsers(Sound sound, float volume, float pitch) {
        for (User user : spectators) {
            user.playSound(sound, volume, pitch);
        }
        teamCards.forEach(teamCard -> {
            for (User user : teamCard.getOnlineUsers()) {
                user.playSound(sound, volume, pitch);
            }
        });
    }

    private void sendTitleToAllUsers(String title, String subTitle) {
        for (User user : spectators) {
            user.sendTitle(title, subTitle);
        }
        teamCards.forEach(teamCard -> {
            for (User user : teamCard.getOnlineUsers()) {
                user.sendTitle(title, subTitle);
            }
        });
    }

    private void setAllPlayerLevel(int level) {
        teamCards.forEach(teamCard -> {
            for (User user : teamCard.getOnlineUsers()) {
                user.setLevel(level);
            }
        });
    }

    private void sendActionBarToAllSpectators(String content) {
        for (User user : spectators) {
            user.sendActionBar(content, false);
        }
        for (User user : getEscapeeTeam().getOnlineUsers()) {
            if (user.getPlayer().getGameMode() == GameMode.SPECTATOR)
                user.sendActionBar(content, false);
        }
        for (User user : getChaserTeam().getOnlineUsers()) {
            if (user.getPlayer().getGameMode() == GameMode.SPECTATOR)
                user.sendActionBar(content, false);
        }
    }

    private void sendMessageToAllUsers(String content) {
        teamCards.forEach(teamCard -> teamCard.sendMessage(content));
        for (User user : spectators) {
            user.sendMessage(content);
        }
        Bukkit.getLogger().log(Level.INFO, areaName + " " + content);
    }

    private boolean isAreaPlayer(Player player) {
        boolean is = false;

        for (TeamCard teamCard : teamCards) {
            if (teamCard.getOnlinePlayers().contains(player)) {
                is = true;
            }
        }

        return is;
    }

    private void teleportTeamsToLobby() {
        teamCards.forEach(teamCard -> {
            teamCard.getOnlineUsers().forEach(user -> {
                user.teleport(ConfigManager.spawnLocation);
                user.setGameMode(GameMode.ADVENTURE);
                user.getPlayer().getInventory().clear();
                user.setLevel(0);
                user.getPlayer().getActivePotionEffects().forEach(potionEffect -> {
                    user.getPlayer().removePotionEffect(potionEffect.getType());
                });
            });
            teamCard.setArea(null);
        });
    }

    private void addPointsToTeam(TeamCard teamCard, int points) {
        teamPoints.putIfAbsent(teamCard.getTeam(), 0);
        teamPoints.put(teamCard.getTeam(), teamPoints.get(teamCard.getTeam()) + points);
    }

    private User getRandomChaser(TeamCard teamCard) {
        for (User user : teamCard.getOnlineUsers()) {
            if (user.getChaserTimes() < 3) {
                return user;
            }
        }

        return null;
    }

    public TeamCard getEscapeeTeam() {
        if (round == 1 || round == 2)
            return teamCards.get(1);
        if (round == 3 || round == 4)
            return teamCards.get(0);

        return null;
    }

    public TeamCard getChaserTeam() {
        if (round == 1 || round == 2)
            return teamCards.get(0);
        if (round == 3 || round == 4)
            return teamCards.get(1);

        return null;
    }

}
