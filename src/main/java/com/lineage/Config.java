package com.lineage;

import javolution.util.FastList;
import l2d.game.geodata.PathFindBuffers;
import l2d.game.loginservercon.AdvIP;
import l2d.game.model.L2Player;
import l2d.game.model.base.Experience;
import l2d.game.model.base.PlayerAccess;
import com.lineage.util.NetList;
import com.lineage.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

import static l2d.game.model.L2Zone.ZoneType.peace_zone;

public class Config {
    protected static Logger _log = Logger.getLogger(Config.class.getName());
    /**
     * Debug/release mode
     */
    public static boolean DEBUG;
    public static boolean DEBUG_MULTISELL;
    public static String DEBUG_VALUE = null;
    public static boolean LOGIN_DEBUG;
    public static boolean COMBO_MODE;
    /**
     * GS Packets Logger
     */
    public static boolean LOG_CLIENT_PACKETS;
    public static boolean LOG_SERVER_PACKETS;
    public static int PACKETLOGGER_FLUSH_SIZE;
    public static NetList PACKETLOGGER_IPS;
    public static FastList<String> PACKETLOGGER_ACCOUNTS;
    public static FastList<String> PACKETLOGGER_CHARACTERS;
    /**
     * Game/Login Server ports
     */
    public static int PORT_GAME;
    public static int PORT_LOGIN;
    public static int LOGIN_TRY_BEFORE_BAN;
    public static int LOGIN_TRY_BEFORE_BAN_TIME;
    public static String GAMESERVER_HOSTNAME;
    public static boolean ADVIPSYSTEM;
    public static ArrayList<AdvIP> GAMEIPS = new ArrayList<AdvIP>();
    public static int IP_UPDATE_TIME;
    public static boolean LOGIN_PING;
    public static int LOGIN_PING_TIME;
    /**
     * AntiFlood for Game/Login
     */
    public static boolean ANTIFLOOD_ENABLE;
    public static int MAX_UNHANDLED_SOCKETS_PER_IP;
    public static int UNHANDLED_SOCKET_MIN_TTL;
    public static String DATABASE_DRIVER;
    public static String DATABASE_URL;
    public static String DATABASE_LOGIN;
    public static String DATABASE_PASSWORD;
    public static int DATABASE_MAX_CONNECTIONS;
    public static int DATABASE_MAX_IDLE_TIMEOUT;
    public static int DATABASE_IDLE_TEST_PERIOD;
    public static long GCTaskDelay;
    // Database additional options
    public static boolean LAZY_ITEM_UPDATE;
    public static boolean LAZY_ITEM_UPDATE_ALL;
    public static int LAZY_ITEM_UPDATE_TIME;
    public static int LAZY_ITEM_UPDATE_ALL_TIME;
    public static int DELAYED_ITEMS_UPDATE_INTERVAL;
    public static int USER_INFO_INTERVAL;
    public static boolean BROADCAST_STATS_INTERVAL;
    public static int BROADCAST_CHAR_INFO_INTERVAL;
    public static int SAVE_GAME_TIME_INTERVAL;
    public static int MAXIMUM_ONLINE_USERS_NOTTRIAL;
    public static boolean AUTO_CREATE_ACCOUNTS;
    public static boolean SERVER_LIST_BRACKET;
    public static boolean SERVER_LIST_CLOCK;
    public static boolean SERVER_LIST_TESTSERVER;
    public static boolean SERVER_GMONLY;
    public static boolean CHECK_LANG_FILES_MODIFY;
    public static boolean USE_FILE_CACHE;
    public static int LINEAR_TERRITORY_CELL_SIZE;
    public static boolean DONTLOADSPAWN;
    public static boolean DONTLOADQUEST;
    public static boolean FORCE_MULTISELL_SELL_PRICE;
    /**
     * ChatBan
     */
    public static boolean MAT_BANCHAT;
    public static int MAT_KARMA;
    public static String BAN_CHANNEL;
    public static int[] BAN_CHANNEL_LIST = new int[18];
    public static int MAT_BAN_COUNT_CHANNELS;
    public static boolean MAT_REPLACE;
    public static String MAT_REPLACE_STRING;
    public static int UNCHATBANTIME;
    public static int SAYTOSHOUTLIMIT;
    public static int SAYTOSHOUTLIMIT2;
    public static int SAYTOTRADELIMIT;
    public static int SAYTOTRADELIMIT2;
    public static ArrayList<String> MAT_LIST = new ArrayList<String>();
    public static boolean MAT_ANNOUNCE;
    public static boolean MAT_ANNOUNCE_NICK;
    public static boolean SAVING_SPS;
    public static boolean MANAHEAL_SPS_BONUS;
    public static int ALT_ADD_RECIPES;
    public static boolean ALT_100_RECIPES_B;
    public static boolean ALT_100_RECIPES_A;
    public static boolean ALT_100_RECIPES_S;
    public static int ALT_MAX_ALLY_SIZE;
    public static int ALT_PARTY_DISTRIBUTION_RANGE;
    public static boolean ALT_ALL_PHYS_SKILLS_OVERHIT;
    public static boolean ALT_REMOVE_SKILLS_ON_DELEVEL;
    public static int ALT_REMOVE_SKILLS_ON_DELEVEL_N_LEVEL;
    public static boolean ALT_USE_ILLEGAL_SKILLS;
    public static boolean ALT_USE_BOW_REUSE_MODIFIER;
    /**
     * Thread pools size
     */
    public static int THREAD_P_GENERAL;
    public static int THREAD_P_MOVE;
    public static int THREAD_P_EFFECTS;
    public static int NPC_AI_MAX_THREAD;
    public static int PLAYER_AI_MAX_THREAD;
    public static int GENERAL_PACKET_THREAD_CORE_SIZE;
    public static int URGENT_PACKET_THREAD_CORE_SIZE;
    public static int DEADLOCKCHECK_INTERVAL;
    public static boolean AUTOSAVE;
    public static boolean AUTO_LOOT;
    public static boolean AUTO_LOOT_HERBS;
    public static boolean AUTO_LOOT_INDIVIDUAL;
    public static boolean AUTO_LOOT_FROM_RAIDS;
    /**
     * Auto-loot for/from players with karma also?
     */
    public static boolean AUTO_LOOT_PK;
    public static int CH_DOORS_AUTO_OPEN_DELAY;
    public static boolean CH_DOORS_AUTO_OPEN;
    /**
     * Account name template
     */
    public static String ANAME_TEMPLATE;
    /**
     * Account password template
     */
    public static String APASSWD_TEMPLATE;
    /**
     * Character name template
     */
    public static String CNAME_TEMPLATE;
    /**
     * Clan name template
     */
    public static String CLAN_NAME_TEMPLATE;
    /**
     * Clan title template
     */
    public static String CLAN_TITLE_TEMPLATE;
    /**
     * Ally name template
     */
    public static String ALLY_NAME_TEMPLATE;
    /**
     * Global chat state
     */
    public static int GLOBAL_CHAT;
    public static int GLOBAL_TRADE_CHAT;
    public static int SHOUT_CHAT_MODE;
    public static int TRADE_CHAT_MODE;

    public static int ALL_LEVEL;
    public static int TRADE_LEVEL;
    public static int SHOUT_LEVEL;
    public static int PRIVATE_LEVEL;

    /**
     * For test servers - evrybody has admin rights
     */
    public static boolean EVERYBODY_HAS_ADMIN_RIGHTS;
    public static boolean ALLOW_SPECIAL_COMMANDS;
    /**
     * Online Players Announce
     */
    public static int ONLINE_PLAYERS_ANNOUNCE_INTERVAL;
    /**
     * For small servers
     */
    public static boolean ALT_GAME_MATHERIALSDROP;
    /**
     * Все мобы не являющиеся рейдами спавнятся в двойном количестве
     */
    public static boolean ALT_DOUBLE_SPAWN;
    public static boolean ALT_ALLOW_AUGMENT_ALL;
    public static boolean ALT_ALLOW_DROP_AUGMENTED;
    /**
     * Give exp and sp for craft
     */
    public static boolean ALT_GAME_EXP_FOR_CRAFT;
    /**
     * Delay for announce SS period (in minutes)
     */
    public static int SS_ANNOUNCE_PERIOD;
    /**
     * Show mob stats/droplist to players?
     */
    public static boolean ALT_GAME_SHOW_DROPLIST;
    public static boolean ALT_GAME_GEN_DROPLIST_ON_DEMAND;
    public static boolean ALT_FULL_NPC_STATS_PAGE;
    public static boolean ALLOW_NPC_SHIFTCLICK;

    /* Show html window at login */
    public static boolean SHOW_HTML_WELCOME;
    public static double SKILLS_CHANCE_MOD;
    public static double SKILLS_CHANCE_MIN;
    public static double SKILLS_CHANCE_CAP;
    public static boolean SKILLS_SHOW_CHANCE;
    public static boolean ALT_SAVE_UNSAVEABLE;
    public static boolean ALT_SHOW_REUSE_MSG;
    /**
     * Таймаут на использование social action
     */
    public static boolean ALT_SOCIAL_ACTION_REUSE;
    /**
     * Отключение книг для изучения скилов
     */
    public static boolean ALT_DISABLE_SPELLBOOKS;
    /**
     * Разрешать ли на арене бои за опыт
     */
    public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
    public static int ALT_GAME_LEVEL_TO_GET_SUBCLASS;
    public static int ALT_MAX_LEVEL;
    public static int ALT_MAX_SUB_LEVEL;
    public static int ALT_GAME_SUB_ADD;
    public static boolean ALT_NO_LASTHIT;
    public static boolean ALT_DONT_ALLOW_PETS_ON_SIEGE;
    public static int ALT_MAMMON_EXCHANGE;
    public static int ALT_MAMMON_UPGRADE;
    public static int ALT_BUFF_LIMIT;
    public static int ALT_DANCE_SONG_LIMIT;
    public static int MULTISELL_SIZE;
    public static boolean SERVICES_CHANGE_NICK_ENABLED;
    public static int SERVICES_CHANGE_NICK_PRICE;
    public static int SERVICES_CHANGE_NICK_ITEM;
    public static boolean SERVICES_CHANGE_PET_NAME_ENABLED;
    public static int SERVICES_CHANGE_PET_NAME_PRICE;
    public static int SERVICES_CHANGE_PET_NAME_ITEM;
    public static boolean SERVICES_EXCHANGE_BABY_PET_ENABLED;
    public static int SERVICES_EXCHANGE_BABY_PET_PRICE;
    public static int SERVICES_EXCHANGE_BABY_PET_ITEM;
    public static boolean SERVICES_CHANGE_SEX_ENABLED;
    public static int SERVICES_CHANGE_SEX_PRICE;
    public static int SERVICES_CHANGE_SEX_ITEM;
    public static boolean SERVICES_CHANGE_NICK_COLOR_ENABLED;
    public static int SERVICES_CHANGE_NICK_COLOR_PRICE;
    public static int SERVICES_CHANGE_NICK_COLOR_ITEM;
    public static String[] SERVICES_CHANGE_NICK_COLOR_LIST;
    public static boolean SERVICES_RATE_BONUS_ENABLED;
    public static int[] SERVICES_RATE_BONUS_PRICE;
    public static int[] SERVICES_RATE_BONUS_ITEM;
    public static float[] SERVICES_RATE_BONUS_VALUE;
    public static int[] SERVICES_RATE_BONUS_DAYS;
    public static float SERVICES_RATE_BONUS_LUCK_EFFECT;
    public static boolean SERVICES_NOBLESS_SELL_ENABLED;
    public static int SERVICES_NOBLESS_SELL_PRICE;
    public static int SERVICES_NOBLESS_SELL_ITEM;
    public static boolean SERVICES_EXPAND_INVENTORY_ENABLED;
    public static int SERVICES_EXPAND_INVENTORY_PRICE;
    public static int SERVICES_EXPAND_INVENTORY_ITEM;
    public static Integer SERVICES_EXPAND_INVENTORY_MAX;
    public static boolean SERVICES_EXPAND_WAREHOUSE_ENABLED;
    public static int SERVICES_EXPAND_WAREHOUSE_PRICE;
    public static int SERVICES_EXPAND_WAREHOUSE_ITEM;
    public static boolean SERVICES_EXPAND_CWH_ENABLED;
    public static int SERVICES_EXPAND_CWH_PRICE;
    public static int SERVICES_EXPAND_CWH_ITEM;
    public static boolean SERVICES_CHANGE_CLAN_ENABLED;
    public static int SERVICES_CHANGE_CLAN_PRICE;
    public static int SERVICES_CHANGE_CLAN_ITEM;
    public static boolean SERVICES_WINDOW_ENABLED;
    public static int SERVICES_WINDOW_PRICE;
    public static int SERVICES_WINDOW_ITEM;
    public static int SERVICES_WINDOW_DAYS;
    public static int SERVICES_WINDOW_MAX;
    public static boolean SERVICES_DONATE;
    public static boolean SERVICES_HOW_TO_GET_COL;
    public static String SERVICES_SELLPETS;
    public static boolean SERVICES_OFFLINE_TRADE_ALLOW;
    public static int SERVICES_OFFLINE_TRADE_MIN_LEVEL;
    public static int SERVICES_OFFLINE_TRADE_NAME_COLOR;
    public static boolean SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING;
    public static int SERVICES_OFFLINE_TRADE_PRICE;
    public static int SERVICES_OFFLINE_TRADE_PRICE_ITEM;
    public static long SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK;
    public static boolean SERVICES_OFFLINE_TRADE_RESTORE_AFTER_RESTART;
    public static boolean SERVICES_GIRAN_HARBOR_ENABLED;
    public static boolean SERVICES_LOCK_ACCOUNT_IP;
    public static boolean SERVICES_CHANGE_PASSWORD;
    public static boolean SERVICES_HERO_WEAPONS_EXCHANGE_ENABLED;
    public static boolean ALLOW_LOTTERY;
    public static int LOTTERY_PRIZE;
    public static int LOTTERY_PRICE;
    public static int LOTTERY_TICKET_PRICE;
    public static float LOTTERY_5_NUMBER_RATE;
    public static float LOTTERY_4_NUMBER_RATE;
    public static float LOTTERY_3_NUMBER_RATE;
    public static int LOTTERY_2_AND_1_NUMBER_PRIZE;
    public static boolean ALT_GAME_REQUIRE_CLAN_CASTLE;
    public static boolean ALT_GAME_REQUIRE_CASTLE_DAWN;
    public static boolean ALT_GAME_ALLOW_ADENA_DAWN;
    /**
     * Olympiad Compitition Starting time
     */
    public static int ALT_OLY_START_TIME;
    /**
     * Olympiad Compition Min
     */
    public static int ALT_OLY_MIN;
    /**
     * Olympaid Comptetition Period
     */
    public static long ALT_OLY_CPERIOD;
    /**
     * Olympiad Battle Period
     */
    public static long ALT_OLY_BATTLE;
    /**
     * Olympiad Battle Wait
     */
    public static long ALT_OLY_BWAIT;
    /**
     * Olympiad Inital Wait
     */
    public static long ALT_OLY_IWAIT;
    /**
     * Olympaid Weekly Period
     */
    public static long ALT_OLY_WPERIOD;
    /**
     * Olympaid Validation Period
     */
    public static long ALT_OLY_VPERIOD;
    public static boolean ENABLE_OLYMPIAD;
    public static boolean ENABLE_OLYMPIAD_SPECTATING;
    public static int CLASS_GAME_MIN;
    public static int NONCLASS_GAME_MIN;
    public static boolean ADD_ALREADY_STARTED_GAMES;
    public static int ALT_TRUE_CHESTS;
    public static long NONOWNER_ITEM_PICKUP_DELAY;
    /**
     * Logging Chat Window
     */
    public static boolean LOG_CHAT;
    public static boolean LOG_KILLS;
    public static boolean LOG_TELNET;
    public static boolean SQL_LOG;
    public static HashMap<Integer, PlayerAccess> gmlist = new HashMap<Integer, PlayerAccess>();
    /**
     * Rate control
     */
    public static float RATE_XP;
    public static float RATE_XP_PARTY;
    public static float RATE_SP;
    public static float RATE_SP_PARTY;
    public static float RATE_QUESTS_REWARD;
    public static float RATE_QUESTS_DROP;
    public static float RATE_QUESTS_EXP;
    public static float RATE_QUESTS_DROP_PROF;
    public static boolean RATE_QUESTS_OCCUPATION_CHANGE;
    public static float RATE_CLAN_REP_SCORE;
    public static int RATE_CLAN_REP_SCORE_MAX_AFFECTED;
    public static float RATE_DROP_ADENA;
    public static float RATE_DROP_ADENA_PARTY;
    public static float RATE_DROP_ITEMS;
    public static float RATE_DROP_ITEMS_PARTY;
    public static float RATE_DROP_RAIDBOSS;
    public static float RATE_DROP_SPOIL;
    public static int RATE_MANOR;
    public static float RATE_FISH_DROP_COUNT;
    public static float RATE_SIEGE_GUARDS_PRICE;
    /**
     * Player Drop Rate control
     */
    public static boolean KARMA_DROP_GM;
    public static boolean KARMA_NEEDED_TO_DROP;
    public static int KARMA_DROP_ITEM_LIMIT;
    public static int KARMA_RANDOM_DROP_LOCATION_LIMIT;
    public static int KARMA_DROPCHANCE_MINIMUM;
    public static int KARMA_DROPCHANCE_MULTIPLIER;
    public static int KARMA_DROPCHANCE_EQUIPMENT;
    public static int KARMA_DROPCHANCE_EQUIPPED_WEAPON;
    public static int AUTODESTROY_ITEM_AFTER;
    public static int AUTODESTROY_PLAYER_ITEM_AFTER;
    public static int DELETE_DAYS;
    public static int PURGE_BYPASS_TASK_FREQUENCY;
    public static File DATAPACK_ROOT;
    public static String AttributeBonusFile;
    public static boolean WEAR_ENABLED;
    public static boolean USE_DATABASE_LAYER;
    public static float BUFFTIME_MODIFIER;
    public static float SUMMON_BUFF_MODIFIER;
    public static int SUMMON_BUFF_TIME;
    public static float SUMMON_SET_BUFF_TYPE;
    public static float BUFFTIME_MODIFIER_CLANHALL;
    public static float SONGDANCETIME_MODIFIER;
    public static float BUFF15MINUTES_MODIFIER;
    public static float MAXLOAD_MODIFIER;
    public static float GATEKEEPER_MODIFIER;
    public static boolean ALT_BUFF_SUMMON;
    public static int GATEKEEPER_FREE;
    public static int CRUMA_GATEKEEPER_LVL;
    public static int ALT_CATACOMB_MODIFIER_HP;
    public static double CHAMPION_CHANCE1;
    public static double CHAMPION_CHANCE2;
    public static boolean CHAMPION_CAN_BE_AGGRO;
    public static boolean CHAMPION_CAN_BE_SOCIAL;

