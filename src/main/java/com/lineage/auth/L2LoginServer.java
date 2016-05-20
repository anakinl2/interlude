package com.lineage.auth;

import com.lineage.Config;
import com.lineage.Server;
import com.lineage.auth.gameservercon.GSConnection;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.ext.network.SelectorConfig;
import com.lineage.ext.network.SelectorThread;
import com.lineage.game.GameServer;
import com.lineage.status.Status;
import com.lineage.util.Log;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class L2LoginServer {
    public static final int PROTOCOL_REV = 0x0102;

    private static L2LoginServer _instance;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(L2LoginServer.class.getName());
    private GSConnection _gameServerListener;
    private SelectorThread<L2LoginClient> _selectorThread;
    public static Status statusServer;
    public LoginController loginController;

    // private GameServer gameServer;

    public static void main(String[] args) {
        _instance = new L2LoginServer();
    }

    public static L2LoginServer getInstance() {
        return _instance;
    }

    public L2LoginServer() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();

        SLF4JBridgeHandler.install();
        java.util.logging.LogManager.getLogManager().getLogger("").setLevel( Level.INFO);
        Server.SERVER_MODE = Server.MODE_LOGINSERVER;
        // Local Constants
        final String LOG_FOLDER = "log"; // Name of folder for log file
        final String LOG_NAME = "config/log.properties"; // Name of log file

        /*** Main ***/
        // Create log folder
        File logFolder = new File("./", LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        InputStream is = null;
        try {
            is = L2LoginServer.class.getClassLoader().getResourceAsStream("config/log.properties");
            LogManager.getLogManager().readConfiguration(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load Config
        Config.loadAllConfigs();
        if (Config.COMBO_MODE) {
            Server.SERVER_MODE = Server.MODE_COMBOSERVER;
            Log.InitGSLoggers();
            Config.loadAllConfigs();
        }

        // Prepare Database
        try {
            L2DatabaseFactory.getInstance();
        } catch (SQLException e) {
            LOGGER.error("FATAL: Failed initializing database. Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        }

        try {
            LoginController.load();
        } catch (GeneralSecurityException e) {
            LOGGER.error("FATAL: Failed initializing LoginController. Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        }

        try {
            GameServerTable.load();
        } catch (GeneralSecurityException e) {
            LOGGER.error("FATAL: Failed to load GameServerTable. Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            LOGGER.error("FATAL: Failed to load GameServerTable. Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        }

        // this.loadBanFile();

        InetAddress ad = null;
        try {
            ad = InetAddress.getByName(Config.LOGIN_HOST);
        } catch (Exception e) {
        }

        L2LoginPacketHandler loginPacketHandler = new L2LoginPacketHandler();
        SelectorHelper sh = new SelectorHelper();
        SelectorConfig<L2LoginClient> sc = new SelectorConfig<L2LoginClient>(null, sh);
        try {
            _selectorThread = new SelectorThread<L2LoginClient>(sc, null, loginPacketHandler, sh, sh, sh);
            _selectorThread.setAcceptFilter(sh);
        } catch (IOException e) {
            LOGGER.error("FATAL: Failed to open Selector. Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        }

        _gameServerListener = GSConnection.getInstance();
        _gameServerListener.start();
        LOGGER.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);

        if (Config.IS_LOGIN_TELNET_ENABLED)
            try {
                statusServer = new Status(Server.MODE_LOGINSERVER);
                statusServer.start();
            } catch (IOException e) {
                LOGGER.error("Failed to start the Telnet Server. Reason: " + e.getMessage());
                if (Config.LOGIN_DEBUG)
                    e.printStackTrace();
            }
        else
            LOGGER.info("LoginServer Telnet server is currently disabled.");

        try {
            _selectorThread.SetAntiFlood(Config.ANTIFLOOD_ENABLE);
            if (Config.ANTIFLOOD_ENABLE)
                _selectorThread.SetAntiFloodSocketsConf(Config.MAX_UNHANDLED_SOCKETS_PER_IP, Config.UNHANDLED_SOCKET_MIN_TTL);
            _selectorThread.openServerSocket(ad, Config.PORT_LOGIN);
        } catch (IOException e) {
            LOGGER.error("FATAL: Failed to open server socket on " + ad + ":" + Config.PORT_LOGIN + ". Reason: " + e.getMessage());
            if (Config.LOGIN_DEBUG)
                e.printStackTrace();
            System.exit(1);
        }
        _selectorThread.start();
        LOGGER.info("Login Server ready on port " + Config.PORT_LOGIN);
        LOGGER.info(IpManager.getInstance().getBannedCount() + " banned IPs defined");

        if (Config.COMBO_MODE)
            try {
                GameServer.checkFreePorts();

                if (Config.IS_TELNET_ENABLED) {
                    Status _statusServer = new Status(Server.MODE_GAMESERVER);
                    _statusServer.start();
                } else
                    LOGGER.info("GameServer Telnet server is currently disabled.");
                new GameServer();
            } catch (Exception e) {
                e.printStackTrace();
            }

        Shutdown.getInstance().startShutdownH(Config.LRESTART_TIME, true);

        System.gc();
        // maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
        long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long totalMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        LOGGER.info("Free memory " + freeMem + " Mb of " + totalMem + " Mb");
        AuthWebStatus.getInstance().updateOnline(false);
    }

    public GSConnection getGameServerListener() {
        return _gameServerListener;
    }

    public void shutdown(boolean restart) {
        AuthWebStatus.getInstance().onShutDown();
        Runtime.getRuntime().exit(restart ? 2 : 0);
    }

    public boolean unblockIp(String ipAddress) {
        return loginController.ipBlocked(ipAddress);
    }

    public boolean setPassword(String account, String password) {
        return loginController.setPassword(account, password);
    }
}
