package com.lineage.game;

import com.lineage.db.*;
import com.lineage.game.instancemanager.*;
import com.lineage.game.tables.*;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.Server;
import com.lineage.ext.mods.balancer.Balancer;
import com.lineage.ext.network.MMOConnection;
import com.lineage.ext.network.MMOSocket;
import com.lineage.ext.network.SelectorConfig;
import com.lineage.ext.network.SelectorThread;
import com.lineage.ext.scripts.Events;
import com.lineage.ext.scripts.ScriptObject;
import com.lineage.game.cache.CrestCache;
import com.lineage.game.cache.PlayerShiftCache;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.loginservercon.LSConnection;
import com.lineage.game.model.AutoChatHandler;
import com.lineage.game.model.AutoSpawnHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.entity.Hero;
import com.lineage.game.model.entity.MonsterRace;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.network.L2GamePacketHandler;
import com.lineage.game.taskmanager.ItemsAutoDestroy;
import com.lineage.game.taskmanager.TaskManager;
import com.lineage.status.Status;
import com.lineage.util.GsaTr;
import com.lineage.util.HWID;
import com.lineage.util.Log;
import com.lineage.util.Strings;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class GameServer {
    private static final Logger _log = Logger.getLogger(GameServer.class.getName());

    private final SelectorThread<L2GameClient> _selectorThread;
    private final ItemTable _itemTable;
    public static GameServer gameServer;

    public static Status statusServer;

    public static Events events;

    public static FastMap<String, ScriptObject> scriptsObjects = new FastMap<String, ScriptObject>();

    private static int _serverStarted;

    public SelectorThread<L2GameClient> getSelectorThread() {
        return _selectorThread;
    }

    public static int time() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static int uptime() {
        return time() - _serverStarted;
    }

    public GameServer() throws Exception {
        System.out.print("\n\r");
        System.out.print(" _     ____  ____                           \n\r");
        System.out.print("| |   |___ \\|  _ \\ _ __ ___  __ _ _ __ ___  \n\r");
        System.out.print("| |     __) | | | | '__/ _ \\/ _` | '_ ` _ \\ \n\r");
        System.out.print("| |___ / __/| |_| | | |  __/ (_| | | | | | |\n\r");
        System.out.print("|_____|_____|____/|_|  \\___|\\__,_|_| |_| |_|\n\r\n");
        System.out.print("Interlute(INTERLUDE,T0) Emulator.\n\r");
        System.out.print("************COPYRIGHT************\n\r");
        System.out.print("Powered by Midnex (C) 2010.\n\r");
        System.out.print("[Skype]: x.midnex\n\r");
        System.out.print("[email]: x.midnex@gmail.com\n\r");
        System.out.print("[Revision]: " + Config.SERVER_VERSION + ".\n\r");
        System.out.print("[Build Date]: " + Config.SERVER_BUILD_DATE + ".\n\r");
        System.out.print("[Build FOR]: " + GsaTr.Buyler + ".\n\r");
        System.out.print("[Build type]: " + GsaTr.Type + ".\n\r");
        System.out.print("************COPYRIGHT************\n\r");
        System.out.print("\n\r");

        Server.gameServer = this;

        _serverStarted = time();

        _log.finest("used mem:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");

        Strings.reload();

        final IdFactory _idFactory = IdFactory.getInstance();
        if (!_idFactory.isInitialized()) {
            _log.severe("Could not read object IDs from DB. Please Check Your Data.");
            throw new Exception("Could not initialize the ID factory");
        }

        ThreadPoolManager.getInstance();

        if (Config.DEADLOCKCHECK_INTERVAL > 0)
            DeadlockDetector.getInstance().start();

        CrestCache.load();

        // start game time control early
        GameTimeController.getInstance();

        // keep the references of Singletons to prevent garbage collection
        CharNameTable.getInstance();

        ClanTable.getInstance();

        FakePlayersTable.getInstance();

        SkillTable.getInstance();
        AugmentationManager.getInstance();

        Balancer.load();

        _itemTable = ItemTable.getInstance();
        if (!_itemTable.isInitialized()) {
            _log.severe("Could not find the >Items files. Please Check Your Data.");
            throw new Exception("Could not initialize the item table");
        }

        final ArmorSetsTable _armorSetsTable = ArmorSetsTable.getInstance();
        if (!_armorSetsTable.isInitialized()) {
            _log.severe("Could not find the ArmorSets files. Please Check Your Data.");
            throw new Exception("Could not initialize the armorSets table");
        }

        events = new Events();

        TradeController.getInstance();

        RecipeController.getInstance();

        SkillTreeTable.getInstance();
        SkillSpellbookTable.getInstance();
        CharTemplateTable.getInstance();

        NpcTable.getInstance();
        if (!NpcTable.isInitialized()) {
            _log.severe("Could not find the extraced files. Please Check Your Data.");
            throw new Exception("Could not initialize the npc table");
        }

        final HennaTable _hennaTable = HennaTable.getInstance();
        if (!_hennaTable.isInitialized())
            throw new Exception("Could not initialize the Henna Table");
        HennaTreeTable.getInstance();
        if (!_hennaTable.isInitialized())
            throw new Exception("Could not initialize the Henna Tree Table");

        LevelUpTable.getInstance();

        GeoEngine.loadGeo();

        DoorTable.getInstance();

        TownManager.getInstance();

        CastleManager.getInstance();
        CastleSiegeManager.load();

        ClanHallManager.getInstance();
        ClanHallSiegeManager.load();

        CastleManorManager.getInstance();

        SpawnTable.getInstance();

        RaidBossSpawnManager.getInstance();

        DimensionalRiftManager.getInstance();

        Announcements.getInstance();

        AutoAnnouncements.getInstance();

        LotteryManager.getInstance();

        MapRegion.getInstance();

        PlayerMessageStack.getInstance();

        if (Config.AUTODESTROY_ITEM_AFTER > 0)
            ItemsAutoDestroy.getInstance();

        MonsterRace.getInstance();

        StaticObjectsTable.getInstance();

        final SevenSigns _sevenSignsEngine = SevenSigns.getInstance();
        SevenSignsFestival.getInstance();

        final AutoSpawnHandler _autoSpawnHandler = AutoSpawnHandler.getInstance();
        _log.config("AutoSpawnHandler: Loaded " + _autoSpawnHandler.size() + " handlers in total.");

        final AutoChatHandler _autoChatHandler = AutoChatHandler.getInstance();
        _log.config("AutoChatHandler: Loaded " + _autoChatHandler.size() + " handlers in total.");

        _sevenSignsEngine.spawnSevenSignsNPC();

        if (Config.ENABLE_OLYMPIAD) {
            Olympiad.load();
            Hero.getInstance();
        }

        CursedWeaponsManager.getInstance();

        if (!Config.WEDDING_ALLOW_WEDDING) {
            CoupleManager.getInstance();
            _log.config("CoupleManager initialized");
        }

        final ItemHandler _itemHandler = ItemHandler.getInstance();
        _log.config("ItemHandler: Loaded " + _itemHandler.size() + " handlers.");

        final AdminCommandHandler _adminCommandHandler = AdminCommandHandler.getInstance();
        _log.config("AdminCommandHandler: Loaded " + _adminCommandHandler.size() + " handlers.");

        final UserCommandHandler _userCommandHandler = UserCommandHandler.getInstance();
        _log.config("UserCommandHandler: Loaded " + _userCommandHandler.size() + " handlers.");

        final VoicedCommandHandler _voicedCommandHandler = VoicedCommandHandler.getInstance();
        _log.config("VoicedCommandHandler: Loaded " + _voicedCommandHandler.size() + " handlers.");

        TaskManager.getInstance();

        MercTicketManager.getInstance();

        BoatManager.getInstance();

        final Shutdown _shutdownHandler = Shutdown.getInstance();
        Runtime.getRuntime().addShutdownHook(_shutdownHandler);

        try {
            // Colosseum doors
            DoorTable.getInstance().getDoor(24190001).openMe();
            DoorTable.getInstance().getDoor(24190002).openMe();
            DoorTable.getInstance().getDoor(24190003).openMe();
            DoorTable.getInstance().getDoor(24190004).openMe();

            // TOI doors
            DoorTable.getInstance().getDoor(23180001).openMe();
            DoorTable.getInstance().getDoor(23180002).openMe();
            DoorTable.getInstance().getDoor(23180003).openMe();
            DoorTable.getInstance().getDoor(23180004).openMe();
            DoorTable.getInstance().getDoor(23180005).openMe();
            DoorTable.getInstance().getDoor(23180006).openMe();

            // Эти двери, похоже выполняют декоративную функцию,
            // находятся во Frozen Labyrinth над мостом по пути к снежной королеве.
            DoorTable.getInstance().getDoor(23140001).openMe();
            DoorTable.getInstance().getDoor(23140002).openMe();

            DoorTable.getInstance().checkAutoOpen();
        } catch (final NullPointerException e) {
            _log.warning("Doors table does not contain the right door info. Update doors.");
            e.printStackTrace();
        }

        _log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());

        AuctionManager.getInstance();

        TeleportTable.getInstance();

        PartyRoomManager.getInstance();

        PlayerShiftCache.restore();

        new File("./log/game").mkdirs();

        int restartTime = 0;
        int restartAt = 0;

        // Время запланированного на определенное время суток рестарта
        if (Config.RESTART_AT_TIME > -1) {
            final Calendar calendarRestartAt = Calendar.getInstance();
            calendarRestartAt.set(Calendar.HOUR_OF_DAY, Config.RESTART_AT_TIME);
            calendarRestartAt.set(Calendar.MINUTE, 0);

            // Если запланированное время уже прошло, то берем +24 часа
            if (calendarRestartAt.getTimeInMillis() < System.currentTimeMillis())
                calendarRestartAt.add(Calendar.HOUR, 24);

            restartAt = (int) (calendarRestartAt.getTimeInMillis() - System.currentTimeMillis()) / 1000;
        }

        // Время регулярного рестарта (через определенное время)
        restartTime = Config.RESTART_TIME * 60 * 60;

        // Проверяем какой рестарт раньше, регулярный или запланированный
        if (restartTime < restartAt && restartTime > 0 || restartTime > restartAt && restartAt == 0)
            Shutdown.getInstance().setAutoRestart(restartTime);
        else if (restartAt > 0)
            Shutdown.getInstance().setAutoRestart(restartAt);

        _log.info("GameServer Started");
        _log.config("Maximum Numbers of Connected Players: " + GsaTr.TrialOnline);

        L2World.loadTaxSum();

        if (Config.PROTECT_ENABLE && Config.PROTECT_GS_ENABLE_HWID_BANS)
            HWID.reloadBannedHWIDs();

        if (Config.PROTECT_ENABLE && Config.PROTECT_GS_ENABLE_HWID_BONUS)
            HWID.reloadBonusHWIDs();

        MMOSocket.getInstance();
        LSConnection.getInstance().start();

        final L2GamePacketHandler gph = new L2GamePacketHandler();
        final SelectorConfig<L2GameClient> sc = new SelectorConfig<L2GameClient>(null, gph);
        sc.setMaxSendPerPass(30);
        sc.setSelectorSleepTime(1);
        _selectorThread = new SelectorThread<L2GameClient>(sc, null, gph, gph, gph, null);
        _selectorThread.SetAntiFlood(Config.ANTIFLOOD_ENABLE);
        if (Config.ANTIFLOOD_ENABLE)
            _selectorThread.SetAntiFloodSocketsConf(Config.MAX_UNHANDLED_SOCKETS_PER_IP, Config.UNHANDLED_SOCKET_MIN_TTL);
        _selectorThread.openServerSocket(null, Config.PORT_GAME);
        _selectorThread.start();

        if (Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL > 0) {
            OnlinePlayers.getInstance();
            _log.config("Online Announce System: [state: activated]");
        } else
            _log.config("Online Announce System: [state: deactivated]");

        if (Config.SERVICES_OFFLINE_TRADE_RESTORE_AFTER_RESTART)
            // Это довольно тяжелая задача поэтому пусть идет отдельным тридом
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK > 0) {
                        final int min_offline_restore = (int) (System.currentTimeMillis() / 1000 - Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK);
                        mysql.set("DELETE FROM character_variables WHERE `name` = 'offline' AND `value` < " + min_offline_restore);
                    }
                    if (Config.GSWLS_MODE)
                        mysql.set("DELETE FROM character_variables WHERE `name` = 'offline' AND `obj_id` IN (SELECT `obj_id` FROM `characters` WHERE `accessLevel` < 0 OR `account_name` IN (SELECT `login` FROM `" + Config.GAME_SERVER_LOGIN_DB + "`.`accounts` WHERE `access_level` < 0))");
                    else
                        mysql.set("DELETE FROM character_variables WHERE `name` = 'offline' AND `obj_id` IN (SELECT `obj_id` FROM `characters` WHERE `accessLevel` < 0 )");
                    final Integer[][] list = mysql.simple_get_int_array(new String[]{"obj_id", "value"}, "character_variables", "name LIKE \"offline\"");
                    for (final Integer[] id : list) {
                        String account_name = null;
                        ThreadConnection con = null;
                        FiltredStatement st = null;
                        ResultSet rs = null;
                        try {
                            con = L2DatabaseFactory.getInstance().getConnection();
                            st = con.createStatement();
                            rs = st.executeQuery("SELECT `account_name` FROM `characters` WHERE `obj_Id`=" + id[0]);
                            if (rs.next())
                                account_name = rs.getString("account_name");
                        } catch (final Exception e) {
                            e.printStackTrace();
                            continue;
                        } finally {
                            DatabaseUtils.closeDatabaseCSR(con, st, rs);
                        }
                        final L2GameClient client = new L2GameClient(new MMOConnection<L2GameClient>(_selectorThread), true);
                        client.setConnection(null);
                        client.setCharSelection(id[0]);
                        final L2Player p = client.loadCharFromDisk(0);
                        if (p == null || p.isDead())
                            continue;
                        client.setLoginName(account_name == null ? "OfflineTrader_" + p.getName() : account_name);
                        p.restoreBonus();
                        p.spawnMe();
                        p.updateTerritories();
                        p.setOnlineStatus(true);
                        p.setOfflineMode(true);
                        p.setConnected(false);
                        p.setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
                        p.restoreEffects();
                        p.restoreDisableSkills();
                        p.broadcastUserInfo(true);
                        if (p.getClan() != null && p.getClan().getClanMember(p.getObjectId()) != null)
                            p.getClan().getClanMember(p.getObjectId()).setPlayerInstance(p);
                        if (Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK > 0)
                            p.startKickTask((Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK + id[1]) * 1000 - System.currentTimeMillis());
                    }
                    _log.info("Restored " + list.length + " offline traders");
                }
            }).start();
        if (Config.GCTaskDelay == 0)
            _log.info("GC task off!");
        else {
            _log.info("GC task run every " + Config.GCTaskDelay / 1000 + " seconds.");
            new GCTask();
        }
    }

    public final ItemTable getItemTable() {
        return _itemTable;
    }

    public static void main(final String[] args) throws Exception {
        Server.SERVER_MODE = Server.MODE_GAMESERVER;
        // Local Constants
        final String LOG_FOLDER = "log"; // Name of folder for log file
        final String LOG_NAME = "config/log.properties"; // Name of log file

        /** * Main ** */
        // Create log folder
        final File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        final InputStream is = GameServer.class.getClassLoader().getResourceAsStream(LOG_NAME);
        LogManager.getLogManager().readConfiguration(is);
        is.close();

        // Initialize config
        Config.loadAllConfigs();
        checkFreePorts();
        L2DatabaseFactory.getInstance();
        Log.InitGSLoggers();

        gameServer = new GameServer();

        if (Config.IS_TELNET_ENABLED) {
            statusServer = new Status(Server.MODE_GAMESERVER);
            statusServer.start();
        } else
            _log.info("Telnet server is currently disabled.");

        System.gc();
        // maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
        final long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        final long totalMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        _log.info("Free memory " + freeMem + " Mb of " + totalMem + " Mb");
        Log.LogServ(Log.GS_started, (int) freeMem, (int) totalMem, IdFactory.getInstance().size(), 0);

        // Shutdown.getInstance().setAutoRestart(Config.RESTART_TIME * 60 * 60);
    }

    public static void checkFreePorts() {
        boolean binded = false;
        while (!binded)
            try {
                ServerSocket ss;
                if (Config.GAMESERVER_HOSTNAME.equalsIgnoreCase("*"))
                    ss = new ServerSocket(Config.PORT_GAME);
                else
                    ss = new ServerSocket(Config.PORT_GAME, 50, InetAddress.getByName(Config.GAMESERVER_HOSTNAME));
                ss.close();
                binded = true;
            } catch (final Exception e) {
                _log.warning("\nPort " + Config.PORT_GAME + " is allready binded. Please free it and restart server.");
                binded = false;
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e2) {
                }
            }
    }
}