    /**
     * Allow L2Walker client
     */
    public static enum L2WalkerAllowed {
        True,
        False,
        Peace
    }

    public static L2WalkerAllowed ALLOW_L2WALKER_CLIENT;
    public static int MAX_L2WALKER_LEVEL;
    public static boolean ALLOW_CRAFT_BOT;
    public static boolean ALLOW_BOT_CAST;
    public static int L2WALKER_PUNISHMENT;
    public static int BUGUSER_PUNISH;
    public static boolean ALLOW_DISCARDITEM;
    public static boolean ALLOW_FREIGHT;
    public static boolean ALLOW_WAREHOUSE;
    public static boolean ALLOW_WATER;
    public static boolean ALLOW_BOAT;
    public static boolean ALLOW_CURSED_WEAPONS;
    public static boolean DROP_CURSED_WEAPONS_ON_KICK;
    public static boolean ALLOW_NOBLE_TP_TO_ALL;
    /**
     * Pets
     */
    public static int SWIMING_SPEED;
    public static int WYVERN_SPEED;
    public static int STRIDER_SPEED;
    public static boolean ANY_PARTY_MEMBER_MAY_SURRENDER;
    /**
     * protocol revision
     */
    public static int MIN_PROTOCOL_REVISION;
    public static int MAX_PROTOCOL_REVISION;
    /**
     * random animation interval
     */
    public static int MIN_NPC_ANIMATION;
    public static int MAX_NPC_ANIMATION;
    /**
     * Время регулярного рестарта (через определенное время)
     */
    public static int RESTART_TIME;
    /**
     * Время запланированного на определенное время суток рестарта
     */
    public static int RESTART_AT_TIME;
    public static int LRESTART_TIME;
    /**
     * Configuration files
     */
    public static final String OTHER_CONFIG_FILE = "config/other.properties";
    public static final String CLANHALL_CONFIG_FILE = "config/clanhall.properties";
    public static final String SPOIL_CONFIG_FILE = "config/spoil.properties";
    public static final String ALT_SETTINGS_FILE = "config/altsettings.properties";
    public static final String PVP_CONFIG_FILE = "config/pvp_settings.properties";
    public static final String GM_PERSONAL_ACCESS_FILE = "./config/GMAccess.xml";
    public static final String TELNET_FILE = "config/telnet.properties";
    public static final String LOGIN_TELNET_FILE = "config/auth_telnet.properties";
    public static final String CONFIGURATION_FILE = "config/gameserver.properties";
    public static final String LOGIN_CONFIGURATION_FILE = "config/authserver.properties";
    public static final String VERSION_FILE = "l2d-version.properties";
    public static final String SIEGE_CASTLE_CONFIGURATION_FILE = "config/siege_castle.properties";
    public static final String SIEGE_CLANHALL_CONFIGURATION_FILE = "config/siege_clanhall.properties";
    public static final String BANNED_IP_XML = "./config/banned.xml";
    public static final String HEXID_FILE = "./config/hexid.txt";
    public static final String MAT_CONFIG_FILE = "./config/lists/swears.list";
    public static final String CHAT_FILE = "config/chat.properties";
    public static final String ADV_IP_FILE = "config/advipsystem.properties";
    public static final String AI_CONFIG_FILE = "config/ai.properties";
    public static final String GEODATA_CONFIG_FILE = "config/geodata.properties";
    public static final String FAKE_PLAYERS_LIST = "./config/lists/fake_players_names.list";
    public static final String SERVICES_FILE = "config/services.properties";
    public static final String PROTECT_FILE = "./config/protection.ini";
    public static final String OLYMPIAD = "config/olympiad.properties";
    public static final String RATE_FILE = "config/rates.properties";
    public static final String VITALITY_FILE = "./config/vitality.ini";
    public static final String WALKER_FILE = "./config/walker.ini";
    public static final String EVENTS = "config/events.properties";
    public static final String DEVELOPER_FILE = "config/developer.properties";
    public static final String WEDDING_FILE = "config/wedding.properties";
    public static final String LOTTERY_FILE = "config/lottery.properties";
    public static final String CHAMPION_FILE = "config/champion_mobs.properties";
    public static final String CRAFTMANAGER_FILE = "./config/craftmanager.ini";
    public static final String EVENTBUFFER_FILE = "./config/eventbuffer.ini";
    /**
     * DRiN's Protection config
     */
    public static boolean PROTECT_ENABLE;
    public static NetList PROTECT_UNPROTECTED_IPS;
    public static boolean PROTECT_GS_STORE_HWID;
    public static boolean PROTECT_GS_LOG_HWID;
    public static String PROTECT_GS_LOG_HWID_QUERY;
    public static boolean PROTECT_GS_ENABLE_HWID_BANS;
    public static boolean PROTECT_GS_ENABLE_HWID_BONUS;
    public static int PROTECT_GS_MAX_SAME_HWIDs;
    public static String LOGIN_HOST;
    public static int GAME_SERVER_LOGIN_PORT;
    public static boolean GAME_SERVER_LOGIN_CRYPT;
    public static boolean GSWLS_MODE;
    public static String GAME_SERVER_LOGIN_HOST;
    public static String GAME_SERVER_LOGIN_DB;
    public static String INTERNAL_HOSTNAME;
    public static String EXTERNAL_HOSTNAME;
    public static String DEFAULT_PASSWORD_ENCODING;
    public static String LEGACY_PASSWORD_ENCODING;
    public static int LOGIN_BLOWFISH_KEYS;
    public static int LOGIN_RSA_KEYPAIRS;
    public static boolean SERVER_SIDE_NPC_NAME;
    public static boolean SERVER_SIDE_NPC_TITLE;
    public static boolean SERVER_SIDE_NPC_TITLE_WITH_LVL;
    public static String ALLOW_CLASS_MASTERS;
    public static boolean ALLOW_CLASS_MASTERS_ON_LEVEL_UP;
    public static String CLASS_MASTERS_SAY;
    public static String CLASS_MASTERS_PRICE;
    public static int CLASS_MASTERS_PRICE_ITEM;
    public static ArrayList<Integer> ALLOW_CLASS_MASTERS_LIST = new ArrayList<Integer>();
    public static int[] CLASS_MASTERS_PRICE_LIST = new int[4];
    public static String SERVER_VERSION;
    public static String SERVER_BUILD_DATE;
    public final static String SERVER_VERSION_UNSUPPORTED = "Unknown Version";
    public static final boolean PATHFIND_DIAGONAL = false;
    /**
     * Inventory slots limits
     */
    public static int INVENTORY_MAXIMUM_NO_DWARF;
    public static int INVENTORY_MAXIMUM_DWARF;
    public static int INVENTORY_MAXIMUM_GM;
    /**
     * Warehouse slots limits
     */
    public static int WAREHOUSE_SLOTS_NO_DWARF;
    public static int WAREHOUSE_SLOTS_DWARF;
    public static int WAREHOUSE_SLOTS_CLAN;
    /**
     * Spoil Rates
     */
    public static float BASE_SPOIL_RATE;
    public static float MINIMUM_SPOIL_RATE;
    public static boolean ALT_SPOIL_FORMULA;
    /**
     * Manor Config
     */
    public static double MANOR_SOWING_BASIC_SUCCESS;
    public static double MANOR_SOWING_ALT_BASIC_SUCCESS;
    public static double MANOR_HARVESTING_BASIC_SUCCESS;
    public static double MANOR_DIFF_PLAYER_TARGET;
    public static double MANOR_DIFF_PLAYER_TARGET_PENALTY;
    public static double MANOR_DIFF_SEED_TARGET;
    public static double MANOR_DIFF_SEED_TARGET_PENALTY;
    /**
     * Karma System Variables
     */
    public static int KARMA_MIN_KARMA;
    public static int KARMA_SP_DIVIDER;
    public static int KARMA_LOST_BASE;
    public static int MIN_PK_TO_ITEMS_DROP;
    public static String KARMA_NONDROPPABLE_ITEMS;
    public static ArrayList<Integer> KARMA_LIST_NONDROPPABLE_ITEMS = new ArrayList<Integer>();
    public static int PVP_TIME;
    public static boolean AUTODELETE_INVALID_QUEST_DATA;
    public static boolean HARD_DB_CLEANUP_ON_START;
    /**
     * Chance that an item will succesfully be enchanted
     */
    public static int ENCHANT_CHANCE_WEAPON;
    public static int ENCHANT_CHANCE_ARMOR;
    public static int ENCHANT_CHANCE_ACCESSORY;
    public static int ENCHANT_CHANCE_CRYSTAL_WEAPON;
    public static int ENCHANT_CHANCE_CRYSTAL_ARMOR;
    public static int ENCHANT_CHANCE_CRYSTAL_ACCESSORY;
    public static int ENCHANT_BLESSED_FAIL;
    public static boolean EnchantCrystalSafe;
    public static int SAFE_ENCHANT_COMMON;
    public static byte ENCHANT_MAX;
    /**
     * Soul Cry Chance
     */
    public static double SOUL_RATE_CHANCE;
    /**
     * Config Alt Enchant Formula
     */
    public static boolean ALT_ENCHANT_FORMULA;
    public static int ALT_ENCHANT_CHANCE_W;
    public static int ALT_ENCHANT_CHANCE_MAGE_W;
    public static int ALT_ENCHANT_CHANCE_ARMOR;
    public static int PENALTY_TO_TWOHANDED_BLUNTS;
    public static boolean REGEN_SIT_WAIT;
    public static float RATE_RAID_REGEN;
    public static int RAID_MAX_LEVEL_DIFF;
    public static boolean PARALIZE_ON_RAID_DIFF;
    public static int STARTING_ADENA;
    public static boolean ENABLE_STARTING_ITEM;
    public static String STARTING_ITEM_ID;
    public static int[] STARTING_ITEM_ID_LIST = new int[15];
    public static String STARTING_ITEM_COUNT;
    public static int[] STARTING_ITEM_COUNT_LIST = new int[15];
    /**
     * Deep Blue Mobs' Drop Rules Enabled
     */
    public static boolean DEEPBLUE_DROP_RULES;
    public static int DEEPBLUE_DROP_MAXDIFF;
    public static int DEEPBLUE_DROP_RAID_MAXDIFF;
    public static boolean UNSTUCK_SKILL;
    /**
     * telnet enabled
     */
    public static boolean IS_TELNET_ENABLED;
    public static boolean IS_LOGIN_TELNET_ENABLED;
    /**
     * Percent CP is restore on respawn
     */
    public static double RESPAWN_RESTORE_CP;
    /**
     * Percent HP is restore on respawn
     */
    public static double RESPAWN_RESTORE_HP;
    /**
     * Percent MP is restore on respawn
     */
    public static double RESPAWN_RESTORE_MP;
    /**
     * Maximum number of available slots for pvt stores (sell/buy) - Dwarves
     */
    public static int MAX_PVTSTORE_SLOTS_DWARF;
    /**
     * Maximum number of available slots for pvt stores (sell/buy) - Others
     */
    public static int MAX_PVTSTORE_SLOTS_OTHER;
    public static int MAX_PVTCRAFT_SLOTS;
    public static boolean ALLOW_CH_DOOR_OPEN_ON_CLICK;
    public static boolean ALT_CH_ALL_BUFFS;
    public static boolean ALT_CH_ALLOW_1H_BUFFS;
    public static boolean ALT_CH_SIMPLE_DIALOG;
    public static int CH_BID_GRADE1_MINCLANLEVEL;
    public static int CH_BID_GRADE1_MINCLANMEMBERS;
    public static int CH_BID_GRADE1_MINCLANMEMBERSLEVEL;
    public static int CH_BID_GRADE2_MINCLANLEVEL;
    public static int CH_BID_GRADE2_MINCLANMEMBERS;
    public static int CH_BID_GRADE2_MINCLANMEMBERSLEVEL;
    public static int CH_BID_GRADE3_MINCLANLEVEL;
    public static int CH_BID_GRADE3_MINCLANMEMBERS;
    public static int CH_BID_GRADE3_MINCLANMEMBERSLEVEL;
    public static float RESIDENCE_LEASE_MULTIPLIER;
    /**
     * Show licence or not just after login (if false, will directly go to the Server List
     */
    public static boolean SHOW_LICENCE;
    /**
     * Deafult punishment for illegal actions
     */
    public static int DEFAULT_PUNISH;
    public static byte[] HEX_ID;
    public static boolean ACCEPT_ALTERNATE_ID;
    public static int REQUEST_ID;
    public static boolean ANNOUNCE_MAMMON_SPAWN;
    public static int GM_NAME_COLOUR;
    public static boolean GM_HERO_AURA;
    public static int NORMAL_NAME_COLOUR;
    public static int CLANLEADER_NAME_COLOUR;
    public static int BOT_NAME_COLOUR;
    public static String BOT_NAME_HEX_COLOUR;
    /**
     * AI
     */
    public static int AI_TASK_DELAY;
    public static boolean RND_WALK;
    public static int RND_WALK_RATE;
    public static int RND_ANIMATION_RATE;
    public static int AGGRO_CHECK_INTERVAL;
    /**
     * Maximum range mobs can randomly go from spawn point
     */
    public static int MAX_DRIFT_RANGE;
    /**
     * Maximum range mobs can pursue agressor from spawn point
     */
    public static int MAX_PURSUE_RANGE;
    public static int MAX_PURSUE_RANGE_RAID;

