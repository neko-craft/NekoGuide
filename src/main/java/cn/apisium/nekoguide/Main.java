package cn.apisium.nekoguide;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.permission.Permissions;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings({"unused", "deprecation"})
@Plugin(name = "NekoGuide", version = "1.0")
@Description("An essential plugin used in NekoCraft.")
@Author("Shirasawa")
@Website("https://apisium.cn")
@ApiVersion(ApiVersion.Target.v1_13)
@Commands({
    @org.bukkit.plugin.java.annotation.command.Command(name = "guide"),
    @org.bukkit.plugin.java.annotation.command.Command(name = "lookme", permission = "nekoguide.lookme")
})
@Permissions({
    @Permission(name = "nekoguide.use"),
    @Permission(name = "nekoguide.edit"),
    @Permission(name = "nekoguide.lookme", defaultValue = PermissionDefault.TRUE)
})
public final class Main extends JavaPlugin implements Listener {
    private Player currentPlayer;
    private Player currentAttach;
    private Location attachLocation;
    private String currentName = "";
    private BukkitTask task;
    private Player nextPlayer;
    private BukkitTask task1, task2;
    private float resolution = 0.003f;
    private int loops = Math.round(1 / resolution);
    private int delay = 50;
    private Location loc00 = null;

    private static final Random r = new Random();
    private static final SimpleDateFormat FORMER = new SimpleDateFormat("HH:mm:ss");

    private long time = 0;
    private LiveRoom client;
    private static final JsonParser parser = new JsonParser();