    public static int СHANCE_SKILLS;
    public static boolean ALLOW_DEATH_PENALTY;
    public static int ALT_DEATH_PENALTY_CHANCE;
    public static int ALT_DEATH_PENALTY_EXPERIENCE_PENALTY;
    public static int ALT_DEATH_PENALTY_KARMA_PENALTY;
    public static boolean HIDE_GM_STATUS;
    public static boolean SHOW_GM_LOGIN;
    public static boolean SAVE_GM_EFFECTS; // Silence, gmspeed, etc...
    /**
     * security options
     */
    public static String DISABLE_CREATION_IDs;
    public static ArrayList<Integer> DISABLE_CREATION_ID_LIST = new ArrayList<Integer>();
    public static String LOG_MULTISELL_IDs;
    public static ArrayList<Integer> LOG_MULTISELL_ID_LIST = new ArrayList<Integer>();
    public static boolean AUTO_LEARN_SKILLS;
    public static int AUTO_LEARN_SKILLS_MAX_LEVEL;
    /**
     * Debug, only for GM
     */
    public static int MOVE_PACKET_DELAY;
    public static int ATTACK_PACKET_DELAY;
    public static int EQUIPM_PACKET_DELAY;
    public static boolean DAMAGE_FROM_FALLING;
    /**
     * Community Board
     */
    public static boolean ALLOW_COMMUNITYBOARD;
    public static String BBS_DEFAULT;
    public static String COMMUNITYBOARD_HTML_ROOT;
    public static boolean COMMUNITYBOARD_SORTPLAYERSLIST;
    public static int NAME_PAGE_SIZE_COMMUNITYBOARD;
    public static int NAME_PER_ROW_COMMUNITYBOARD;
    public static long COMMUNITYBOARD_PLAYERSLIST_CACHE;
    public static String ALLOW_COMMUNITYBOARD_PLAYERSLIST;
    /**
     * Wedding Options
     */
    public static boolean WEDDING_ALLOW_WEDDING;
    public static int WEDDING_PRICE;
    public static boolean WEDDING_PUNISH_INFIDELITY;
    public static boolean WEDDING_TELEPORT;
    public static int WEDDING_TELEPORT_PRICE;
    public static int WEDDING_TELEPORT_INTERVAL;
    public static boolean WEDDING_SAMESEX;
    public static boolean WEDDING_FORMALWEAR;
    public static int WEDDING_DIVORCE_COSTS;
    public static int WEDDING_GIVE_ITEM;
    public static int WEDDING_GIVE_COUNT;
    public static boolean FORCE_STATUSUPDATE;
    /**
     * Castle siege options *
     */
    public static boolean SIEGE_OPERATE_DOORS;
    public static boolean SIEGE_OPERATE_DOORS_LORD_ONLY;
    /**
     * Augmentations *
     */
    public static int AUGMENTATION_NG_SKILL_CHANCE; // Chance to get a skill while using a NoGrade Life Stone
    public static int AUGMENTATION_NG_GLOW_CHANCE; // Chance to get a Glow effect while using a NoGrade Life Stone(only if you get a skill)
    public static int AUGMENTATION_MID_SKILL_CHANCE; // Chance to get a skill while using a MidGrade Life Stone
    public static int AUGMENTATION_MID_GLOW_CHANCE; // Chance to get a Glow effect while using a MidGrade Life Stone(only if you get a skill)
    public static int AUGMENTATION_HIGH_SKILL_CHANCE; // Chance to get a skill while using a HighGrade Life Stone
    public static int AUGMENTATION_HIGH_GLOW_CHANCE; // Chance to get a Glow effect while using a HighGrade Life Stone
    public static int AUGMENTATION_TOP_SKILL_CHANCE; // Chance to get a skill while using a TopGrade Life Stone
    public static int AUGMENTATION_TOP_GLOW_CHANCE; // Chance to get a Glow effect while using a TopGrade Life Stone
    public static int AUGMENTATION_BASESTAT_CHANCE; // Chance to get a BaseStatModifier in the augmentation process
    /**
     * limits of stats *
     */
    public static boolean DEBUG_STAT_LIMITS;
    public static int MAX_HP;
    public static int MAX_MP;
    public static int MAX_CP;
    public static int MAX_RUNSPD;
    public static int MAX_PDEF;
    public static int MAX_MDEF;
    public static int MAX_PATK;
    public static int MAX_MATK;
    public static int MAX_PATKSPD;
    public static int MAX_MATKSPD;
    public static int MAX_ACC_COM;
    public static int MAX_EVAS_RATE;
    public static int MAX_CRIT_BASE;
    public static int MAX_MCRIT_RATE;
    /**
     * Enchant Config *
     */
    public static int SAFE_ENCHANT_FULL_BODY;
    public static int FESTIVAL_MIN_PARTY_SIZE;
    /**
     * Four Sepulchers Config*
     */
    public static int FS_TIME_ATTACK;
    public static int FS_TIME_COOLDOWN;
    public static int FS_TIME_ENTRY;
    public static int FS_TIME_WARMUP;
    public static int FS_PARTY_MEMBER_COUNT;
    /**
     * Dimensional Rift Config *
     */
    public static int RIFT_MIN_PARTY_SIZE;
    public static int RIFT_SPAWN_DELAY; // Time in ms the party has to wait until the mobs spawn
    public static int RIFT_MAX_JUMPS;
    public static int RIFT_AUTO_JUMPS_TIME;
    public static int RIFT_AUTO_JUMPS_TIME_RAND;
    public static int RIFT_ENTER_COST_RECRUIT;
    public static int RIFT_ENTER_COST_SOLDIER;
    public static int RIFT_ENTER_COST_OFFICER;
    public static int RIFT_ENTER_COST_CAPTAIN;
    public static int RIFT_ENTER_COST_COMMANDER;
    public static int RIFT_ENTER_COST_HERO;
    /**
     * Some more LS Settings
     */
    public static FastList<String> INTERNAL_IP = null;
    /**
     * Продвинутый список локальных сетей / ip-адресов
     */
    public static NetList INTERNAL_NETLIST = null;
    public static boolean ALLOW_TALK_WHILE_SITTING;
    public static boolean ALLOW_FAKE_PLAYERS;
    public static int FAKE_PLAYERS_PERCENT;
    public static boolean PARTY_LEADER_ONLY_CAN_INVITE;
    /**
     * Разрешены ли клановые скилы? *
     */
    public static boolean ALLOW_CLANSKILLS;
    public static boolean LOGIN_GG_CHECK;
    public static boolean GG_CHECK;
    /**
     * Allow Manor system
     */
    public static boolean ALLOW_MANOR;
    /**
     * Manor Refresh Starting time
     */
    public static int MANOR_REFRESH_TIME;
    /**
     * Manor Refresh Min
     */
    public static int MANOR_REFRESH_MIN;
    /**
     * Manor Next Period Approve Starting time
     */
    public static int MANOR_APPROVE_TIME;
    /**
     * Manor Next Period Approve Min
     */
    public static int MANOR_APPROVE_MIN;
    /**
     * Manor Maintenance Time
     */
    public static int MANOR_MAINTENANCE_PERIOD;
    /**
     * Manor Save All Actions
     */
    public static boolean MANOR_SAVE_ALL_ACTIONS;
    /**
     * Manor Save Period Rate
     */
    public static int MANOR_SAVE_PERIOD_RATE;
    public static int SHIFT_BY;
    public static int SHIFT_BY_FOR_Z;
    public static String SNAPSHOTS_DIRECTORY;
    public static boolean DUMP_MEMORY_ON_SHUTDOWN;
    public static int DM_Id;
    public static int DM_Count;
    public static int DM_Interval;
    public static int DM_MinPlayer;
    public static int DM_MaxPlayer;
    public static int DM_MinLevel;
    public static int DM_MaxLevel;
    public static int SERVICES_RATE_SPECIAL_ITEM_ID;
    public static int SERVICES_RATE_SPECIAL_ITEM_COUNT;
    public static int SERVICES_RATE_SPECIAL_RATE;
    public static int SERVICES_RATE_SPECIAL_DAYS;
    public static boolean SERVICES_RATE_SPECIAL_ENABLED;
    public static boolean SERVICES_CLASSMASTERS_BASIC_SHOP;
    public static boolean SERVICES_CLASSMASTERS_COL_SHOP;
    public static int EVENT_LastHeroItemID;
    public static int EVENT_LastHeroItemCOUNT;
    public static int EVENT_LastHeroTime;
    public static boolean EVENT_LastHeroRate;
    public static int EVENT_LastHeroChanceToStart;
    public static boolean EVENT_LastHeroQuestion;
    public static int EVENT_LastHeroMaxCountPlayers;
    public static int EVENT_TvTItemID;
    public static int EVENT_TvTItemCOUNT;
    public static int EVENT_TvTTime;
    public static boolean EVENT_TvTRate;
    public static int EVENT_TvTChanceToStart;
    public static boolean EVENT_TvTQuestion;
    public static int EVENT_TvTMaxCountPlayers;
    public static float EVENT_TFH_POLLEN_CHANCE;
    public static float EVENT_GLITTMEDAL_NORMAL_CHANCE;
    public static float EVENT_GLITTMEDAL_GLIT_CHANCE;
    public static float EVENT_L2DAY_LETTER_CHANCE;
    public static float EVENT_FIVE_YEARS_LINEAGE_LETTER_CHANCE;
    public static float EVENT_CHANGE_OF_HEART_CHANCE;
    public static boolean SERVICES_NO_TRADE_ONLY_OFFLINE;
    public static float SERVICES_TRADE_TAX;
    public static float SERVICES_OFFSHORE_TRADE_TAX;
    public static boolean SERVICES_OFFSHORE_NO_CASTLE_TAX;
    public static boolean SERVICES_TRADE_TAX_ONLY_OFFLINE;
    public static boolean SERVICES_TRADE_ONLY_FAR;
    public static int SERVICES_TRADE_RADIUS;

    public static int SERVICES_BUFFER_MIN_LVL;
    public static int SERVICES_BUFFER_MAX_LVL;
    public static int SERVICES_BUFFER_PRICE;
    public static boolean SERVICES_BUFFER_SIEGE;
    public static boolean SERVICES_BUFFER_ENABLED;
    public static boolean SERVICES_BUFFER_PET_ENABLED;

    /**
     * Geodata config
     */
    public static String GEOFILES_PATTERN;
    public static boolean GEODATA_ENABLE;
    public static boolean PATH_CLEAN;
    public static boolean ALLOW_DOORS;
    public static boolean SIMPLE_PATHFIND_FOR_MOBS;
    public static boolean ALLOW_FALL_FROM_WALLS;
    public static boolean ALLOW_KEYBOARD_MOVE;
    public static int PATHFIND_BOOST;
    public static int MAX_Z_DIFF;
    public static double WEIGHT0;
    public static int WEIGHT1;
    public static int WEIGHT2;
    public static boolean COMPACT_GEO;
    public static boolean SHOW_GEO_FILE;
    public static String VERTICAL_SPLIT_REGIONS;
    public static int DIV_BY;
    public static int DIV_BY_FOR_Z;

    /**
     * DonateShop *
     */
    public static String ENCHANT_WEAPON_NUMBER;
    public static int[] ENCHANT_WEAPON_NUMBER_LIST = new int[18];
    public static String ENCHANT_WEAPON_COAL;
    public static int[] ENCHANT_WEAPON_COAL_LIST = new int[18];
    public static int ENCHANT_ARMOR_PRICE;
    public static int ENCHANT_JEWEL_PRICE;
    public static int NOBLE_PRICE;
    public static int DONATE_ID_PRICE;
    /**
     * Enchater *
     */
    public static boolean ENCHANT_MASTER_ENABLED;
    public static int ENCHANT_MASTER_WEAPON_PRICE;
    public static int ENCHANT_MASTER_ARMOR_PRICE;
    public static int ENCHANT_MASTER_JEWEL_PRICE;
    public static int ENCHANT_MASTER_PRICE_ID;
    public static int ENCHANT_MASTER_MAX;

    /**
     * DOS/Flood protections
     */
    public static boolean ENABLE_DDOS_PROTECTION_SYSTEM;
    public static boolean ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM;
    public static String IPTABLES_COMMAND;

	
	/* Конфиги эвент баффера */

    private static void initProtection(final boolean gs) {


        if (Config.class.getClassLoader().getResource(PROTECT_FILE)!=null)
            try {
                final Properties protectSettings = new Properties();
                final InputStream is = Config.class.getClassLoader().getResourceAsStream(PROTECT_FILE);
                protectSettings.load(is);
                is.close();

                PROTECT_UNPROTECTED_IPS = new NetList();

                final String _ips = getProperty(protectSettings, "UpProtectedIPs", "");
                if (_ips.equals(""))
                    PROTECT_UNPROTECTED_IPS.LoadFromString(_ips, ",");

                if (gs) {
                    PROTECT_GS_STORE_HWID = getBooleanProperty(protectSettings, "StoreHWID", false);
                    PROTECT_GS_LOG_HWID = getBooleanProperty(protectSettings, "LogHWIDs", false);
                    PROTECT_GS_LOG_HWID_QUERY = "INSERT INTO " + getProperty(protectSettings, "LogHWIDsPath", "hwids_log") + " (`Account`, `IP`, `HWID`) VALUES (?,?,?);";
                    PROTECT_GS_ENABLE_HWID_BANS = getBooleanProperty(protectSettings, "EnableHWIDBans", false);
                    PROTECT_GS_ENABLE_HWID_BONUS = getBooleanProperty(protectSettings, "EnableHWIDBonus", false);
                    PROTECT_GS_MAX_SAME_HWIDs = getIntProperty(protectSettings, "MaxSameHWIDs", 0);
                }

            } catch (final Exception e) {
            }
    }

    private static void loadAntiFlood(final Properties _settings) {
        try {
            ANTIFLOOD_ENABLE = getBooleanProperty(_settings, "AntiFloodEnable", false);
            if (!ANTIFLOOD_ENABLE)
                return;
            MAX_UNHANDLED_SOCKETS_PER_IP = getIntProperty(_settings, "MaxUnhandledSocketsPerIP", 5);
            UNHANDLED_SOCKET_MIN_TTL = getIntProperty(_settings, "UnhandledSocketsMinTTL", 5000);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load AntiFlood Properties.");
        }
    }

    public static void reloadPacketLoggerConfig() {
        try {
            final Properties serverSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE);
            serverSettings.load(is);
            is.close();

            LOG_CLIENT_PACKETS = getBooleanProperty(serverSettings, "LogClientPackets", false);
            LOG_SERVER_PACKETS = getBooleanProperty(serverSettings, "LogServerPackets", false);
            PACKETLOGGER_FLUSH_SIZE = getIntProperty(serverSettings, "LogPacketsFlushSize", 8192);
            String temp = getProperty(serverSettings, "LogPacketsFromIPs", "").trim();
            if (temp.isEmpty())
                PACKETLOGGER_IPS = null;
            else {
                PACKETLOGGER_IPS = new NetList();
                PACKETLOGGER_IPS.LoadFromString(temp, ",");
            }

            temp = getProperty(serverSettings, "LogPacketsFromAccounts", "").trim();
            if (temp.isEmpty())
                PACKETLOGGER_ACCOUNTS = null;
            else {
                PACKETLOGGER_ACCOUNTS = new FastList<String>();
                for (final String s : temp.split(","))
                    PACKETLOGGER_ACCOUNTS.add(s);
            }

            temp = getProperty(serverSettings, "LogPacketsFromChars", "").trim();
            if (temp.isEmpty())
                PACKETLOGGER_CHARACTERS = null;
            else {
                PACKETLOGGER_CHARACTERS = new FastList<String>();
                for (final String s : temp.split(","))
                    PACKETLOGGER_CHARACTERS.add(s);
            }
            temp = null;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load PacketLogger Config.");
        }
    }

    public static void loadAllConfigs() {
        // protection
        initProtection(Server.SERVER_MODE == Server.MODE_GAMESERVER || Server.SERVER_MODE == Server.MODE_COMBOSERVER);

        if (Server.SERVER_MODE == Server.MODE_GAMESERVER || Server.SERVER_MODE == Server.MODE_COMBOSERVER) {
            loadGameServerConfig();
            loadVersionConfig();
            loadGameTelnetConfig();
            loadRateConfig();
            loadWeddingConfig();
            loadLotteryConfig();
            loadClanHallConfig();
            loadOtherConfig();
            loadSpoilConfig();
            loadAlternativeConfig();
            loadDevelopersConfig();
            loadChatConfig();
            loadChampionConfig();
            loadServicesConfig();
            loadPvPConfig();
            loadAIConfig();
            loadGeodataConfig();
            loadHexidFile();
            loadEventConfig();
            loadOlympConfig();

            loadGMAccess();
            abuseLoad();
            if (ADVIPSYSTEM)
                ipsLoad();
        } else if (Server.SERVER_MODE == Server.MODE_LOGINSERVER) {
            loadLoginServerConfig();
            loadLoginTelnetConfig();
        } else
            _log.severe("Could not Load Config: server mode was not set");
    }

    public static void loadGameServerConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + CONFIGURATION_FILE);
        try {
            final Properties serverSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE);
            serverSettings.load(is);
            is.close();

            loadAntiFlood(serverSettings);
            reloadPacketLoggerConfig();
            GAME_SERVER_LOGIN_HOST = getProperty(serverSettings, "LoginHost", "127.0.0.1").trim();
            GAME_SERVER_LOGIN_PORT = getIntProperty(serverSettings, "LoginPort", 9013);
            GAME_SERVER_LOGIN_CRYPT = getBooleanProperty(serverSettings, "LoginUseCrypt", true);
            ADVIPSYSTEM = getBooleanProperty(serverSettings, "AdvIPSystem", false);

            HIDE_GM_STATUS = getBooleanProperty(serverSettings, "HideGMStatus", false);
            SHOW_GM_LOGIN = getBooleanProperty(serverSettings, "ShowGMLogin", true);
            SAVE_GM_EFFECTS = getBooleanProperty(serverSettings, "SaveGMEffects", false);
            GCTaskDelay = getLongProperty(serverSettings, "GCTaskDelay", 0) * 1000;
            REQUEST_ID = getIntProperty(serverSettings, "RequestServerID", 0);
            ACCEPT_ALTERNATE_ID = getBooleanProperty(serverSettings, "AcceptAlternateID", true);

            PORT_GAME = getIntProperty(serverSettings, "GameserverPort", 7777);
            PORT_LOGIN = getIntProperty(serverSettings, "LoginserverPort", 2106);
            CNAME_TEMPLATE = getProperty(serverSettings, "CnameTemplate", "[A-Za-z0-9\u0410-\u042f\u0430-\u044f]{2,16}");
            CLAN_NAME_TEMPLATE = getProperty(serverSettings, "ClanNameTemplate", "[A-Za-z0-9\u0410-\u042f\u0430-\u044f]{3,16}");
            CLAN_TITLE_TEMPLATE = getProperty(serverSettings, "ClanTitleTemplate", "[A-Za-z0-9\u0410-\u042f\u0430-\u044f \\p{Punct}]{1,16}");
            ALLY_NAME_TEMPLATE = getProperty(serverSettings, "AllyNameTemplate", "[A-Za-z0-9\u0410-\u042f\u0430-\u044f]{3,16}");
            ANAME_TEMPLATE = getProperty(serverSettings, "AnameTemplate", "[A-Za-z0-9]{3,14}");
            APASSWD_TEMPLATE = getProperty(serverSettings, "ApasswdTemplate", "[A-Za-z0-9]{5,16}");
            LOGIN_TRY_BEFORE_BAN = getIntProperty(serverSettings, "LoginTryBeforeBan", 10);
            LOGIN_TRY_BEFORE_BAN_TIME = getIntProperty(serverSettings, "LoginTryBeforeBanTime", 10);
            GAMESERVER_HOSTNAME = getProperty(serverSettings, "GameserverHostname");

            LOG_CHAT = getBooleanProperty(serverSettings, "LogChat", false);
            LOG_KILLS = getBooleanProperty(serverSettings, "LogKills", false);
            LOG_TELNET = getBooleanProperty(serverSettings, "LogTelnet", false);
            SQL_LOG = getBooleanProperty(serverSettings, "SqlLog", false);

            AUTODESTROY_ITEM_AFTER = getIntProperty(serverSettings, "AutoDestroyDroppedItemAfter", 0);
            AUTODESTROY_PLAYER_ITEM_AFTER = getIntProperty(serverSettings, "AutoDestroyPlayerDroppedItemAfter", 0);
            DELETE_DAYS = getIntProperty(serverSettings, "DeleteCharAfterDays", 7);
            PURGE_BYPASS_TASK_FREQUENCY = getIntProperty(serverSettings, "PurgeTaskFrequency", 60);

            DATAPACK_ROOT = new File(getProperty(serverSettings, "DatapackRoot", ".")).getCanonicalFile();
            AttributeBonusFile = getProperty(serverSettings, "AttributeBonusFile", "data/attribute_bonus.xml");

            ALLOW_L2WALKER_CLIENT = L2WalkerAllowed.valueOf(getProperty(serverSettings, "AllowL2Walker", "False"));
            MAX_L2WALKER_LEVEL = getIntProperty(serverSettings, "MaxL2WalkerLevel", 5);

            ALLOW_CRAFT_BOT = getBooleanProperty(serverSettings, "AllowCraftBot", true);
            ALLOW_BOT_CAST = getBooleanProperty(serverSettings, "AllowBotCast", false);
            L2WALKER_PUNISHMENT = getIntProperty(serverSettings, "L2WalkerPunishment", 1);
            BUGUSER_PUNISH = getIntProperty(serverSettings, "BugUserPunishment", 2);
            DEFAULT_PUNISH = getIntProperty(serverSettings, "IllegalActionPunishment", 1);
            FORCE_MULTISELL_SELL_PRICE = getBooleanProperty(serverSettings, "ForceMulteSellPrice", true);
            ALLOW_DISCARDITEM = getBooleanProperty(serverSettings, "AllowDiscardItem", true);
            ALLOW_FREIGHT = getBooleanProperty(serverSettings, "AllowFreight", true);
            ALLOW_WAREHOUSE = getBooleanProperty(serverSettings, "AllowWarehouse", true);
            ALLOW_WATER = getBooleanProperty(serverSettings, "AllowWater", true);
            ALLOW_BOAT = getBooleanProperty(serverSettings, "AllowBoat", false);
            ALLOW_CURSED_WEAPONS = getBooleanProperty(serverSettings, "AllowCursedWeapons", false);
            DROP_CURSED_WEAPONS_ON_KICK = getBooleanProperty(serverSettings, "DropCursedWeaponsOnKick", false);

            MIN_PROTOCOL_REVISION = getIntProperty(serverSettings, "MinProtocolRevision", 12);
            MAX_PROTOCOL_REVISION = getIntProperty(serverSettings, "MaxProtocolRevision", 17);

            if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
                throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server configuration file.");

            MIN_NPC_ANIMATION = getIntProperty(serverSettings, "MinNPCAnimation", 5);
            MAX_NPC_ANIMATION = getIntProperty(serverSettings, "MaxNPCAnimation", 90);

            ALLOW_COMMUNITYBOARD = getBooleanProperty(serverSettings, "AllowCommunityBoard", true);
            BBS_DEFAULT = getProperty(serverSettings, "BBSDefault", "_bbshome");
            COMMUNITYBOARD_HTML_ROOT = getProperty(serverSettings, "CommunityBoardHtmlRoot", "data/html/CommunityBoard/");
            COMMUNITYBOARD_SORTPLAYERSLIST = getBooleanProperty(serverSettings, "CommunityBoardSortPlayersList", false);
            NAME_PAGE_SIZE_COMMUNITYBOARD = getIntProperty(serverSettings, "NamePageSizeOnCommunityBoard", 50);
            NAME_PER_ROW_COMMUNITYBOARD = getIntProperty(serverSettings, "NamePerRowOnCommunityBoard", 5);
            COMMUNITYBOARD_PLAYERSLIST_CACHE = getIntProperty(serverSettings, "CommunityBoardPlayersListCache", 0) * 1000;
            ALLOW_COMMUNITYBOARD_PLAYERSLIST = getProperty(serverSettings, "AllowCommunityBoardPlayersList", "all");

            INTERNAL_HOSTNAME = getProperty(serverSettings, "InternalHostname", "*");
            EXTERNAL_HOSTNAME = getProperty(serverSettings, "ExternalHostname", "*");

            SERVER_SIDE_NPC_NAME = getBooleanProperty(serverSettings, "ServerSideNpcName", false);
            SERVER_SIDE_NPC_TITLE_WITH_LVL = getBooleanProperty(serverSettings, "ServerSideNpcTitleWithLvl", false);
            SERVER_SIDE_NPC_TITLE = getBooleanProperty(serverSettings, "ServerSideNpcTitle", false);
            AUTODELETE_INVALID_QUEST_DATA = getBooleanProperty(serverSettings, "AutoDeleteInvalidQuestData", false);

            HARD_DB_CLEANUP_ON_START = getBooleanProperty(serverSettings, "HardDbCleanUpOnStart", false);

            AUTOSAVE = getBooleanProperty(serverSettings, "Autosave", true);

            MAXIMUM_ONLINE_USERS_NOTTRIAL = getIntProperty(serverSettings, "MaximumOnlineUsers", 100);

            DATABASE_DRIVER = getProperty(serverSettings, "Driver", "com.mysql.jdbc.Driver");
            DATABASE_URL = getProperty(serverSettings, "URL", "jdbc:mysql://localhost/l2d");
            DATABASE_LOGIN = getProperty(serverSettings, "Login", "root");
            DATABASE_PASSWORD = getProperty(serverSettings, "Password", "");
            DATABASE_MAX_CONNECTIONS = getIntProperty(serverSettings, "MaximumDbConnections", 10);
            DATABASE_MAX_IDLE_TIMEOUT = getIntProperty(serverSettings, "MaxIdleConnectionTimeout", 600);
            DATABASE_IDLE_TEST_PERIOD = getIntProperty(serverSettings, "IdleConnectionTestPeriod", 60);
            USE_DATABASE_LAYER = getBooleanProperty(serverSettings, "UseDatabaseLayer", true);
            GSWLS_MODE = getBooleanProperty(serverSettings, "GSWLSMode", false);
            GAME_SERVER_LOGIN_DB = getProperty(serverSettings, "LoginDB", DATABASE_URL.split("/")[DATABASE_URL.split("/").length - 1]);

            LAZY_ITEM_UPDATE = getBooleanProperty(serverSettings, "LazyItemUpdate", false);
            LAZY_ITEM_UPDATE_ALL = getBooleanProperty(serverSettings, "LazyItemUpdateAll", false);
            LAZY_ITEM_UPDATE_TIME = getIntProperty(serverSettings, "LazyItemUpdateTime", 60000);
            LAZY_ITEM_UPDATE_ALL_TIME = getIntProperty(serverSettings, "LazyItemUpdateAllTime", 60000);
            DELAYED_ITEMS_UPDATE_INTERVAL = getIntProperty(serverSettings, "DelayedItemsUpdateInterval", 10000);
            USER_INFO_INTERVAL = getIntProperty(serverSettings, "UserInfoInterval", 100);
            BROADCAST_STATS_INTERVAL = getBooleanProperty(serverSettings, "BroadcastStatsInterval", true);
            BROADCAST_CHAR_INFO_INTERVAL = getIntProperty(serverSettings, "BroadcastCharInfoInterval", 100);
            SAVE_GAME_TIME_INTERVAL = getIntProperty(serverSettings, "SaveGameTimeInterval", 120);

            RESTART_TIME = getIntProperty(serverSettings, "AutoRestart", 0);
            RESTART_AT_TIME = getIntProperty(serverSettings, "AutoRestartAt", 5);
            if (RESTART_AT_TIME > 24)
                RESTART_AT_TIME = 24;

            CHECK_LANG_FILES_MODIFY = getBooleanProperty(serverSettings, "checkLangFilesModify", false);

            USE_FILE_CACHE = getBooleanProperty(serverSettings, "useFileCache", true);

            LINEAR_TERRITORY_CELL_SIZE = getIntProperty(serverSettings, "LinearTerritoryCellSize", 32);

            DISABLE_CREATION_IDs = getProperty(serverSettings, "DisableCreateItems", "");
            if (DISABLE_CREATION_IDs.length() != 0)
                for (final String id : DISABLE_CREATION_IDs.split(","))
                    DISABLE_CREATION_ID_LIST.add(Integer.parseInt(id));

            LOG_MULTISELL_IDs = getProperty(serverSettings, "LogMultisellId", "");
            if (LOG_MULTISELL_IDs.length() != 0)
                for (final String id : LOG_MULTISELL_IDs.split(","))
                    LOG_MULTISELL_ID_LIST.add(Integer.parseInt(id));

            DAMAGE_FROM_FALLING = getBooleanProperty(serverSettings, "DamageFromFalling", true);

            FORCE_STATUSUPDATE = getBooleanProperty(serverSettings, "ForceStatusUpdate", false);

            GG_CHECK = getBooleanProperty(serverSettings, "GGCheck", true);

            SNAPSHOTS_DIRECTORY = getProperty(serverSettings, "SnapshotsDirectory", "./log/snapshots");
            DUMP_MEMORY_ON_SHUTDOWN = getBooleanProperty(serverSettings, "MemorySnapshotOnShutdown", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CONFIGURATION_FILE + " File.");
        }
    }

    public static void loadVersionConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + VERSION_FILE);
        try {
            final Properties serverVersion = new Properties();
            final InputStream is = Config.class.getResourceAsStream(Config.VERSION_FILE);
            serverVersion.load(is);
            is.close();

            SERVER_VERSION = getProperty(serverVersion, "version", SERVER_VERSION_UNSUPPORTED);
            SERVER_BUILD_DATE = getProperty(serverVersion, "builddate", "Undefined Date.");
        } catch (final Exception e) {
            // Ignore Properties file if it doesn't exist
            SERVER_VERSION = SERVER_VERSION_UNSUPPORTED;
            SERVER_BUILD_DATE = "Undefined Date.";
        }
    }

    public static void loadGameTelnetConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + TELNET_FILE);
        try {
            final Properties telnetSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(TELNET_FILE);
            telnetSettings.load(is);
            is.close();

            IS_TELNET_ENABLED = getBooleanProperty(telnetSettings, "EnableTelnet", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + TELNET_FILE + " File.");
        }
    }

    public static void loadRateConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + RATE_FILE);
        try {
            final Properties rateSettings = new Properties();
            final InputStream is =Config.class.getClassLoader().getResourceAsStream(RATE_FILE);
            rateSettings.load(is);
            is.close();

            RATE_XP = getFloatProperty(rateSettings, "RateXp", 1.);
            RATE_SP = getFloatProperty(rateSettings, "RateSp", 1.);
            RATE_QUESTS_REWARD = getFloatProperty(rateSettings, "RateQuestsReward", 1.);
            RATE_QUESTS_DROP = getFloatProperty(rateSettings, "RateQuestsDrop", 1.);
            RATE_QUESTS_EXP = getFloatProperty(rateSettings, "RateQuestsExp", 1.);
            RATE_QUESTS_DROP_PROF = getFloatProperty(rateSettings, "RateQuestsDropProf", 1.);
            RATE_QUESTS_OCCUPATION_CHANGE = getBooleanProperty(rateSettings, "RateQuestsRewardOccupationChange", true);
            RATE_CLAN_REP_SCORE = getFloatProperty(rateSettings, "RateClanRepScore", 1.);
            RATE_CLAN_REP_SCORE_MAX_AFFECTED = getIntProperty(rateSettings, "RateClanRepScoreMaxAffected", 2);
            RATE_DROP_ADENA = getFloatProperty(rateSettings, "RateDropAdena", 1.);
            RATE_DROP_ITEMS = getFloatProperty(rateSettings, "RateDropItems", 1.);
            RATE_DROP_ADENA_PARTY = getFloatProperty(rateSettings, "RateDropAdenaParty", 1.);
            RATE_DROP_ITEMS_PARTY = getFloatProperty(rateSettings, "RateDropItemsParty", 1.);
            RATE_XP_PARTY = getFloatProperty(rateSettings, "RateXpParty", 1.);
            RATE_SP_PARTY = getFloatProperty(rateSettings, "RateSpParty", 1.);
            RATE_DROP_SPOIL = getFloatProperty(rateSettings, "RateDropSpoil", 1.);
            RATE_MANOR = getIntProperty(rateSettings, "RateManor", 1);
            RATE_FISH_DROP_COUNT = getFloatProperty(rateSettings, "RateFishDropCount", 1.);
            RATE_SIEGE_GUARDS_PRICE = getFloatProperty(rateSettings, "RateSiegeGuardsPrice", 1.);
            RATE_DROP_RAIDBOSS = getFloatProperty(rateSettings, "RateRaidBoss", 1.);
            RATE_RAID_REGEN = getFloatProperty(rateSettings, "RateRaidRegen", 1.);
            RAID_MAX_LEVEL_DIFF = getIntProperty(rateSettings, "RaidMaxLevelDiff", 8);
            PARALIZE_ON_RAID_DIFF = getBooleanProperty(rateSettings, "ParalizeOnRaidLevelDiff", true);

        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + RATE_FILE + " File.");
        }
    }

    public static void loadWeddingConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + WEDDING_FILE);
        try {
            final Properties weddingSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(WEDDING_FILE);
            weddingSettings.load(is);
            is.close();

            WEDDING_ALLOW_WEDDING = getBooleanProperty(weddingSettings, "AllowWedding", false);
            WEDDING_PRICE = getIntProperty(weddingSettings, "WeddingPrice", 500000);
            WEDDING_PUNISH_INFIDELITY = getBooleanProperty(weddingSettings, "WeddingPunishInfidelity", true);
            WEDDING_TELEPORT = getBooleanProperty(weddingSettings, "WeddingTeleport", true);
            WEDDING_TELEPORT_PRICE = getIntProperty(weddingSettings, "WeddingTeleportPrice", 500000);
            WEDDING_TELEPORT_INTERVAL = getIntProperty(weddingSettings, "WeddingTeleportInterval", 120);
            WEDDING_SAMESEX = getBooleanProperty(weddingSettings, "WeddingAllowSameSex", true);
            WEDDING_FORMALWEAR = getBooleanProperty(weddingSettings, "WeddingFormalWear", true);
            WEDDING_DIVORCE_COSTS = getIntProperty(weddingSettings, "WeddingDivorceCosts", 20);
            WEDDING_GIVE_ITEM = getIntProperty(weddingSettings, "WeddingGiveItem", 9140);
            WEDDING_GIVE_COUNT = getIntProperty(weddingSettings, "WeddingGiveCount", 1);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + WEDDING_FILE + " File.");
        }
    }

    public static void loadLotteryConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + LOTTERY_FILE);
        try {
            final Properties lotterySettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(LOTTERY_FILE);
            lotterySettings.load(is);
            is.close();

            ALLOW_LOTTERY = getBooleanProperty(lotterySettings, "AllowLottery", false);
            LOTTERY_PRIZE = getIntProperty(lotterySettings, "LotteryPrize", 50000);
            LOTTERY_PRICE = getIntProperty(lotterySettings, "AltLotteryPrice", 2000);
            LOTTERY_TICKET_PRICE = getIntProperty(lotterySettings, "LotteryTicketPrice", 2000);
            LOTTERY_5_NUMBER_RATE = getFloatProperty(lotterySettings, "Lottery5NumberRate", 0.6);
            LOTTERY_4_NUMBER_RATE = getFloatProperty(lotterySettings, "Lottery4NumberRate", 0.4);
            LOTTERY_3_NUMBER_RATE = getFloatProperty(lotterySettings, "Lottery3NumberRate", 0.2);
            LOTTERY_2_AND_1_NUMBER_PRIZE = getIntProperty(lotterySettings, "Lottery2and1NumberPrize", 200);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + LOTTERY_FILE + " File.");
        }
    }

    public static void loadClanHallConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + CLANHALL_CONFIG_FILE);
        try {
            final Properties clanhallSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(CLANHALL_CONFIG_FILE);
            clanhallSettings.load(is);
            is.close();

            CH_BID_GRADE1_MINCLANLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade1_MinClanLevel", 2);
            CH_BID_GRADE1_MINCLANMEMBERS = getIntProperty(clanhallSettings, "ClanHallBid_Grade1_MinClanMembers", 1);
            CH_BID_GRADE1_MINCLANMEMBERSLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade1_MinClanMembersAvgLevel", 1);
            CH_BID_GRADE2_MINCLANLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade2_MinClanLevel", 2);
            CH_BID_GRADE2_MINCLANMEMBERS = getIntProperty(clanhallSettings, "ClanHallBid_Grade2_MinClanMembers", 1);
            CH_BID_GRADE2_MINCLANMEMBERSLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade2_MinClanMembersAvgLevel", 1);
            CH_BID_GRADE3_MINCLANLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade3_MinClanLevel", 2);
            CH_BID_GRADE3_MINCLANMEMBERS = getIntProperty(clanhallSettings, "ClanHallBid_Grade3_MinClanMembers", 1);
            CH_BID_GRADE3_MINCLANMEMBERSLEVEL = getIntProperty(clanhallSettings, "ClanHallBid_Grade3_MinClanMembersAvgLevel", 1);
            RESIDENCE_LEASE_MULTIPLIER = getFloatProperty(clanhallSettings, "ResidenceLeaseMultiplier", 1.);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CLANHALL_CONFIG_FILE + " File.");
        }
    }

    public static void loadOtherConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + OTHER_CONFIG_FILE);
        try {
            final Properties otherSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(OTHER_CONFIG_FILE);
            otherSettings.load(is);
            is.close();

            DEEPBLUE_DROP_RULES = getBooleanProperty(otherSettings, "UseDeepBlueDropRules", true);
            DEEPBLUE_DROP_MAXDIFF = getIntProperty(otherSettings, "DeepBlueDropMaxDiff", 8);
            DEEPBLUE_DROP_RAID_MAXDIFF = getIntProperty(otherSettings, "DeepBlueDropRaidMaxDiff", 2);

            WYVERN_SPEED = Integer.parseInt(otherSettings.getProperty("WyvernSpeed", "100"));
            STRIDER_SPEED = Integer.parseInt(otherSettings.getProperty("StriderSpeed", "80"));
            SWIMING_SPEED = Integer.parseInt(otherSettings.getProperty("SwimingSpeedTemplate", "50"));

            ANY_PARTY_MEMBER_MAY_SURRENDER = getBooleanProperty(otherSettings, "AnyPartyMemberMaySurrender", false);
            /* Inventory slots limits */
            INVENTORY_MAXIMUM_NO_DWARF = getIntProperty(otherSettings, "MaximumSlotsForNoDwarf", 80);
            INVENTORY_MAXIMUM_DWARF = getIntProperty(otherSettings, "MaximumSlotsForDwarf", 100);
            INVENTORY_MAXIMUM_GM = getIntProperty(otherSettings, "MaximumSlotsForGMPlayer", 250);

            MULTISELL_SIZE = getIntProperty(otherSettings, "MultisellPageSize", 10);
			/* Warehouse slots limits */
            WAREHOUSE_SLOTS_NO_DWARF = getIntProperty(otherSettings, "BaseWarehouseSlotsForNoDwarf", 100);
            WAREHOUSE_SLOTS_DWARF = getIntProperty(otherSettings, "BaseWarehouseSlotsForDwarf", 120);
            WAREHOUSE_SLOTS_CLAN = getIntProperty(otherSettings, "MaximumWarehouseSlotsForClan", 200);

			/* Alt Enchant Formula */
            ALT_ENCHANT_FORMULA = getBooleanProperty(otherSettings, "AltEnchantFormulaForPvPServers", false);
            ALT_ENCHANT_CHANCE_W = getIntProperty(otherSettings, "EChanceWeapon", 80);
            ALT_ENCHANT_CHANCE_MAGE_W = getIntProperty(otherSettings, "EChanceMageWeapon", 80);
            ALT_ENCHANT_CHANCE_ARMOR = getIntProperty(otherSettings, "EChanceArmor", 80);
            PENALTY_TO_TWOHANDED_BLUNTS = getIntProperty(otherSettings, "PenaltyforEChanceToHandBlunt", 18);

			/* chance to enchant an item over safe level */
            ENCHANT_CHANCE_WEAPON = getIntProperty(otherSettings, "EnchantChance", 66);
            ENCHANT_CHANCE_ARMOR = getIntProperty(otherSettings, "EnchantChanceArmor", ENCHANT_CHANCE_WEAPON);
            ENCHANT_CHANCE_ACCESSORY = getIntProperty(otherSettings, "EnchantChanceAccessory", ENCHANT_CHANCE_ARMOR);
            ENCHANT_CHANCE_CRYSTAL_WEAPON = getIntProperty(otherSettings, "EnchantChanceCrystal", 66);
            ENCHANT_CHANCE_CRYSTAL_ARMOR = getIntProperty(otherSettings, "EnchantChanceCrystalArmor", ENCHANT_CHANCE_CRYSTAL_WEAPON);
            ENCHANT_CHANCE_CRYSTAL_ACCESSORY = getIntProperty(otherSettings, "EnchantChanceCrystalAccessory", ENCHANT_CHANCE_CRYSTAL_ARMOR);
            ENCHANT_BLESSED_FAIL = getIntProperty(otherSettings, "EnchantBlessedFail", 0);
            EnchantCrystalSafe = getBooleanProperty(otherSettings, "EnchantCrystalSafe", false);
            SAFE_ENCHANT_COMMON = getIntProperty(otherSettings, "SafeEnchantCommon", 3);
            SAFE_ENCHANT_FULL_BODY = getIntProperty(otherSettings, "SafeEnchantFullBody", 4);
            ENCHANT_MAX = getByteProperty(otherSettings, "EnchantMax", 20);

            SOUL_RATE_CHANCE = getDoubleProperty(otherSettings, "SoulRateChance", 5);

            REGEN_SIT_WAIT = getBooleanProperty(otherSettings, "RegenSitWait", false);

            STARTING_ADENA = getIntProperty(otherSettings, "StartingAdena", 0);
            ENABLE_STARTING_ITEM = Boolean.parseBoolean(otherSettings.getProperty("EnableStartingItem", "False"));
            STARTING_ITEM_ID = otherSettings.getProperty("StartingItemId", "");

            if (STARTING_ITEM_ID != "") {
                int num = 0;
                for (String id : STARTING_ITEM_ID.split(",")) {
                    STARTING_ITEM_ID_LIST[num] = Integer.parseInt(id);
                    num++;
                }
            }

            STARTING_ITEM_COUNT = otherSettings.getProperty("StartingItemCount", "");
            if (STARTING_ITEM_COUNT != "") {
                int num = 0;
                for (String id : STARTING_ITEM_COUNT.split(",")) {
                    STARTING_ITEM_COUNT_LIST[num] = Integer.parseInt(id);
                    num++;
                }
            }
            UNSTUCK_SKILL = getBooleanProperty(otherSettings, "UnstuckSkill", true);

			/* Amount of HP, MP, and CP is restored */
            RESPAWN_RESTORE_CP = getDoubleProperty(otherSettings, "RespawnRestoreCP", -1) / 100;
            RESPAWN_RESTORE_HP = getDoubleProperty(otherSettings, "RespawnRestoreHP", 65) / 100;
            RESPAWN_RESTORE_MP = getDoubleProperty(otherSettings, "RespawnRestoreMP", -1) / 100;

			/* Maximum number of available slots for pvt stores */
            MAX_PVTSTORE_SLOTS_DWARF = getIntProperty(otherSettings, "MaxPvtStoreSlotsDwarf", 5);
            MAX_PVTSTORE_SLOTS_OTHER = getIntProperty(otherSettings, "MaxPvtStoreSlotsOther", 4);
            MAX_PVTCRAFT_SLOTS = getIntProperty(otherSettings, "MaxPvtManufactureSlots", 20);

            ANNOUNCE_MAMMON_SPAWN = getBooleanProperty(otherSettings, "AnnounceMammonSpawn", true);

            GM_NAME_COLOUR = getIntHexProperty(otherSettings, "GMNameColour", 0xFFFFFF);
            GM_HERO_AURA = getBooleanProperty(otherSettings, "GMHeroAura", true);
            NORMAL_NAME_COLOUR = getIntHexProperty(otherSettings, "NormalNameColour", 0xFFFFFF);
            CLANLEADER_NAME_COLOUR = getIntHexProperty(otherSettings, "ClanleaderNameColour", 0xFFFFFF);
            BOT_NAME_COLOUR = getIntHexProperty(otherSettings, "BotNameColour", 0xFFFFFF);
            BOT_NAME_HEX_COLOUR = getProperty(otherSettings, "BotNameHexColour", "FFFFFF");

            SHOW_HTML_WELCOME = getBooleanProperty(otherSettings, "ShowHTMLWelcome", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + OTHER_CONFIG_FILE + " File.");
        }
    }

    public static void loadSpoilConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + SPOIL_CONFIG_FILE);
        try {
            final Properties spoilSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(SPOIL_CONFIG_FILE);
            spoilSettings.load(is);
            is.close();

            BASE_SPOIL_RATE = getFloatProperty(spoilSettings, "BasePercentChanceOfSpoilSuccess", 78.);
            MINIMUM_SPOIL_RATE = getFloatProperty(spoilSettings, "MinimumPercentChanceOfSpoilSuccess", 1.);
            ALT_SPOIL_FORMULA = getBooleanProperty(spoilSettings, "AltFormula", false);
            MANOR_SOWING_BASIC_SUCCESS = getIntProperty(spoilSettings, "BasePercentChanceOfSowingSuccess", 100);
            MANOR_SOWING_ALT_BASIC_SUCCESS = getIntProperty(spoilSettings, "BasePercentChanceOfSowingAltSuccess", 10);
            MANOR_HARVESTING_BASIC_SUCCESS = getIntProperty(spoilSettings, "BasePercentChanceOfHarvestingSuccess", 90);
            MANOR_DIFF_PLAYER_TARGET = getIntProperty(spoilSettings, "MinDiffPlayerMob", 5);
            MANOR_DIFF_PLAYER_TARGET_PENALTY = getIntProperty(spoilSettings, "DiffPlayerMobPenalty", 5);
            MANOR_DIFF_SEED_TARGET = getIntProperty(spoilSettings, "MinDiffSeedMob", 5);
            MANOR_DIFF_SEED_TARGET_PENALTY = getIntProperty(spoilSettings, "DiffSeedMobPenalty", 5);
            ALLOW_MANOR = getBooleanProperty(spoilSettings, "AllowManor", true);
            MANOR_REFRESH_TIME = getIntProperty(spoilSettings, "AltManorRefreshTime", 20);
            MANOR_REFRESH_MIN = getIntProperty(spoilSettings, "AltManorRefreshMin", 00);
            MANOR_APPROVE_TIME = getIntProperty(spoilSettings, "AltManorApproveTime", 6);
            MANOR_APPROVE_MIN = getIntProperty(spoilSettings, "AltManorApproveMin", 00);
            MANOR_MAINTENANCE_PERIOD = getIntProperty(spoilSettings, "AltManorMaintenancePeriod", 360000);
            MANOR_SAVE_ALL_ACTIONS = getBooleanProperty(spoilSettings, "AltManorSaveAllActions", false);
            MANOR_SAVE_PERIOD_RATE = getIntProperty(spoilSettings, "AltManorSavePeriodRate", 2);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + SPOIL_CONFIG_FILE + " File.");
        }
    }

    public static void loadAlternativeConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + ALT_SETTINGS_FILE);
        try {
            final Properties altSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(ALT_SETTINGS_FILE);
            altSettings.load(is);
            is.close();

            CH_DOORS_AUTO_OPEN_DELAY = getIntProperty(altSettings, "CHDoorAutoOpenDelay", 500);
            CH_DOORS_AUTO_OPEN = getBooleanProperty(altSettings, "CHDoorAutoOpen", false);
            SKILLS_CHANCE_MOD = getDoubleProperty(altSettings, "SkillsChanceMod", 20);
            SKILLS_CHANCE_MIN = getDoubleProperty(altSettings, "SkillsChanceMin", 5);
            SKILLS_CHANCE_CAP = getDoubleProperty(altSettings, "SkillsChanceCap", 95);
            SKILLS_SHOW_CHANCE = getBooleanProperty(altSettings, "SkillsShowChance", true);
            ALT_SAVE_UNSAVEABLE = getBooleanProperty(altSettings, "AltSaveUnsaveable", false);
            ALT_SHOW_REUSE_MSG = getBooleanProperty(altSettings, "AltShowSkillReuseMessage", true);
            WEAR_ENABLED = getBooleanProperty(altSettings, "WearEnabled", false);
            AUTO_LOOT = getBooleanProperty(altSettings, "AutoLoot", false);
            AUTO_LOOT_HERBS = getBooleanProperty(altSettings, "AutoLootHerbs", false);
            AUTO_LOOT_INDIVIDUAL = getBooleanProperty(altSettings, "AutoLootIndividual", false);
            AUTO_LOOT_FROM_RAIDS = getBooleanProperty(altSettings, "AutoLootFromRaids", false);

            AUTO_LOOT_PK = getBooleanProperty(altSettings, "AutoLootPK", false);
            SAVING_SPS = getBooleanProperty(altSettings, "SavingSpS", false);
            MANAHEAL_SPS_BONUS = getBooleanProperty(altSettings, "ManahealSpSBonus", false);
            ALT_TRUE_CHESTS = getIntProperty(altSettings, "TrueChests", 50);
            ALT_GAME_MATHERIALSDROP = getBooleanProperty(altSettings, "AltMatherialsDrop", false);
            ALT_DOUBLE_SPAWN = getBooleanProperty(altSettings, "DoubleSpawn", false);
            ALT_ALLOW_AUGMENT_ALL = getBooleanProperty(altSettings, "AugmentAll", false);
            ALT_ALLOW_DROP_AUGMENTED = getBooleanProperty(altSettings, "AlowDropAugmented", false);
            ALT_GAME_EXP_FOR_CRAFT = getBooleanProperty(altSettings, "AltExpForCraft", true);
            ALT_GAME_SHOW_DROPLIST = getBooleanProperty(altSettings, "AltShowDroplist", true);
            ALT_GAME_GEN_DROPLIST_ON_DEMAND = getBooleanProperty(altSettings, "AltGenerateDroplistOnDemand", false);
            ALLOW_NPC_SHIFTCLICK = getBooleanProperty(altSettings, "AllowShiftClick", true);
            ALT_FULL_NPC_STATS_PAGE = getBooleanProperty(altSettings, "AltFullStatsPage", false);
            ALT_GAME_SUBCLASS_WITHOUT_QUESTS = getBooleanProperty(altSettings, "AltAllowSubClassWithoutQuest", false);
            ALT_GAME_LEVEL_TO_GET_SUBCLASS = getIntProperty(altSettings, "AltLevelToGetSubclass", 75);
            ALT_GAME_SUB_ADD = getIntProperty(altSettings, "AltSubAdd", 0);
            ALT_MAX_LEVEL = Math.min(getIntProperty(altSettings, "AltMaxLevel", 85), Experience.LEVEL.length - 1);
            ALT_MAX_SUB_LEVEL = Math.min(getIntProperty(altSettings, "AltMaxSubLevel", 80), Experience.LEVEL.length - 1);
            ALT_GAME_REQUIRE_CLAN_CASTLE = getBooleanProperty(altSettings, "AltRequireClanCastle", false);
            ALT_GAME_REQUIRE_CASTLE_DAWN = getBooleanProperty(altSettings, "AltRequireCastleDawn", true);
            ALT_GAME_ALLOW_ADENA_DAWN = getBooleanProperty(altSettings, "AltAllowAdenaDawn", true);
            ALT_ADD_RECIPES = getIntProperty(altSettings, "AltAddRecipes", 0);
            ALT_100_RECIPES_B = getBooleanProperty(altSettings, "Alt100PercentRecipesB", false);
            ALT_100_RECIPES_A = getBooleanProperty(altSettings, "Alt100PercentRecipesA", false);
            ALT_100_RECIPES_S = getBooleanProperty(altSettings, "Alt100PercentRecipesS", false);
            SS_ANNOUNCE_PERIOD = getIntProperty(altSettings, "SSAnnouncePeriod", 0);
            AUTO_LEARN_SKILLS = getBooleanProperty(altSettings, "AutoLearnSkills", false);
            AUTO_LEARN_SKILLS_MAX_LEVEL = getIntProperty(altSettings, "AutoLearnSkillsMaxLevel", 85);
            ALT_SOCIAL_ACTION_REUSE = getBooleanProperty(altSettings, "AltSocialActionReuse", false);
            ALT_DISABLE_SPELLBOOKS = getBooleanProperty(altSettings, "AltDisableSpellbooks", false);
            ALT_MAMMON_UPGRADE = getIntProperty(altSettings, "MammonUpgrade", 6130000);
            ALT_MAMMON_EXCHANGE = getIntProperty(altSettings, "MammonExchange", 6130000);
            ALT_BUFF_LIMIT = getIntProperty(altSettings, "BuffLimit", 20);
            ALT_DANCE_SONG_LIMIT = getIntProperty(altSettings, "DanceSongsLimit", 12);
            ALLOW_DEATH_PENALTY = getBooleanProperty(altSettings, "EnableDeathPenalty", true);
            ALT_DEATH_PENALTY_CHANCE = getIntProperty(altSettings, "DeathPenaltyChance", 10);
            ALT_DEATH_PENALTY_EXPERIENCE_PENALTY = getIntProperty(altSettings, "DeathPenaltyRateExpPenalty", 1);
            ALT_DEATH_PENALTY_KARMA_PENALTY = getIntProperty(altSettings, "DeathPenaltyRateKarma", 1);
            NONOWNER_ITEM_PICKUP_DELAY = getLongProperty(altSettings, "NonOwnerItemPickupDelay", 15) * 1000L;
            ALT_NO_LASTHIT = getBooleanProperty(altSettings, "NoLasthitOnRaid", false);
            ALT_DONT_ALLOW_PETS_ON_SIEGE = getBooleanProperty(altSettings, "DontAllowPetsOnSiege", false);

            DEBUG_STAT_LIMITS = getBooleanProperty(altSettings, "DisableStatLimits", false);
            MAX_HP = getIntProperty(altSettings, "MaxHP", 20000);
            MAX_MP = getIntProperty(altSettings, "MaxMP", 10000);
            MAX_CP = getIntProperty(altSettings, "MaxCP", 10000);
            MAX_RUNSPD = getIntProperty(altSettings, "MaxRunSpd", 250);
            MAX_PDEF = getIntProperty(altSettings, "MaxPDef", 15000);
            MAX_MDEF = getIntProperty(altSettings, "MaxMDef", 15000);
            MAX_PATK = getIntProperty(altSettings, "MaxPAtk", 20000);
            MAX_MATK = getIntProperty(altSettings, "MaxMAtk", 15000);
            MAX_PATKSPD = getIntProperty(altSettings, "MaxPAtkSpd", 1500);
            MAX_MATKSPD = getIntProperty(altSettings, "MaxMAtkSpd", 1999);
            MAX_ACC_COM = getIntProperty(altSettings, "MaxAccuracy", 200);
            MAX_EVAS_RATE = getIntProperty(altSettings, "MaxEvasion", 200);
            MAX_CRIT_BASE = getIntProperty(altSettings, "MaxCritical", 500);
            MAX_MCRIT_RATE = getIntProperty(altSettings, "MaxMCritical", 20);

            FESTIVAL_MIN_PARTY_SIZE = getIntProperty(altSettings, "FestivalMinPartySize", 5);

            RIFT_MIN_PARTY_SIZE = getIntProperty(altSettings, "RiftMinPartySize", 5);
            RIFT_SPAWN_DELAY = getIntProperty(altSettings, "RiftSpawnDelay", 10000);
            RIFT_MAX_JUMPS = getIntProperty(altSettings, "MaxRiftJumps", 4);
            RIFT_AUTO_JUMPS_TIME = getIntProperty(altSettings, "AutoJumpsDelay", 8);
            RIFT_AUTO_JUMPS_TIME_RAND = getIntProperty(altSettings, "AutoJumpsDelayRandom", 120000);

            RIFT_ENTER_COST_RECRUIT = getIntProperty(altSettings, "RecruitFC", 18);
            RIFT_ENTER_COST_SOLDIER = getIntProperty(altSettings, "SoldierFC", 21);
            RIFT_ENTER_COST_OFFICER = getIntProperty(altSettings, "OfficerFC", 24);
            RIFT_ENTER_COST_CAPTAIN = getIntProperty(altSettings, "CaptainFC", 27);
            RIFT_ENTER_COST_COMMANDER = getIntProperty(altSettings, "CommanderFC", 30);
            RIFT_ENTER_COST_HERO = getIntProperty(altSettings, "HeroFC", 33);
            ALLOW_CLANSKILLS = getBooleanProperty(altSettings, "AllowClanSkills", true);
            PARTY_LEADER_ONLY_CAN_INVITE = getBooleanProperty(altSettings, "PartyLeaderOnlyCanInvite", true);
            ALLOW_TALK_WHILE_SITTING = getBooleanProperty(altSettings, "AllowTalkWhileSitting", true);
            ALLOW_NOBLE_TP_TO_ALL = getBooleanProperty(altSettings, "AllowNobleTPToAll", false);

            ALLOW_FAKE_PLAYERS = getBooleanProperty(altSettings, "AllowFakePlayers", false);
            FAKE_PLAYERS_PERCENT = getIntProperty(altSettings, "FakePlayersPercent", 100);
            BUFFTIME_MODIFIER = getFloatProperty(altSettings, "BuffTimeModifier", 1.0);
            BUFFTIME_MODIFIER_CLANHALL = getFloatProperty(altSettings, "ClanHallBuffTimeModifier", 1.0);
            SONGDANCETIME_MODIFIER = getFloatProperty(altSettings, "SongDanceTimeModifier", 1.0);
            BUFF15MINUTES_MODIFIER = getFloatProperty(altSettings, "Buff15MinutesModififer", 1.0);
            SUMMON_BUFF_MODIFIER = Float.parseFloat(altSettings.getProperty("SummonBuffModifier", "1.0"));
            SUMMON_BUFF_TIME = Integer.parseInt(altSettings.getProperty("SummonBuffTime", "1800000"));
            SUMMON_SET_BUFF_TYPE = Float.parseFloat(altSettings.getProperty("SummonBuffType", "1"));

            MAXLOAD_MODIFIER = getFloatProperty(altSettings, "MaxLoadModifier", 1.0);
            GATEKEEPER_MODIFIER = getFloatProperty(altSettings, "GkCostMultiplier", 1.0);
            GATEKEEPER_FREE = getIntProperty(altSettings, "GkFree", 40);
            CRUMA_GATEKEEPER_LVL = getIntProperty(altSettings, "GkCruma", 65);
            ALT_BUFF_SUMMON = getBooleanProperty(altSettings, "BuffSummon", true);
            ALT_CATACOMB_MODIFIER_HP = getIntProperty(altSettings, "AltCatacombMonstersMultHP", 4);

            ALT_MAX_ALLY_SIZE = getIntProperty(altSettings, "AltMaxAllySize", 3);
            ALT_PARTY_DISTRIBUTION_RANGE = getIntProperty(altSettings, "AltPartyDistributionRange", 1500);

            ALT_ALL_PHYS_SKILLS_OVERHIT = getBooleanProperty(altSettings, "AltAllPhysSkillsOverhit", true);
            ALT_REMOVE_SKILLS_ON_DELEVEL = getBooleanProperty(altSettings, "AltRemoveSkillsOnDelevel", true);
            ALT_REMOVE_SKILLS_ON_DELEVEL_N_LEVEL = getIntProperty(altSettings, "AltRemoveSkillsOnDelevel_N_Level", 10);
            ALT_USE_ILLEGAL_SKILLS = getBooleanProperty(altSettings, "AltUseIllegalSkills", false);
            ALT_USE_BOW_REUSE_MODIFIER = getBooleanProperty(altSettings, "AltUseBowReuseModifier", true);
            ALLOW_CH_DOOR_OPEN_ON_CLICK = getBooleanProperty(altSettings, "AllowChDoorOpenOnClick", true);
            ALT_CH_ALL_BUFFS = getBooleanProperty(altSettings, "AltChAllBuffs", false);
            ALT_CH_ALLOW_1H_BUFFS = getBooleanProperty(altSettings, "AltChAllowHourBuff", false);
            ALT_CH_SIMPLE_DIALOG = getBooleanProperty(altSettings, "AltChSimpleDialog", false);
            SIEGE_OPERATE_DOORS = getBooleanProperty(altSettings, "SiegeOperateDoors", true);
            SIEGE_OPERATE_DOORS_LORD_ONLY = getBooleanProperty(altSettings, "SiegeOperateDoorsLordOnly", true);

            AUGMENTATION_NG_SKILL_CHANCE = getIntProperty(altSettings, "AugmentationNGSkillChance", 15);
            AUGMENTATION_NG_GLOW_CHANCE = getIntProperty(altSettings, "AugmentationNGGlowChance", 0);
            AUGMENTATION_MID_SKILL_CHANCE = getIntProperty(altSettings, "AugmentationMidSkillChance", 30);
            AUGMENTATION_MID_GLOW_CHANCE = getIntProperty(altSettings, "AugmentationMidGlowChance", 40);
            AUGMENTATION_HIGH_SKILL_CHANCE = getIntProperty(altSettings, "AugmentationHighSkillChance", 45);
            AUGMENTATION_HIGH_GLOW_CHANCE = getIntProperty(altSettings, "AugmentationHighGlowChance", 70);
            AUGMENTATION_TOP_SKILL_CHANCE = getIntProperty(altSettings, "AugmentationTopSkillChance", 60);
            AUGMENTATION_TOP_GLOW_CHANCE = getIntProperty(altSettings, "AugmentationTopGlowChance", 100);
            AUGMENTATION_BASESTAT_CHANCE = getIntProperty(altSettings, "AugmentationBaseStatChance", 1);

            FS_TIME_ATTACK = getIntProperty(altSettings, "TimeOfAttack", 50);
            FS_TIME_COOLDOWN = getIntProperty(altSettings, "TimeOfCoolDown", 5);
            FS_TIME_ENTRY = getIntProperty(altSettings, "TimeOfEntry", 3);
            FS_TIME_WARMUP = getIntProperty(altSettings, "TimeOfWarmUp", 2);
            FS_PARTY_MEMBER_COUNT = getIntProperty(altSettings, "NumberOfNecessaryPartyMembers", 4);
            if (FS_TIME_ATTACK <= 0)
                FS_TIME_ATTACK = 50;
            if (FS_TIME_COOLDOWN <= 0)
                FS_TIME_COOLDOWN = 5;
            if (FS_TIME_ENTRY <= 0)
                FS_TIME_ENTRY = 3;
            if (FS_TIME_ENTRY <= 0)
                FS_TIME_ENTRY = 3;
            if (FS_TIME_ENTRY <= 0)
                FS_TIME_ENTRY = 3;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + ALT_SETTINGS_FILE + " File.");
        }
    }

    public static void loadDevelopersConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + DEVELOPER_FILE);
        try {
            final Properties devSettings = new Properties();
            final InputStream is =Config.class.getClassLoader().getResourceAsStream(DEVELOPER_FILE);
            devSettings.load(is);
            is.close();

            DEBUG = getBooleanProperty(devSettings, "Debug", false);
            DEBUG_MULTISELL = getBooleanProperty(devSettings, "DebugMultisell", false);
            SERVER_LIST_TESTSERVER = getBooleanProperty(devSettings, "TestServer", false);

            EVERYBODY_HAS_ADMIN_RIGHTS = getBooleanProperty(devSettings, "EverybodyHasAdminRights", false);

            SHIFT_BY = getIntProperty(devSettings, "ShiftBy", 12);
            SHIFT_BY_FOR_Z = getIntProperty(devSettings, "ShiftByForZ", 10);

            MOVE_PACKET_DELAY = getIntProperty(devSettings, "MovePacketDelay", 100);
            ATTACK_PACKET_DELAY = getIntProperty(devSettings, "AttackPacketDelay", 50);
            EQUIPM_PACKET_DELAY = getIntProperty(devSettings, "EquipmPacketDelay", 50);

            SERVER_LIST_BRACKET = getBooleanProperty(devSettings, "ServerListBrackets", false);
            SERVER_LIST_CLOCK = getBooleanProperty(devSettings, "ServerListClock", false);
            SERVER_GMONLY = getBooleanProperty(devSettings, "ServerGMOnly", false);

            THREAD_P_GENERAL = getIntProperty(devSettings, "ThreadPoolSizeGeneral", 15);
            THREAD_P_MOVE = getIntProperty(devSettings, "ThreadPoolSizeMove", 25);
            THREAD_P_EFFECTS = getIntProperty(devSettings, "ThreadPoolSizeEffects", 10);
            NPC_AI_MAX_THREAD = getIntProperty(devSettings, "NpcAiMaxThread", 10);
            PLAYER_AI_MAX_THREAD = getIntProperty(devSettings, "PlayerAiMaxThread", 20);

            GENERAL_PACKET_THREAD_CORE_SIZE = getIntProperty(devSettings, "GeneralPacketThreadCoreSize", 4);
            URGENT_PACKET_THREAD_CORE_SIZE = getIntProperty(devSettings, "UrgentPacketThreadCoreSize", 2);

            DEADLOCKCHECK_INTERVAL = getIntProperty(devSettings, "DeadLockCheck", 10000);

            DONTLOADSPAWN = getBooleanProperty(devSettings, "StartWhisoutSpawn", false);
            DONTLOADQUEST = getBooleanProperty(devSettings, "StartWhisoutQuest", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + DEVELOPER_FILE + " File.");
        }
    }

    public static void loadChatConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + CHAT_FILE);
        try {
            final Properties chatSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(CHAT_FILE);
            chatSettings.load(is);
            is.close();

            GLOBAL_CHAT = getIntProperty(chatSettings, "GlobalChat", 0);
            GLOBAL_TRADE_CHAT = getIntProperty(chatSettings, "GlobalTradeChat", 0);
            SHOUT_CHAT_MODE = getIntProperty(chatSettings, "ShoutChatMode", 1);
            TRADE_CHAT_MODE = getIntProperty(chatSettings, "TradeChatMode", 1);

            ALL_LEVEL = getIntProperty(chatSettings, "ALL_LEVEL", 0);
            TRADE_LEVEL = getIntProperty(chatSettings, "TRADE_LEVEL", 0);
            SHOUT_LEVEL = getIntProperty(chatSettings, "SHOUT_LEVEL", 0);
            PRIVATE_LEVEL = getIntProperty(chatSettings, "PRIVATE_LEVEL", 0);

            ALLOW_SPECIAL_COMMANDS = getBooleanProperty(chatSettings, "AllowSpecialCommands", false);
            MAT_BANCHAT = getBooleanProperty(chatSettings, "MAT_BANCHAT", false);
            MAT_KARMA = getIntProperty(chatSettings, "MAT_KARMA", 0);
            BAN_CHANNEL = getProperty(chatSettings, "MAT_BAN_CHANNEL", "0");
            MAT_BAN_COUNT_CHANNELS = 1;
            for (final String id : BAN_CHANNEL.split(",")) {
                BAN_CHANNEL_LIST[MAT_BAN_COUNT_CHANNELS] = Integer.parseInt(id);
                MAT_BAN_COUNT_CHANNELS++;
            }
            MAT_REPLACE = getBooleanProperty(chatSettings, "MAT_REPLACE", false);
            MAT_REPLACE_STRING = getProperty(chatSettings, "MAT_REPLACE_STRING", "[censored]");
            MAT_ANNOUNCE = getBooleanProperty(chatSettings, "MAT_ANNOUNCE", true);
            MAT_ANNOUNCE_NICK = getBooleanProperty(chatSettings, "MAT_ANNOUNCE_NICK", true);
            UNCHATBANTIME = getIntProperty(chatSettings, "Timer_to_UnBan", 30);
            SAYTOSHOUTLIMIT = getIntProperty(chatSettings, "SayToShoutLimit", 5);
            SAYTOSHOUTLIMIT2 = SAYTOSHOUTLIMIT * 1000;
            SAYTOTRADELIMIT = getIntProperty(chatSettings, "SayToTradeLimit", 5);
            SAYTOTRADELIMIT2 = SAYTOTRADELIMIT * 1000;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CHAT_FILE + " File.");
        }
    }

    public static void loadChampionConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + CHAMPION_FILE);
        try {
            final Properties champSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(CHAMPION_FILE);
            champSettings.load(is);
            is.close();

            CHAMPION_CHANCE1 = getDoubleProperty(champSettings, "AltChampionChance1", 0.);
            CHAMPION_CHANCE2 = getDoubleProperty(champSettings, "AltChampionChance2", 0.);
            CHAMPION_CAN_BE_AGGRO = getBooleanProperty(champSettings, "AltChampionAggro", false);
            CHAMPION_CAN_BE_SOCIAL = getBooleanProperty(champSettings, "AltChampionSocial", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CHAMPION_FILE + " File.");
        }
    }

    public static void loadServicesConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + SERVICES_FILE);
        try {
            final Properties servicesSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(SERVICES_FILE);
            servicesSettings.load(is);
            is.close();

            ALLOW_CLASS_MASTERS = getProperty(servicesSettings, "AllowClassMasters", "0");

            if (ALLOW_CLASS_MASTERS.length() != 0 && !ALLOW_CLASS_MASTERS.equals("0"))
                for (final String id : ALLOW_CLASS_MASTERS.split(","))
                    ALLOW_CLASS_MASTERS_LIST.add(Integer.parseInt(id));
            // Пока что не работает так как нужно, проблема с кодировкой конфигов =\
            CLASS_MASTERS_SAY = getProperty(servicesSettings, "ClassMastersSay", "Поздравляем, %player% с получением профессии: %prof%!");
            ALLOW_CLASS_MASTERS_ON_LEVEL_UP = getBooleanProperty(servicesSettings, "ClassMastersOnLevelUp", false);
            CLASS_MASTERS_PRICE = getProperty(servicesSettings, "ClassMastersPrice", "0,0,0");
            if (CLASS_MASTERS_PRICE.length() >= 5) {
                int level = 1;
                for (final String id : CLASS_MASTERS_PRICE.split(",")) {
                    CLASS_MASTERS_PRICE_LIST[level] = Integer.parseInt(id);
                    level++;
                }
            }
            CLASS_MASTERS_PRICE_ITEM = getIntProperty(servicesSettings, "ClassMastersPriceItem", 57);

            SERVICES_CHANGE_NICK_ENABLED = getBooleanProperty(servicesSettings, "NickChangeEnabled", false);
            SERVICES_CHANGE_NICK_PRICE = getIntProperty(servicesSettings, "NickChangePrice", 100);
            SERVICES_CHANGE_NICK_ITEM = getIntProperty(servicesSettings, "NickChangeItem", 4037);

            SERVICES_CHANGE_PET_NAME_ENABLED = getBooleanProperty(servicesSettings, "PetNameChangeEnabled", false);
            SERVICES_CHANGE_PET_NAME_PRICE = getIntProperty(servicesSettings, "PetNameChangePrice", 100);
            SERVICES_CHANGE_PET_NAME_ITEM = getIntProperty(servicesSettings, "PetNameChangeItem", 4037);

            SERVICES_EXCHANGE_BABY_PET_ENABLED = getBooleanProperty(servicesSettings, "BabyPetExchangeEnabled", false);
            SERVICES_EXCHANGE_BABY_PET_PRICE = getIntProperty(servicesSettings, "BabyPetExchangePrice", 100);
            SERVICES_EXCHANGE_BABY_PET_ITEM = getIntProperty(servicesSettings, "BabyPetExchangeItem", 4037);

            SERVICES_CHANGE_SEX_ENABLED = getBooleanProperty(servicesSettings, "SexChangeEnabled", false);
            SERVICES_CHANGE_SEX_PRICE = getIntProperty(servicesSettings, "SexChangePrice", 100);
            SERVICES_CHANGE_SEX_ITEM = getIntProperty(servicesSettings, "SexChangeItem", 4037);

            SERVICES_CLASSMASTERS_BASIC_SHOP = getBooleanProperty(servicesSettings, "CM_BasicShop", false);
            SERVICES_CLASSMASTERS_COL_SHOP = getBooleanProperty(servicesSettings, "CM_CoLShop", false);

            SERVICES_CHANGE_NICK_COLOR_ENABLED = getBooleanProperty(servicesSettings, "NickColorChangeEnabled", false);
            SERVICES_CHANGE_NICK_COLOR_PRICE = getIntProperty(servicesSettings, "NickColorChangePrice", 100);
            SERVICES_CHANGE_NICK_COLOR_ITEM = getIntProperty(servicesSettings, "NickColorChangeItem", 4037);
            SERVICES_CHANGE_NICK_COLOR_LIST = getProperty(servicesSettings, "NickColorChangeList", "00FF00").split(";");

            SERVICES_RATE_BONUS_ENABLED = getBooleanProperty(servicesSettings, "RateBonusEnabled", false);
            SERVICES_RATE_BONUS_PRICE = getIntArray(servicesSettings, "RateBonusPrice", new int[]{1500});
            SERVICES_RATE_BONUS_ITEM = getIntArray(servicesSettings, "RateBonusItem", new int[]{4037});
            SERVICES_RATE_BONUS_VALUE = getFloatArray(servicesSettings, "RateBonusValue", new float[]{(float) 1.25});
            SERVICES_RATE_BONUS_DAYS = getIntArray(servicesSettings, "RateBonusTime", new int[]{30});
            SERVICES_RATE_BONUS_LUCK_EFFECT = getFloatProperty(servicesSettings, "RateBonusLuckEffect", 1.0);

            SERVICES_RATE_SPECIAL_ITEM_ID = getIntProperty(servicesSettings, "BONUS_ITEM", 4037);
            SERVICES_RATE_SPECIAL_ITEM_COUNT = getIntProperty(servicesSettings, "BONUS_PRICE", 50);
            SERVICES_RATE_SPECIAL_RATE = getIntProperty(servicesSettings, "BONUS_RATE", 50);
            SERVICES_RATE_SPECIAL_DAYS = getIntProperty(servicesSettings, "BONUS_DAYS", 7);
            SERVICES_RATE_SPECIAL_ENABLED = getBooleanProperty(servicesSettings, "BONUS_ENABLED", false);

            SERVICES_NOBLESS_SELL_ENABLED = getBooleanProperty(servicesSettings, "NoblessSellEnabled", false);
            SERVICES_NOBLESS_SELL_PRICE = getIntProperty(servicesSettings, "NoblessSellPrice", 1000);
            SERVICES_NOBLESS_SELL_ITEM = getIntProperty(servicesSettings, "NoblessSellItem", 4037);

            SERVICES_EXPAND_INVENTORY_ENABLED = getBooleanProperty(servicesSettings, "ExpandInventoryEnabled", false);
            SERVICES_EXPAND_INVENTORY_PRICE = getIntProperty(servicesSettings, "ExpandInventoryPrice", 1000);
            SERVICES_EXPAND_INVENTORY_ITEM = getIntProperty(servicesSettings, "ExpandInventoryItem", 4037);
            SERVICES_EXPAND_INVENTORY_MAX = getIntProperty(servicesSettings, "ExpandInventoryMax", 250);

            SERVICES_EXPAND_WAREHOUSE_ENABLED = getBooleanProperty(servicesSettings, "ExpandWarehouseEnabled", false);
            SERVICES_EXPAND_WAREHOUSE_PRICE = getIntProperty(servicesSettings, "ExpandWarehousePrice", 1000);
            SERVICES_EXPAND_WAREHOUSE_ITEM = getIntProperty(servicesSettings, "ExpandWarehouseItem", 4037);

            SERVICES_EXPAND_CWH_ENABLED = getBooleanProperty(servicesSettings, "ExpandCWHEnabled", false);
            SERVICES_EXPAND_CWH_PRICE = getIntProperty(servicesSettings, "ExpandCWHPrice", 1000);
            SERVICES_EXPAND_CWH_ITEM = getIntProperty(servicesSettings, "ExpandCWHItem", 4037);

            SERVICES_CHANGE_CLAN_ENABLED = getBooleanProperty(servicesSettings, "ClanChangeEnabled", false);
            SERVICES_CHANGE_CLAN_PRICE = getIntProperty(servicesSettings, "ClanChangePrice", 100);
            SERVICES_CHANGE_CLAN_ITEM = getIntProperty(servicesSettings, "ClanChangeItem", 4037);

            SERVICES_WINDOW_ENABLED = getBooleanProperty(servicesSettings, "WindowEnabled", false);
            SERVICES_WINDOW_PRICE = getIntProperty(servicesSettings, "WindowPrice", 1000);
            SERVICES_WINDOW_ITEM = getIntProperty(servicesSettings, "WindowItem", 4037);
            SERVICES_WINDOW_DAYS = getIntProperty(servicesSettings, "WindowDays", 7);
            SERVICES_WINDOW_MAX = getIntProperty(servicesSettings, "WindowMax", 3);

            SERVICES_DONATE = getBooleanProperty(servicesSettings, "Donate", false);

            SERVICES_HOW_TO_GET_COL = getBooleanProperty(servicesSettings, "HowToGetCoL", false);

            SERVICES_SELLPETS = getProperty(servicesSettings, "SellPets", "");

            SERVICES_OFFLINE_TRADE_ALLOW = getBooleanProperty(servicesSettings, "AllowOfflineTrade", false);
            SERVICES_OFFLINE_TRADE_MIN_LEVEL = getIntProperty(servicesSettings, "OfflineMinLevel", 0);
            SERVICES_OFFLINE_TRADE_NAME_COLOR = getIntHexProperty(servicesSettings, "OfflineTradeNameColor", 0xB0FFFF);
            SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING = getBooleanProperty(servicesSettings, "KickOfflineNotTrading", true);
            SERVICES_OFFLINE_TRADE_PRICE_ITEM = getIntProperty(servicesSettings, "OfflineTradePriceItem", 0);
            SERVICES_OFFLINE_TRADE_PRICE = getIntProperty(servicesSettings, "OfflineTradePrice", 0);
            SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK = getLongProperty(servicesSettings, "OfflineTradeDaysToKick", 14) * 60 * 60 * 24;
            SERVICES_OFFLINE_TRADE_RESTORE_AFTER_RESTART = getBooleanProperty(servicesSettings, "OfflineRestoreAfterRestart", true);

            SERVICES_NO_TRADE_ONLY_OFFLINE = getBooleanProperty(servicesSettings, "NoTradeOnlyOffline", false);
            SERVICES_TRADE_TAX = getFloatProperty(servicesSettings, "TradeTax", 0.0);
            SERVICES_OFFSHORE_TRADE_TAX = getFloatProperty(servicesSettings, "OffshoreTradeTax", 0.0);
            SERVICES_TRADE_TAX_ONLY_OFFLINE = getBooleanProperty(servicesSettings, "TradeTaxOnlyOffline", false);
            SERVICES_OFFSHORE_NO_CASTLE_TAX = getBooleanProperty(servicesSettings, "NoCastleTaxInOffshore", false);
            SERVICES_TRADE_RADIUS = getIntProperty(servicesSettings, "TradeRadius", 30);
            SERVICES_TRADE_ONLY_FAR = getBooleanProperty(servicesSettings, "TradeOnlyFar", false);

            SERVICES_GIRAN_HARBOR_ENABLED = getBooleanProperty(servicesSettings, "GiranHarborZone", false);
            SERVICES_LOCK_ACCOUNT_IP = getBooleanProperty(servicesSettings, "LockAccountIP", false);
            SERVICES_CHANGE_PASSWORD = getBooleanProperty(servicesSettings, "ChangePassword", false);

            SERVICES_HERO_WEAPONS_EXCHANGE_ENABLED = getBooleanProperty(servicesSettings, "HeroWeaponsExchangeEnabled", false);

            ONLINE_PLAYERS_ANNOUNCE_INTERVAL = getIntProperty(servicesSettings, "OnlineAnnounceInterval", 900);

            SERVICES_BUFFER_MIN_LVL = getIntProperty(servicesSettings, "BufferMinLvl", 1);
            SERVICES_BUFFER_MAX_LVL = getIntProperty(servicesSettings, "BufferMaxLvl", 90);
            SERVICES_BUFFER_PRICE = getIntProperty(servicesSettings, "BufferPrice", 5000);
            SERVICES_BUFFER_SIEGE = getBooleanProperty(servicesSettings, "BufferSiege", false);
            SERVICES_BUFFER_ENABLED = getBooleanProperty(servicesSettings, "BufferEnabled", false);
            SERVICES_BUFFER_PET_ENABLED = getBooleanProperty(servicesSettings, "BufferPetEnabled", false);

        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + SERVICES_FILE + " File.");
        }
    }

    public static void loadPvPConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + PVP_CONFIG_FILE);
        try {
            final Properties pvpSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(PVP_CONFIG_FILE);
            pvpSettings.load(is);
            is.close();

			/* KARMA SYSTEM */
            KARMA_MIN_KARMA = getIntProperty(pvpSettings, "MinKarma", 240);
            KARMA_SP_DIVIDER = getIntProperty(pvpSettings, "SPDivider", 7);
            KARMA_LOST_BASE = getIntProperty(pvpSettings, "BaseKarmaLost", 0);

            KARMA_DROP_GM = getBooleanProperty(pvpSettings, "CanGMDropEquipment", false);
            KARMA_NEEDED_TO_DROP = getBooleanProperty(pvpSettings, "KarmaNeededToDrop", true);

            KARMA_NONDROPPABLE_ITEMS = getProperty(pvpSettings, "ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,3500,3501,3502,4422,4423,4424,2375,6648,6649,6650,6842,6834,6835,6836,6837,6838,6839,6840,5575,7694,6841,8181");

            KARMA_DROP_ITEM_LIMIT = getIntProperty(pvpSettings, "MaxItemsDroppable", 10);
            MIN_PK_TO_ITEMS_DROP = getIntProperty(pvpSettings, "MinPKToDropItems", 5);

            KARMA_RANDOM_DROP_LOCATION_LIMIT = getIntProperty(pvpSettings, "MaxDropThrowDistance", 70);

            KARMA_DROPCHANCE_MINIMUM = getIntProperty(pvpSettings, "ChanceOfDropMinimum", 1);
            KARMA_DROPCHANCE_MULTIPLIER = getIntProperty(pvpSettings, "ChanceOfDropMultiplier", 1);
            KARMA_DROPCHANCE_EQUIPMENT = getIntProperty(pvpSettings, "ChanceOfDropEquipped", 20);
            KARMA_DROPCHANCE_EQUIPPED_WEAPON = getIntProperty(pvpSettings, "ChanceOfDropEquippedWeapon", 25);

            KARMA_LIST_NONDROPPABLE_ITEMS = new ArrayList<Integer>();
            for (final String id : KARMA_NONDROPPABLE_ITEMS.split(","))
                KARMA_LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id));

            PVP_TIME = getIntProperty(pvpSettings, "PvPTime", 40000);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + PVP_CONFIG_FILE + " File.");
        }
    }

    public static void loadAIConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + AI_CONFIG_FILE);
        try {
            final Properties aiSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(AI_CONFIG_FILE);
            aiSettings.load(is);
            is.close();

            AI_TASK_DELAY = getIntProperty(aiSettings, "AiTaskDelay", 1000);

            RND_WALK = getBooleanProperty(aiSettings, "RndWalk", true);
            RND_WALK_RATE = getIntProperty(aiSettings, "RndWalkRate", 1);
            RND_ANIMATION_RATE = getIntProperty(aiSettings, "RndAnimationRate", 2);

            AGGRO_CHECK_INTERVAL = getIntProperty(aiSettings, "AggroCheckInterval", 250);

            MAX_DRIFT_RANGE = getIntProperty(aiSettings, "MaxDriftRange", 100);
            MAX_PURSUE_RANGE = getIntProperty(aiSettings, "MaxPursueRange", 2000);
            MAX_PURSUE_RANGE_RAID = getIntProperty(aiSettings, "MaxPursueRangeRaid", 5000);

            СHANCE_SKILLS = getIntProperty(aiSettings, "СhanceSkills", 80);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + AI_CONFIG_FILE + " File.");
        }
    }

    public static void loadGeodataConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + GEODATA_CONFIG_FILE);
        try {
            final Properties geodataSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(GEODATA_CONFIG_FILE);
            geodataSettings.load(is);
            is.close();

            GEOFILES_PATTERN = getProperty(geodataSettings, "GeoFilesPattern", "(\\d{2}_\\d{2})\\.l2j");
            GEODATA_ENABLE = getBooleanProperty(geodataSettings, "EnableGeodata", true);
            PATH_CLEAN = getBooleanProperty(geodataSettings, "PathClean", true);
            ALLOW_DOORS = getBooleanProperty(geodataSettings, "AllowDoors", false);
            SIMPLE_PATHFIND_FOR_MOBS = getBooleanProperty(geodataSettings, "SimplePathFindForMobs", true);
            ALLOW_FALL_FROM_WALLS = getBooleanProperty(geodataSettings, "AllowFallFromWalls", false);
            ALLOW_KEYBOARD_MOVE = getBooleanProperty(geodataSettings, "AllowMoveWithKeyboard", true);
            PATHFIND_BOOST = getIntProperty(geodataSettings, "PathFindBoost", 2);
            MAX_Z_DIFF = getIntProperty(geodataSettings, "MaxZDiff", 32);
            COMPACT_GEO = getBooleanProperty(geodataSettings, "CompactGeoData", false);
            SHOW_GEO_FILE = getBooleanProperty(geodataSettings, "ShowGeodataLoadFile", false);
            WEIGHT0 = getDoubleProperty(geodataSettings, "Weight0", 0.5D);
            WEIGHT1 = getIntProperty(geodataSettings, "Weight1", 8);
            WEIGHT2 = getIntProperty(geodataSettings, "Weight2", 4);
            PathFindBuffers.initBuffers(getProperty(geodataSettings, "PathFindBuffers", "8x100;8x128;8x192;4x256;2x320;2x384;1x500"));
            DIV_BY = getIntProperty(geodataSettings, "DivBy", 2048);
            DIV_BY_FOR_Z = getIntProperty(geodataSettings, "DivByForZ", 1024);
            VERTICAL_SPLIT_REGIONS = getProperty(geodataSettings, "VerticalSplitRegions", "23_18");

        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + GEODATA_CONFIG_FILE + " File.");
        }
    }

    public static void loadHexidFile() {
        if (DEBUG)
            _log.info("[Loading]: " + HEXID_FILE);
        try {
            final Properties Settings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(HEXID_FILE);
            Settings.load(is);
            is.close();
            HEX_ID = new BigInteger(getProperty(Settings, "HexID"), 16).toByteArray();
        } catch (final Exception e) {
            _log.info(HEXID_FILE + " Not Found");
        }
    }

    public static void loadEventConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + EVENTS);
        try {
            final Properties EventSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(EVENTS);
            EventSettings.load(is);
            is.close();

            DM_Id = getIntProperty(EventSettings, "DM_Id", 4037);
            DM_Count = getIntProperty(EventSettings, "DM_Count", 1);
            DM_Interval = getIntProperty(EventSettings, "DM_Interval", 60);
            DM_MinPlayer = getIntProperty(EventSettings, "DM_MinPlayer", 10);
            DM_MaxPlayer = getIntProperty(EventSettings, "DM_MaxPlayer", 100);
            DM_MinLevel = getIntProperty(EventSettings, "DM_MinLevel", 1);
            DM_MaxLevel = getIntProperty(EventSettings, "DM_MaxLevel", 85);

            EVENT_LastHeroItemID = getIntProperty(EventSettings, "LastHero_bonus_id", 57);
            EVENT_LastHeroItemCOUNT = getIntProperty(EventSettings, "LastHero_bonus_count", 5000);
            EVENT_LastHeroTime = getIntProperty(EventSettings, "LastHero_time", 3);
            EVENT_LastHeroRate = getBooleanProperty(EventSettings, "LastHero_rate", true);
            EVENT_LastHeroChanceToStart = getIntProperty(EventSettings, "LastHero_ChanceToStart", 5);
            EVENT_LastHeroQuestion = getBooleanProperty(EventSettings, "LastHero_Question", true);
            EVENT_LastHeroMaxCountPlayers = getIntProperty(EventSettings, "LastHero_MaxCountPlayers", 50);

            EVENT_TvTItemID = getIntProperty(EventSettings, "TvT_bonus_id", 57);
            EVENT_TvTItemCOUNT = getIntProperty(EventSettings, "TvT_bonus_count", 5000);
            EVENT_TvTTime = getIntProperty(EventSettings, "TvT_time", 3);
            EVENT_TvTRate = getBooleanProperty(EventSettings, "TvT_rate", true);
            EVENT_TvTChanceToStart = getIntProperty(EventSettings, "TvT_ChanceToStart", 5);
            EVENT_TvTQuestion = getBooleanProperty(EventSettings, "TvT_Question", true);
            EVENT_TvTMaxCountPlayers = getIntProperty(EventSettings, "TvT_MaxCountPlayers", 50);

            ENCHANT_MASTER_ENABLED = getBooleanProperty(EventSettings, "EnchantMasterEnabled", false);

            EVENT_TFH_POLLEN_CHANCE = getFloatProperty(EventSettings, "TFH_POLLEN_CHANCE", 5.);

            EVENT_GLITTMEDAL_NORMAL_CHANCE = getFloatProperty(EventSettings, "MEDAL_CHANCE", 10.);
            EVENT_GLITTMEDAL_GLIT_CHANCE = getFloatProperty(EventSettings, "GLITTMEDAL_CHANCE", 0.1);

            EVENT_L2DAY_LETTER_CHANCE = getFloatProperty(EventSettings, "L2DAY_LETTER_CHANCE", 1.);
            EVENT_FIVE_YEARS_LINEAGE_LETTER_CHANCE = getFloatProperty(EventSettings, "FIVE_YEARS_LINEAGE_LETTER_CHANCE", 1.);
            EVENT_CHANGE_OF_HEART_CHANCE = getFloatProperty(EventSettings, "EVENT_CHANGE_OF_HEART_CHANCE", 5.);

            ENCHANT_WEAPON_NUMBER = getProperty(EventSettings, "EnchWeapNumber", "");
            if (ENCHANT_WEAPON_NUMBER != "") {
                int num = 0;
                for (final String id : ENCHANT_WEAPON_NUMBER.split(",")) {
                    ENCHANT_WEAPON_NUMBER_LIST[num] = Integer.parseInt(id);
                    num++;
                }
            }

            ENCHANT_WEAPON_COAL = getProperty(EventSettings, "EnchWeapCoal", "");
            if (ENCHANT_WEAPON_COAL != "") {
                int num = 0;
                for (final String id : ENCHANT_WEAPON_COAL.split(",")) {
                    ENCHANT_WEAPON_COAL_LIST[num] = Integer.parseInt(id);
                    num++;
                }
            }

            ENCHANT_ARMOR_PRICE = getIntProperty(EventSettings, "EnchArmorPrice", 1);
            DONATE_ID_PRICE = getIntProperty(EventSettings, "DonateIdPrice", 4037);
            ENCHANT_JEWEL_PRICE = getIntProperty(EventSettings, "EnchJewPrice", 1);
            NOBLE_PRICE = getIntProperty(EventSettings, "NoblePrice", 1);

            ENCHANT_MASTER_WEAPON_PRICE = getIntProperty(EventSettings, "EnchMasterWeapPrice", 1);
            ENCHANT_MASTER_ARMOR_PRICE = getIntProperty(EventSettings, "EnchMasterArmorPrice", 1);
            ENCHANT_MASTER_PRICE_ID = getIntProperty(EventSettings, "PriceEnchantMasterId", 57);
            ENCHANT_MASTER_MAX = getIntProperty(EventSettings, "EnchMasterMaxEnch", 25);
            ENCHANT_MASTER_JEWEL_PRICE = getIntProperty(EventSettings, "EnchMasterJewPrice", 1);

        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + EVENTS + " File.");
        }
    }

    public static void loadOlympConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + OLYMPIAD);
        try {
            final Properties olympSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(OLYMPIAD);
            olympSettings.load(is);
            is.close();

            ENABLE_OLYMPIAD = getBooleanProperty(olympSettings, "EnableOlympiad", false);
            ENABLE_OLYMPIAD_SPECTATING = getBooleanProperty(olympSettings, "EnableOlympiadSpectating", true);
            ALT_OLY_START_TIME = getIntProperty(olympSettings, "AltOlyStartTime", 18);
            ALT_OLY_MIN = getIntProperty(olympSettings, "AltOlyMin", 00);
            ALT_OLY_CPERIOD = getLongProperty(olympSettings, "AltOlyCPeriod", 21600000);
            ALT_OLY_BATTLE = getLongProperty(olympSettings, "AltOlyBattle", 360000);
            ALT_OLY_BWAIT = getLongProperty(olympSettings, "AltOlyBWait", 600000);
            ALT_OLY_IWAIT = getLongProperty(olympSettings, "AltOlyIWait", 300000);
            ALT_OLY_WPERIOD = getLongProperty(olympSettings, "AltOlyWPeriod", 604800000);
            ALT_OLY_VPERIOD = getLongProperty(olympSettings, "AltOlyVPeriod", 43200000);
            CLASS_GAME_MIN = getIntProperty(olympSettings, "ClassGameMin", 5);
            NONCLASS_GAME_MIN = getIntProperty(olympSettings, "NonClassGameMin", 9);
            ADD_ALREADY_STARTED_GAMES = getBooleanProperty(olympSettings, "AddAlreadyStartedGames", true);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + EVENTS + " File.");
        }
    }

    public static void loadLoginServerConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + LOGIN_CONFIGURATION_FILE);
        try {
            final Properties serverSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(LOGIN_CONFIGURATION_FILE);
            serverSettings.load(is);
            is.close();

            loadAntiFlood(serverSettings);
            LOGIN_HOST = getProperty(serverSettings, "LoginserverHostname", "127.0.0.1");
            GAME_SERVER_LOGIN_PORT = getIntProperty(serverSettings, "LoginPort", 9013);
            GAME_SERVER_LOGIN_HOST = getProperty(serverSettings, "LoginHost", "127.0.0.1");
            PORT_LOGIN = getIntProperty(serverSettings, "LoginserverPort", 2106);

            DEFAULT_PASSWORD_ENCODING = getProperty(serverSettings, "DefaultPasswordEncoding", "Whirlpool");
            LEGACY_PASSWORD_ENCODING = getProperty(serverSettings, "LegacyPasswordEncoding", "SHA1;DES");

            LOGIN_BLOWFISH_KEYS = getIntProperty(serverSettings, "BlowFishKeys", 20);
            LOGIN_RSA_KEYPAIRS = getIntProperty(serverSettings, "RSAKeyPairs", 10);

            COMBO_MODE = getBooleanProperty(serverSettings, "ComboMode", false);
            LOGIN_DEBUG = getBooleanProperty(serverSettings, "Debug", false);

            LOGIN_TRY_BEFORE_BAN = getIntProperty(serverSettings, "LoginTryBeforeBan", 10);
            LOGIN_PING = getBooleanProperty(serverSettings, "PingServer", true);
            LOGIN_PING_TIME = getIntProperty(serverSettings, "WaitPingTime", 5);

            DATABASE_DRIVER = getProperty(serverSettings, "Driver", "com.mysql.jdbc.Driver");
            DATABASE_URL = getProperty(serverSettings, "URL", "jdbc:mysql://localhost/l2d");
            DATABASE_LOGIN = getProperty(serverSettings, "Login", "root");
            DATABASE_PASSWORD = getProperty(serverSettings, "Password", "");
            DATABASE_MAX_CONNECTIONS = getIntProperty(serverSettings, "MaximumDbConnections", 10);
            DATABASE_MAX_IDLE_TIMEOUT = getIntProperty(serverSettings, "MaxIdleConnectionTimeout", 600);
            DATABASE_IDLE_TEST_PERIOD = getIntProperty(serverSettings, "IdleConnectionTestPeriod", 60);

            SHOW_LICENCE = getBooleanProperty(serverSettings, "ShowLicence", true);
            SQL_LOG = getBooleanProperty(serverSettings, "SqlLog", false);
            AUTO_CREATE_ACCOUNTS = getBooleanProperty(serverSettings, "AutoCreateAccounts", true);
            IP_UPDATE_TIME = getIntProperty(serverSettings, "IpUpdateTime", 15);
            ANAME_TEMPLATE = getProperty(serverSettings, "AnameTemplate", "[A-Za-z0-9]{3,14}");
            APASSWD_TEMPLATE = getProperty(serverSettings, "ApasswdTemplate", "[A-Za-z0-9]{5,16}");
            LOGIN_GG_CHECK = getBooleanProperty(serverSettings, "GGCheck", true);
            LRESTART_TIME = getIntProperty(serverSettings, "AutoRestart", -1);

            IPTABLES_COMMAND = getProperty(serverSettings, "IptablesCommand", "/sbin/iptables -I INPUT -p tcp --dport 7777 -s $ip -j ACCEPT");
            ENABLE_DDOS_PROTECTION_SYSTEM = getBooleanProperty(serverSettings, "EnableDdosProtectionSystem", false);
            ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM = getBooleanProperty(serverSettings, "EnableDebugDdosProtectionSystem", false);

            final String internalIpList = getProperty(serverSettings, "InternalIpList", "127.0.0.1,192.168.0.0-192.168.255.255,10.0.0.0-10.255.255.255,172.16.0.0-172.16.31.255");
            if (internalIpList.startsWith("NetList@")) {
                INTERNAL_NETLIST = new NetList();
                INTERNAL_NETLIST.LoadFromFile(internalIpList.replaceFirst("NetList@", ""));
                _log.info("Loaded " + INTERNAL_NETLIST.NetsCount() + " Internal Nets");
            } else {
                INTERNAL_IP = new FastList<String>();
                INTERNAL_IP.addAll(Arrays.asList(internalIpList.split(",")));
            }

        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + LOGIN_CONFIGURATION_FILE + " File.");
        }
    }

    public static void loadLoginTelnetConfig() {
        if (DEBUG)
            _log.info("[Loading]: " + LOGIN_TELNET_FILE);
        try {
            final Properties telnetSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(LOGIN_TELNET_FILE);
            telnetSettings.load(is);
            is.close();

            IS_LOGIN_TELNET_ENABLED = getBooleanProperty(telnetSettings, "EnableTelnet", false);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + TELNET_FILE + " File.");
        }
    }

    public static boolean allowL2Walker(final L2Player player) {
        return ALLOW_L2WALKER_CLIENT == L2WalkerAllowed.True || player.getPlayerAccess().AllowWalker || ALLOW_L2WALKER_CLIENT == L2WalkerAllowed.Peace && player.isInZone(peace_zone) && player.getLevel() <= MAX_L2WALKER_LEVEL;
    }

    // it has no instancies
    private Config() {
    }

    public static void saveHexid(final String string) {
        saveHexid(string, HEXID_FILE);
    }

    public static void saveHexid(final String string, final String fileName) {
        try {
            final Properties hexSetting = new Properties();
            final File file = new File(fileName);
            file.createNewFile();
            final OutputStream out = new FileOutputStream(file);
            hexSetting.setProperty("HexID", string);
            hexSetting.store(out, "the hexID to auth into login");
            out.close();
        } catch (final Exception e) {
            System.out.println("Failed to save hex id to " + fileName + " File.");
            e.printStackTrace();
        }
    }

    public static void abuseLoad() {
        if (DEBUG)
            _log.info("[Loading]: " + MAT_CONFIG_FILE);

        MAT_LIST = new ArrayList<String>();

        LineNumberReader lnr = null;
        try {
            int i = 0;
            String line;

            lnr = new LineNumberReader(new InputStreamReader(Config.class.getClassLoader().getResourceAsStream(MAT_CONFIG_FILE), "UTF-8"));

            while ((line = lnr.readLine()) != null) {
                final StringTokenizer st = new StringTokenizer(line, "\n\r");
                if (st.hasMoreTokens()) {
                    final String Mat = st.nextToken();
                    MAT_LIST.add(Mat);

                    i++;
                }
            }
            if (DEBUG)
                _log.info("Abuse: Loaded " + i + " abuse words.");
        } catch (final IOException e1) {
            _log.warning("Error reading abuse: " + e1);
        } finally {
            try {
                if (lnr != null)
                    lnr.close();
            } catch (final Exception e2) {
                // nothing
            }
        }
    }

    private static void ipsLoad() {
        if (DEBUG)
            _log.info("[Loading]: " + ADV_IP_FILE);
        try {
            final Properties ipsSettings = new Properties();
            final InputStream is = Config.class.getClassLoader().getResourceAsStream(ADV_IP_FILE);
            ipsSettings.load(is);
            is.close();

            String NetMask;
            String ip;
            for (int i = 0; i < ipsSettings.size() / 2; i++) {
                NetMask = getProperty(ipsSettings, "NetMask" + (i + 1));
                ip = getProperty(ipsSettings, "IPAdress" + (i + 1));
                for (final String mask : NetMask.split(",")) {
                    final AdvIP advip = new AdvIP();
                    advip.ipadress = ip;
                    advip.ipmask = mask.split("/")[0];
                    advip.bitmask = mask.split("/")[1];
                    GAMEIPS.add(advip);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + ADV_IP_FILE + " File.");
        }
    }

    public static void loadGMAccess() {
        if (DEBUG)
            _log.info("[Loading]: " + GM_PERSONAL_ACCESS_FILE);
        gmlist.clear();
        loadGMAccess(new File(Config.class.getClassLoader().getResource(GM_PERSONAL_ACCESS_FILE).getFile()));
        final File dir = new File(Config.class.getClassLoader().getResource("./config/GMAccess.d/").getFile());
        if (!dir.exists()) {
            _log.config("Dir " + dir.getAbsolutePath() + " not exists");
            return;
        }
        final File[] files = dir.listFiles();
        for (final File f : files)
            if (f.getName().endsWith(".xml"))
                loadGMAccess(f);
    }

    public static void loadGMAccess(final File file) {
        try {
            Field fld;
            // File file = new File(filename);
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            final Document doc = factory.newDocumentBuilder().parse(file);

            for (Node z = doc.getFirstChild(); z != null; z = z.getNextSibling())
                for (Node n = z.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (!n.getNodeName().equalsIgnoreCase("char"))
                        continue;

                    final PlayerAccess pa = new PlayerAccess();
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        final Class<? extends PlayerAccess> cls = pa.getClass();
                        final String node = d.getNodeName();

                        if (node.equalsIgnoreCase("#text"))
                            continue;
                        try {
                            fld = cls.getField(node);
                        } catch (final NoSuchFieldException e) {
                            _log.info("Not found desclarate ACCESS name: " + node + " in XML Player access Object");
                            continue;
                        }

                        if (fld.getType().getName().equalsIgnoreCase("boolean"))
                            fld.setBoolean(pa, Boolean.parseBoolean(d.getAttributes().getNamedItem("set").getNodeValue()));
                        else if (fld.getType().getName().equalsIgnoreCase("int"))
                            fld.setInt(pa, Integer.valueOf(d.getAttributes().getNamedItem("set").getNodeValue()));
                    }
                    gmlist.put(pa.PlayerID, pa);
                }
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + GM_PERSONAL_ACCESS_FILE + " File.");
        }
    }

    private static String getProperty(final Properties prop, final String name) {
        return prop.getProperty(name.trim(), null);
    }

    private static String getProperty(final Properties prop, final String name, final String _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : s;
    }

    private static int getIntProperty(final Properties prop, final String name, final int _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Integer.parseInt(s.trim());
    }

    private static int getIntHexProperty(final Properties prop, final String name, final int _default) {
        String s = getProperty(prop, name);
        if (s == null)
            return _default;
        s = s.trim();
        if (!s.startsWith("0x"))
            s = "0x" + s;
        return Integer.decode(s);
    }

    private static long getLongProperty(final Properties prop, final String name, final long _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Long.parseLong(s.trim());
    }

    private static byte getByteProperty(final Properties prop, final String name, final byte _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Byte.parseByte(s.trim());
    }

    private static byte getByteProperty(final Properties prop, final String name, final int _default) {
        return getByteProperty(prop, name, (byte) _default);
    }

    private static boolean getBooleanProperty(final Properties prop, final String name, final boolean _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Boolean.parseBoolean(s.trim());
    }

    private static float getFloatProperty(final Properties prop, final String name, final float _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Float.parseFloat(s.trim());
    }

    private static float getFloatProperty(final Properties prop, final String name, final double _default) {
        return getFloatProperty(prop, name, (float) _default);
    }

    private static double getDoubleProperty(final Properties prop, final String name, final double _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Double.parseDouble(s.trim());
    }

    private static int[] getIntArray(final Properties prop, final String name, final int[] _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Util.parseCommaSeparatedIntegerArray(s.trim());
    }

    private static float[] getFloatArray(final Properties prop, final String name, final float[] _default) {
        final String s = getProperty(prop, name);
        return s == null ? _default : Util.parseCommaSeparatedFloatArray(s.trim());
    }

    public static String HandleConfig(final L2Player activeChar, final String s) {
        final String[] parameter = s.split("=");
        final String pName = parameter[0].trim();

        try {
            final Field field = Config.class.getField(pName);
            if (parameter.length < 2)
                return pName + "=" + field.get(null);
            final String pValue = parameter[1].trim();
            if (setField(activeChar, field, pValue))
                return "Config field set succesfully: " + pName + "=" + field.get(null);
            return "Config field [" + pName + "] set fail!";
        } catch (final NoSuchFieldException e) {
            return "Parameter " + pName + " not found";
        } catch (final Exception e) {
            e.printStackTrace();
            return "Exception on HandleConfig";
        }
    }

    private static boolean setField(final L2Player activeChar, final Field field, final String param) {
        try {
            field.setBoolean(null, Boolean.parseBoolean(param));
        } catch (final Exception e) {
            try {
                field.setInt(null, Integer.parseInt(param));
            } catch (final Exception e1) {
                try {
                    field.setLong(null, Long.parseLong(param));
                } catch (final Exception e2) {
                    try {
                        field.setDouble(null, Double.parseDouble(param));
                    } catch (final Exception e3) {
                        try {
                            field.setFloat(null, Float.parseFloat(param));
                        } catch (final Exception e4) {
                            try {
                                field.set(null, param);
                            } catch (final Exception e5) {
                                if (activeChar != null)
                                    activeChar.sendMessage("Error while set field: " + param + " " + e.getMessage());
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}