    private final String beforeStr = "";
    private volatile String pausedUUID;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        delay = getConfig().getInt("delay", 20);
        resolution = (float) getConfig().getDouble("resolution", 20);
        loops = Math.round(1 / resolution);

        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getServer().getPluginCommand("guide");
        assert cmd != null;
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        cmd = getServer().getPluginCommand("lookme");
        assert cmd != null;
        cmd.setExecutor((it, a, b, c) -> {
            if (currentPlayer == it) {
                it.sendMessage("§e[NekoGuide]: §c你没有执行指令的权限!");
                return true;
            }
            if (!(it instanceof Player)) {
                it.sendMessage("§e[NekoGuide]: §c你不是玩家!");
                return true;
            }
            if (currentPlayer == null)  {
                it.sendMessage("§e[NekoGuide]: §c现在还没有开始直播!");
                return true;
            }
            long t = System.currentTimeMillis();
            if (t - time < 10000) {
                it.sendMessage("§e[NekoGuide]: §c切换过于频繁, 请稍后再试!");
                return true;
            }
            Player p = c.length == 1 ? getServer().getPlayer(c[0]) : (Player) it;
            if (p == null) {
                it.sendMessage("§e[NekoGuide]: §c找不到目标玩家!");
                return true;
            }
            time = t;
            nextPlayer = p;
            it.sendMessage("§e[NekoGuide]: §a切换成功!");
            return true;
        });
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§e[NekoGuide]: §c你不是玩家!");
            return true;
        }
        switch (args.length) {
            case 0:
                if (!sender.hasPermission("nekoguide.use")) {
                    sender.sendMessage("§e[NekoGuide]: §c你没有执行指令的权限!");
                    return true;
                }
                startOrStopGuide(p);
                return true;
            case 1:
                if (args[0].equalsIgnoreCase("list")) {
                    List<Map<?, ?>> locations = getConfig().getMapList("locations");
                    sender.sendMessage("§b==============§e[NekoGuide]§b==============");
                    int i = 0;
                    for (Map<?, ?> it : locations) {
                        Location l = (Location) it.get("location");
                        TextComponent t = new TextComponent("   §e" + i++ + ". §a" + it.get("name") + " §7" +
                            l.getWorld().getName() + ", " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
                        t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + l.getBlockX() +
                            " " + l.getBlockY() + " " + l.getBlockZ()));
                        sender.sendMessage(t);
                    }
                    sender.sendMessage("§b======================================");
                    return true;
                }
                break;
            case 2:
                switch (args[0]) {
                    case "other" -> {
                        if (!sender.hasPermission("nekoguide.use")) {
                            sender.sendMessage("§e[NekoGuide]: §c你没有执行指令的权限!");
                            return true;
                        }
                        final Player player = getServer().getPlayerExact(args[1]);
                        if (player == null) sender.sendMessage("§e[NekoGuide]: §c找不到该玩家!");
                        else startOrStopGuide(player);
                        return true;
                    }
                    case "add" -> {
                        if (!sender.hasPermission("nekoguide.edit")) {
                            sender.sendMessage("§e[NekoGuide]: §c你没有执行指令的权限!");
                            return true;
                        }
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", args[1]);
                        map.put("location", p.getLocation());
                        List<Map<?, ?>> list = getConfig().getMapList("locations");
                        list.add(map);
                        getConfig().set("locations", list);
                        saveConfig();
                        sender.sendMessage("§e[NekoGuide]: §a保存成功!");
                        return true;
                    }
                    case "delete" -> {
                        if (!sender.hasPermission("nekoguide.edit")) {
                            sender.sendMessage("§e[NekoGuide]: §c你没有执行指令的权限!");
                            return true;
                        }
                        try {
                            List<Map<?, ?>> list2 = getConfig().getMapList("locations");
                            list2.remove(Integer.parseInt(args[1]));
                            getConfig().set("locations", list2);
                            saveConfig();
                            sender.sendMessage("§e[NekoGuide]: §a保存成功!");
                        } catch (Exception e) {
                            e.printStackTrace();
                            sender.sendMessage("§e[NekoGuide]: §a保存失败!");
                        }
                        return true;
                    }
                }
                break;
        }
        sender.sendMessage("§e[NekoGuide]: §c错误的指令!");
        return true;
    }

    @SuppressWarnings("BusyWait")
    private void startOrStopGuide(Player p) {
        if (Objects.requireNonNull(getConfig().getList("locations")).isEmpty()) {
            p.sendMessage("§e[NekoGuide]: §c当前固定坐标列表为空!");
            return;
        }
        if (currentPlayer != null && currentPlayer == p) {
            cleanAttach();
            currentPlayer = null;
            if (client != null) {
                client.stop();
                client = null;
            }
            pausedUUID = null;
            p.sendMessage("§e[NekoGuide]: §a已停止!");
            return;
        }
        if (pausedUUID != null) pausedUUID = null;
        p.sendMessage("§e[NekoGuide]: §a任务开始!");
        currentPlayer = p;
        currentPlayer.setGameMode(GameMode.SPECTATOR);
        task = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            while (pausedUUID != null) try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (currentPlayer == null) {
                if (task != null) {
                    task.cancel();
                    task = null;
                    if (client != null) {
                        client.stop();
                        client = null;
                    }
                }
                if (task1 != null) {
                    task1.cancel();
                    task1 = null;
                }
                if (task2 != null) {
                    task2.cancel();
                    task2 = null;
                }
            } else
                currentPlayer.sendActionBar(getConfig().getString("title", "").replace("&", "§") +
                " §7| §b服务器在线人数: §a" + getServer().getOnlinePlayers().size() + " §7| §b当前视角: §a" +
                (currentAttach == null ? currentName : currentAttach.getName()) + " §7| §b当前时间: §a" +
                    FORMER.format(new Date()));
        }, 0, 20);
        task1 = getServer().getScheduler().runTaskAsynchronously(this, () -> {
            while (pausedUUID != null) try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (currentPlayer != null) {
                if (currentAttach == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (nextPlayer != null) {
                        currentAttach = nextPlayer;
                        nextPlayer = null;
                    } else {
                        final Object[] players = getServer().getOnlinePlayers().stream()
                            .filter(it -> it.getGameMode() == GameMode.SURVIVAL && !it.isDead())
                            .toArray();
                        final double num = getConfig().getDouble("fixedLocationProbability", 0.2);
                        if (players.length != 0 && (players.length < 6 ? r.nextDouble() > 1 - num :
                            r.nextDouble() > num)) {
                            currentAttach = (Player) players[players.length == 1 ? 0 : r.nextInt(players.length - 1)];
                            attachLocation = null;
                            currentName = "";
                        } else {
                            List<Map<?, ?>> l = getConfig().getMapList("locations");
                            int size = l.size() - 1;
                            currentAttach = null;
                            Map<?, ?> map = l.get(size > 0 ? r.nextInt(size) : size);
                            currentName = (String) map.get("name");
                            attachLocation = (Location) map.get("location");
                        }
                    }
                }
                if (currentAttach == null && attachLocation == null) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else {
                    if (currentAttach != null) currentAttach.setGlowing(true);
                }
                FourPoints fp = new FourPoints((currentAttach == null ? attachLocation.clone()
                    : currentAttach.getLocation()).add(0, 15, 0)),
                        playerFp = currentAttach == null ? null : new FourPoints(currentAttach);
                try {
                    loop: for (int j = 0; j < 7; j++) {
                        if ((currentAttach == null && attachLocation == null) || currentPlayer == null) break;
                        fp.next((currentAttach == null ? attachLocation.clone()
                            : currentAttach.getLocation()).add(0, 15, 0));
                        if (playerFp != null) {
                            Location targetLoc = currentAttach.getLocation();
                            playerFp.next(targetLoc);
                        }
                        for (int i = 0; i <= loops; i++) {
                            if ((currentAttach == null && attachLocation == null) || currentPlayer == null) break loop;
                            Location targetLoc = playerFp == null ? attachLocation
                                    : getCatmullRomPosition(i * resolution, playerFp.p0, playerFp.p1, playerFp.p2, playerFp.p3);
                            Location loc = getCatmullRomPosition(i * resolution, fp.p0, fp.p1, fp.p2, fp.p3);
                            if (currentAttach != null) {
                                Location playerLoc = currentAttach.getLocation();
                                if (playerLoc.getWorld() != loc.getWorld() || playerLoc.distance(loc) > 80) {
                                    cleanAttach();
                                    break loop;
                                }
                            }
                            double dx = loc.getX() - targetLoc.getX();
                            double dy = loc.getY() - targetLoc.getY();
                            double dz = loc.getZ() - targetLoc.getZ();
                            loc.setYaw((float) (Math.atan2(dx, -dz) * 180 / Math.PI));
                            loc.setPitch((float) (Math.atan2(dy, Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2))) * 180 / Math.PI));
                            loc00 = loc;
                            Thread.sleep(delay);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                cleanAttach();
            }
        });
        task2 = getServer().getScheduler().runTaskTimer(this, () -> {
            if (currentPlayer != null && loc00 != null) currentPlayer.teleport(loc00);
        }, 0, 0);
        connect();
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (task1 != null) {
            task1.cancel();
            task1 = null;
        }
        if (task2 != null) {
            task2.cancel();
            task2 = null;
        }
        pausedUUID = null;
        cleanPlayer();
        cleanAttach();
    }

    private static Location getCatmullRomPosition(float t, Location p0, Location p1, Location p2, Location p3) {
        final Location a = p1.clone().multiply(2);
        final Location b = p2.clone().subtract(p0);
        final Location c = p0.clone().multiply(2).subtract(p1.clone().multiply(5))
            .add(p2.clone().multiply(4)).subtract(p3);
        final Location d = p3.clone().subtract(p0).add(p1.clone().multiply(3)).subtract(p2.clone().multiply(3));

        return a.add(b.multiply(t).add(c.multiply(t * t)).add(d.multiply(t * t * t))).multiply(0.5);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (e.getPlayer() == nextPlayer) nextPlayer = null;
        if (e.getPlayer() == currentPlayer) {
            pausedUUID = e.getPlayer().getUniqueId().toString();
            cleanPlayer();
        } else if (e.getPlayer() == currentAttach) cleanAttach();
    }

    @EventHandler
    void onPlayerTeleport(PlayerTeleportEvent e) {
        if (e.getPlayer() == nextPlayer) nextPlayer = null;
        if (e.getPlayer() == currentAttach) cleanAttach();
    }

    @EventHandler
    void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getEntity() == nextPlayer) nextPlayer = null;
        if (e.getEntity() == currentAttach) cleanAttach();
        else if (e.getEntity() == currentPlayer) e.setCancelled(true);
    }

    @EventHandler
    void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getPlayer() == nextPlayer) nextPlayer = null;
        if (e.getPlayer() == currentPlayer && e.getNewGameMode() != GameMode.SPECTATOR) e.setCancelled(true);
        else if (e.getPlayer() == currentAttach) cleanAttach();
    }

    @EventHandler
    void onEntityToggleGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() == nextPlayer) nextPlayer = null;
        if (e.getEntity() == currentAttach) cleanAttach();
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent e) {
        if (pausedUUID != null && e.getPlayer().getUniqueId().toString().equals(pausedUUID)) {
            currentPlayer = e.getPlayer();
            pausedUUID = null;
        }
    }

    @EventHandler
    void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() == currentPlayer) e.setCancelled(true);
    }

    private void cleanAttach() {
        if (currentPlayer != null && currentAttach != null) currentAttach.setGlowing(false);
        currentAttach = null;
    }

    private void cleanPlayer() {
        currentPlayer = null;
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    private void connect() {
        final int id = getConfig().getInt("roomId");
        if (currentPlayer == null || id == 0 || client != null) return;
        getLogger().info("Connecting... " + id);

        client = new LiveRoom(id)
            .onMessage(it -> {
            JsonObject json = parser.parse(it).getAsJsonObject();
            switch (json.get("cmd").getAsString()) {
                case "DANMU_MSG":
                    JsonArray info = json.get("info").getAsJsonArray();
                    getServer().broadcastMessage("§a[直播间] §f" + info.get(2).getAsJsonArray().get(1).getAsString() +
                        "§7: " + info.get(1).getAsString());
                    break;
                case "SEND_GIFT":
                    if (currentPlayer != null) {
                        JsonObject data = json.get("data").getAsJsonObject();
                        currentPlayer.sendMessage("§a[直播间] §7感谢 §f" + data.get("uname").getAsString() +
                            " §7赠送的 §f" + data.get("num").getAsInt() + "个" + data.get("giftName").getAsString() +
                            "§7!");
                    }
                    break;
                case "WELCOME":
                    if (currentPlayer != null) currentPlayer.sendMessage("§a[直播间] §7欢迎 §f" +
                        json.get("data").getAsJsonObject().get("uname").getAsString() + " §7进入直播间!");
            }
            })
            .onConnected(() -> getLogger().info("Connected"))
            .onDisconnected(() -> getLogger().info("Disconnected"));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return sender.hasPermission("nekoguide.edit") && args.length == 1
            ? Lists.newArrayList("list", "add", "delete", "other")
            : null;
    }
}
