package com.lineage.game.model;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.db.mysql;
import com.lineage.ext.Bonus;
import com.lineage.ext.mods.ClassChange;
import com.lineage.ext.mods.balancer.Balancer;
import com.lineage.ext.mods.balancer.Balancer.bflag;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Events;
import com.lineage.ext.scripts.Scripts;
import com.lineage.ext.scripts.Scripts.ScriptClassAndMethod;
import com.lineage.game.GameTimeController;
import com.lineage.game.RecipeController;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.ai.L2PlayableAI;
import com.lineage.game.ai.L2PlayableAI.nextAction;
import com.lineage.game.ai.L2PlayerAI;
import com.lineage.game.cache.Msg;
import com.lineage.game.cache.PlayerShiftCache;
import com.lineage.game.clientpackets.EnterWorld;
import com.lineage.game.communitybbs.BB.Forum;
import com.lineage.game.communitybbs.Manager.ForumsBBSManager;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.CastleSiegeManager;
import com.lineage.game.instancemanager.ClanHallManager;
import com.lineage.game.instancemanager.CoupleManager;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.instancemanager.DimensionalRiftManager;
import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.loginservercon.LSConnection;
import com.lineage.game.loginservercon.gspackets.ChangeAccessLevel;
import com.lineage.game.model.BypassManager.BypassType;
import com.lineage.game.model.BypassManager.DecodedBypass;
import com.lineage.game.model.BypassManager.EncodedBypass;
import com.lineage.game.model.L2Clan.RankPrivs;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.model.L2Multisell.MultiSellListContainer;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.base.PlayerAccess;
import com.lineage.game.model.base.Race;
import com.lineage.game.model.entity.Duel;
import com.lineage.game.model.entity.Duel.DuelState;
import com.lineage.game.model.entity.Hero;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.model.entity.olympiad.OlympiadGame;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.model.entity.residence.ResidenceType;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.model.instances.L2ClanHallManagerInstance;
import com.lineage.game.model.instances.L2CubicInstance;
import com.lineage.game.model.instances.L2CubicInstance.CubicType;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2GuardInstance;
import com.lineage.game.model.instances.L2HennaInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.model.instances.L2StaticObjectInstance;
import com.lineage.game.model.instances.L2TamedBeastInstance;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestEventType;
import com.lineage.game.model.quest.QuestState;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.AbnormalStatusUpdate;
import com.lineage.game.serverpackets.CameraMode;
import com.lineage.game.serverpackets.ChangeWaitType;
import com.lineage.game.serverpackets.CharInfo;
import com.lineage.game.serverpackets.CharMoveToLocation;
import com.lineage.game.serverpackets.ConfirmDlg;
import com.lineage.game.serverpackets.DeleteObject;
import com.lineage.game.serverpackets.DoorInfo;
import com.lineage.game.serverpackets.DoorStatusUpdate;
import com.lineage.game.serverpackets.DropItem;
import com.lineage.game.serverpackets.EtcStatusUpdate;
import com.lineage.game.serverpackets.ExAutoSoulShot;
import com.lineage.game.serverpackets.ExDuelUpdateUserInfo;
import com.lineage.game.serverpackets.ExFishingEnd;
import com.lineage.game.serverpackets.ExOlympiadMode;
import com.lineage.game.serverpackets.ExOlympiadSpelledInfo;
import com.lineage.game.serverpackets.ExOlympiadUserInfo;
import com.lineage.game.serverpackets.ExOlympiadUserInfoSpectator;
import com.lineage.game.serverpackets.ExSetCompassZoneCode;
import com.lineage.game.serverpackets.ExStorageMaxCount;
import com.lineage.game.serverpackets.ExUseSharedGroupItem;
import com.lineage.game.serverpackets.GetItem;
import com.lineage.game.serverpackets.HennaInfo;
import com.lineage.game.serverpackets.ItemList;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.NpcInfo;
import com.lineage.game.serverpackets.NpcInfoPoly;
import com.lineage.game.serverpackets.ObserverEnd;
import com.lineage.game.serverpackets.ObserverStart;
import com.lineage.game.serverpackets.PartySmallWindowUpdate;
import com.lineage.game.serverpackets.PartySpelled;
import com.lineage.game.serverpackets.PetInfo;
import com.lineage.game.serverpackets.PetItemList;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeShowMemberListDelete;
import com.lineage.game.serverpackets.PledgeShowMemberListDeleteAll;
import com.lineage.game.serverpackets.PledgeShowMemberListUpdate;
import com.lineage.game.serverpackets.PledgeStatusChanged;
import com.lineage.game.serverpackets.PrivateStoreListBuy;
import com.lineage.game.serverpackets.PrivateStoreListSell;
import com.lineage.game.serverpackets.PrivateStoreMsgBuy;
import com.lineage.game.serverpackets.PrivateStoreMsgSell;
import com.lineage.game.serverpackets.QuestList;
import com.lineage.game.serverpackets.RecipeShopMsg;
import com.lineage.game.serverpackets.RecipeShopSellList;
import com.lineage.game.serverpackets.RelationChanged;
import com.lineage.game.serverpackets.Ride;
import com.lineage.game.serverpackets.SendTradeDone;
import com.lineage.game.serverpackets.SetupGauge;
import com.lineage.game.serverpackets.ShortBuffStatusUpdate;
import com.lineage.game.serverpackets.ShortCutInit;
import com.lineage.game.serverpackets.ShortCutRegister;
import com.lineage.game.serverpackets.SkillCoolTime;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.serverpackets.Snoop;
import com.lineage.game.serverpackets.SocialAction;
import com.lineage.game.serverpackets.SpawnEmitter;
import com.lineage.game.serverpackets.SpawnItem;
import com.lineage.game.serverpackets.SpawnItemPoly;
import com.lineage.game.serverpackets.SpecialCamera;
import com.lineage.game.serverpackets.StaticObject;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.TargetSelected;
import com.lineage.game.serverpackets.TargetUnselected;
import com.lineage.game.serverpackets.TeleportToLocation;
import com.lineage.game.serverpackets.UserInfo;
import com.lineage.game.serverpackets.VehicleDeparture;
import com.lineage.game.serverpackets.VehicleInfo;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.SkillTimeStamp;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.effects.EffectTemplate;
import com.lineage.game.tables.CharTemplateTable;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.tables.HennaTable;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.tables.MapRegion;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.PetDataTable;
import com.lineage.game.tables.ReflectionTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.tables.SkillTreeTable;
import com.lineage.game.taskmanager.AutoSaveManager;
import com.lineage.game.taskmanager.BreakWarnManager;
import com.lineage.game.templates.L2Armor;
import com.lineage.game.templates.L2Armor.ArmorType;
import com.lineage.game.templates.L2Henna;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2PlayerTemplate;
import com.lineage.game.templates.L2Weapon;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.util.EffectsComparator;
import com.lineage.util.Files;
import com.lineage.util.GArray;
import com.lineage.util.GCSArray;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.Rnd;
import com.lineage.util.SqlBatch;
import com.lineage.util.Strings;
import com.lineage.util.Util;

public final class L2Player extends L2Playable {
    public static final short STORE_PRIVATE_NONE = 0;
    public static final short STORE_PRIVATE_SELL = 1;
    public static final short STORE_PRIVATE_BUY = 3;
    public static final short STORE_PRIVATE_MANUFACTURE = 5;
    public static final short STORE_PRIVATE_SELL_PACKAGE = 8;

    private static final short STORE_OBSERVING_GAMES = 7;
    private static final int RANK_NOVICE = 0;//
    private static final int RANK_INITIATE = 1;//
    private static final int RANK_MINOR = 2;//
    private static final int RANK_EXPERT = 3;//
    private static final int RANK_THUG = 4;//
    private static final int RANK_ADEPT = 5;//
    private static final int RANK_CONJURER = 6;//
    private static final int RANK_ELITE = 7;//
    private static final int RANK_ARCHON = 8;//
    private static final int RANK_LEADER = 9;//

    //public static final int GRAND_DUKE = 10;
    //public static final int KING = 11;
    //public static final int RELATION_UNKNOWN_1 = 0x00008; // unknown
    //public static final int RELATION_UNKNOWN_2 = 0x00010; // unknown
    //public static final int RELATION_UNKNOWN_3 = 0x00020; // unknown
    //public static final int RELATION_UNKNOWN_4 = 0x00040; // unknown
    //public static final int IMPERATOR = 12;
    //public static final int RELATION_UNKNOWN_5 = 0x00100; // unknown
    //public static final int RELATION_UNKNOWN_6 = 0x02000; // unknown
    //public static final int RELATION_UNKNOWN_7 = 0x04000; // unknown
    //public static final int RELATION_UNKNOWN_8 = 0x20000; // unknown
    //public static final int RELATION_UNKNOWN_9 = 0x40000; // unknown

    private static final int RELATION_LEADER = 0x00080; // leader at siege
    private static final int RELATION_PVP_FLAG = 0x00002; // pvp
    private static final int RELATION_HAS_KARMA = 0x00004; // karma
    private static final int RELATION_INSIEGE = 0x00200; // true if in siege
    private static final int RELATION_ATTACKER = 0x00400; // true when attacker
    private static final int RELATION_ALLY = 0x00800; // blue siege icon, cannot have if red
    private static final int RELATION_ENEMY = 0x01000; // true when red icon, doesn't matter with blue
    private static final int RELATION_MUTUAL_WAR = 0x08000; // double fist
    private static final int RELATION_1SIDED_WAR = 0x10000; // single fist


    /**
     * The table containing all minimum level needed for each Expertise (None, D, C, B, A, S)
     */
    private static final int[] EXPERTISE_LEVELS = {SkillTreeTable.getExpertiseLevel(0), // NONE
            SkillTreeTable.getExpertiseLevel(1), // D
            SkillTreeTable.getExpertiseLevel(2), // C
            SkillTreeTable.getExpertiseLevel(3), // B
            SkillTreeTable.getExpertiseLevel(4), // A
            SkillTreeTable.getExpertiseLevel(5), // S
    };

    static final Logger _log = Logger.getLogger(L2Player.class.getName());
    public HashMap<Integer, L2SubClass> _classlist = new HashMap<Integer, L2SubClass>(4);
    public HashMap<Integer, Long> _killed_List = new HashMap<Integer, Long>();
    public GArray<PvPinfo> _lastPvPs = new GArray<PvPinfo>();

    public String _shift_page;
    public String _shift_page_last_kills;
    public long _last_shift_append;

    // client radar
    public L2Radar radar;
    public boolean _exploring = false;

    public byte updateKnownCounter = 0;

    /**
     * The current higher Expertise of the L2Player (None=0, D=1, C=2, B=3, A=4, S=5)
     */
    public int expertiseIndex = 0;

    public int _telemode = 0;
    public int _unstuck = 0;

    /**
     * Эта точка проверяется при нештатном выходе чара, и если не равна null чар возвращается в нее Используется например для возвращения при падении с виверны Поле heading используется для хранения
     * денег возвращаемых при сбое
     */
    public Location _stablePoint = null;

    /**
     * new loto ticket *
     */
    public int _loto[] = new int[5];
    /**
     * new race ticket *
     */
    public int _race[] = new int[2];

    public boolean _isSitting = false;
    public ScheduledFuture<?> _taskforfish;
    public boolean entering = true;

    public HashMap<String, Integer> packetsStat = null;
    public boolean packetsCount = false;

    public Future<?> _unjailTask;

    /**
     * private List<L2Player> _snoopListener = new FastList<L2Player>(); private List<L2Player> _snoopedPlayer = new FastList<L2Player>(); public void broadcastSnoop(int type, String name, String
     * _text) { if(_snoopListener.size() > 0) { Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text); for(L2Player pci : _snoopListener) if(pci != null) pci.sendPacket(sn); } } public
     * void addSnooper(L2Player pci) { if(!_snoopListener.contains(pci)) _snoopListener.add(pci); } public void removeSnooper(L2Player pci) { _snoopListener.remove(pci); } public void
     * addSnooped(L2Player pci) { if(!_snoopedPlayer.contains(pci)) _snoopedPlayer.add(pci); } public void removeSnooped(L2Player pci) { _snoopedPlayer.remove(pci); }
     */

    public final FastMap<Integer, SkillTimeStamp> skillReuseTimeStamps = new FastMap<Integer, SkillTimeStamp>();

    // fast fix for dice spam
    public long lastDiceThrown = 0;
    int expertisePenalty = 0;

    TransactionType _currentTransactionType = TransactionType.NONE;

    String _scriptName = "";
    Object[] _scriptArgs = new Object[0];

    int _eventPoints = 0;

    protected ConcurrentSkipListSet<Integer> _activeSoulShots = new ConcurrentSkipListSet<Integer>();

    protected int _baseClass = -1;
    protected L2SubClass _activeClass = null;

    protected HashMap<Integer, Long> _StatKills = new HashMap<Integer, Long>(128);
    protected HashMap<Integer, Long> _StatDrop = new HashMap<Integer, Long>(128);
    protected HashMap<Integer, Long> _StatCraft = new HashMap<Integer, Long>(32);

    /**
     * 0=White, 1=Purple, 2=PurpleBlink
     */
    protected int _pvpFlag;

    private int _lastpage = 1;


    private FastMap<String, GArray<Integer>> _buffs = new FastMap<String, GArray<Integer>>();

    private String[] RANK_NAMES = {"Novice", "Initiate", "Minor", "Expert", "Thug", "Adept", "Conjurer", "Elite", "Archon", "Leader"};

    private L2GameClient _connection;

    /**
     * The Karma of the L2Player (if higher than 0, the name of the L2Player appears in red)
     */
    private int _karma;

    /**
     * The number of player killed during a PvP (the player killed was PvP Flagged)
     */
    private int _pvpKills;

    /**
     * The hexadecimal Color of players name (white is 0xFFFFFF)
     */
    private int _nameColor;

    /**
     * The hexadecimal Color of players title (white is 0xFFFFFF)
     */
    private int _titlecolor;

    /**
     * The PK counter of the L2Player (= Number of non PvP Flagged player killed)
     */
    private int _pkKills;

    /**
     * The number of recommendation obtained by the L2Player
     */
    private int _recomHave; // how much I was recommended by others

    /**
     * The number of recommendation that the L2Player can give
     */
    private int _recomLeft; // how many recommendations I can give to others

    /**
     * List with the recommendations that I've give
     */
    private List<Integer> _recomChars = new FastList<Integer>();

    private int _curWeightPenalty = 0;

    private int _deleteTimer;
    private PcInventory _inventory = new PcInventory(this);
    private PcWarehouse _warehouse = new PcWarehouse(this);
    private PcFreight _freight = new PcFreight(this);

    private boolean AutoLoot = Config.AUTO_LOOT, AutoLootHerbs = Config.AUTO_LOOT_HERBS;

    /**
     * True if the L2Player is sitting
     */
    private boolean _sittingTask;
    /**
     * Time counter when L2Player is sitting
     */
    private int _waitTimeWhenSit;

    /**
     * True if the L2Player is using the relax skill
     */
    private boolean _relax;

    /**
     * The face type Identifier of the L2Player
     */
    private int _face;

    /**
     * The hair style Identifier of the L2Player
     */
    private int _hairStyle;

    /**
     * The hair color Identifier of the L2Player
     */
    private int _hairColor;

    /**
     * The table containing all Quests began by the L2Player
     */
    private HashMap<String, QuestState> _quests = new HashMap<String, QuestState>();

    /**
     * The list containing all shortCuts of this L2Player
     */
    private ShortCuts _shortCuts = new ShortCuts(this);

    /**
     * The list containing all macroses of this L2Player
     */
    private MacroList _macroses = new MacroList(this);

    private L2TradeList _tradeList;
    private L2ManufactureList _createList;
    private ConcurrentLinkedQueue<TradeItem> _sellList;
    private ConcurrentLinkedQueue<TradeItem> _buyList;

    /**
     * The Private Store type of the L2Player (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5)
     */
    private short _privatestore;
    private ClassId _skillLearningClassId;

    // hennas
    private final L2HennaInstance[] _henna = new L2HennaInstance[3];
    private short _hennaSTR;
    private short _hennaINT;
    private short _hennaDEX;
    private short _hennaMEN;
    private short _hennaWIT;
    private short _hennaCON;

    /**
     * The L2Summon of the L2Player
     */
    private L2Summon _summon = null;
    private final StatsChangeRecorder _statsChangeRecorder = new StatsChangeRecorder(this);

    // these values are only stored temporarily
    private int _partyMatchingLevels;
    private int _partyMatchingRegion;
    private Integer _partyRoom = 0;

    private L2Party _party;
    // clan related attributes

    /**
     * The Clan object of the L2Player
     */
    private L2Clan _clan;

    private int _pledgeClass = 0;
    private int _pledgeType = 0;
    private int _powerGrade = 0;
    private int _lvlJoinedAcademy = 0;
    private int _apprentice = 0;

    private long _leaveClanTime;
    private long _deleteClanTime;

    private long _onlineTime;
    private long _onlineBeginTime;

    private long _NoChannel; // Nochannel mode
    private long _NoChannelBegin;

    // GM Stuff
    private int _accessLevel;
    private PlayerAccess _playerAccess = new PlayerAccess();

    private boolean _messageRefusal = false; // message refusal mode
    private boolean _tradeRefusal = false; // Trade refusal
    private boolean _exchangeRefusal = false; // Exchange refusal

    // this is needed to find the inviting player for Party response
    // there can only be one active party request at once
    private L2Player _currentTransactionRequester;
    private long _currentTransactionTimeout;
    private L2ItemInstance _arrowItem;

    /**
     * The fists L2Weapon of the L2Player (used when no weapon is equipped)
     */
    private L2Weapon _fistsWeaponItem;

    private long _uptime;
    private String _accountName;

    private HashMap<Integer, String> _chars = new HashMap<Integer, String>(8);

    /**
     * The table containing all L2RecipeList of the L2Player
     */
    private Map<Integer, L2Recipe> _recipebook = new TreeMap<Integer, L2Recipe>();
    private Map<Integer, L2Recipe> _commonrecipebook = new TreeMap<Integer, L2Recipe>();

    // stats watch
    private int oldMaxHP;
    private int oldMaxMP;

    private L2ItemInstance _enchantScroll = null;

    private Warehouse.WarehouseType _usingWHType;
    private boolean _isOnline = false;
    private boolean _isDeleting = false;

    private boolean _inventoryDisable = false;

    private GCSArray<L2CubicInstance> _cubics = null;

    /**
     * The L2NpcInstance corresponding to the last Folk which one the player talked.
     */
    private L2NpcInstance _lastNpc = null;
    private String _lastBBS_script_operation = null;

    /**
     * тут храним мультиселл с которым работаем, полезно...
     */
    private MultiSellListContainer _multisell = null;

    /**
     * 1 if the player is invisible
     */
    private boolean _invisible = false;

    /**
     * Location before entering Observer Mode
     */
    private Location _obsLoc = new Location(0, 0, 0);
    private L2WorldRegion _observNeighbor;
    private boolean _observerMode = false;

    private FastMap<Integer, String> _blockList = new FastMap<Integer, String>(); // characters blocked with '/block <charname>' cmd

    private boolean _blockAll = false; // /blockall cmd handling

    private boolean _isConnected = true;

    private boolean _hero = false;
    private int _team = 0;
    private boolean _checksForTeam = false;

    // time on login in game
    private long _lastAccess;

    /**
     * True if the L2Player is in a boat
     */
    private L2BoatInstance _Boat;
    private Location _inBoatPosition;

    private Bonus _bonus;
    private Future<?> _bonusExpiration;

    private boolean _noble = false;
    private boolean _inOlympiadMode = false;
    private int _olympiadGameId = -1;
    private int _olympiadSide = -1;

    /**
     * ally with ketra or varka related wars
     */
    private int _varka = 0;
    private int _ketra = 0;
    private int _ram = 0;

    /**
     * The Siege state
     */
    private int _siegeState = 0;

    private ScheduledFuture<?> _taskWater;

    private Forum _forumMemo;

    private int _cursedWeaponEquippedId = 0;

    private L2Fishing _fishCombat;
    private boolean _fishing = false;
    private Location _fishLoc = new Location(0, 0, 0);
    private L2ItemInstance _lure = null;

    private Future<?> _kickTask;

    private boolean _isInCombatZone;
    private boolean _isOnSiegeField;
    private boolean _isInPeaceZone;
    private boolean _isInSSZone;

    private boolean _offline = false;

    private int pcBangPoints;

    private int _expandInventory = 0;
    private int _expandWarehouse = 0;

    private boolean _notShowBuffAnim = false;
    private GArray<EncodedBypass> bypasses = null, bypasses_bbs = null;

    private long _lastReuseMessage;
    private int _lastReuseMessageSkill;

    private boolean _logoutStarted = false;

    private Future<?> _broadcastCharInfoTask;

    private Future<?> _userInfoTask;

    private String msg = "Materials:";
    private boolean _isInJail; // true, если чар в тюрьме
    private long _jailStartTime; // время (currentTimeMillis()) помещения чара в тюрьму

    private Object _storeLock = new Object();

    private int _mountNpcId;
    private int _mountObjId;
    private int _mountLevel;

    private FastMap<String, String> user_variables = new FastMap<String, String>();

    private int _ReviveRequested = 0;
    private double _RevivePower = 0;
    private boolean _RevivePet = false;

    /**
     * Координаты точки призыва персонажа
     */
    private Location _SummonCharacterCoords;

    /**
     * Флаг необходимости потребления Summoning Cystall-а при призыве персонажа
     */
    private int _SummonConsumeCrystall = 0;

    private FishData _fish;

    private boolean _maried = false;
    private int _partnerId = 0;
    private int _coupleId = 0;
    private boolean _engagerequest = false;
    private int _engageid = 0;
    private boolean _maryrequest = false;
    private boolean _maryaccepted = false;
    private boolean _IsWearingFormalWear = false;

    private List<L2Player> _snoopListener = new FastList<L2Player>();
    private List<L2Player> _snoopedPlayer = new FastList<L2Player>();

    private boolean _charmOfCourage = false;

    private int _increasedForce = 0;
    private int _consumedSouls = 0;

    private boolean _isInDangerArea;

    private ResidenceType _inResidence = ResidenceType.None;

    private Location _lastClientPosition;
    private Location _lastServerPosition;

    private int _useSeed = 0;

    private Future<?> _PvPRegTask;
    private long _lastPvpAttack;

    private Duel _duel;

    private L2TamedBeastInstance _tamedBeast = null;

    private long _lastAttackPacket = 0;

    private long _lastMovePacket = 0;

    private long _lastEquipmPacket = 0;

    private Location _groundSkillLoc;

    private int _buyListId;

    private int _incorrectValidateCount = 0;

    /**
     * Create a new L2Player and add it in the characters table of the database.<BR><BR> <B><U> Actions</U> :</B><BR><BR> <li>Create a new L2Player with an account name </li> <li>Set the
     * name, the Hair Style, the Hair Color and the Face type of the L2Player</li> <li>Add the player in the characters table of the database</li><BR><BR>
     *
     * @param accountName The name of the L2Player
     * @param name        The name of the L2Player
     * @param hairStyle   The hair style Identifier of the L2Player
     * @param hairColor   The hair color Identifier of the L2Player
     * @param face        The face type Identifier of the L2Player
     * @return The L2Player added to the database or null
     */
    public static L2Player create(final int classId, final byte sex, final String accountName, final String name, final byte hairStyle, final byte hairColor, final byte face) {
        final L2PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(classId, sex != 0);

        // Create a new L2Player with an account name
        final L2Player player = new L2Player(IdFactory.getInstance().getNextId(), template, accountName);

        player.setName(name);
        player.setTitle("");
        player.setHairStyle(hairStyle);
        player.setHairColor(hairColor);
        player.setFace(face);

        // Add the player in the characters table of the database
        if (!player.createDb())
            return null;

        return player;
    }

    public void setHairStyle(final int hairStyle) {
        _hairStyle = hairStyle;
    }

    public void setHairColor(final int hairColor) {
        _hairColor = hairColor;
    }

    public void setFace(final int face) {
        _face = face;
    }

    /**
     * Create a new player in the characters table of the database.
     */
    private boolean createDb() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("INSERT INTO `characters` (account_name, obj_Id, char_name, face, hairStyle, hairColor, sex, karma, pvpkills, pkkills, clanid, deletetime, title, accesslevel, online, leaveclan, deleteclan, nochannel, pledge_type, pledge_rank, lvl_joined_academy, apprentice) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setString(1, _accountName);
            statement.setInt(2, getObjectId());
            statement.setString(3, getName());
            statement.setInt(4, getFace());
            statement.setInt(5, getHairStyle());
            statement.setInt(6, getHairColor());
            statement.setInt(7, getSex());
            statement.setInt(8, getKarma());
            statement.setInt(9, getPvpKills());
            statement.setInt(10, getPkKills());
            statement.setInt(11, getClanId());
            statement.setInt(12, getDeleteTimer());
            statement.setString(13, getTitle());
            statement.setInt(14, _accessLevel);
            statement.setInt(15, isOnline() ? 1 : 0);
            statement.setLong(16, getLeaveClanTime() / 1000);
            statement.setLong(17, getDeleteClanTime() / 1000);
            statement.setLong(18, _NoChannel > 0 ? _NoChannel / 1000 : _NoChannel);
            statement.setInt(19, getPledgeType());
            statement.setInt(20, getPowerGrade());
            statement.setInt(21, getLvlJoinedAcademy());
            statement.setInt(22, getApprentice());
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("INSERT INTO character_subclasses (char_obj_id, class_id, exp, sp, curHp, curMp, curCp, maxHp, maxMp, maxCp, level, active, isBase, death_penalty, skills, pvp) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getTemplate().classId.getId());
            statement.setInt(3, 0);
            statement.setInt(4, 0);
            statement.setDouble(5, getTemplate().baseHpMax + getTemplate().lvlHpAdd + getTemplate().lvlHpMod);
            statement.setDouble(6, getTemplate().baseMpMax + getTemplate().lvlMpAdd + getTemplate().lvlMpMod);
            statement.setDouble(7, getTemplate().baseCpMax + getTemplate().lvlCpAdd + getTemplate().lvlCpMod);
            statement.setDouble(8, getTemplate().baseHpMax + getTemplate().lvlHpAdd + getTemplate().lvlHpMod);
            statement.setDouble(9, getTemplate().baseMpMax + getTemplate().lvlMpAdd + getTemplate().lvlMpMod);
            statement.setDouble(10, getTemplate().baseCpMax + getTemplate().lvlCpAdd + getTemplate().lvlCpMod);
            statement.setInt(11, 1);
            statement.setInt(12, 1);
            statement.setInt(13, 1);
            statement.setInt(14, 0);
            statement.setString(15, "");
            statement.setInt(16, 0);
            statement.executeUpdate();
        } catch (final Exception e) {
            _log.log(Level.WARNING, "could not insert char data:", e);
            return false;
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
        return true;
    }

    public int getFace() {
        return _face;
    }

    public int getHairStyle() {
        return _hairStyle;
    }

    public int getHairColor() {
        return _hairColor;
    }

    public byte getSex() {
        return getTemplate().isMale ? (byte) 0 : (byte) 1;
    }

    @Override
    public int getKarma() {
        return _karma;
    }

    public int getPvpKills() {
        return _pvpKills;
    }

    public int getPkKills() {
        return _pkKills;
    }

    public int getClanId() {
        return _clan == null ? 0 : _clan.getClanId();
    }

    public int getDeleteTimer() {
        return _deleteTimer;
    }

    public boolean isOnline() {
        return _isOnline;
    }

    public long getLeaveClanTime() {
        return _leaveClanTime;
    }

    public long getDeleteClanTime() {
        return _deleteClanTime;
    }

    public int getPledgeType() {
        return _pledgeType;
    }

    public int getPowerGrade() {
        return _powerGrade;
    }

    public int getLvlJoinedAcademy() {
        return _lvlJoinedAcademy;
    }

    public int getApprentice() {
        return _apprentice;
    }

    @Override
    public final L2PlayerTemplate getTemplate() {
        return (L2PlayerTemplate) _template;
    }

    /**
     * Retrieve a L2Player from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR><BR>
     */
    public static L2Player load(final int objectId) {
        return restore(objectId);
    }

    /**
     * Retrieve a L2Player from the characters table of the database and add it in _allObjects of the L2World
     *
     * @return The L2Player loaded from the database
     */
    private static L2Player restore(final int objectId) {
        L2Player player = null;
        ThreadConnection con = null;
        FiltredStatement statement = null;
        FiltredStatement statement2 = null;
        ResultSet pl_rset = null;
        ResultSet ps_rset = null;
        try {
            // Retrieve the L2Player from the characters table of the database
            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.createStatement();
            statement2 = con.createStatement();
            pl_rset = statement.executeQuery("SELECT * FROM `characters` WHERE `obj_Id`='" + objectId + "' LIMIT 1");
            ps_rset = statement2.executeQuery("SELECT `class_id` FROM `character_subclasses` WHERE `char_obj_id`='" + objectId + "' AND `isBase`='1' LIMIT 1");

            if (pl_rset.next() && ps_rset.next()) {
                final int classId = ps_rset.getInt("class_id");
                final boolean female = pl_rset.getInt("sex") == 1;
                final L2PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(classId, female);

                player = new L2Player(objectId, template);

                player.loadVariables();

                player.setBaseClass(classId);
                player._accountName = pl_rset.getString("account_name");
                player.setName(pl_rset.getString("char_name"));

                player.setFace(pl_rset.getByte("face"));
                player.setHairStyle(pl_rset.getByte("hairStyle"));
                player.setHairColor(pl_rset.getByte("hairColor"));
                player.setHeading(pl_rset.getInt("heading"));

                player.setKarma(pl_rset.getInt("karma"));
                player.setPvpKills(pl_rset.getInt("pvpkills"));
                player.setPkKills(pl_rset.getInt("pkkills"));
                player.setLeaveClanTime(pl_rset.getLong("leaveclan") * 1000);
                if (player.getLeaveClanTime() > 0 && player.canJoinClan())
                    player.setLeaveClanTime(0);
                player.setDeleteClanTime(pl_rset.getLong("deleteclan") * 1000);
                if (player.getDeleteClanTime() > 0 && player.canCreateClan())
                    player.setDeleteClanTime(0);

                player.setNoChannel(pl_rset.getLong("nochannel") * 1000);
                if (player.getNoChannel() > 0 && player.getNoChannelRemained() < 0)
                    player.updateNoChannel(0);

                player.setOnlineTime(pl_rset.getLong("onlinetime") * 1000);

                player.setNoble(pl_rset.getBoolean("noble"));
                player.setVarka(pl_rset.getInt("varka"));
                player.setKetra(pl_rset.getInt("ketra"));
                player.setRam(pl_rset.getInt("ram"));

                final int clanId = pl_rset.getInt("clanid");
                if (clanId > 0) {
                    if (Config.DEBUG)
                        System.out.println("Char clan id is loaded as " + clanId);
                    player.setClan(ClanTable.getInstance().getClan(clanId));
                    player.setPledgeType(pl_rset.getInt("pledge_type"));
                    player.setPowerGrade(pl_rset.getInt("pledge_rank"));
                    player.setLvlJoinedAcademy(pl_rset.getInt("lvl_joined_academy"));
                    player.setApprentice(pl_rset.getInt("apprentice"));
                    player.updatePledgeClass();
                    if (Config.DEBUG)
                        System.out.println("Char clan is loaded");
                }

                player.setDeleteTimer(pl_rset.getInt("deletetime"));

                player.setTitle(pl_rset.getString("title"));

                if (player.getVar("namecolor") == null)
                    if (player.getPlayerAccess().IsGM)
                        player.setNameColor(Config.GM_NAME_COLOUR);
                    else if (player.getClan() != null && player.getClan().getLeaderId() == player.getObjectId())
                        player.setNameColor(Config.CLANLEADER_NAME_COLOUR);
                    else
                        player.setNameColor(Config.NORMAL_NAME_COLOUR);
                else
                    player.setNameColor(Integer.decode("0x" + player.getVar("namecolor")));

                if (Config.AUTO_LOOT_INDIVIDUAL) {
                    player.AutoLoot = player.getVarB("AutoLoot", Config.AUTO_LOOT);
                    player.AutoLootHerbs = player.getVarB("AutoLootHerbs", Config.AUTO_LOOT_HERBS);
                }

                player.setFistsWeaponItem(player.findFistsWeaponItem(classId));
                player.setUptime(System.currentTimeMillis());
                player.setLastAccess(pl_rset.getLong("lastAccess"));

                player.setRecomHave(pl_rset.getInt("rec_have"));
                player.setRecomLeft(pl_rset.getInt("rec_left"));

                player.setPcBangPoints(pl_rset.getInt("pcBangPoints"));

                player.restoreRecipeBook();

                player.restoreTradeList();
                if (player.getVar("storemode") != null && player.getVar("offline") != null) {
                    player.setPrivateStoreType(Short.parseShort(player.getVar("storemode")));
                    player.setSitting(true);
                }

                if (Config.ENABLE_OLYMPIAD && Hero.getInstance().isHero(player.getObjectId()))
                    player.setHero(true);

                restoreCharSubClasses(player);
                Quest.playerEnter(player);

                // 15 секунд после входа в игру на персонажа не агрятся мобы
                player.setNonAggroTime(System.currentTimeMillis() + 15000);

                // для сервиса виверн - возврат денег если сервер упал во время полета
                String wm = player.getVar("wyvern_moneyback");
                if (wm != null && Integer.parseInt(wm) > 0)
                    player.addAdena(Integer.parseInt(wm));
                player.unsetVar("wyvern_moneyback");

                // Проверяем на джейл
                if (player.getVar("jailed") != null) {
                    // посылаем наф плеера
                    player.setXYZInvisible(-114648, -249384, -2984);
                    if (player.getParty() != null)
                        if (player.getParty().isLeader(player)) {
                            GArray<L2Player> members = player.getParty().getPartyMembers();
                            for (L2Player cha : members)
                                cha.leaveParty();
                        } else
                            player.leaveParty();
                    final String[] re = player.getVar("jailedFrom").split(";");
                    final Location loc = new Location(Integer.parseInt(re[0]), Integer.parseInt(re[1]), Integer.parseInt(re[2]));
                    player._unjailTask = ThreadPoolManager.getInstance().scheduleGeneral(player.new UnJailTask(loc), Integer.parseInt(player.getVar("jailed")) * 60000);
                    if (Config.DEBUG)
                        _log.fine("L2Player.restore(): jailed player " + player.getName() + " for " + player.getVar("jailed") + " minutes");
                } else {
                    player.setXYZInvisible(pl_rset.getInt("x"), pl_rset.getInt("y"), pl_rset.getInt("z"));
                    wm = player.getVar("reflection");
                    if (wm != null && ReflectionTable.getInstance().get(Integer.parseInt(wm)) != null)
                        player.setReflection(Integer.parseInt(wm));
                    else
                        player.setReflection(0);
                }

                try {
                    final String var = player.getVar("ExpandInventory");
                    if (var != null)
                        player.setExpandInventory(Integer.parseInt(var));
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                try {
                    final String var = player.getVar("ExpandWarehouse");
                    if (var != null)
                        player.setExpandWarehouse(Integer.parseInt(var));
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                try {
                    final String var = player.getVar("notShowBuffAnim");
                    if (var != null)
                        player.setNotShowBuffAnim(Boolean.parseBoolean(var));
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                FiltredPreparedStatement stmt = null;
                ResultSet chars = null;
                try {
                    stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id!=?");
                    stmt.setString(1, player._accountName);
                    stmt.setInt(2, objectId);
                    chars = stmt.executeQuery();
                    while (chars.next()) {
                        final Integer charId = chars.getInt("obj_Id");
                        final String charName = chars.getString("char_name");
                        player._chars.put(charId, charName);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    DatabaseUtils.closeDatabaseSR(stmt, chars);
                }

                if (!L2World.validCoords(player.getX(), player.getY()) || player.getX() == 0 && player.getY() == 0) {
                    final Location loc = MapRegion.getTeleToClosestTown(player);
                    player.setXYZInvisible(loc.x, loc.y, loc.z);
                }

                if (Config.ENABLE_OLYMPIAD && ZoneManager.getInstance().checkIfInZone(L2Zone.ZoneType.OlympiadStadia, player)) {
                    player.sendMessage(new CustomMessage("l2d.game.clientpackets.EnterWorld.TeleportedReasonOlympiad", player));
                    final Location loc = MapRegion.getTeleToClosestTown(player);
                    player.setXYZInvisible(loc.x, loc.y, loc.z);
                }

                final L2Zone noRestartZone = ZoneManager.getInstance().getZoneByTypeAndObject(L2Zone.ZoneType.no_restart, player);
                if (noRestartZone != null) {
                    final long allowed_time = noRestartZone.getRestartTime();
                    final long last_time = player.getLastAccess();
                    final long curr_time = System.currentTimeMillis() / 1000;

                    if (curr_time - last_time > allowed_time) {
                        player.sendMessage(new CustomMessage("l2d.game.clientpackets.EnterWorld.TeleportedReasonNoRestart", player));
                        final Location loc = MapRegion.getTeleToClosestTown(player);
                        player.setXYZInvisible(loc.x, loc.y, loc.z);
                    }
                }

                if (!player.isGM() && player.isInZone(L2Zone.ZoneType.Siege)) {
                    final Siege siege = SiegeManager.getSiege(player, true);
                    if (siege != null && !siege.checkIsDefender(player.getClan()))
                        if (siege.getHeadquarter(player.getClan()) == null) {
                            final Location loc = MapRegion.getTeleToClosestTown(player);
                            player.setXYZInvisible(loc.x, loc.y, loc.z);
                        } else {
                            final Location loc = MapRegion.getTeleToHeadquarter(player);
                            player.setXYZInvisible(loc.x, loc.y, loc.z);
                        }
                }

                if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getLoc(), false)) {
                    final Location loc = DimensionalRiftManager.getInstance().getRoom(0, 0).getTeleportCoords();
                    player.setXYZInvisible(loc.x, loc.y, loc.z);
                }

                player.getInventory().validateItems();
                player.revalidatePenalties();
                player.restoreBlockList();
                BreakWarnManager.getInstance().addWarnTask(player);
                AutoSaveManager.getInstance().addPlayerTask(player);
            }
        } catch (final Exception e) {
            _log.log(Level.WARNING, "restore: could not restore char data:", e);
        } finally {
            DatabaseUtils.closeDatabaseSR(statement2, ps_rset);
            DatabaseUtils.closeDatabaseCSR(con, statement, pl_rset);
        }
        return player;
    }

    public void loadVariables() {
        ThreadConnection con = null;
        FiltredPreparedStatement offline = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            offline = con.prepareStatement("SELECT * FROM character_variables WHERE obj_id = ?");
            offline.setInt(1, _objectId);
            rs = offline.executeQuery();
            while (rs.next()) {
                final String name = rs.getString("name");
                final String value = Strings.stripSlashes(rs.getString("value"));
                user_variables.put(name, value);
            }

            if (getVar("lang@") == null)
                setVar("lang@", "en");
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, offline, rs);
        }
    }

    public void setVar(final String name, final String value) {
        user_variables.put(name, value);
        mysql.set("REPLACE INTO character_variables  (obj_id, type, name, value, expire_time) VALUES (" + _objectId + ",'user-var','" + Strings.addSlashes(name) + "','" + Strings.addSlashes(value) + "',-1)");
    }

    public void setBaseClass(final int baseClass) {
        _baseClass = baseClass;
    }

    public void setKarma(int karma) {
        if (karma < 0)
            karma = 0;

        if (_karma == karma)
            return;

        _karma = karma;

        if (karma > 0)
            for (final L2Character object : L2World.getAroundCharacters(this))
                if (object instanceof L2GuardInstance && object.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
                    object.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);

        sendChanges();

        if (getPet() != null)
            getPet().broadcastPetInfo();
    }

    @Override
    public void sendChanges() {
        _statsChangeRecorder.sendChanges();
    }

    @Override
    public L2Summon getPet() {
        return _summon;
    }

    public void setPvpKills(final int pvpKills) {
        _pvpKills = pvpKills;
        updatePledgeClass();
    }

    public void setPkKills(final int pkKills) {
        _pkKills = pkKills;
    }

    public boolean canJoinClan() {
        if (_leaveClanTime == 0)
            return true;
        if (System.currentTimeMillis() - _leaveClanTime >= 12 * 60 * 60 * 1000) {
            _leaveClanTime = 0;
            return true;
        }
        return false;
    }

    public void setLeaveClanTime(final long time) {
        _leaveClanTime = time;
    }

    public boolean canCreateClan() {
        if (_deleteClanTime == 0)
            return true;
        if (System.currentTimeMillis() - _deleteClanTime >= 2 * 24 * 60 * 60 * 1000) {
            _deleteClanTime = 0;
            return true;
        }
        return false;
    }

    public void setDeleteClanTime(final long time) {
        _deleteClanTime = time;
    }

    public void setNoChannel(final long time) {
        _NoChannel = time;
        if (_NoChannel > 2145909600000L || _NoChannel < 0)
            _NoChannel = -1;

        if (_NoChannel > 0)
            _NoChannelBegin = System.currentTimeMillis();
        else
            _NoChannelBegin = 0;

        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Send a Server->Client packet StatusUpdate to the L2Player.<BR><BR>
     */
    @Override
    public void sendPacket(final L2GameServerPacket packet) {
        if (_isConnected)
            try {
                if (_connection != null)
                    _connection.sendPacket(packet);
                // _connection.sendPacket(SystemMessage.sendString(packet.getType()));
                // System.out.println(packet.getType());

                if (packetsCount && isGM()) {
                    if (packetsStat == null)
                        packetsStat = new HashMap<String, Integer>();
                    Integer count = packetsStat.get(packet.getClass().getSimpleName());
                    if (count == null)
                        count = 0;
                    count++;
                    packetsStat.put(packet.getClass().getSimpleName(), count);
                }
            } catch (final Exception e) {
                _log.log(Level.INFO, "", e);
                e.printStackTrace();
            }
    }

    public long getNoChannelRemained() {
        if (_NoChannel == 0)
            return 0;
        else if (_NoChannel < 0)
            return -1;
        else {
            final long remained = _NoChannel - System.currentTimeMillis() + _NoChannelBegin;
            if (remained < 0)
                return 0;

            return remained;
        }
    }

    public long getNoChannel() {
        return _NoChannel;
    }

    public void updateNoChannel(final long time) {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;

        setNoChannel(time);

        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            final String stmt = "UPDATE characters SET nochannel = ? WHERE obj_Id=?";
            statement = con.prepareStatement(stmt);
            statement.setLong(1, _NoChannel > 0 ? _NoChannel / 1000 : _NoChannel);
            statement.setInt(2, getObjectId());
            statement.executeUpdate();
        } catch (final Exception e) {
            _log.warning("Could not activate nochannel:" + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void setOnlineTime(final long time) {
        _onlineTime = time;
        _onlineBeginTime = System.currentTimeMillis();
    }

    public void setNoble(final boolean noble) {
        _noble = noble;
        updatePledgeClass();
    }

    /* varka silenos and ketra orc quests related functions */
    public void setVarka(final int faction) {
        _varka = faction;
    }

    public void setKetra(final int faction) {
        _ketra = faction;
    }

    public void setRam(final int faction) {
        _ram = faction;
    }

    /**
     * Set the _clan object, _clanId, _clanLeader Flag and title of the L2Player.<BR><BR>
     *
     * @param clan the clat to set
     */
    public void setClan(final L2Clan clan) {
        _clan = clan;

        if (clan == null) {
            _pledgeType = 0;
            _pledgeClass = 0;
            _powerGrade = 0;
            _lvlJoinedAcademy = 0;
            _apprentice = 0;
            return;
        }

        if (!clan.isMember(getObjectId())) {
            // char has been kicked from clan
            _log.fine("Char " + _name + " is kicked from clan: " + clan.getName());
            setClan(null);
            setTitle("");
            return;
        }

        setTitle("");
    }

    public void setPledgeType(final int typeId) {
        _pledgeType = typeId;
    }

    public void setPowerGrade(final int grade) {
        _powerGrade = grade;
    }

    public void setLvlJoinedAcademy(final int lvl) {
        _lvlJoinedAcademy = lvl;
    }

    public void setApprentice(final int apprentice) {
        _apprentice = apprentice;
    }

    public void updatePledgeClass() {
        _pledgeClass = getRank();
    }

    public int getRank() {
        if (getPvpKills() < 25)
            return RANK_NOVICE;
        else if (getPvpKills() < 100)
            return RANK_INITIATE;
        else if (getPvpKills() < 200)
            return RANK_MINOR;
        else if (getPvpKills() < 400)
            return RANK_EXPERT;
        else if (getPvpKills() < 800)
            return RANK_THUG;
        else if (getPvpKills() < 1200)
            return RANK_ADEPT;
        else if (getPvpKills() < 1800)
            return RANK_CONJURER;
        else if (getPvpKills() < 2500)
            return RANK_ELITE;
        else if (getPvpKills() < 3000)
            return RANK_ARCHON;
        return RANK_LEADER;
    }

    public void setDeleteTimer(final int deleteTimer) {
        _deleteTimer = deleteTimer;
    }

    public PlayerAccess getPlayerAccess() {
        return _playerAccess;
    }

    public void setNameColor(final int nameColor) {
        if (nameColor != Config.NORMAL_NAME_COLOUR && nameColor != Config.CLANLEADER_NAME_COLOUR && nameColor != Config.GM_NAME_COLOUR && nameColor != Config.SERVICES_OFFLINE_TRADE_NAME_COLOR)
            setVar("namecolor", Integer.toHexString(nameColor));
        else if (nameColor == Config.NORMAL_NAME_COLOUR)
            unsetVar("namecolor");
        _nameColor = nameColor;
    }

    public boolean getVarB(final String name, final boolean defaultVal) {
        final String var = user_variables.get(name);
        if (var == null)
            return defaultVal;
        return !(var.equals("0") || var.equalsIgnoreCase("false"));
    }

    public void setFistsWeaponItem(final L2Weapon weaponItem) {
        _fistsWeaponItem = weaponItem;
    }

    public L2Weapon findFistsWeaponItem(final int classId) {
        // human fighter fists
        if (classId >= 0x00 && classId <= 0x09)
            return (L2Weapon) ItemTable.getInstance().getTemplate(246);

        // human mage fists
        if (classId >= 0x0a && classId <= 0x11)
            return (L2Weapon) ItemTable.getInstance().getTemplate(251);

        // elven fighter fists
        if (classId >= 0x12 && classId <= 0x18)
            return (L2Weapon) ItemTable.getInstance().getTemplate(244);

        // elven mage fists
        if (classId >= 0x19 && classId <= 0x1e)
            return (L2Weapon) ItemTable.getInstance().getTemplate(249);

        // dark elven fighter fists
        if (classId >= 0x1f && classId <= 0x25)
            return (L2Weapon) ItemTable.getInstance().getTemplate(245);

        // dark elven mage fists
        if (classId >= 0x26 && classId <= 0x2b)
            return (L2Weapon) ItemTable.getInstance().getTemplate(250);

        // orc fighter fists
        if (classId >= 0x2c && classId <= 0x30)
            return (L2Weapon) ItemTable.getInstance().getTemplate(248);

        // orc mage fists
        if (classId >= 0x31 && classId <= 0x34)
            return (L2Weapon) ItemTable.getInstance().getTemplate(252);

        // dwarven fists
        if (classId >= 0x35 && classId <= 0x39)
            return (L2Weapon) ItemTable.getInstance().getTemplate(247);

        return null;
    }

    public void setUptime(final long time) {
        _uptime = time;
    }

    public void setLastAccess(final long value) {
        _lastAccess = value;
    }

    public void setRecomHave(final int value) {
        if (value > 255)
            _recomHave = 255;
        else if (value < 0)
            _recomHave = 0;
        else
            _recomHave = value;
    }

    public void setRecomLeft(final int value) {
        _recomLeft = value;
    }

    public void restoreRecipeBook() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT id FROM character_recipebook WHERE char_id=?");
            statement.setInt(1, getObjectId());
            rset = statement.executeQuery();

            while (rset.next()) {
                final int id = rset.getInt("id");
                final L2Recipe recipe = RecipeController.getInstance().getRecipeByRecipeId(id);
                registerRecipe(recipe, false);
            }
        } catch (final Exception e) {
            _log.warning("count not recipe skills:" + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }
    }

    /**
     * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2Player
     */
    public void registerRecipe(final L2Recipe recipe, final boolean saveDB) {
        if (recipe.isDwarvenRecipe())
            _recipebook.put(recipe.getId(), recipe);
        else
            _commonrecipebook.put(recipe.getId(), recipe);
        if (saveDB)
            mysql.set("REPLACE INTO character_recipebook (char_id, id) values(" + getObjectId() + "," + recipe.getId() + ")");
    }

    public void restoreTradeList() {
        if (getVar("selllist") != null) {
            _sellList = new ConcurrentLinkedQueue<TradeItem>();
            final String[] items = getVar("selllist").split(":");
            for (final String item : items) {
                if (item.equals(""))
                    continue;
                final String[] values = item.split(";");
                if (values.length < 3)
                    continue;
                final TradeItem i = new TradeItem();
                final int oId = Integer.parseInt(values[0]);
                int count = Integer.parseInt(values[1]);
                final int price = Integer.parseInt(values[2]);
                i.setObjectId(oId);

                final L2ItemInstance itemToSell = getInventory().getItemByObjectId(oId);

                if (count < 1 || itemToSell == null)
                    continue;

                if (count > itemToSell.getIntegerLimitedCount())
                    count = itemToSell.getIntegerLimitedCount();

                i.setCount(count);
                i.setOwnersPrice(price);
                i.setItemId(itemToSell.getItemId());
                i.setEnchantLevel(itemToSell.getEnchantLevel());
                _sellList.add(i);
            }
            if (_tradeList == null)
                _tradeList = new L2TradeList();
            if (getVar("sellstorename") != null)
                _tradeList.setSellStoreName(getVar("sellstorename"));
        }
        if (getVar("buylist") != null) {
            _buyList = new ConcurrentLinkedQueue<TradeItem>();
            final String[] items = getVar("buylist").split(":");
            for (final String item : items) {
                if (item.equals(""))
                    continue;
                final String[] values = item.split(";");
                if (values.length < 3)
                    continue;
                final TradeItem i = new TradeItem();
                i.setItemId(Integer.parseInt(values[0]));
                i.setCount(Integer.parseInt(values[1]));
                i.setOwnersPrice(Integer.parseInt(values[2]));
                _buyList.add(i);
            }
            if (_tradeList == null)
                _tradeList = new L2TradeList();
            if (getVar("buystorename") != null)
                _tradeList.setBuyStoreName(getVar("buystorename"));
        }
        if (getVar("createlist") != null) {
            _createList = new L2ManufactureList();
            final String[] items = getVar("createlist").split(":");
            for (final String item : items) {
                if (item.equals(""))
                    continue;
                final String[] values = item.split(";");
                if (values.length < 2)
                    continue;
                _createList.add(new L2ManufactureItem(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
            }
            if (getVar("manufacturename") != null)
                _createList.setStoreName(getVar("manufacturename"));
        }
    }

    public void setPrivateStoreType(final short type) {
        _privatestore = type;
        if (type != STORE_PRIVATE_NONE)
            setVar("storemode", String.valueOf(type));
        else
            unsetVar("storemode");
    }

    public void setSitting(final boolean val) {
        _isSitting = val;
    }

    public void setHero(final boolean hero) {
        _hero = hero;
        updatePledgeClass();
    }

    /**
     * Restore list of character professions and set up active proof Used when character is loading
     */
    public static void restoreCharSubClasses(final L2Player player) {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT class_id,exp,sp,level,curHp,curCp,curMp,active,isBase,death_penalty,skills,pvp FROM character_subclasses WHERE char_obj_id=?");
            statement.setInt(1, player.getObjectId());
            rset = statement.executeQuery();
            while (rset.next()) {
                final L2SubClass subClass = new L2SubClass();
                subClass.setBase(rset.getInt("isBase") != 0);
                subClass.setClassId(rset.getShort("class_id"));
                subClass.setLevel(rset.getByte("level"));
                subClass.setExp(rset.getLong("exp"));
                subClass.setSp(rset.getInt("sp"));
                subClass.setHp(rset.getDouble("curHp"));
                subClass.setMp(rset.getDouble("curMp"));
                subClass.setCp(rset.getDouble("curCp"));
                subClass.setActive(rset.getInt("active") != 0);
                subClass.setDeathPenalty(new DeathPenalty(player, rset.getByte("death_penalty")));
                subClass.setSkills(rset.getString("skills"));
                subClass.setPvPCount(rset.getInt("pvp"));
                subClass.setPlayer(player);

                player.getSubClasses().put(subClass.getClassId(), subClass);
            }

            if (player.getSubClasses().size() == 0)
                throw new Exception("There are no one subclass for player: " + player);

            final int BaseClassId = player.getBaseClassId();
            if (BaseClassId == -1)
                throw new Exception("There are no base subclass for player: " + player);

            for (final L2SubClass subClass : player.getSubClasses().values())
                if (subClass.isActive()) {
                    player.setActiveSubClass(subClass.getClassId(), false);
                    break;
                }

            if (player.getActiveClass() == null) {
                // если из-за какого-либо сбоя ни один из сабкласов не отмечен как активный помечаем базовый как активный
                final L2SubClass subClass = player.getSubClasses().get(BaseClassId);
                subClass.setActive(true);
                player.setActiveSubClass(subClass.getClassId(), false);
            }
        } catch (final Exception e) {
            _log.warning("Could not restore char sub-classes: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }
    }

    public int getBaseClassId() {
        return _baseClass;
    }

    public L2SubClass getActiveClass() {
        return _activeClass;
    }

    /**
     * Устанавливает активный сабкласс <li>Retrieve from the database all skills of this L2Player and add them to _skills </li> <li>Retrieve from the database all macroses of this L2Player and add
     * them to _macroses</li> <li>Retrieve from the database all shortCuts of this L2Player and add them to _shortCuts</li><BR><BR>
     */
    public void setActiveSubClass(final int subId, final boolean store) {
        final L2SubClass sub = getSubClasses().get(subId);
        if (sub == null) {
            System.out.print("WARNING! setActiveSubClass :: sub == null :: subId == " + subId);
            Thread.dumpStack();
            return;
        }

        if (getActiveClass() != null) {
            storeEffects();
            storeDisableSkills();
        }

        if (QuestManager.getQuest(422) != null) {
            final String qn = QuestManager.getQuest(422).getName();
            if (qn != null) {
                final QuestState qs = getQuestState(qn);
                if (qs != null)
                    qs.exitCurrentQuest(true);
            }
        }

        if (store) {
            final L2SubClass oldsub = getActiveClass();
            oldsub.setCp(getCurrentCp());
            // oldsub.setExp(getExp());
            // oldsub.setLevel(getLevel());
            // oldsub.setSp(getSp());
            oldsub.setHp(getCurrentHp());
            oldsub.setMp(getCurrentMp());
            oldsub.setActive(false);
            getSubClasses().put(getActiveClassId(), oldsub);
        }

        sub.setActive(true);
        setActiveClass(sub);
        getSubClasses().put(getActiveClassId(), sub);

        setClassId(subId, false);

        removeAllSkills();

        getEffectList().stopAllEffects();

        if (getPet() != null && getPet().isSummon() && isMageClass())
            getPet().unSummon();

        if (_cubics != null && !_cubics.isEmpty()) {
            for (final L2CubicInstance cubic : _cubics) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }
            _cubics.clear();
            _cubics = null;
        }

        checkRecom();
        restoreSkills();
        restoreSubclassSkills();
        rewardSkills();
        sendPacket(new ExStorageMaxCount(this));
        sendPacket(new SkillList(this));

        getInventory().refreshListeners();

        for (int i = 0; i < 3; i++)
            _henna[i] = null;

        restoreHenna();
        sendPacket(new HennaInfo(this));

        restoreEffects();
        restoreBufferSchemes();
        restoreLastKills();
        if (isInWorld())
            restoreDisableSkills();

        setCurrentHpMp(sub.getHp(), sub.getMp());
        setCurrentCp(sub.getCp());
        broadcastUserInfo(true);
        updateStats();

        _shortCuts.restore();
        sendPacket(new ShortCutInit(this));
        for (final int shotId : getAutoSoulShot())
            sendPacket(new ExAutoSoulShot(shotId, true));
        sendPacket(new SkillCoolTime(this));

        broadcastPacket(new SocialAction(getObjectId(), 15));

        getDeathPenalty().restore();

        setIncreasedForce(0);
    }

    public void storeEffects() {
        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.createStatement();
            statement.executeUpdate("DELETE FROM character_effects_save WHERE char_obj_id = " + getObjectId() + " AND class_index=" + getActiveClassId());

            if (_effectList == null || _effectList.isEmpty())
                return;

            int order = 0;
            final SqlBatch b = new SqlBatch("INSERT IGNORE INTO `character_effects_save` (`char_obj_id`,`skill_id`,`skill_level`,`effect_count`,`effect_cur_time`,`duration`,`order`,`class_index`) VALUES");

            synchronized (getEffectList()) {
                StringBuilder sb;
                for (L2Effect effect : getEffectList().getAllEffects())
                    if (effect != null && effect.isInUse() && !effect.getSkill().isToggle() && effect.getEffectType() != EffectType.HealOverTime && effect.getEffectType() != EffectType.CombatPointHealOverTime) {
                        if (effect.isSaveable()) {
                            sb = new StringBuilder("(");
                            sb.append(getObjectId()).append(",");
                            sb.append(effect.getSkill().getId()).append(",");
                            sb.append(effect.getSkill().getLevel()).append(",");
                            sb.append(effect.getCount()).append(",");
                            sb.append(effect.getTime()).append(",");
                            sb.append(effect.getPeriod()).append(",");
                            sb.append(order).append(",");
                            sb.append(getActiveClassId()).append(")");
                            b.write(sb.toString());
                        }
                        if ((effect = effect.getNext()) != null && effect.isSaveable()) {
                            sb = new StringBuilder("(");
                            sb.append(getObjectId()).append(",");
                            sb.append(effect.getSkill().getId()).append(",");
                            sb.append(effect.getSkill().getLevel()).append(",");
                            sb.append(effect.getCount()).append(",");
                            sb.append(effect.getTime()).append(",");
                            sb.append(effect.getPeriod()).append(",");
                            sb.append(order).append(",");
                            sb.append(getActiveClassId()).append(")");
                            b.write(sb.toString());
                        }
                        order++;
                    }
                if (Config.ALT_SAVE_UNSAVEABLE && _cubics != null)
                    for (final L2CubicInstance cubic : _cubics) {
                        sb = new StringBuilder("(");
                        sb.append(getObjectId()).append(",");
                        sb.append(cubic.getId() + L2CubicInstance.CUBIC_STORE_OFFSET).append(",");
                        sb.append(cubic.getLevel()).append(",1,");
                        sb.append(cubic.lifeLeft()).append(",1,");
                        sb.append(order++).append(",");
                        sb.append(getActiveClassId()).append(")");
                        b.write(sb.toString());
                    }
            }
            if (!b.isEmpty())
                statement.executeUpdate(b.close());
        } catch (final Exception e) {
            _log.warning("Could not store active effects data: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void storeDisableSkills() {
        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.createStatement();
            statement.executeUpdate("DELETE FROM character_skills_save WHERE char_obj_id = " + getObjectId() + " AND class_index=" + getActiveClassId());

            if (skillReuseTimeStamps.isEmpty())
                return;

            final SqlBatch b = new SqlBatch("INSERT IGNORE INTO `character_skills_save` (`char_obj_id`,`skill_id`,`class_index`,`end_time`,`reuse_delay_org`) VALUES");
            synchronized (skillReuseTimeStamps) {
                StringBuilder sb;
                for (final Entry<Integer, SkillTimeStamp> tmp : getSkillReuseTimeStamps().entrySet()) {
                    sb = new StringBuilder("(");
                    sb.append(getObjectId()).append(",");
                    sb.append(tmp.getKey()).append(",");
                    sb.append(getActiveClassId()).append(",");
                    sb.append(tmp.getValue().getEndTime()).append(",");
                    sb.append(tmp.getValue().getReuseBasic()).append(")");
                    b.write(sb.toString());
                }
            }
            if (!b.isEmpty())
                statement.executeUpdate(b.close());
        } catch (final Exception e) {
            _log.warning("Could not store disable skills data: " + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    // ------------------- Quest Engine ----------------------

    public QuestState getQuestState(String quest) {
        return _quests != null ? _quests.get(quest) : null;
    }

    public void setActiveClass(final L2SubClass activeClass) {
        if (activeClass == null) {
            System.out.print("WARNING! setActiveClass(null);");
            Thread.dumpStack();
        }
        _activeClass = activeClass;
    }

    public int getActiveClassId() {
        return getActiveClass().getClassId();
    }

    /**
     * Set the template of the L2Player.
     *
     * @param id The Identifier of the L2PlayerTemplate to set to the L2Player
     */
    public synchronized void setClassId(final int id, final boolean noban) {
        if (!noban && !(ClassId.values()[id].equalsOrChildOf(ClassId.values()[getActiveClassId()]) || getPlayerAccess().CanChangeClass || Config.EVERYBODY_HAS_ADMIN_RIGHTS)) {
            Thread.dumpStack();
            Util.handleIllegalPlayerAction(this, "L2Player[1535]", "tried to change class " + getActiveClassId() + " to " + id, 1);
            return;
        }

        // Если новый ID не принадлежит имеющимся классам значит это новая профа
        if (!getSubClasses().containsKey(id)) {
            final L2SubClass cclass = getActiveClass();
            getSubClasses().remove(getActiveClassId());
            changeClassInDb(cclass.getClassId(), id);
            if (cclass.isBase()) {
                setBaseClass(id);
                addClanPointsOnProfession(id);
            }
            cclass.setClassId(id);
            getSubClasses().put(id, cclass);
            rewardSkills();
            storeCharSubClasses();

            // Социалка при получении профы
            broadcastPacket(new MagicSkillUse(this, this, 5103, 1, 1000, 0));
            // broadcastPacket(new SocialAction(getObjectId(), 16));
            sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
            broadcastUserInfo(true);
        }

        final L2PlayerTemplate t = CharTemplateTable.getInstance().getTemplate(id, getSex() == 1);
        if (t == null) {
            _log.severe("Missing template for classId: " + id);
            // do not throw error - only print error
            return;
        }

        // Set the template of the L2Player
        setTemplate(t);

        // Update class icon in party and clan
        if (isInParty())
            getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
        if (getClan() != null)
            getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
    }

    /**
     * Changing index of class in DB, used for changing class when finished professional quests
     *
     * @param oldclass
     * @param newclass
     */
    public synchronized void changeClassInDb(final int oldclass, final int newclass) {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("UPDATE character_subclasses SET class_id=? WHERE char_obj_id=? AND class_id=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("UPDATE character_hennas SET class_index=? WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("UPDATE character_shortcuts SET class_index=? WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("UPDATE character_skills SET class_index=? WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("UPDATE character_effects_save SET class_index=? WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);

            statement = con.prepareStatement("UPDATE character_skills_save SET class_index=? WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, newclass);
            statement.setInt(2, getObjectId());
            statement.setInt(3, oldclass);
            statement.executeUpdate();
            DatabaseUtils.closeStatement(statement);
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void addClanPointsOnProfession(final int id) {
        if (getLvlJoinedAcademy() != 0 && _clan != null && _clan.getLevel() >= 5 && ClassId.values()[id].getLevel() == 2) {
            _clan.incReputation(100, true, "Academy");
            _clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
            _clan.broadcastToOnlineMembers(new PledgeStatusChanged(_clan));
        } else if (getLvlJoinedAcademy() != 0 && _clan != null && _clan.getLevel() >= 5 && ClassId.values()[id].getLevel() > 2) {
            int earnedPoints = 0;
            if (getLvlJoinedAcademy() <= 16)
                earnedPoints = 650;
            else if (getLvlJoinedAcademy() >= 39)
                earnedPoints = 190;
            else
                earnedPoints = 650 - (getLvlJoinedAcademy() - 16) * 20;

            _clan.removeClanMember(getObjectId());
            final SystemMessage sm = new SystemMessage(SystemMessage.CLAN_ACADEMY_MEMBER_S1_HAS_SUCCESSFULLY_COMPLETED_THE_2ND_CLASS_TRANSFER_AND_OBTAINED_S2_CLAN_REPUTATION_POINTS);
            sm.addString(getName());
            sm.addNumber(_clan.incReputation(earnedPoints, true, "Academy"));
            _clan.broadcastToOnlineMembers(sm);
            _clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
            _clan.broadcastToOnlineMembers(new PledgeStatusChanged(_clan));
            _clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListDelete(getName()), this);

            setLvlJoinedAcademy(0);
            setClan(null);
            setTitle("");
            sendPacket(Msg.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES);
            setLeaveClanTime(0);

            broadcastUserInfo(true);

            sendPacket(new PledgeShowMemberListDeleteAll());

            final L2ItemInstance academyCirclet = ItemTable.getInstance().createItem(8181);
            getInventory().addItem(academyCirclet);
            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addString("Academy Reward").addNumber(1));
        }
    }

    /**
     * Сохраняет информацию о классах в БД
     */
    public void storeCharSubClasses() {
        final L2SubClass main = getActiveClass();
        if (main != null) {
            main.setCp(getCurrentCp());
            // main.setExp(getExp());
            // main.setLevel(getLevel());
            // main.setSp(getSp());
            main.setHp(getCurrentHp());
            main.setMp(getCurrentMp());
            main.setActive(true);
            getSubClasses().put(getActiveClassId(), main);
        } else
            _log.warning("Could not store char sub data, main class " + getActiveClassId() + " not found for " + this);

        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.createStatement();

            StringBuilder sb;
            for (final L2SubClass subClass : getSubClasses().values()) {
                sb = new StringBuilder("UPDATE character_subclasses SET ");
                sb.append("exp=").append(subClass.getExp()).append(",");
                sb.append("sp=").append(subClass.getSp()).append(",");
                sb.append("curHp=").append(subClass.getHp()).append(",");
                sb.append("curMp=").append(subClass.getMp()).append(",");
                sb.append("curCp=").append(subClass.getCp()).append(",");
                sb.append("level=").append(subClass.getLevel()).append(",");
                sb.append("active=").append(subClass.isActive() ? 1 : 0).append(",");
                sb.append("death_penalty=").append(subClass.getDeathPenalty().getLevelOnSaveDB()).append(",");
                sb.append("skills='").append(subClass.getSkills()).append("',");
                sb.append("pvp=").append(subClass.getPvPCount());
                sb.append(" WHERE char_obj_id=").append(getObjectId()).append(" AND class_id=").append(subClass.getClassId()).append(" LIMIT 1");
                statement.executeUpdate(sb.toString());
            }

            sb = new StringBuilder("UPDATE LOW_PRIORITY character_subclasses SET ");
            sb.append("maxHp=").append(getMaxHp()).append(",");
            sb.append("maxMp=").append(getMaxMp()).append(",");
            sb.append("maxCp=").append(getMaxCp());
            sb.append(" WHERE char_obj_id=").append(getObjectId()).append(" AND active=1 LIMIT 1");
            statement.executeUpdate(sb.toString());
        } catch (final Exception e) {
            _log.warning("Could not store char sub data: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public boolean isInParty() {
        return _party != null;
    }

    /**
     * @return True if the L2Player is a Mage.<BR><BR>
     */
    @Override
    public boolean isMageClass() {
        return _template.baseMAtk > 3;
    }

    private void checkRecom() {
        final Calendar temp = Calendar.getInstance();
        temp.set(Calendar.HOUR_OF_DAY, 13);
        temp.set(Calendar.MINUTE, 0);
        temp.set(Calendar.SECOND, 0);
        long count = Math.round((System.currentTimeMillis() / 1000 - _lastAccess) / 86400);
        if (count == 0 && _lastAccess < temp.getTimeInMillis() / 1000 && System.currentTimeMillis() > temp.getTimeInMillis())
            count++;

        for (int i = 1; i < count; i++)
            if (_recomHave < 200)
                _recomHave -= 2;
            else
                _recomHave -= 3;

        if (_recomHave < 0)
            _recomHave = 0;

        if (getLevel() < 10)
            return;

        if (count > 0)
            restartRecom();
    }

    @Override
    public final byte getLevel() {
        return _activeClass == null ? 1 : _activeClass.getLevel();
    }

    public void restartRecom() {
        try {
            _recomChars.clear();

            if (getLevel() < 20)
                _recomLeft = 3;
            else if (getLevel() < 40)
                _recomLeft = 6;
            else
                _recomLeft = 9;

            if (_recomHave < 200)
                _recomHave -= 2;
            else
                _recomHave -= 3;

            if (_recomHave < 0)
                _recomHave = 0;
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve from the database all skills of this L2Player and add them to _skills.
     */
    private void restoreSkills() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            // Retrieve all skills of this L2Player from the database
            // Send the SQL query : SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? to the database
            final String _SQL = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement(_SQL);
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());
            rset = statement.executeQuery();

            // Go though the recordset of this SQL query
            while (rset.next()) {
                final int id = rset.getInt("skill_id");
                final int level = rset.getInt("skill_level");

                if (id > 9000)
                    continue; // fake skills for base stats

                // Create a L2Skill object for each record
                final L2Skill skill = SkillTable.getInstance().getInfo(id, level);

                if (skill == null)
                    continue;

                // Remove skill if not possible
                if (!Config.ALT_USE_ILLEGAL_SKILLS)
                    if (!_playerAccess.IsGM && !skill.isCommon() && !SkillTreeTable.getInstance().isSkillPossible(this, skill.getId(), skill._level)) {
                        int ReturnSP = SkillTreeTable.getInstance().getSkillCost(this, skill);
                        if (ReturnSP == Integer.MAX_VALUE || ReturnSP < 0)
                            ReturnSP = 0;
                        removeSkill(skill, true);
                        removeSkillFromShortCut(skill.getId());
                        if (ReturnSP > 0)
                            setSp(getSp() + ReturnSP);
                        illegalAction("has skill " + skill.getName() + " / ReturnSP: " + ReturnSP, 0);
                        continue;
                    }

                // Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
                super.addSkill(skill);
            }

            // Restore noble skills
            if (_noble) {
                super.addSkill(SkillTable.getInstance().getInfo(1323, 1));
                if (isClanLeader() && getClan().getHasCastle() > 0)
                    super.addSkill(SkillTable.getInstance().getInfo(327, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1324, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1325, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1326, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1327, 1));
            }

            // Restore Hero skills at main class only
            if (_hero && getBaseClassId() == getActiveClassId()) {
                super.addSkill(SkillTable.getInstance().getInfo(395, 1));
                super.addSkill(SkillTable.getInstance().getInfo(396, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1374, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1375, 1));
                super.addSkill(SkillTable.getInstance().getInfo(1376, 1));
            }

            if (_clan != null) {
                // Restore clan leader siege skills
                if (_clan.getLeaderId() == getObjectId() && _clan.getLevel() >= CastleSiegeManager.getSiegeClanMinLevel())
                    SiegeManager.addSiegeSkills(this);

                // Restore clan skills
                _clan.addAndShowSkillsToPlayer(this);
            }

            // Give dwarven craft skill
            if (getActiveClassId() >= 53 && getActiveClassId() <= 57 || getActiveClassId() == 117 || getActiveClassId() == 118)
                super.addSkill(SkillTable.getInstance().getInfo(1321, 1));
            super.addSkill(SkillTable.getInstance().getInfo(1322, 1));

            if (Config.UNSTUCK_SKILL && getSkillLevel(1050) < 0)
                super.addSkill(SkillTable.getInstance().getInfo(2099, 1));
        } catch (final Exception e) {
            _log.warning("Could not restore skills for player objId: " + getObjectId());
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }
    }

    /**
     * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.
     *
     * @return The L2Skill removed
     */
    public L2Skill removeSkill(final L2Skill skill, final boolean fromDB) {
        // Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
        L2Skill oldSkill = super.removeSkill(skill);

        if (!fromDB)
            return oldSkill;

        if (oldSkill == null)
            oldSkill = skill;

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            // Remove or update a L2Player skill from the character_skills table of the database
            con = L2DatabaseFactory.getInstance().getConnection();
            if (oldSkill != null) {
                statement = con.prepareStatement("DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?");
                statement.setInt(1, oldSkill.getId());
                statement.setInt(2, getObjectId());
                statement.setInt(3, getActiveClassId());
                statement.execute();
            }
        } catch (final Exception e) {
            _log.log(Level.WARNING, "Error could not delete Skill:", e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }

        return oldSkill;
    }

    public void removeSkillFromShortCut(final int skillId) {
        _shortCuts.deleteShortCutBySkillId(skillId);
    }

    public void setSp(final int sp) {
        if (_activeClass != null)
            _activeClass.setSp(sp);
    }

    public int getSp() {
        return _activeClass == null ? 0 : _activeClass.getSp();
    }

    public final void illegalAction(final String msg, final Integer jail_items) {
        Log.IllegalPlayerAction(this, msg, jail_items);
    }

    public boolean isClanLeader() {
        return _clan != null && _objectId == _clan.getLeaderId();
    }

    private void restoreSubclassSkills() {
        if (!getActiveClass().isBase())
            return;
        try {
            for (final L2SubClass subClass : getSubClasses().values())
                if (!subClass.getSkills().isEmpty())
                    for (final String i : subClass.getSkills().split(";")) {
                        final int id = Integer.parseInt(i);
                        final int level = Math.max(1, getSkillLevel(id) + 1);
                        final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
                        if (skill != null)
                            super.addSkill(skill);
                        else
                            System.out.println("Not found skill id: " + id + ", level: " + level);
                    }
        } catch (final Exception e) {
            _log.warning("Could not restore subclass skills for player objId: " + getObjectId());
            e.printStackTrace();
        }
    }

    /**
     * Give Expertise skill of this level.<BR><BR> <B><U> Actions</U> :</B><BR><BR> <li>Get the Level of the L2Player </li> <li>Add the Expertise skill corresponding to its Expertise level</li>
     * <li>Update the overloaded status of the L2Player</li><BR><BR> <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR><BR>
     */
    public void rewardSkills() {
        // Calculate the current higher Expertise of the L2Player
        final int level = getLevel();
        for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
            if (level >= EXPERTISE_LEVELS[i])
                expertiseIndex = i;

        // Add the Expertise skill corresponding to its Expertise level
        if (expertiseIndex > 0) {
            final L2Skill skill = SkillTable.getInstance().getInfo(239, expertiseIndex);
            addSkill(skill, false);
        }

        boolean update = false;
        if (Config.AUTO_LEARN_SKILLS && level <= Config.AUTO_LEARN_SKILLS_MAX_LEVEL) {
            int unLearnable = 0;
            ArrayList<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
            while (skills.size() > unLearnable) {
                unLearnable = 0;
                for (final L2SkillLearn s : skills) {
                    final L2Skill sk = SkillTable.getInstance().getInfo(s.id, s.skillLevel);
                    if (sk == null || !sk.getCanLearn(getClassId())) {
                        unLearnable++;
                        continue;
                    }
                    addSkill(sk, true);
                }
                skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
            }
            update = true;
        } else
            // Скиллы дающиеся бесплатно не требуют изучения
            for (final L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(this, getClassId()))
                if (skill._repCost == 0 && skill._spCost == 0 && skill.itemCount == 0) {
                    final L2Skill sk = SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel());
                    addSkill(sk, true);
                    if (getAllShortCuts().size() > 0 && sk.getLevel() > 1)
                        for (final L2ShortCut sc : getAllShortCuts())
                            if (sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL) {
                                final L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, sk.getLevel());
                                sendPacket(new ShortCutRegister(newsc));
                                registerShortCut(newsc);
                            }
                    update = true;
                }

        if (update)
            sendPacket(new SkillList(this));

        // This function gets called on login, so not such a bad place to check weight
        // Update the overloaded status of the L2Player
        refreshOverloaded();
        refreshExpertisePenalty();
    }

    public ClassId getClassId() {
        return getTemplate().classId;
    }

    /**
     * Add a skill to the L2Player _skills and its Func objects to the calculator set of the L2Player and save update in the character_skills table of the database.
     *
     * @return The L2Skill replaced or null if just added a new L2Skill
     */
    public L2Skill addSkill(final L2Skill newSkill, final boolean store) {
        if (newSkill == null)
            return null;

        // Add a skill to the L2Player _skills and its Func objects to the calculator set of the L2Player
        final L2Skill oldSkill = super.addSkill(newSkill);

        if (newSkill.equals(oldSkill))
            return oldSkill;

        // Add or update a L2Player skill in the character_skills table of the database
        if (store)
            storeSkill(newSkill, oldSkill);

        return oldSkill;
    }

    /**
     * Add or update a L2Player skill in the character_skills table of the database.
     */
    private void storeSkill(final L2Skill newSkill, final L2Skill oldSkill) {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            if (oldSkill != null && newSkill != null) {
                statement = con.prepareStatement("UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?");
                statement.setInt(1, newSkill.getLevel());
                statement.setInt(2, oldSkill.getId());
                statement.setInt(3, getObjectId());
                statement.setInt(4, getActiveClassId());
                statement.execute();
            } else if (newSkill != null) {
                statement = con.prepareStatement("REPLACE INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) values(?,?,?,?,?)");
                statement.setInt(1, getObjectId());
                statement.setInt(2, newSkill.getId());
                statement.setInt(3, newSkill.getLevel());
                statement.setString(4, newSkill.getName());
                statement.setInt(5, getActiveClassId());
                statement.execute();
            } else
                _log.warning("could not store new skill. its NULL");
        } catch (final Exception e) {
            _log.log(Level.WARNING, "Error could not store Skills:", e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    // ----------------- End of Quest Engine -------------------

    public Collection<L2ShortCut> getAllShortCuts() {
        return _shortCuts.getAllShortCuts();
    }

    public void registerShortCut(final L2ShortCut shortcut) {
        _shortCuts.registerShortCut(shortcut);
    }

    public void refreshOverloaded() {
        if (isMassUpdating() || getMaxLoad() <= 0 /*|| Config.DISABLE_WEIGHT_PENALTY*/)
            return;

        setOverloaded(getCurrentLoad() > getMaxLoad());
        final double weightproc = 100. * (getCurrentLoad() - calcStat(Stats.MAX_NO_PENALTY_LOAD, 0, this, null)) / getMaxLoad();
        int newWeightPenalty = 0;

        if (weightproc < 50)
            newWeightPenalty = 0;
        else if (weightproc < 66.6)
            newWeightPenalty = 1;
        else if (weightproc < 80)
            newWeightPenalty = 2;
        else if (weightproc < 100)
            newWeightPenalty = 3;
        else
            newWeightPenalty = 4;

        if (_curWeightPenalty == newWeightPenalty)
            return;

        _curWeightPenalty = newWeightPenalty;
        if (_curWeightPenalty > 0)
            super.addSkill(SkillTable.getInstance().getInfo(4270, _curWeightPenalty));
        else
            super.removeSkill(getKnownSkill(4270));

        sendPacket(new EtcStatusUpdate(this));
    }

    public int getMaxLoad() {
        // Weight Limit = (CON Modifier*69000)*Skills
        // Source http://l2d.bravehost.com/weightlimit.html (May 2007)
        // Fitted exponential curve to the data
        final int con = getCON();
        if (con < 1)
            return (int) (31000 * Config.MAXLOAD_MODIFIER);
        else if (con > 59)
            return (int) (176000 * Config.MAXLOAD_MODIFIER);
        else
            return (int) calcStat(Stats.MAX_LOAD, Math.pow(1.029993928, con) * 30495.627366 * Config.MAXLOAD_MODIFIER, this, null);
    }

    public int getCurrentLoad() {
        return getInventory().getTotalWeight();
    }

    public void refreshExpertisePenalty() {
        if (isMassUpdating())
            return;

        int newPenalty = 0;
        final L2ItemInstance[] items = getInventory().getItems();
        for (final L2ItemInstance item : items)
            if (item != null && item.isEquipped()) {
                final int crystaltype = item.getItem().getCrystalType().ordinal();
                if (crystaltype > newPenalty)
                    newPenalty = crystaltype;
            }

        newPenalty = newPenalty - expertiseIndex;
        if (newPenalty <= 0)
            newPenalty = 0;

        if (expertisePenalty == newPenalty)
            return;

        expertisePenalty = newPenalty;
        if (newPenalty > 0)
            super.addSkill(SkillTable.getInstance().getInfo(4267, expertisePenalty));
        else
            super.removeSkill(getKnownSkill(4267));
        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Retrieve from the database all Henna of this L2Player, add them to _henna and calculate stats of the L2Player.<BR><BR>
     */
    private void restoreHenna() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("select slot, symbol_id from character_hennas where char_obj_id=? AND class_index=?");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());
            rset = statement.executeQuery();

            for (int i = 0; i < 3; i++)
                _henna[i] = null;

            while (rset.next()) {
                final int slot = rset.getInt("slot");
                if (slot < 1 || slot > 3)
                    continue;

                final int symbol_id = rset.getInt("symbol_id");

                L2HennaInstance sym;

                if (symbol_id != 0) {
                    final L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);
                    if (tpl != null) {
                        sym = new L2HennaInstance(tpl);
                        _henna[slot - 1] = sym;
                    }
                }
            }
        } catch (final Exception e) {
            _log.warning("could not restore henna: " + e);
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }

        // Calculate Henna modifiers of this L2Player
        recalcHennaStats();
    }

    /**
     * Calculate Henna modifiers of this L2Player.
     */
    private void recalcHennaStats() {
        _hennaINT = 0;
        _hennaSTR = 0;
        _hennaCON = 0;
        _hennaMEN = 0;
        _hennaWIT = 0;
        _hennaDEX = 0;

        for (int i = 0; i < 3; i++) {
            if (_henna[i] == null)
                continue;
            _hennaINT += _henna[i].getStatINT();
            _hennaSTR += _henna[i].getStatSTR();
            _hennaMEN += _henna[i].getStatMEM();
            _hennaCON += _henna[i].getStatCON();
            _hennaWIT += _henna[i].getStatWIT();
            _hennaDEX += _henna[i].getStatDEX();
        }

        if (_hennaINT > 5)
            _hennaINT = 5;
        if (_hennaSTR > 5)
            _hennaSTR = 5;
        if (_hennaMEN > 5)
            _hennaMEN = 5;
        if (_hennaCON > 5)
            _hennaCON = 5;
        if (_hennaWIT > 5)
            _hennaWIT = 5;
        if (_hennaDEX > 5)
            _hennaDEX = 5;
    }

    public void restoreEffects() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("SELECT `skill_id`,`skill_level`,`effect_count`,`effect_cur_time`,`duration` FROM `character_effects_save` WHERE `char_obj_id`=? AND `class_index`=? ORDER BY `order` ASC");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());

            rset = statement.executeQuery();
            while (rset.next()) {
                int skillId = rset.getInt("skill_id");
                final int skillLvl = rset.getInt("skill_level");
                final int effectCount = rset.getInt("effect_count");
                final long effectCurTime = rset.getLong("effect_cur_time");
                final long duration = rset.getLong("duration");

                if (skillId >= L2CubicInstance.CUBIC_STORE_OFFSET) // cubic
                {
                    skillId -= L2CubicInstance.CUBIC_STORE_OFFSET;
                    addCubic(skillId, skillLvl, (int) effectCurTime, true);
                    continue;
                }

                final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);

                if (skill == null) {
                    System.out.println("Can't restore Effect\tskill: " + skillId + ":" + skillLvl + " " + toFullString());
                    Thread.dumpStack();
                } else
                    for (final EffectTemplate et : skill.getEffectTemplates()) {
                        if (et == null)
                            continue;
                        final Env env = new Env(this, this, skill);
                        final L2Effect effect = et.getEffect(env);
                        if (effect != null) {
                            if (effectCount == 1) {
                                effect.setCount(effectCount);
                                effect.setPeriod(duration - effectCurTime);
                            } else {
                                effect.setPeriod(duration);
                                effect.setCount(effectCount);
                            }
                            getEffectList().addEffect(effect);
                        }
                    }
            }

            DatabaseUtils.closeDatabaseSR(statement, rset);

            statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_id = ? AND class_index=?");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());
            statement.executeUpdate();
        } catch (final Exception e) {
            _log.warning("Could not restore active effects data [charId: " + getObjectId() + "; ActiveClassId: " + getActiveClassId() + "]: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }

        updateEffectIcons();
        broadcastUserInfo(true);
    }

    public void addCubic(final int id, final int level, final int lifetime, final boolean givenByOther) {
        if (_cubics != null)
            for (final L2CubicInstance old : _cubics)
                if (old.getId() == id)
                    old.deleteMe();
        if (_cubics == null)
            _cubics = new GCSArray<L2CubicInstance>(4);
        final int mastery = Math.max(0, getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY));
        if (_cubics.size() > mastery) {
            sendPacket(new SystemMessage(SystemMessage.CUBIC_SUMMONING_FAILED));
            return;
        }
        _cubics.add(new L2CubicInstance(this, id, level, lifetime, givenByOther));
    }

    public final String toFullString() {
        final StringBuffer sb = new StringBuffer(160);

        sb.append("Player '").append(getName()).append("' [oid=").append(_objectId).append(", account='").append(getAccountName()).append(", ip=").append(getIP()).append("']");
        return sb.toString();
    }

    @Override
    public void updateEffectIcons() {
        if (isMassUpdating())
            return;

        final L2Effect[] effects = getEffectList().getAllFirstEffects();
        Arrays.sort(effects, EffectsComparator.getInstance());

        final PartySpelled ps = new PartySpelled(this, false);
        final AbnormalStatusUpdate mi = new AbnormalStatusUpdate();
        final ExOlympiadSpelledInfo os = new ExOlympiadSpelledInfo();

        for (final L2Effect effect : effects) {
            if (effect == null || !effect.isInUse())
                continue;

            if (effect.getStackType().equalsIgnoreCase("HpRecoverCast"))
                sendPacket(new ShortBuffStatusUpdate(effect));
            else {
                effect.addIcon(mi);
                if (_party != null)
                    effect.addPartySpelledIcon(ps);
            }

            if (Config.ENABLE_OLYMPIAD && isInOlympiadMode())
                if (Olympiad.getSpectators(getOlympiadGameId()) != null)
                    effect.addOlympiadSpelledIcon(this, os);
        }

        sendPacket(mi);
        if (_party != null)
            _party.broadcastToPartyMembers(ps);

        if (Config.ENABLE_OLYMPIAD && Olympiad.getSpectators(getOlympiadGameId()) != null)
            for (final L2Player spectator : Olympiad.getSpectators(getOlympiadGameId())) {
                if (spectator == null)
                    continue;
                spectator.sendPacket(os);
            }
    }

    public boolean isInOlympiadMode() {
        return _inOlympiadMode;
    }

    public int getOlympiadGameId() {
        return _olympiadGameId;
    }

    public void restoreBufferSchemes() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT * FROM character_buffs_schemes WHERE char_id=?");
            statement.setInt(1, getObjectId());
            rset = statement.executeQuery();
            while (rset.next()) {
                int skillId = rset.getInt("buff_id");
                String scheme = rset.getString("s_name");
                if (!_buffs.containsKey(scheme))
                    _buffs.put(scheme, new GArray<Integer>());
                _buffs.get(scheme).add(skillId);
            }
        } catch (final Exception e) {
            _log.warning("Could not restore bufflist data [charId: " + getObjectId() + "]: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }
    }

    public void restoreLastKills() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT * FROM character_pvplog WHERE player=?");
            statement.setInt(1, getObjectId());
            rset = statement.executeQuery();
            while (rset.next()) {
                _lastPvPs.add(new PvPinfo(rset.getString("time"), rset.getString("oponent"), rset.getString("result")));
            }
        } catch (final Exception e) {
            _log.warning("Could not restore pvplog data [charId: " + getObjectId() + "]: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }
    }

    public void restoreDisableSkills() {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("SELECT skill_id,end_time,reuse_delay_org FROM character_skills_save WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());

            rset = statement.executeQuery();
            while (rset.next()) {
                final int skillId = rset.getInt("skill_id");
                final int skillLevel = Math.max(getSkillLevel(skillId), 1);
                final long endTime = rset.getLong("end_time");
                final long rDelayOrg = rset.getLong("reuse_delay_org");
                final long curTime = System.currentTimeMillis();

                final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

                if (skill != null && endTime - curTime > 10) {
                    getSkillReuseTimeStamps().put(skillId, new SkillTimeStamp(skillId, endTime, rDelayOrg));
                    disableSkill(skillId, endTime - curTime);
                    disableItem(skill, rDelayOrg, endTime - curTime);
                }
            }
            DatabaseUtils.closeDatabaseSR(statement, rset);

            statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id = ? AND class_index=?");
            statement.setInt(1, getObjectId());
            statement.setInt(2, getActiveClassId());
            statement.executeUpdate();
        } catch (final Exception e) {
            _log.warning("Could not restore active skills data for " + getObjectId() + "/" + getActiveClassId());
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rset);
        }

        updateEffectIcons();
    }

    @Override
    public void disableItem(final L2Skill handler, final long timeTotal, final long timeLeft) {
        if (handler.isHandler() && timeLeft > 1000)
            if (handler.getReuseGroupId() > 0) {
                sendPacket(new ExUseSharedGroupItem(handler._itemConsumeId[0], handler.getReuseGroupId(), (int) timeLeft, (int) timeTotal));
                for (final Integer skill_id : handler.getReuseGroup())
                    if (!isSkillDisabled(skill_id))
                        disableSkill(skill_id, timeLeft);
            }

        itemreuses(handler);
    }

    @Override
    public boolean isSkillDisabled(final Integer skillId) {
        synchronized (skillReuseTimeStamps) {
            final SkillTimeStamp sts = skillReuseTimeStamps.get(skillId);
            if (sts == null)
                return false;
            if (sts.hasNotPassed())
                return true;
            skillReuseTimeStamps.remove(skillId);
        }
        return super.isSkillDisabled(skillId);
    }

    private void itemreuses(L2Skill handler) {
        for (SkillTimeStamp sk : getSkillReuseTimeStamps().values())
            sendPacket(new ExUseSharedGroupItem(handler._itemConsumeId[0], handler.getReuseGroupId(), (int) sk.getReuseCurrent(), (int) sk.getReuseBasic()));
    }

    /**
     * Отправляет UserInfo даному игроку и CharInfo всем окружающим.<BR><BR> <B><U> Концепт</U> :</B><BR><BR> Сервер шлет игроку UserInfo. Сервер вызывает метод
     * {@link L2Player#broadcastPacketToOthers(L2GameServerPacket)} для рассылки CharInfo<BR><BR> <B><U> Действия</U> :</B><BR><BR> <li>Отсылка игроку UserInfo(личные и
     * общие данные)</li> <li>Отсылка другим игрокам CharInfo(Public data only)</li><BR><BR> <FONT COLOR=#FF0000><B> <U>Внимание</U> : НЕ ПОСЫЛАЙТЕ UserInfo другим игрокам либо CharInfo даному
     * игроку.<BR> НЕ ВЫЗЫВАЕЙТЕ ЭТОТ МЕТОД КРОМЕ ОСОБЫХ ОБСТОЯТЕЛЬСТВ(смена сабкласса к примеру)!!! Траффик дико кушается у игроков и начинаются лаги.<br> Используйте метод
     * {@link L2Player#sendChanges()}</B></FONT><BR><BR>
     */
    @Override
    public void broadcastUserInfo(boolean force) {
        sendUserInfo(force);

        if (isInvisible())
            return;

        if (Config.BROADCAST_CHAR_INFO_INTERVAL == 0)
            force = true;

        if (force) {
            broadcastCharInfo();
            if (_broadcastCharInfoTask != null) {
                _broadcastCharInfoTask.cancel(true);
                _broadcastCharInfoTask = null;
            }
            return;
        }

        if (_broadcastCharInfoTask != null)
            return;

        _broadcastCharInfoTask = ThreadPoolManager.getInstance().scheduleAi(new BroadcastCharInfoTask(), Config.BROADCAST_CHAR_INFO_INTERVAL, true);
    }

    public void sendUserInfo(boolean force) {
        if (entering || isLogoutStarted())
            return;

        if (Config.USER_INFO_INTERVAL == 0)
            force = true;

        if (force) {
            sendPacket(new UserInfo(this));
            if (_userInfoTask != null) {
                _userInfoTask.cancel(true);
                _userInfoTask = null;
            }
            return;
        }

        if (_userInfoTask != null)
            return;

        _userInfoTask = ThreadPoolManager.getInstance().scheduleAi(new UserInfoTask(), Config.USER_INFO_INTERVAL, true);
    }

    public boolean isLogoutStarted() {
        return _logoutStarted;
    }

    public boolean isInvisible() {
        return _invisible;
    }

    private void broadcastCharInfo() {
        if (isInvisible())
            return;

        for (final L2Player player : L2World.getAroundPlayers(this))
            if (player != null && _objectId != player.getObjectId()) {
                player.sendPacket(new CharInfo(this, player, true));
                if (_clan != null && player.getClan() != null && player.getClan().isAtWarWith(_clan.getClanId()))
                    player.sendPacket(new RelationChanged(this, isAutoAttackable(player), getRelation(player)));
            }
    }

    public int getRelation(final L2Player target) {
        int result = 0;

        if (_pvpFlag != 0)
            result |= RELATION_PVP_FLAG;

        if (_karma > 0)
            result |= RELATION_HAS_KARMA;

        if (_siegeState != 0) {
            result |= RELATION_INSIEGE;
            if (isClanLeader())
                result |= RELATION_LEADER;

            if (_siegeState != target.getSiegeState())
                result |= RELATION_ENEMY;
            else
                result |= RELATION_ALLY;
            if (_siegeState == 1)
                result |= RELATION_ATTACKER;
        }

        if (_clan != null && target.getClan() != null)
            if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY && getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
                if (target.getClan().isAtWarWith(_clan.getClanId())) {
                    result |= RELATION_1SIDED_WAR;
                    if (_clan.isAtWarWith(target.getClan().getClanId()))
                        result |= RELATION_MUTUAL_WAR;
                }

        return result;
    }

    /**
     * Возвращает состояние осады L2Player.<BR> 1 = attacker, 2 = defender, 0 = не учавствует
     *
     * @return состояние осады
     */
    public int getSiegeState() {
        return _siegeState;
    }

    /**
     * Update Stats of the L2Player client side by sending Server->Client packet UserInfo/StatusUpdate to this L2Player and CharInfo/StatusUpdate to all L2Player in its _KnownPlayers
     * (broadcast).<BR><BR>
     */
    @Override
    public void updateStats() {
        refreshOverloaded();
        refreshExpertisePenalty();
        sendChanges();
    }

    public ConcurrentSkipListSet<Integer> getAutoSoulShot() {
        return _activeSoulShots;
    }

    @Override
    public void setIncreasedForce(int i) {
        if (i > 8)
            i = 8;

        if (i < 0)
            i = 0;

        if (i != 0 && i > _increasedForce)
            sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_INCREASED_TO_S1_LEVEL).addNumber(i));

        _increasedForce = i;
        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Добавляет адену игроку.<BR><BR>
     *
     * @param adena -
     *              сколько адены дать
     * @return L2ItemInstance - новое количество адены TODO добавить параметр update как в reduceAdena
     */
    public L2ItemInstance addAdena(final int adena) {
        return getInventory().addAdena(adena);
    }

    public void unsetVar(final String name) {
        if (name == null)
            return;

        if (user_variables.remove(name) != null)
            mysql.set("DELETE FROM `character_variables` WHERE `obj_id`='" + _objectId + "' AND `type`='user-var' AND `name`='" + name + "' LIMIT 1");
    }

    public L2Party getParty() {
        return _party;
    }

    public void leaveParty() {
        if (isInParty()) {
            _party.oustPartyMember(this);
            _party = null;
        }
    }

    @Override
    public void setReflection(final int i) {
        super.setReflection(i);
        if (_summon != null && !_summon.isDead())
            _summon.setReflection(i);
        if (i != 0) {
            final String var = getVar("reflection");
            if (var == null || !var.equals(String.valueOf(i)))
                setVar("reflection", String.valueOf(i));
        } else
            unsetVar("reflection");
    }

    public void setExpandInventory(final int inventory) {
        _expandInventory = inventory;
    }

    public void setExpandWarehouse(final int warehouse) {
        _expandWarehouse = warehouse;
    }

    public String getVar(final String name) {
        return user_variables.get(name);
    }

    public void setNotShowBuffAnim(final boolean value) {
        _notShowBuffAnim = value;
    }

    public long getLastAccess() {
        return _lastAccess;
    }

    public boolean isGM() {
        return _playerAccess == null ? false : _playerAccess.IsGM;
    }

    public L2Clan getClan() {
        return _clan;
    }

    @Override
    public void setXYZInvisible(final int x, final int y, final int z) {
        super.setXYZInvisible(x, y, z);
    }

    private void revalidatePenalties() {
        _curWeightPenalty = 0;
        expertisePenalty = 0;
        refreshOverloaded();
        refreshExpertisePenalty();
    }

    private void restoreBlockList() {
        _blockList.clear();

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT target_Id, target_Name FROM character_blocklist WHERE obj_Id = ?");
            statement.setInt(1, getObjectId());
            rs = statement.executeQuery();
            while (rs.next())
                _blockList.put(rs.getInt("target_Id"), rs.getString("target_Name"));
        } catch (final SQLException e) {
            _log.warning("Can't restore player blocklist " + e);
        } finally {
            DatabaseUtils.closeDatabaseCSR(con, statement, rs);
        }
    }

    /**
     * Constructor of L2Player (use L2Character constructor).<BR><BR> <B><U> Actions</U> :</B><BR><BR> <li>Call the L2Character constructor to create an empty _skills slot and copy basic
     * Calculator set to this L2Player </li> <li>Create a L2Radar object</li> <li>Retrieve from the database all items of this L2Player and add them to _inventory </li> <FONT COLOR=#FF0000><B>
     * <U>Caution</U> : This method DOESN'T SET the account name of the L2Player</B></FONT><BR><BR>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2PlayerTemplate to apply to the L2Player
     */
    private L2Player(final int objectId, final L2PlayerTemplate template) {
        this(objectId, template, null);

        // restore inventory
        getInventory().restore();

        // Create an AI
        setAI(new L2PlayerAI(this));

        // Create a L2Radar object
        radar = new L2Radar(this);

        if (!Config.EVERYBODY_HAS_ADMIN_RIGHTS)
            setPlayerAccess(Config.gmlist.get(objectId));
        else
            setPlayerAccess(Config.gmlist.get(new Integer(0)));

        // Retrieve from the database all macroses of this L2Player and add them to _macroses
        _macroses.restore();
    }

    public void setPlayerAccess(final PlayerAccess pa) {
        if (pa != null)
            _playerAccess = pa;
        else
            _playerAccess = new PlayerAccess();

        setAccessLevel(_playerAccess.IsGM || _playerAccess.Menu ? 100 : 0);
    }

    /**
     * Нигде не используется, но может пригодиться для БД
     */
    public void setAccessLevel(final int level) {
        _accessLevel = level;
    }

    /**
     * Constructor of L2Player (use L2Character constructor).<BR><BR> <B><U> Actions</U> :</B><BR><BR> <li>Call the L2Character constructor to create an empty _skills slot and copy basic
     * Calculator set to this L2Player </li> <li>Set the name of the L2Player</li><BR><BR> <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2Player to 1</B></FONT><BR><BR>
     *
     * @param objectId    Identifier of the object to initialized
     * @param template    The L2PlayerTemplate to apply to the L2Player
     * @param accountName The name of the account including this L2Player
     */
    private L2Player(final int objectId, final L2PlayerTemplate template, final String accountName) {
        super(objectId, template, null);

        _accountName = accountName;
        _nameColor = 0xFFFFFF;
        _titlecolor = 0xFFFF77;
        _baseClass = getClassId().getId();
    }

    /**
     * Возвращает количество PcBangPoint'ов даного игрока
     *
     * @return количество PcCafe Bang Points
     */
    public int getPcBangPoints() {
        return pcBangPoints;
    }

    /**
     * Устанавливает количество Pc Cafe Bang Points для даного игрока
     *
     * @param pcBangPoints новое количество PcCafeBangPoints
     */
    public void setPcBangPoints(final int pcBangPoints) {
        this.pcBangPoints = pcBangPoints;
    }

    public FastMap<Integer, SkillTimeStamp> getSkillReuseTimeStamps() {
        return skillReuseTimeStamps;
    }

    @Override
    public String toString() {
        return "player '" + getName() + "'";
    }

    public void addAutoSoulShot(final Integer itemId) {
        _activeSoulShots.add(itemId);
    }

    public synchronized void addBuff(String sheme_name, int id) {
        if (!_buffs.get(sheme_name).contains(id))
            _buffs.get(sheme_name).add(id);
    }

    /**
     * Добавляет чару опыт и/или сп с учетом личного бонуса
     */
    @Override
    public void addExpAndSp(final long addToExp, final long addToSp) {
        addExpAndSp(addToExp, addToSp, true, true);
    }

    /**
     * Add a Henna to the L2Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2Player.<BR><BR>
     *
     * @param henna L2HennaInstance для добавления
     */
    public boolean addHenna(final L2HennaInstance henna) {
        if (getHennaEmptySlots() == 0) {
            sendPacket(Msg.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL);
            return false;
        }

        // int slot = 0;
        for (int i = 0; i < 3; i++)
            if (_henna[i] == null) {
                _henna[i] = henna;

                // Calculate Henna modifiers of this L2Player
                recalcHennaStats();

                ThreadConnection con = null;
                FiltredPreparedStatement statement = null;
                try {
                    con = L2DatabaseFactory.getInstance().getConnection();
                    statement = con.prepareStatement("INSERT INTO `character_hennas` (char_obj_id, symbol_id, slot, class_index) VALUES (?,?,?,?)");
                    statement.setInt(1, getObjectId());
                    statement.setInt(2, henna.getSymbolId());
                    statement.setInt(3, i + 1);
                    statement.setInt(4, getActiveClassId());
                    statement.execute();
                } catch (final Exception e) {
                    _log.warning("could not save char henna: " + e);
                } finally {
                    DatabaseUtils.closeDatabaseCS(con, statement);
                }

                sendPacket(new HennaInfo(this));
                sendUserInfo(true);

                return true;
            }

        return false;
    }

    public int getHennaEmptySlots() {
        int totalSlots = 1 + getClassId().level();
        for (int i = 0; i < 3; i++)
            if (_henna[i] != null)
                totalSlots--;

        if (totalSlots <= 0)
            return 0;

        return totalSlots;
    }

    public synchronized void addScheme(String pending_sheme_name) {
        _buffs.put(pending_sheme_name.toLowerCase(), new GArray<Integer>());
    }

    @Override
    public void addSkillTimeStamp(final Integer skillId, final long reuseDelay) {
        synchronized (skillReuseTimeStamps) {
            skillReuseTimeStamps.put(skillId, new SkillTimeStamp(skillId, reuseDelay));
        }
    }

    public void addSnooped(final L2Player pci) {
        if (!_snoopedPlayer.contains(pci))
            _snoopedPlayer.add(pci);
    }

    public void addSnooper(final L2Player pci) {
        if (!_snoopListener.contains(pci))
            _snoopListener.add(pci);
    }

    public void addToBlockList(final String charName) {
        if (charName == null || charName.equalsIgnoreCase(getName()) || isInBlockList(charName)) {
            // уже в списке
            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_REGISTER_THE_USER_TO_YOUR_IGNORE_LIST));
            return;
        }

        final L2Player block_target = L2World.getPlayer(charName);

        if (block_target != null) {
            if (block_target.isGM()) {
                sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_A_GM));
                return;
            }
            _blockList.put(block_target.getObjectId(), block_target.getName());
            sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST).addString(block_target.getName()));
            block_target.sendPacket(new SystemMessage(SystemMessage.S1__HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST).addString(getName()));
            return;
        }

        // чар не в игре
        final int charId = Util.GetCharIDbyName(charName);

        if (charId == 0) {
            // чар не существует
            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_REGISTER_THE_USER_TO_YOUR_IGNORE_LIST));
            return;
        }

        if (Config.gmlist.containsKey(charId) && Config.gmlist.get(charId).IsGM) {
            sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_A_GM));
            return;
        }
        _blockList.put(charId, charName);
        sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST).addString(charName));
    }

    public boolean isInBlockList(final String charName) {
        for (final int blockId : _blockList.keySet())
            if (charName.equalsIgnoreCase(_blockList.get(blockId)))
                return true;
        return false;
    }

    public void addVisibleObject(final L2Object object, final L2Character dropper) {
        if (isLogoutStarted() || object == null || object.getObjectId() == getObjectId() || !object.isVisible())
            return;

        if (object.isPolymorphed())
            switch (object.getPolytype()) {
                case POLY_ITEM:
                    sendPacket(new SpawnItemPoly(object));
                    showMoves(object);
                    return;
                case POLY_NPC:
                    sendPacket(new NpcInfoPoly(object, this));
                    showMoves(object);
                    return;
            }

        if (object instanceof L2ItemInstance) {
            if (dropper != null)
                sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
            else
                sendPacket(new SpawnItem((L2ItemInstance) object));
            return;
        }

        if (object instanceof L2DoorInstance) {
            sendPacket(new DoorInfo((L2DoorInstance) object));
            sendPacket(new DoorStatusUpdate((L2DoorInstance) object));
            return;
        }

        if (object instanceof L2StaticObjectInstance) {
            sendPacket(new StaticObject((L2StaticObjectInstance) object));
            return;
        }

        if (object instanceof L2ClanHallManagerInstance)
            ((L2ClanHallManagerInstance) object).sendDecoInfo(this);

        if (object instanceof L2NpcInstance) {
            sendPacket(new NpcInfo((L2NpcInstance) object, this));
            showMoves(object);
            return;
        }

        if (object instanceof L2Summon) {
            final L2Summon summon = (L2Summon) object;

            if (summon.getPlayer() == this) {
                sendPacket(new PetInfo(summon, summon.isShowSpawnAnimation()));
                sendPacket(new PartySpelled(summon, true));

                if (summon.isPet())
                    sendPacket(new PetItemList((L2PetInstance) summon));
            } else if (getParty() != null && getParty().containsMember(summon.getPlayer())) {
                sendPacket(new NpcInfo(summon, this, summon.isShowSpawnAnimation()));
                sendPacket(new PartySpelled(summon, true));
            } else
                sendPacket(new NpcInfo(summon, this, summon.isShowSpawnAnimation()));

            showMoves(object);
            return;
        }

        if (object.isPlayer()) {
            final L2Player otherPlayer = (L2Player) object;
            if (otherPlayer.isInvisible() && getObjectId() != otherPlayer.getObjectId())
                return;

            if (otherPlayer.getPrivateStoreType() != STORE_PRIVATE_NONE && getVarB("notraders"))
                return;

            if (getObjectId() != otherPlayer.getObjectId()) {
                if (otherPlayer.isMounted()) {
                    sendPacket(new CharInfo(otherPlayer, this, false));
                    sendPacket(new Ride(otherPlayer));
                }
                sendPacket(new CharInfo(otherPlayer, this, true));
            }

            if (otherPlayer.getPrivateStoreType() != STORE_PRIVATE_NONE)
                if (otherPlayer.getPrivateStoreType() == STORE_PRIVATE_BUY)
                    sendPacket(new PrivateStoreMsgBuy(otherPlayer));
                else if (otherPlayer.getPrivateStoreType() == STORE_PRIVATE_SELL || otherPlayer.getPrivateStoreType() == STORE_PRIVATE_SELL_PACKAGE)
                    sendPacket(new PrivateStoreMsgSell(otherPlayer));
                else if (otherPlayer.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
                    sendPacket(new RecipeShopMsg(otherPlayer));

            if (otherPlayer.isCastingNow() && otherPlayer.getCastingSkill() != null && otherPlayer.getAnimationEndTime() > 0)
                if (otherPlayer.getCastingTarget() != null && otherPlayer.getCastingTarget().isCharacter())
                    sendPacket(new MagicSkillUse(otherPlayer, otherPlayer.getCastingTarget(), otherPlayer.getCastingSkill().getId(), otherPlayer.getCastingSkill().getLevel(), (int) (otherPlayer.getAnimationEndTime() - System.currentTimeMillis()), 0));

            if (_clan != null && otherPlayer.getClan() != null && _clan.isAtWarWith(otherPlayer.getClan().getClanId()))
                sendPacket(new RelationChanged(otherPlayer, otherPlayer.isAutoAttackable(this), otherPlayer.getRelation(this)));

            showMoves(object);
            return;
        }

        if (object instanceof L2BoatInstance)
            if (!isInBoat() && object != getBoat()) {
                final L2BoatInstance boat = (L2BoatInstance) object;
                sendPacket(new VehicleInfo(boat));
                if (boat.isMoving)
                    sendPacket(new VehicleDeparture(boat));
            }
    }

    public boolean getVarB(final String name) {
        final String var = user_variables.get(name);
        return !(var == null || var.equals("0") || var.equalsIgnoreCase("false"));
    }

    public boolean isMounted() {
        return _mountNpcId > 0;
    }

    public short getPrivateStoreType() {
        if (inObserverMode())
            return STORE_OBSERVING_GAMES;

        return _privatestore;
    }

    @Override
    public boolean inObserverMode() {
        return _observerMode;
    }

    public void showMoves(final L2Object object) {
        if (object != null && object.isCharacter()) {
            final L2Character obj = (L2Character) object;
            if (obj.isMoving || obj.isFollow)
                sendPacket(new CharMoveToLocation(obj));
        }
    }

    /**
     * @return Returns the inBoat.
     */
    @Override
    public boolean isInBoat() {
        return _Boat != null;
    }

    /**
     * @return
     */
    public L2BoatInstance getBoat() {
        return _Boat;
    }

    public synchronized void appearObserverMode() {
        if (!_observerMode || _observNeighbor == null || getCurrentRegion() == null) {
            _observerMode = false;
            _observNeighbor = null;
            return;
        }

        // Очищаем все видимые обьекты
        for (final L2WorldRegion neighbor : getCurrentRegion().getNeighbors())
            neighbor.removeObjectsFromPlayer(this);

        // Добавляем фэйк в точку наблюдения
        if (!_observNeighbor.equals(getCurrentRegion()))
            _observNeighbor.addObject(this);

        // Показываем чару все обьекты, что находятся в точке наблюдения и соседних регионах
        for (final L2WorldRegion neighbor : _observNeighbor.getNeighbors())
            neighbor.showObjectsToPlayer(this);
    }

    public void broadcastSnoop(final int type, final String name, final String _text) {
        if (_snoopListener.size() > 0) {
            final Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
            for (final L2Player pci : _snoopListener)
                if (pci != null)
                    pci.sendPacket(sn);
        }
    }

    @Override
    public void broadcastStatusUpdate() {
        // Send the Server->Client packet StatusUpdate with current HP and MP to all L2Player that must be informed of HP/MP updates of this L2Player
        if (Config.FORCE_STATUSUPDATE)
            super.broadcastStatusUpdate();
        else if (!needStatusUpdate()) // По идее еше должно срезать траффик. Будут глюки с отображением - убрать это условие.
            return;

        // Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2Player
        final StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, (int) _currentHp);
        su.addAttribute(StatusUpdate.CUR_MP, (int) _currentMp);
        su.addAttribute(StatusUpdate.CUR_CP, (int) _currentCp);
        sendPacket(su);

        // Check if a party is in progress
        if (isInParty())
            // Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2Player of the Party
            getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));

        if (getDuel() != null)
            getDuel().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));

        if (isInOlympiadMode()) {
            for (final L2Player player : L2World.getAroundPlayers(this))
                if (player.getOlympiadGameId() == getOlympiadGameId())
                    player.sendPacket(new ExOlympiadUserInfo(this));

            if (Olympiad.getSpectators(_olympiadGameId) != null)
                for (final L2Player spectator : Olympiad.getSpectators(_olympiadGameId))
                    if (spectator != null)
                        spectator.sendPacket(new ExOlympiadUserInfoSpectator(this, getOlympiadSide()));
        }
    }

    @Override
    public Duel getDuel() {
        return _duel;
    }

    public int getOlympiadSide() {
        return _olympiadSide;
    }

    public boolean canRecom(final L2Player target) {
        return !_recomChars.contains(target.getName().hashCode());
    }

    public void changeSex() {
        boolean male = true;
        if (getSex() == 1)
            male = false;
        _template = CharTemplateTable.getInstance().getTemplate(getClassId(), !male);
    }

    /**
     * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2Player then return True.
     */
    protected boolean checkAndEquipArrows() {
        // Check if nothing is equipped in left hand
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null) {
            // Get the L2ItemInstance of the arrows needed for this bow
            if (getActiveWeaponItem().getItemType() == WeaponType.BOW)
                _arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

            // Equip arrows needed in left hand
            if (_arrowItem != null)
                getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
        } else
            // Get the L2ItemInstance of arrows equipped in left hand
            _arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

        return _arrowItem != null;
    }

    /**
     * @return the active weapon item (always equipped in the right hand).<BR><BR>
     */
    @Override
    public L2Weapon getActiveWeaponItem() {
        final L2ItemInstance weapon = getActiveWeaponInstance();

        if (weapon == null)
            return getFistsWeaponItem();

        return (L2Weapon) weapon.getItem();
    }

    /**
     * @return the active weapon instance (always equipped in the right hand).<BR><BR>
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
    }

    public L2Weapon getFistsWeaponItem() {
        return _fistsWeaponItem;
    }

    /**
     * Системные сообщения для темных эльфов о вкл/выкл ShadowSence (skill id = 294)
     */
    public void checkDayNightMessages() {
        final short level = getSkillLevel(294);
        if (level > 0)
            if (GameTimeController.getInstance().isNowNight())
                sendPacket(new SystemMessage(SystemMessage.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(294, level));
            else
                sendPacket(new SystemMessage(SystemMessage.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(294, level));
        sendChanges();
    }

    /**
     * Системные сообщения о текущем состоянии хп
     */
    @Override
    public void checkHpMessages(final double curHp, final double newHp) {
        // сюда пасивные скиллы
        final byte[] _hp = {30, 30};
        final int[] skills = {290, 291};

        // сюда активные эффекты
        final int[] _effects_skills_id = {139, 176, 292, 292, 420};
        final byte[] _effects_hp = {30, 30, 30, 60, 30};

        final double percent = getMaxHp() / 100;
        final double _curHpPercent = curHp / percent;
        final double _newHpPercent = newHp / percent;
        boolean needsUpdate = false;

        // check for passive skills
        for (int i = 0; i < skills.length; i++) {
            final short level = getSkillLevel(skills[i]);
            if (level > 0)
                if (_curHpPercent > _hp[i] && _newHpPercent <= _hp[i]) {
                    sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_DECREASED_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(skills[i], level));
                    needsUpdate = true;
                } else if (_curHpPercent <= _hp[i] && _newHpPercent > _hp[i]) {
                    sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_INCREASED_THE_EFFECT_OF_S1_WILL_DISAPPEAR).addSkillName(skills[i], level));
                    needsUpdate = true;
                }
        }

        // check for active effects
        for (Integer i = 0; i < _effects_skills_id.length; i++)
            if (getEffectList().getEffectsBySkillId(_effects_skills_id[i]) != null)
                if (_curHpPercent > _effects_hp[i] && _newHpPercent <= _effects_hp[i]) {
                    sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_DECREASED_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(_effects_skills_id[i], (short) 1));
                    needsUpdate = true;
                } else if (_curHpPercent <= _effects_hp[i] && _newHpPercent > _effects_hp[i]) {
                    sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_INCREASED_THE_EFFECT_OF_S1_WILL_DISAPPEAR).addSkillName(_effects_skills_id[i], (short) 1));
                    needsUpdate = true;
                }

        if (needsUpdate)
            sendChanges();
    }

    @Override
    public int getMaxHp() {
        // Get the Max HP (base+modifier) of the L2Player
        final int val = super.getMaxHp();

        if (val != oldMaxHP) {
            oldMaxHP = val;

            // Launch a regen task if the new Max HP is higher than the old one
            if (getCurrentHp() != val)
                setCurrentHp(getCurrentHp(), false); // trigger start of regeneration
        }

        return val;
    }

    public void checkWaterState() {
        if (isInZoneWater())
            startWaterTask();
        else
            stopWaterTask();
    }

    public void startWaterTask() {
        if (isDead())
            stopWaterTask();
        else if (Config.ALLOW_WATER && _taskWater == null) {
            final int timeinwater = (int) (calcStat(Stats.BREATH, 86, null, null) * 1000);
            sendPacket(new SetupGauge(2, timeinwater));
            _taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
            sendChanges();
        }
    }

    public void stopWaterTask() {
        if (_taskWater != null) {
            _taskWater.cancel(true);

            _taskWater = null;
            sendPacket(new SetupGauge(2, 0));
            sendChanges();
        }
    }

    public boolean checksForShop(final boolean RequestManufacture) {
        if (!getPlayerAccess().UseTrade) {
            sendPacket(Msg.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES);
            return false;
        }

        final String tradeBan = getVar("tradeBan");
        if (tradeBan != null && (tradeBan.equals("-1") || Long.parseLong(tradeBan) >= System.currentTimeMillis())) {
            sendMessage("Your trade is banned! Expires: " + (tradeBan.equals("-1") ? "never" : Util.formatTime((Long.parseLong(tradeBan) - System.currentTimeMillis()) / 1000)) + ".");
            return false;
        }

        final String BLOCK_ZONE = RequestManufacture ? L2Zone.BLOCKED_ACTION_PRIVATE_WORKSHOP : L2Zone.BLOCKED_ACTION_PRIVATE_STORE;
        if (isActionBlocked(BLOCK_ZONE) && !isInStoreMode())
            if (!Config.SERVICES_NO_TRADE_ONLY_OFFLINE || Config.SERVICES_NO_TRADE_ONLY_OFFLINE && isInOfflineMode()) {
                sendPacket(RequestManufacture ? new SystemMessage(SystemMessage.A_PRIVATE_WORKSHOP_MAY_NOT_BE_OPENED_IN_THIS_AREA) : Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_IN_THIS_AREA);
                return false;
            }

        if (isCastingNow()) {
            sendPacket(Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_WHILE_USING_A_SKILL);
            return false;
        }

        if (isInCombat()) {
            sendPacket(Msg.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
            return false;
        }

        if (isOutOfControl() || isActionsDisabled() || isMounted() || isInOlympiadMode() || getDuel() != null)
            return false;

        if (Config.SERVICES_TRADE_ONLY_FAR && !isInStoreMode()) {
            boolean tradenear = false;
            for (final L2Player player : L2World.getAroundPlayers(this, Config.SERVICES_TRADE_RADIUS, 200))
                if (player.isInStoreMode()) {
                    tradenear = true;
                    break;
                }

            if (L2World.getAroundNpc(this, Config.SERVICES_TRADE_RADIUS + 100, 200).size() > 0)
                tradenear = true;

            if (tradenear) {
                sendMessage(new CustomMessage("trade.OtherTradersNear", this));
                return false;
            }
        }

        return true;
    }

    @Override
    public void sendMessage(final String message) {
        sendPacket(SystemMessage.sendString(message));
    }

    public boolean isInOfflineMode() {
        return _offline;
    }

    public boolean isInStoreMode() {
        return _privatestore != STORE_PRIVATE_NONE && _privatestore != STORE_OBSERVING_GAMES;
    }

    public void cleanBypasses(final boolean bbs) {
        final GArray<EncodedBypass> bypassStorage = getStoredBypasses(bbs);
        synchronized (bypassStorage) {
            bypassStorage.clearSize();
        }
    }

    private GArray<EncodedBypass> getStoredBypasses(final boolean bbs) {
        if (bbs) {
            if (bypasses_bbs == null)
                bypasses_bbs = new GArray<EncodedBypass>();
            return bypasses_bbs;
        }
        if (bypasses == null)
            bypasses = new GArray<EncodedBypass>();
        return bypasses;
    }

    public void cleanDebuffs() {
        for (L2Effect e : getEffectList().getAllEffects())
            if (e.getSkill().isOffensive() && e.getSkill().getId() != 11818)
                e.exit();
    }

    public void closeNetConnection() {
        if (_connection != null)
            _connection.closeNow(false);
    }

    @Override
    public boolean consumeItem(final int itemConsumeId, final int itemCount) {
        final L2ItemInstance item = getInventory().getItemByItemId(itemConsumeId);
        if (item == null || item.getCount() < itemCount)
            return false;
        if (getInventory().destroyItem(item, itemCount, false) != null) {
            sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(itemConsumeId).addNumber(itemCount));
            return true;
        }
        return false;
    }

    public void deathPenalty(final L2Character killer) {
        final boolean atwar = killer.getPlayer() != null ? atWarWith(killer.getPlayer()) : false;

        double deathPenaltyBonus = getDeathPenalty().getLevel() * Config.ALT_DEATH_PENALTY_EXPERIENCE_PENALTY;
        if (deathPenaltyBonus < 2)
            deathPenaltyBonus = 1;
        else
            deathPenaltyBonus = deathPenaltyBonus / 2;

        // The death steal you some Exp: 10-40 lvl 8% loose
        double percentLost = 8.0;

        final byte level = getLevel();
        if (level >= 79)
            percentLost = 1.0;
        else if (level >= 78)
            percentLost = 1.5;
        else if (level >= 76)
            percentLost = 2.0;
        else if (level >= 40)
            percentLost = 4.0;

        if (isFestivalParticipant() || atwar)
            percentLost = percentLost / 4.0;

        // Calculate the Experience loss
        int lostexp = (int) Math.round((Experience.LEVEL[level + 1] - Experience.LEVEL[level]) * percentLost / 100);
        lostexp *= deathPenaltyBonus;

        lostexp = (int) calcStat(Stats.EXP_LOST, lostexp, killer, null);

        // Потеря опыта на зарегистрированной осаде 1/4 от обычной смерти, с Charm of Courage нет потери
        // На чужой осаде - как при обычной смерти от *моба*
        if (isInZone(L2Zone.ZoneType.Siege)) {
            final Siege siege = SiegeManager.getSiege(getX(), getY(), true);
            if (siege != null && (siege.checkIsDefender(_clan) || siege.checkIsAttacker(_clan)))
                if (isCharmOfCourage())
                    lostexp = 0;
                else
                    lostexp = lostexp / 4;
        }

        _log.fine(_name + "is dead, so exp to remove:" + lostexp);

        final long before = getExp();
        addExpAndSp(-lostexp, 0, false, false);
        final long lost = before - getExp();

        if (lost > 0)
            setVar("lostexp", String.valueOf(lost));
    }

    private boolean atWarWith(final L2Player player) {
        return _clan != null && player.getClan() != null && getPledgeType() != -1 && player.getPledgeType() != -1 && _clan.isAtWarWith(player.getClan().getClanId());
    }

    /**
     * @return True if L2Player is a participant in the Festival of Darkness.<BR><BR>
     */
    public boolean isFestivalParticipant() {
        return SevenSignsFestival.getInstance().isParticipant(this);
    }

    public boolean isCharmOfCourage() {
        return _charmOfCourage;
    }

    /**
     * Добавляет чару опыт и/или сп, с учетом личного бонуса или нет
     */
    @Override
    public void addExpAndSp(long addToExp, long addToSp, final boolean applyBonus, final boolean appyToPet) {
        if (applyBonus) {
            addToExp *= Config.RATE_XP * getRateExp();
            addToSp *= Config.RATE_SP * getRateSp();
        }

        if (addToExp > 0) {
            if (appyToPet) {
                final L2Summon _pet = getPet();
                if (_pet != null && !_pet.isDead())
                    // Sin Eater забирает всю экспу у персонажа
                    if (_pet.getNpcId() == PetDataTable.SIN_EATER_ID) {
                        _pet.addExpAndSp(addToExp, 0);
                        addToExp = 0;
                    }
                    // Петы забирают 10% экспы у персонажа
                    else if (_pet.isPet())
                        if (_pet.getLevel() - getLevel() < 10) {
                            _pet.addExpAndSp((long) (addToExp * _pet.getExpPenalty()), 0);
                            addToExp *= 1f - _pet.getExpPenalty();
                        } else {
                            _pet.addExpAndSp((long) (addToExp * _pet.getExpPenalty() / 10f), 0);
                            addToExp *= 1f - _pet.getExpPenalty() / 10f;
                        }
                    else if (_pet.isSummon())
                        addToExp *= 1f - _pet.getExpPenalty();
            }

            // Remove Karma when the player kills L2MonsterInstance
            if (!isCursedWeaponEquipped() && addToSp > 0 && _karma > 0)
                _karma -= addToSp / (Config.KARMA_SP_DIVIDER * Config.RATE_SP);

            if (_karma < 0)
                _karma = 0;

            final long max_xp = getVarB("NoExp") ? Experience.LEVEL[getLevel() + 1] - 1 : getMaxExp();
            addToExp = Math.min(addToExp, max_xp - getExp());
        }

        addExp(addToExp);
        addSp(addToSp);

        if (addToSp > 0 || addToExp > 0)
            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_EXPERIENCE_AND_S2_SP).addString(String.valueOf(addToExp)).addNumber((int) addToSp));

        final long _exp = getExp();
        int level = getLevel();

        boolean flag1 = true;
        while (_exp >= Experience.LEVEL[level + 1] && increaseLevel(flag1)) {
            level = getLevel();
            flag1 = false;
        }

        while (_exp < Experience.LEVEL[level] && decreaseLevel())
            level = getLevel();

        sendChanges();
    }

    @Override
    public float getRateExp() {
        // return calcStat(Stats.EXP, _party != null ? _party._rateExp : _bonus.RATE_XP, null, null);
        return (float) calcStat(Stats.EXP, _party == null ? _bonus.RATE_XP : _party._rateExp > 0.05D ? _party._rateExp : _bonus.RATE_XP, null, null);
    }

    @Override
    public float getRateSp() {
        // return calcStat(Stats.SP, _party != null ? _party._rateSp : _bonus.RATE_SP, null, null);
        return (float) calcStat(Stats.SP, _party == null ? _bonus.RATE_SP : _party._rateSp > 0.05D ? _party._rateSp : _bonus.RATE_SP, null, null);
    }

    @Override
    public boolean isCursedWeaponEquipped() {
        return _cursedWeaponEquippedId != 0;
    }

    public long getMaxExp() {
        return _activeClass == null ? Experience.LEVEL[Experience.getMaxLevel() + 1] : _activeClass.getMaxExp();
    }

    public void addExp(final long val) {
        if (_activeClass != null)
            _activeClass.addExp(val);
    }

    public void addSp(final long val) {
        if (_activeClass != null)
            _activeClass.addSp(val);
    }

    private boolean increaseLevel(boolean showAura) {
        if (_activeClass == null || !_activeClass.incLevel())
            return false;

        if (showAura) {
            sendPacket(Msg.YOU_HAVE_INCREASED_YOUR_LEVEL);
            broadcastPacket(new SocialAction(getObjectId(), 15));
        }

        setCurrentHpMp(getMaxHp(), getMaxMp());
        setCurrentCp(getMaxCp());

        // Recalculate the party level
        if (isInParty())
            getParty().recalculatePartyData();

        if (_clan != null) {
            final PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(this);
            for (final L2Player clanMember : _clan.getOnlineMembers(0))
                clanMember.sendPacket(memberUpdate);
        }

        // Give Expertise skill of this level
        rewardSkills();

        final ClassId classId = getClassId();
        final int jobLevel = classId.getLevel();
        final int level = getLevel();
        if ((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
            ClassChange.showHtml(this);
        return true;
    }

    @Override
    public int getMaxMp() {
        // Get the Max MP (base+modifier) of the L2Player
        final int val = super.getMaxMp();

        if (val != oldMaxMP) {
            oldMaxMP = val;

            // Launch a regen task if the new Max MP is higher than the old one
            if (!isDead() && getCurrentMp() != val)
                setCurrentMp(getCurrentMp()); // trigger start of regeneration
        }

        return val;
    }

    private boolean decreaseLevel() {
        if (_activeClass == null || !_activeClass.decLevel())
            return false;

        // Recalculate the party level
        if (isInParty())
            getParty().recalculatePartyData();

        if (_clan != null) {
            final PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(this);
            for (final L2Player clanMember : _clan.getOnlineMembers(getObjectId()))
                if (!clanMember.equals(this))
                    clanMember.sendPacket(memberUpdate);
        }

        if (Config.ALT_REMOVE_SKILLS_ON_DELEVEL)
            checkSkills();
        // Give Expertise skill of this level
        rewardSkills();

        return true;
    }

    public void checkSkills() {
        for (final L2Skill sk : getAllSkillsArray())
            if (SkillTreeTable.getMinSkillLevel(sk.getId(), getClassId(), sk.getLevel()) > getLevel() + Config.ALT_REMOVE_SKILLS_ON_DELEVEL_N_LEVEL) {
                final int id = sk.getId();
                final int level = sk.getLevel();
                removeSkill(sk, true);
                if (level > 1) {
                    final L2Skill skill = SkillTable.getInstance().getInfo(id, level - 1);
                    addSkill(skill, true);
                }
            }
    }

    public long getExp() {
        return _activeClass == null ? 0 : _activeClass.getExp();
    }

    public DecodedBypass decodeBypass(final String bypass) {
        final BypassType bpType = BypassManager.getBypassType(bypass);
        final boolean bbs = bpType == BypassType.ENCODED_BBS || bpType == BypassType.SIMPLE_BBS;
        final GArray<EncodedBypass> bypassStorage = getStoredBypasses(bbs);
        if (bpType == BypassType.ENCODED || bpType == BypassType.ENCODED_BBS)
            return BypassManager.decode(bypass, bypassStorage, bbs, this);
        if (bpType == BypassType.SIMPLE)
            return new DecodedBypass(bypass, false).trim();
        if (bpType == BypassType.SIMPLE_BBS && !bypass.startsWith("_bbsscripts"))
            return new DecodedBypass(bypass, true).trim();
        //_log.warning("Direct access to bypass: " + bypass + " / Player: " + getName());
        return new DecodedBypass(bypass, false).trim();
    }

    public void delCubic(final L2CubicInstance cubic) {
        if (_cubics != null)
            _cubics.remove(cubic);
    }

    public void delQuestState(final String quest) {
        _quests.remove(quest);
    }

    public void deleteMacro(final int id) {
        _macroses.deleteMacro(id);
    }

    public void deleteShortCut(final int slot, final int page) {
        _shortCuts.deleteShortCut(slot, page);
    }

    public void deleteSubclassSkills() {
        try {
            for (final L2SubClass subClass : getSubClasses().values())
                if (!subClass.getSkills().isEmpty())
                    for (final String i : subClass.getSkills().split(";"))
                        super.removeSkillById(Integer.parseInt(i));
        } catch (final Exception e) {
            _log.warning("Could not delete subclass skills for player objId: " + getObjectId());
            e.printStackTrace();
        }
    }

    @Override
    public void doAttack(final L2Character target) {
        super.doAttack(target);

        if (_cubics != null)
            for (final L2CubicInstance cubic : _cubics)
                if (cubic.getType() != CubicType.LIFE_CUBIC)
                    cubic.doAction(target);
    }

    public void doAutoLootOrDrop(final L2ItemInstance item, final L2NpcInstance fromNpc) {
        if (fromNpc.isRaid() && !Config.AUTO_LOOT_FROM_RAIDS && !item.isHerb()) {
            item.dropToTheGround(this, fromNpc);
            return;
        }

        // Herbs
        if (item.isHerb()) {
            if (!AutoLootHerbs) {
                item.dropToTheGround(this, fromNpc);
                return;
            }
            final L2Skill[] skills = item.getItem().getAttachedSkills();
            if (skills != null && skills.length > 0)
                for (final L2Skill skill : skills) {
                    altUseSkill(skill, this);
                    if (getPet() != null && getPet().isSummon() && !getPet().isDead() && (item.getItemId() <= 8605 || item.getItemId() == 8614))
                        getPet().altUseSkill(skill, getPet());
                }
            item.decayMe();
            L2World.removeObject(item);
            broadcastPacket(new GetItem(item, getObjectId()));
            return;
        }

        if (!AutoLoot) {
            item.dropToTheGround(this, fromNpc);
            return;
        }
        // Check if the L2Player is in a Party
        if (!isInParty()) {
            if (!getInventory().validateWeight(item)) {
                sendActionFailed();
                sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                item.dropToTheGround(this, fromNpc);
                return;
            }

            if (!getInventory().validateCapacity(item)) {
                sendActionFailed();
                sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
                item.dropToTheGround(this, fromNpc);
                return;
            }

            if (item.isMaterial())
                msg += item.getCount() + " " + item.getItem().getName() + " | ";
            else {
                // Send a System Message to the L2Player
                if (item.getItemId() == 57)
                    sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA).addNumber(item.getCount()));
                else if (item.getCount() == 1) {
                    final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1); // you picked up $s1
                    smsg.addItemName(item.getItemId());
                    sendPacket(smsg);
                } else {
                    final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1); // you picked up $s1$s2
                    smsg.addItemName(item.getItemId());
                    smsg.addNumber(item.getCount());
                    sendPacket(smsg);
                }
            }
            // Add the Item to the L2Player inventory
            final L2ItemInstance target2 = getInventory().addItem(item);
            Log.LogItem(this, fromNpc, Log.GetItemByAutoLoot, target2);

            sendChanges();
        } else if (item.getItemId() == 57)
            // Distribute Adena between Party members
            getParty().distributeAdena(item, fromNpc, this);
        else
            // Distribute Item between Party members
            getParty().distributeItem(this, item, fromNpc);

        broadcastPickUpMsg(item);
    }

    /**
     * Выполнить каст скилла.
     */
    @Override
    public void doCast(final L2Skill skill, final L2Character target, final boolean forceUse) {
        if (skill == null)
            return;

        super.doCast(skill, target, forceUse);

        if (skill.isOffensive() && target != null)
            if (_cubics != null)
                for (final L2CubicInstance cubic : _cubics)
                    if (cubic.getType() != CubicType.LIFE_CUBIC)
                        cubic.doAction(target);
    }

    @Override
    public void doDie(final L2Character killer) {
        // Check for active charm of luck for death penalty
        getDeathPenalty().checkCharmOfLuck();

        if (getTradeList() != null) {
            getTradeList().removeAll();
            sendPacket(new SendTradeDone(0));
            setTradeList(null);
            setTransactionRequester(null);
        }

        setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);

        // Kill the L2Player
        super.doDie(killer);

        // Dont unsummon a summon, it can kill few enemies. But pet must returned back into its item
        // Unsummon siege summons
        if (_summon != null && (_summon.isPet() || _summon.isSiegeWeapon()))
            _summon.unSummon();

        // Unsummon Cubics and agathion
        if (!isBlessedByNoblesse() && !isSalvation() && _cubics != null && !_cubics.isEmpty()) {
            for (L2CubicInstance cubic : _cubics) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }
            _cubics.clear();
            _cubics = null;
        }

        if (Config.LOG_KILLS) {
            final String coords = " at (" + getX() + "," + getY() + "," + getZ() + ")";
            if (killer instanceof L2NpcInstance)
                Log.add("" + this + " karma " + _karma + " killed by mob " + killer.getNpcId() + coords, "kills");
            else if (killer instanceof L2Summon && killer.getPlayer() != null)
                Log.add("" + this + " karma " + _karma + " killed by summon of " + killer.getPlayer() + coords, "kills");
            else
                Log.add("" + this + " karma " + _karma + " killed by " + killer + coords, "kills");
        }

        if (Config.ALLOW_CURSED_WEAPONS)
            if (isCursedWeaponEquipped()) {
                _pvpFlag = 0;

                CursedWeaponsManager.getInstance().dropPlayer(this);
                return;
            } else if (killer.isPlayer() && killer.isCursedWeaponEquipped()) {
                _pvpFlag = 0;

                // noinspection ConstantConditions
                CursedWeaponsManager.getInstance().increaseKills(((L2Player) killer).getCursedWeaponEquippedId());
                return;
            }

        doPKPVPManage(killer);

        // Set the PvP Flag of the L2Player
        _pvpFlag = 0;

        altDeathPenalty(killer);

        // And in the end of process notify death penalty that owner died :)
        getDeathPenalty().notifyDead(killer);

        setIncreasedForce(0);

        if (isInParty() && getParty().isInDimensionalRift())
            getParty().getDimensionalRift().memberDead(this);

        stopWaterTask();
    }

    public L2TradeList getTradeList() {
        return _tradeList;
    }

    public void setTradeList(final L2TradeList x) {
        _tradeList = x;
    }

    public void setTransactionRequester(final L2Player requestor) {
        _currentTransactionRequester = requestor;
        _currentTransactionTimeout = -1;
    }

    public int getCursedWeaponEquippedId() {
        return _cursedWeaponEquippedId;
    }

    private void doPKPVPManage(L2Character killer) {
        for (final L2ItemInstance i : getInventory().getItemsList())
            if ((i.getCustomFlags() & L2ItemInstance.FLAG_ALWAYS_DROP_ON_DIE) == L2ItemInstance.FLAG_ALWAYS_DROP_ON_DIE)
                i.dropToTheGround(this, getLoc());

        if (killer == null || killer == _summon)
            return;

        if (killer.getObjectId() == _objectId)
            return;

        if (isInZoneBattle() || killer.isInZoneBattle())
            return;

        if (killer instanceof L2Summon)
            killer = killer.getPlayer();

        // Processing Karma/PKCount/PvPCount for killer
        if (killer.isPlayer()) {
            final L2Player pk = (L2Player) killer;
            final int repValue = getLevel() - pk.getLevel() >= 20 ? 2 : 1;
            final boolean war = atMutualWarWith(pk);

            if (getLevel() > 4 && _clan != null && pk.getClan() != null)
                if (war || _clan.getSiege() != null && _clan.getSiege() == pk.getClan().getSiege() && (_clan.isDefender() && pk.getClan().isAttacker() || _clan.isAttacker() && pk.getClan().isDefender()))
                    if (pk.getClan().getReputationScore() > 0 && _clan.getLevel() >= 5 && _clan.getReputationScore() > 0 && pk.getClan().getLevel() >= 5) {
                        _clan.broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.YOUR_CLAN_MEMBER_S1_WAS_KILLED_S2_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_REPUTATION_SCORE_AND_ADDED_TO_YOUR_OPPONENT_CLAN_REPUTATION_SCORE).addString(getName()).addNumber(-_clan.incReputation(-repValue, true, "ClanWar")), this);
                        pk.getClan().broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.FOR_KILLING_AN_OPPOSING_CLAN_MEMBER_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_OPPONENTS_CLAN_REPUTATION_SCORE).addNumber(pk.getClan().incReputation(repValue, true, "ClanWar")), pk);
                        _clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
                        _clan.broadcastToOnlineMembers(new PledgeStatusChanged(_clan));
                    }

            if (isInZone(L2Zone.ZoneType.Siege))
                return;

            if (_pvpFlag > 0 || war) {
                if (!pk.isTryingToFarm(this)) {
                    pk.setPvpKills(pk.getPvpKills() + 1);
                    pk.getActiveClass().setPvPCount(pk.getActiveClass().getPvPCount() + 1);
                    pk.addPvPLog(new PvPinfo(System.currentTimeMillis(), getName(), "WON"));
                    addPvPLog(new PvPinfo(System.currentTimeMillis(), pk.getName(), "LOST"));
                } else {
                    pk.addPvPLog(new PvPinfo(System.currentTimeMillis(), getName(), "IGNORED"));
                    addPvPLog(new PvPinfo(System.currentTimeMillis(), pk.getName(), "IGNORED"));
                }
            } else
                doKillInPeace(pk);

            // Send a Server->Client UserInfo packet to attacker with its PvP Kills Counter
            pk.sendUserInfo(false);
        }

        final int karma = _karma;
        decreaseKarma(Config.KARMA_LOST_BASE);

        if (_pkKills < Config.MIN_PK_TO_ITEMS_DROP || karma == 0 && Config.KARMA_NEEDED_TO_DROP || isFestivalParticipant())
            return;

        // No drop from GM's
        if (!Config.KARMA_DROP_GM && isGM())
            return;

        final int max_drop_count = Config.KARMA_DROP_ITEM_LIMIT;

        double dropRate = (double) _pkKills * Config.KARMA_DROPCHANCE_MULTIPLIER;

        if (dropRate < Config.KARMA_DROPCHANCE_MINIMUM)
            dropRate = Config.KARMA_DROPCHANCE_MINIMUM;

        dropRate /= 100.0;

        final double dropEquipRate = Config.KARMA_DROPCHANCE_EQUIPMENT / 100.0;
        final double dropWeaponRate = Config.KARMA_DROPCHANCE_EQUIPPED_WEAPON / 100.0;
        final double dropItemRate = Math.max(0, 1.0 - (dropEquipRate + dropWeaponRate));

        // _log.info("dropEquipRate=" + dropEquipRate + "; dropWeaponRate=" + dropWeaponRate + "; dropItemRate=" + dropItemRate);

        ArrayList<L2ItemInstance> dropped_items = new ArrayList<L2ItemInstance>();
        // Items to check = max_drop_count * 3; (Inventory + Weapon + Equipment)
        if (getInventory().getSize() > 0)
            for (int i = 0, cycles = 100; i < max_drop_count * 3 && cycles > 0 && dropped_items.size() < max_drop_count; cycles--) {
                final L2ItemInstance random_item = getInventory().getItems()[Rnd.get(getInventory().getSize())];

                if (random_item.isAugmented() || random_item.isShadowItem() || random_item.isTemporalItem() || random_item.isHeroItem())
                    continue;

                if (random_item.getItem().getType1() == L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
                    continue;

                if (random_item.getItem().getType2() == L2Item.TYPE2_QUEST)
                    continue;

                if (dropped_items.contains(random_item))
                    continue;

                i++;

                if (random_item.isEquipped()) {
                    if (random_item.getItem().getType2() == L2Item.TYPE2_WEAPON)
                        dropped_items = checkAddItemToDrop(dropped_items, random_item, dropWeaponRate * dropRate);
                    else
                        dropped_items = checkAddItemToDrop(dropped_items, random_item, dropEquipRate * dropRate);
                } else
                    dropped_items = checkAddItemToDrop(dropped_items, random_item, dropItemRate * dropRate);
            }

        // Dropping items, if present
        if (dropped_items.isEmpty())
            return;

        for (L2ItemInstance item : dropped_items) {
            if (item.isEquipped())
                getInventory().unEquipItemInSlot(item.getEquipSlot());

            item = getInventory().dropItem(item, item.getCount());

            if (killer.isPlayer() && Config.AUTO_LOOT && Config.AUTO_LOOT_PK)
                ((L2Player) killer).getInventory().addItem(item);
            else if (killer.isSummon() && Config.AUTO_LOOT && Config.AUTO_LOOT_PK)
                killer.getPlayer().getInventory().addItem(item);
            else {
                Location pos = Rnd.coordsRandomize(this, Config.KARMA_RANDOM_DROP_LOCATION_LIMIT);
                for (int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(getX(), getY(), getZ(), pos.x, pos.y, pos.z); i++)
                    pos = Rnd.coordsRandomize(this, Config.KARMA_RANDOM_DROP_LOCATION_LIMIT);
                if (!GeoEngine.canMoveWithCollision(getX(), getY(), getZ(), pos.x, pos.y, pos.z)) {
                    pos.x = killer.getX();
                    pos.y = killer.getY();
                    pos.z = killer.getZ();
                }
                item.dropMe(this, pos);
            }

            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_DROPPED_S1).addItemName(item.getItemId()));
        }
        refreshOverloaded();
    }

    public boolean atMutualWarWith(final L2Player player) {
        return _clan != null && player.getClan() != null && getPledgeType() != -1 && player.getPledgeType() != -1 && _clan.isAtWarWith(player.getClan().getClanId()) && player.getClan().isAtWarWith(_clan.getClanId());
    }

    public boolean isTryingToFarm(L2Player killed) {
        //Check level diff.
        if (getLevel() - killed.getLevel() > 6 || getLevel() - killed.getLevel() < -6)
            return true;

        //Lets check stats.
        if (killed.getPDef(null) < 100 || killed.getMDef(null, null) < 100)
            return true;

        //Check same clan.
        if (getClanId() > 0 && getClanId() == killed.getClanId())
            return true;

        //is same ip?
        if (killed.getIP().equals(getIP()))
            return true;

        //check lasttime killed.
        if (_killed_List.containsKey(killed.getObjectId()))
            if ((_killed_List.get(killed.getObjectId()) + 180000) > System.currentTimeMillis())
                return true;

        if (_killed_List.size() >= 20)
            _killed_List = new HashMap<Integer, Long>();

        _killed_List.put(killed.getObjectId(), System.currentTimeMillis());
        return false;
    }

    @Override
    public int getMDef(final L2Character target, final L2Skill skill) {
        double init = 0;

        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) == null)
            init += L2Armor.EMPTY_EARRING;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) == null)
            init += L2Armor.EMPTY_EARRING;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) == null)
            init += L2Armor.EMPTY_NECKLACE;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) == null)
            init += L2Armor.EMPTY_RING;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) == null)
            init += L2Armor.EMPTY_RING;

        return Balancer.getModify(bflag.mdef, (int) calcStat(Stats.MAGIC_DEFENCE, init, target, skill), getClassId().getId());
    }

    @Override
    public int getPDef(final L2Character target) {
        double init = 4; // empty cloak and underwear slots

        final L2ItemInstance chest = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
        if (chest == null)
            init += isMageClass() ? L2Armor.EMPTY_BODY_MYSTIC : L2Armor.EMPTY_BODY_FIGHTER;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null && (chest == null || chest.getBodyPart() != L2Item.SLOT_FULL_ARMOR))
            init += isMageClass() ? L2Armor.EMPTY_LEGS_MYSTIC : L2Armor.EMPTY_LEGS_FIGHTER;

        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) == null)
            init += L2Armor.EMPTY_HELMET;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
            init += L2Armor.EMPTY_GLOVES;
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
            init += L2Armor.EMPTY_BOOTS;

        return Balancer.getModify(bflag.pdef, (int) calcStat(Stats.POWER_DEFENCE, init, target, null), getClassId().getId());
    }

    public String getIP() {
        if (_connection == null)
            return "<not connected>";
        return _connection.getIpAddr();
    }

    private void addPvPLog(PvPinfo info) {
        if (_lastPvPs.size() >= 20) {
            GArray<PvPinfo> clean = new GArray<PvPinfo>();
            int count = 0;
            for (PvPinfo pvp : _lastPvPs) {
                if (count < 20)
                    clean.add(pvp);
                count++;
            }
            _lastPvPs = clean;
        }
        _lastPvPs.add(info);
    }

    private void doKillInPeace(final L2Player killer) // Check if the L2Player killed haven't Karma
    {
        if (_karma <= 0)
            doPurePk(killer);
        else {
            if (!killer.isTryingToFarm(this)) {
                killer.setPvpKills(killer.getPvpKills() + 1);
                killer.getActiveClass().setPvPCount(killer.getActiveClass().getPvPCount() + 1);
                killer.addPvPLog(new PvPinfo(System.currentTimeMillis(), getName(), "WON"));
                addPvPLog(new PvPinfo(System.currentTimeMillis(), killer.getName(), "LOST"));
            } else {
                killer.addPvPLog(new PvPinfo(System.currentTimeMillis(), getName(), "IGNORED"));
                addPvPLog(new PvPinfo(System.currentTimeMillis(), killer.getName(), "IGNORED"));
            }
        }
    }

    private void doPurePk(final L2Player killer) {
        // Check if the attacker has a PK counter greater than 0
        final int pkCountMulti = Math.max(killer.getPkKills() / 2, 1);

        // Calculate the level difference Multiplier between attacker and killed L2Player
        // final int lvlDiffMulti = Math.max(killer.getLevel() / _level, 1);

        // Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
        // Add karma to attacker and increase its PK counter
        killer.increaseKarma(Config.KARMA_MIN_KARMA * pkCountMulti); // * lvlDiffMulti);
        killer.setPkKills(killer.getPkKills() + 1);
    }

    /**
     * Decrease Karma of the L2Player and Send it StatusUpdate packet with Karma and PvP Flag (broadcast).
     */
    void increaseKarma(final long add_karma) {
        long new_karma = _karma + add_karma;

        if (new_karma > Integer.MAX_VALUE)
            new_karma = Integer.MAX_VALUE;

        if (_karma == 0 && new_karma > 0) {
            _karma = (int) new_karma;
            for (final L2Character cha : L2World.getAroundCharacters(this))
                if (cha instanceof L2GuardInstance && cha.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
                    cha.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
        } else
            _karma = (int) new_karma;

        // Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast)
        updateKarma();
    }

    /**
     * Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast).
     */
    private void updateKarma() {
        final StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.KARMA, _karma);
        broadcastPacket(su);
    }

    /**
     * Decrease Karma of the L2Player and Send it StatusUpdate packet with Karma and PvP Flag (broadcast).
     */
    private void decreaseKarma(final int i) {
        _karma -= i;
        if (_karma <= 0) {
            _karma = 0;
            // Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2Player and all L2Player to inform (broadcast)
            setKarmaFlag(0);
            return;
        }
        // Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast)
        updateKarma();
    }

    /**
     * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2Player and all L2Player to inform (broadcast).<BR><BR>
     */
    private void setKarmaFlag(final int flag) {
        final StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.KARMA, _karma);
        su.addAttribute(StatusUpdate.PVP_FLAG, flag);
        broadcastPacket(su);
    }

    private ArrayList<L2ItemInstance> checkAddItemToDrop(final ArrayList<L2ItemInstance> array, final L2ItemInstance item, final double rate) {
        if (Rnd.get() >= rate)
            return array;

        if (Config.KARMA_LIST_NONDROPPABLE_ITEMS.contains(item.getItemId()))
            return array;

        array.add(item);

        return array;
    }

    private void altDeathPenalty(final L2Character killer) {
        // Reduce the Experience of the L2Player in function of the calculated Death Penalty
        //if(isInZoneBattle() && killer instanceof L2Playable)
        //	return;
        //deathPenalty(killer);
    }

    @Override
    public void doPickupItem(final L2Object object) {
        // Check if the L2Object to pick up is a L2ItemInstance
        if (!(object instanceof L2ItemInstance)) {
            _log.warning("trying to pickup wrong target." + getTarget());
            return;
        }

        sendActionFailed();
        stopMove();

        final L2ItemInstance item = (L2ItemInstance) object;

        synchronized (item) {
            // Check if me not owner of item and, if in party, not in owner party and nonowner pickup delay still active
            if (item.getDropTimeOwner() != 0 && item.getItemDropOwner() != null && item.getDropTimeOwner() > System.currentTimeMillis() && this != item.getItemDropOwner() && (!isInParty() || isInParty() && item.getItemDropOwner().isInParty() && getParty() != item.getItemDropOwner().getParty())) {
                SystemMessage sm;
                if (item.getItemId() == 57) {
                    sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
                    sm.addNumber(item.getCount());
                } else {
                    sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                    sm.addItemName(item.getItemId());
                }
                sendPacket(sm);
                return;
            }

            if (!item.isVisible())
                return;

            // Herbs
            if (item.isHerb()) {
                final L2Skill[] skills = item.getItem().getAttachedSkills();
                if (skills != null && skills.length > 0)
                    for (final L2Skill skill : skills) {
                        altUseSkill(skill, this);
                        if (getPet() != null && getPet().isSummon() && !getPet().isDead() && (item.getItemId() <= 8605 || item.getItemId() == 8614))
                            getPet().altUseSkill(skill, getPet());
                    }
                item.decayMe();
                L2World.removeObject(item);
                broadcastPacket(new GetItem(item, getObjectId()));
                return;
            }

            if ((item.getCustomFlags() & L2ItemInstance.FLAG_EQUIP_ON_PICKUP) == L2ItemInstance.FLAG_EQUIP_ON_PICKUP) {
                if (!getInventory().validateWeight(item)) {
                    sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                    return;
                }

                if (!getInventory().validateCapacity(item)) {
                    sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
                    return;
                }

                final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1);
                smsg.addItemName(item.getItemId());
                sendPacket(smsg);
                item.pickupMe(this);
                getInventory().equipItem(item);
                broadcastPacket(new GetItem(item, getObjectId()));
                broadcastPickUpMsg(item);
                sendPacket(new ItemList(this, false));
                return;
            }

            if (!isInParty()) {
                if (!getInventory().validateWeight(item)) {
                    sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                    return;
                }

                if (!getInventory().validateCapacity(item)) {
                    sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
                    return;
                }

                SystemMessage smsg;
                if (item.getItemId() == 57) {
                    smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA);
                    smsg.addNumber(item.getIntegerLimitedCount());
                } else if (item.getIntegerLimitedCount() == 1) {
                    if (item.getEnchantLevel() > 0) {
                        smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED__S1S2);
                        smsg.addNumber(item.getEnchantLevel());
                        smsg.addItemName(item.getItemId());
                    } else {
                        smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1);
                        smsg.addItemName(item.getItemId());
                    }
                } else {
                    smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1);
                    smsg.addItemName(item.getItemId());
                    smsg.addNumber(item.getIntegerLimitedCount());
                }
                sendPacket(smsg);

                item.pickupMe(this);

                final L2ItemInstance target2 = getInventory().addItem(item);
                Log.LogItem(this, Log.PickupItem, target2);

                sendChanges();
            } else if (item.getItemId() == 57) {
                item.pickupMe(this);
                getParty().distributeAdena(item, this);
            } else {
                // Нужно обязательно сначало удалить предмет с земли.
                item.pickupMe(null);
                getParty().distributeItem(this, item);
            }

            broadcastPacket(new GetItem(item, getObjectId()));
            broadcastPickUpMsg(item);
        }
    }

    void doZoneCheck(final int messageNumber) {
        final boolean oldIsInDangerArea = isInDangerArea();
        final boolean oldIsInCombatZone = isInCombatZone();
        final boolean oldIsOnSiegeField = isOnSiegeField();
        final boolean oldIsInPeaceZone = isInPeaceZone();
        final boolean oldSSQZone = isInSSZone();

        setInDangerArea(isInZone(L2Zone.ZoneType.poison) || isInZone(L2Zone.ZoneType.instant_skill) || isInZone(L2Zone.ZoneType.swamp) || isInZone(L2Zone.ZoneType.damage));
        setInCombatZone(isInZoneBattle());
        setOnSiegeField(isInZone(L2Zone.ZoneType.Siege));
        setInPeaceZone(isInZone(L2Zone.ZoneType.peace_zone));
        setInSSZone(isInZoneIncludeZ(L2Zone.ZoneType.ssq_zone));

        if (oldIsInDangerArea != isInDangerArea() || oldIsInCombatZone != isInCombatZone() || oldIsOnSiegeField != isOnSiegeField() || oldIsInPeaceZone != isInPeaceZone() || oldSSQZone != isInSSZone()) {
            sendPacket(new ExSetCompassZoneCode(this));
            sendPacket(new EtcStatusUpdate(this));
            if (messageNumber != 0)
                sendPacket(new SystemMessage(messageNumber));
        }

        if (oldIsOnSiegeField != isOnSiegeField())
            if (isOnSiegeField())
                sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ENTERED_A_COMBAT_ZONE));
            else
                sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_LEFT_A_COMBAT_ZONE));

        if (oldIsOnSiegeField != isOnSiegeField() && !isOnSiegeField() && !isTeleporting() && getPvpFlag() == 0)
            startPvPFlag(null);

        revalidateInResidence();
    }

    public void setInDangerArea(final boolean value) {
        _isInDangerArea = value;
    }

    public void setInCombatZone(final boolean flag) {
        _isInCombatZone = flag;
    }

    public void setOnSiegeField(final boolean flag) {
        _isOnSiegeField = flag;
    }

    public void setInPeaceZone(final boolean b) {
        _isInPeaceZone = b;
    }

    public void setInSSZone(final boolean b) {
        _isInSSZone = b;
    }

    public boolean isInDangerArea() {
        return _isInDangerArea;
    }

    public boolean isInCombatZone() {
        return _isInCombatZone;
    }

    public boolean isInPeaceZone() {
        return _isInPeaceZone;
    }

    public boolean isInSSZone() {
        return _isInSSZone;
    }

    public boolean isOnSiegeField() {
        return _isOnSiegeField;
    }

    @Override
    public int getPvpFlag() {
        return _pvpFlag;
    }

    @Override
    public void startPvPFlag(final L2Character target) {
        long startTime = System.currentTimeMillis();
        if (target != null && target.getPvpFlag() != 0)
            startTime -= Config.PVP_TIME / 2;
        if (_pvpFlag != 0 && _lastPvpAttack > startTime)
            return;
        _lastPvpAttack = startTime;

        updatePvPFlag(1);

        if (_PvPRegTask == null)
            _PvPRegTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new PvPFlag(), 1000, 1000);
    }

    public void updatePvPFlag(final int value) {
        if (_pvpFlag == value)
            return;
        setPvpFlag(value);

        if (_karma < 1) {
            broadcastPacket(new StatusUpdate(getObjectId()).addAttribute(StatusUpdate.PVP_FLAG, value));
            if (getPet() != null)
                getPet().broadcastPetInfo();
        }
        for (final L2Player player : L2World.getAroundPlayers(this))
            player.sendPacket(new RelationChanged(this, isAutoAttackable(player), getRelation(player)));
    }

    public void setPvpFlag(final int pvpFlag) {
        _pvpFlag = pvpFlag;
    }

    public void revalidateInResidence() {
        final L2Clan clan = _clan;
        if (clan == null)
            return;
        final int clanHallIndex = clan.getHasHideout();
        if (clanHallIndex != 0) {
            final ClanHall clansHall = ClanHallManager.getInstance().getClanHall(clanHallIndex);
            if (clansHall != null && clansHall.checkIfInZone(getX(), getY())) {
                setInResidence(ResidenceType.Clanhall);
                return;
            }
        }
        final int castleIndex = clan.getHasCastle();
        if (castleIndex != 0) {
            final Castle castle = CastleManager.getInstance().getCastleByIndex(castleIndex);
            if (castle != null && castle.checkIfInZone(getX(), getY())) {
                setInResidence(ResidenceType.Castle);
                return;
            }
        }
        setInResidence(ResidenceType.None);
    }

    public void setInResidence(final ResidenceType inResidence) {
        _inResidence = inResidence;
    }

    public String encodeBypasses(final String htmlCode, final boolean bbs) {
        final GArray<EncodedBypass> bypassStorage = getStoredBypasses(bbs);
        synchronized (bypassStorage) {
            return BypassManager.encode(htmlCode, bypassStorage, bbs);
        }
    }

    public void endFishing(final boolean win) {
        final ExFishingEnd efe = new ExFishingEnd(win, this);
        broadcastPacket(efe);
        _fishing = false;
        _fishLoc = new Location(0, 0, 0);
        broadcastUserInfo(true);
        if (_fishCombat == null)
            sendPacket(Msg.BAITS_HAVE_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
        _fishCombat = null;
        _lure = null;
        // Ends fishing
        sendPacket(Msg.ENDS_FISHING);
        setImobilised(false);
        stopLookingForFishTask();
    }

    public void stopLookingForFishTask() {
        if (_taskforfish != null) {
            _taskforfish.cancel(false);
            _taskforfish = null;
        }
    }

    public void engageAnswer(final int answer) {
        if (!_engagerequest || _engageid == 0)
            return;

        final L2Player ptarget = (L2Player) L2World.findObject(_engageid);
        setEngageRequest(false, 0);
        if (ptarget != null)
            if (answer == 1) {
                CoupleManager.getInstance().createCouple(ptarget, this);
                ptarget.sendMessage(new CustomMessage("l2d.game.model.L2Player.EngageAnswerYes", this));
            } else
                ptarget.sendMessage(new CustomMessage("l2d.game.model.L2Player.EngageAnswerNo", this));
    }

    public void setEngageRequest(final boolean state, final int playerid) {
        _engagerequest = state;
        _engageid = playerid;
    }

    public void enterMovieMode() {
        setTarget(null);
        stopMove();
        setIsInvul(true);
        setImobilised(true);
        sendPacket(new CameraMode(1));
    }

    @Override
    public void setTarget(L2Object newTarget) {
        // Check if the new target is visible
        if (newTarget != null && !newTarget.isVisible())
            newTarget = null;

        // Can't target and attack festival monsters if not participant
        // Temporary disable this feature, cause geo works fine
        /*
		 * if(newTarget instanceof L2FestivalMonsterInstance && !isFestivalParticipant()) { _log.warning("Target = null for player: " + this.getName() + " and mob: " + newTarget.getObjectId());
		 * newTarget = null; }
		 */

        // Can't target and attack rift invaders if not in the same room
        if (isInParty() && getParty().isInDimensionalRift()) {
            final Integer riftType = getParty().getDimensionalRift().getType();
            final Integer riftRoom = getParty().getDimensionalRift().getCurrentRoom();
            if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
                newTarget = null;
        }

        final L2Object oldTarget = getTarget();

        if (oldTarget != null) {
            if (oldTarget.equals(newTarget))
                return;

            // Remove the L2Player from the _statusListener of the old target if it was a L2Character
            if (oldTarget.isCharacter())
                ((L2Character) oldTarget).removeStatusListener(this);

            broadcastPacket(new TargetUnselected(this));
        }

        if (newTarget != null) {
            // Add the L2Player to the _statusListener of the new target if it's a L2Character
            if (newTarget.isCharacter())
                ((L2Character) newTarget).addStatusListener(this);

            broadcastPacket(new TargetSelected(getObjectId(), newTarget.getObjectId(), getLoc()));
        }

        super.setTarget(newTarget);
    }

    public synchronized boolean enterObserverMode(final int x, final int y, final int z) {
        final L2WorldRegion oldRegion = L2World.getRegion(getX(), getY(), getZ());
        if (oldRegion == null)
            return false;

        _observNeighbor = L2World.getRegion(x, y, z);
        if (_observNeighbor == null)
            return false;

        setTarget(null);
        stopMove();
        sitDown();
        block();

        _observerMode = true;

        // Отображаем надпись над головой
        broadcastUserInfo(true);

        // Переходим в режим обсервинга
        sendPacket(new ObserverStart(x, y, z));

        return true;
    }

    @Override
    public void sitDown() {
        if (isSitting() || _sittingTask || isAlikeDead())
            return;

        if (isStunned() || isSleeping() || isParalyzed() || isAttackingNow() || isCastingNow() || isMoving) {
            getAI().setNextAction(nextAction.REST, null, null, false, false);
            return;
        }

        resetWaitSitTime();
        getAI().setIntention(CtrlIntention.AI_INTENTION_REST, null, null);
        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
        _sittingTask = true;
        _isSitting = true;
        ThreadPoolManager.getInstance().scheduleAi(new EndSitDown(), 2500, true);
    }

    @Override
    public boolean isSitting() {
        return inObserverMode() || _isSitting;
    }

    public void resetWaitSitTime() {
        _waitTimeWhenSit = 0;
    }

    public synchronized void enterOlympiadObserverMode(final int[] coords, final int id) {
        final L2WorldRegion oldRegion = L2World.getRegion(getX(), getY(), getZ());
        if (oldRegion == null)
            return;

        _observNeighbor = L2World.getRegion(coords[0], coords[1], coords[2]);
        if (_observNeighbor == null)
            return;

        setTarget(null);
        stopMove();

        if (isSitting())
            standUp();

        block();

        _olympiadGameId = id;
        _observerMode = true;

        // Отображаем надпись над головой
        broadcastUserInfo(true);

        // Меняем интерфейс
        sendPacket(new ExOlympiadMode(3));

        // Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
        setLastClientPosition(null);
        setLastServerPosition(null);

        // "Телепортируемся"
        sendPacket(new TeleportToLocation(this, coords[0], coords[1], coords[2]));
    }

    @Override
    public void standUp() {
        if (_isSitting && !_sittingTask && !isInStoreMode() && !isAlikeDead()) {
            if (_relax) {
                setRelax(false);
                getEffectList().stopEffects(EffectType.Relax);
            }
            getAI().clearNextAction();
            broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
            _sittingTask = true;
            _isSitting = true;
            ThreadPoolManager.getInstance().scheduleAi(new EndStandUp(), 2500, true);
        }
    }

    public void setRelax(final boolean val) {
        _relax = val;
    }

    @Override
    public void setLastClientPosition(final Location position) {
        _lastClientPosition = position;
    }

    @Override
    public void setLastServerPosition(final Location position) {
        _lastServerPosition = position;
    }

    public void falling(final int height) {
        if (isDead() || isFlying())
            return;
        final int maxHp = getMaxHp();
        final int curHp = (int) getCurrentHp();
        final int damage = (int) calcStat(Stats.FALL, maxHp / 1000 * height, null, null);
        if (curHp - damage < 1)
            setCurrentHp(1, false);
        else
            setCurrentHp(curHp - damage, false);
        sendPacket(new SystemMessage(SystemMessage.YOU_RECEIVED_S1_DAMAGE_FROM_TAKING_A_HIGH_FALL).addNumber(damage));
    }

    public boolean findRecipe(final L2Recipe id) {
        return _recipebook.containsValue(id) || _commonrecipebook.containsValue(id);
    }

    public boolean findRecipe(final int id) {
        return _recipebook.containsKey(id) || _commonrecipebook.containsKey(id);
    }

    @Override
    public L2PlayableAI getAI() {
        if (_ai == null)
            _ai = new L2PlayerAI(this);
        return (L2PlayableAI) _ai;
    }

    /**
     * Нигде не используется, но может пригодиться для БД
     */
    @Override
    public int getAccessLevel() {
        return _accessLevel;
    }

    /**
     * Возвращает список персонажей на аккаунте, за исключением текущего
     *
     * @return Список персонажей
     */
    public HashMap<Integer, String> getAccountChars() {
        return _chars;
    }

    public int getAdena() {
        return getInventory().getAdena();
    }

    public Quest[] getAllActiveQuests() {
        final ArrayList<Quest> quests = new ArrayList<Quest>();
        for (final QuestState qs : _quests.values())
            if (qs != null && qs.isStarted() && !qs.isCompleted())
                quests.add(qs.getQuest());
        return quests.toArray(new Quest[quests.size()]);
    }

    public QuestState[] getAllQuestsStates() {
        return _quests.values().toArray(new QuestState[_quests.size()]);
    }

    @Override
    public int getAllyCrestId() {
        return getAlliance() == null ? 0 : getAlliance().getAllyCrestId();
    }

    public L2Alliance getAlliance() {
        return _clan == null ? null : _clan.getAlliance();
    }

    /**
     * @return the Alliance Identifier of the L2Player.<BR><BR>
     */
    public int getAllyId() {
        return _clan == null ? 0 : _clan.getAllyId();
    }

    public Collection<String> getBlockList() {
        return _blockList.values();
    }

    public Bonus getBonus() {
        return _bonus;
    }

    public ConcurrentLinkedQueue<TradeItem> getBuyList() {
        return _buyList != null ? _buyList : new ConcurrentLinkedQueue<TradeItem>();
    }

    public int getBuyListId() {
        return _buyListId;
    }

    public Castle getCastle() {
        return CastleManager.getInstance().getCastleByOwner(_clan);
    }

    public boolean getChargedFishShot() {
        final L2ItemInstance weapon = getActiveWeaponInstance();
        return weapon != null && weapon.getChargedFishshot();
    }

    @Override
    public boolean getChargedSoulShot() {
        final L2ItemInstance weapon = getActiveWeaponInstance();
        return weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT;
    }

    @Override
    public int getChargedSpiritShot() {
        final L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon == null)
            return 0;
        return weapon.getChargedSpiritshot();
    }

    @Override
    public int getClanCrestId() {
        return _clan == null ? 0 : _clan.getCrestId();
    }

    @Override
    public int getClanCrestLargeId() {
        return _clan == null ? 0 : _clan.getCrestLargeId();
    }

    public ClanHall getClanHall() {
        return ClanHallManager.getInstance().getClanHallByOwner(_clan);
    }

    public int getClanPrivileges() {
        if (_clan == null)
            return 0;
        if (isClanLeader())
            return L2Clan.CP_ALL;
        if (_powerGrade < 1 || _powerGrade > 9)
            return 0;
        final RankPrivs privs = _clan.getRankPrivs(_powerGrade);
        if (privs != null)
            return privs.getPrivs();
        return 0;
    }

    @Override
    public float getColHeight() {
        if (isMounted())
            return NpcTable.getTemplate(getMountNpcId()).collisionHeight + getBaseTemplate().collisionHeight;
        else
            return getBaseTemplate().collisionHeight;
    }

    public int getMountNpcId() {
        return _mountNpcId;
    }

    @Override
    public L2PlayerTemplate getBaseTemplate() {
        return (L2PlayerTemplate) _baseTemplate;
    }

    @Override
    public float getColRadius() {
        if (isMounted())
            return NpcTable.getTemplate(getMountNpcId()).collisionRadius;
        else
            return getBaseTemplate().collisionRadius;
    }

    public Collection<L2Recipe> getCommonRecipeBook() {
        return _commonrecipebook.values();
    }

    public int getCommonRecipeLimit() {
        return (int) calcStat(Stats.COMMON_RECIPE_LIMIT, 50, null, null) + Config.ALT_ADD_RECIPES;
    }

    @Override
    public int getConsumedSouls() {
        return _consumedSouls;
    }

    public int getCoupleId() {
        return _coupleId;
    }

    public L2ManufactureList getCreateList() {
        return _createList;
    }

    public L2CubicInstance getCubic(final int id) {
        if (_cubics != null)
            for (final L2CubicInstance cubic : _cubics)
                if (cubic.getId() == id)
                    return cubic;
        return null;
    }

    public GCSArray<L2CubicInstance> getCubics() {
        return _cubics == null ? new GCSArray<L2CubicInstance>(0) : _cubics;
    }

    public DeathPenalty getDeathPenalty() {
        return getActiveClass().getDeathPenalty();
    }

    /**
     * @return a table containing all L2RecipeList of the L2Player.<BR><BR>
     */
    public Collection<L2Recipe> getDwarvenRecipeBook() {
        return _recipebook.values();
    }

    public int getDwarvenRecipeLimit() {
        return (int) calcStat(Stats.DWARVEN_RECIPE_LIMIT, 50, null, null) + Config.ALT_ADD_RECIPES;
    }

    /**
     * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR><BR>
     */
    public int getEnchantEffect() {
        final L2ItemInstance wpn = getActiveWeaponInstance();

        if (wpn == null)
            return 0;

        return Math.min(127, wpn.getEnchantLevel());
    }

    public L2ItemInstance getEnchantScroll() {
        return _enchantScroll;
    }

    public int getEngageId() {
        return _engageid;
    }

    public int getEventPoints() {
        return _eventPoints;
    }

    public boolean getExchangeRefusal() {
        return _exchangeRefusal;
    }

    public int getExpandInventory() {
        return _expandInventory;
    }

    public int getExpandWarehouse() {
        return _expandWarehouse;
    }

    public L2Fishing getFishCombat() {
        return _fishCombat;
    }

    public Location getFishLoc() {
        return _fishLoc;
    }

    public Warehouse getFreight() {
        return _freight;
    }

    public int getFreightLimit() {
        // FIXME Не учитывается количество предметов, уже имеющееся на складе
        return getWarehouseLimit();
    }

    public int getWarehouseLimit() {
        return (int) calcStat(Stats.STORAGE_LIMIT, 0, null, null);
    }

    public Location getGroundSkillLoc() {
        return _groundSkillLoc;
    }

    /**
     * @param slot id слота у перса
     * @return the Henna of this L2Player corresponding to the selected slot.<BR><BR>
     */
    public L2HennaInstance getHenna(final int slot) {
        if (slot < 1 || slot > 3)
            return null;
        return _henna[slot - 1];
    }

    public int getHennaStatCON() {
        return _hennaCON;
    }

    public int getHennaStatDEX() {
        return _hennaDEX;
    }

    public int getHennaStatINT() {
        return _hennaINT;
    }

    public int getHennaStatMEN() {
        return _hennaMEN;
    }

    public int getHennaStatSTR() {
        return _hennaSTR;
    }

    public int getHennaStatWIT() {
        return _hennaWIT;
    }

    public Location getInBoatPosition() {
        return _inBoatPosition;
    }

    public ResidenceType getInResidence() {
        return _inResidence;
    }

    public int getIncorrectValidateCount() {
        return _incorrectValidateCount;
    }

    @Override
    public int getIncreasedForce() {
        return _increasedForce;
    }

    @Override
    public PcInventory getInventory() {
        return _inventory;
    }

    public String getLang() {
        return getVar("lang@");
    }

    public boolean getLang(final String lang) {
        return getLang().equalsIgnoreCase(lang) ? true : false;
    }

    public long getLastAttackPacket() {
        return _lastAttackPacket;
    }

    public String getLastBbsOperaion() {
        return _lastBBS_script_operation;
    }

    public Location getLastClientPosition() {
        return _lastClientPosition;
    }

    public long getLastEquipmPacket() {
        return _lastEquipmPacket;
    }

    public long getLastMovePacket() {
        return _lastMovePacket;
    }

    /**
     * @return the _lastFolkNpc of the L2Player corresponding to the last Folk witch one the player talked.<BR><BR>
     */
    public L2NpcInstance getLastNpc() {
        return _lastNpc;
    }

    public int getLastPage() {
        return _lastpage;
    }

    public long getLastReuseMsg() {
        return _lastReuseMessage;
    }

    public long getLastReuseMsgSkill() {
        return _lastReuseMessageSkill;
    }

    public Location getLastServerPosition() {
        return _lastServerPosition;
    }

    @Override
    public double getLevelMod() {
        return (89. + getLevel()) / 100.0;
    }

    public int getLoto(final int i) {
        return _loto[i];
    }

    public L2ItemInstance getLure() {
        return _lure;
    }

    public MacroList getMacroses() {
        return _macroses;
    }

    public Forum getMemo() {
        if (_forumMemo == null) {
            if (ForumsBBSManager.getInstance().getForumByName("MemoRoot") == null)
                return null;
            if (ForumsBBSManager.getInstance().getForumByName("MemoRoot").GetChildByName(_accountName) == null)
                ForumsBBSManager.getInstance().CreateNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
            setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").GetChildByName(_accountName));
        }
        return _forumMemo;
    }

    /**
     * @param forum
     */
    public void setMemo(final Forum forum) {
        _forumMemo = forum;
    }

    public boolean getMessageRefusal() {
        return _messageRefusal;
    }

    public int getMountLevel() {
        return _mountLevel;
    }

    public int getMountObjId() {
        return _mountObjId;
    }

    public int getMountType() {
        switch (getMountNpcId()) {
            case PetDataTable.STRIDER_WIND_ID:
            case PetDataTable.STRIDER_STAR_ID:
            case PetDataTable.STRIDER_TWILIGHT_ID:
                return 1;
            case PetDataTable.WYVERN_ID: // Wyvern
                return 2;
        }
        return 0;
    }

    public MultiSellListContainer getMultisell() {
        return _multisell;
    }

    public int getNameColor() {
        if (inObserverMode())
            return Color.black.getRGB();

        return _nameColor;
    }

    public L2GameClient getNetConnection() {
        return _connection;
    }

    @Override
    public int getNpcId() {
        return -2;
    }

    public Location getObsLoc() {
        return _obsLoc;
    }

    public L2WorldRegion getObservNeighbor() {
        return _observNeighbor;
    }

    public int getPartnerId() {
        return _partnerId;
    }

    public int getPartyMatchingLevels() {
        return _partyMatchingLevels;
    }

    public int getPartyMatchingRegion() {
        return _partyMatchingRegion;
    }

    public int getPledgeClass() {
        return _pledgeClass;
    }

    @SuppressWarnings("unchecked")
    public QuestState getQuestState(Class quest) {
        return _quests != null ? _quests.get(quest.getSimpleName()) : null;
    }

    public ArrayList<QuestState> getQuestsForEvent(final L2NpcInstance npc, final QuestEventType event) {
        final ArrayList<QuestState> states = new ArrayList<QuestState>();
        final Quest[] quests = npc.getTemplate().getEventQuests(event);
        if (quests != null)
            for (final Quest quest : quests)
                if (getQuestState(quest.getName()) != null && !getQuestState(quest.getName()).isCompleted())
                    states.add(getQuestState(quest.getName()));
        return states;
    }

    public Race getRace() {
        return getBaseTemplate().race;
    }

    public int getRace(final int i) {
        return _race[i];
    }

    @Override
    public float getRateAdena() {
        // return _party == null ? _bonus.RATE_DROP_ADENA : _party._rateAdena;
        return _party == null ? _bonus.RATE_DROP_ADENA : _party._rateAdena > 0.05D ? _party._rateAdena : _bonus.RATE_DROP_ADENA;
    }

    @Override
    public float getRateItems() {
        // return _party == null ? _bonus.RATE_DROP_ITEMS : _party._rateDrop;
        return _party == null ? _bonus.RATE_DROP_ITEMS : _party._rateDrop > 0.05D ? _party._rateDrop : _bonus.RATE_DROP_ITEMS;
    }

    @Override
    public float getRateSpoil() {
        // return _party == null ? _bonus.RATE_DROP_SPOIL : _party._rateSpoil;
        return _party == null ? _bonus.RATE_DROP_SPOIL : _party._rateSpoil > 0.05D ? _party._rateSpoil : _bonus.RATE_DROP_SPOIL;
    }

    public GArray<Integer> getScheme(String sheme_name) {
        if (_buffs.containsKey(sheme_name))
            return _buffs.get(sheme_name);
        return null;
    }

    /**
     * @return the secondary weapon item (always equipped in the left hand) or the fists weapon.<BR><BR>
     */
    @Override
    public L2Weapon getSecondaryWeaponItem() {
        final L2ItemInstance weapon = getSecondaryWeaponInstance();

        if (weapon == null)
            return getFistsWeaponItem();

        final L2Item item = weapon.getItem();

        if (item instanceof L2Weapon)
            return (L2Weapon) item;

        return null;
    }

    /**
     * @return the secondary weapon instance (always equipped in the left hand).<BR><BR>
     */
    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
    }

    public ConcurrentLinkedQueue<TradeItem> getSellList() {
        return _sellList != null ? _sellList : new ConcurrentLinkedQueue<TradeItem>();
    }

    public synchronized FastMap<String, GArray<Integer>> getShemes() {
        return _buffs;
    }

    public String getShift() {
        return _shift_page;
    }

    public L2ShortCut getShortCut(final int slot, final int page) {
        return _shortCuts.getShortCut(slot, page);
    }

    public boolean getSittingTask() {
        return _sittingTask;
    }

    public ClassId getSkillLearningClassId() {
        return _skillLearningClassId;
    }

    @Override
    public int getSpeed(int baseSpeed) {
        if (isMounted()) {
            final L2PetData petData = PetDataTable.getInstance().getInfo(_mountNpcId, _mountLevel);
            int speed = 187;
            if (petData != null)
                speed = petData.getSpeed();
            double mod = 1.;
            final int level = getLevel();
            if (_mountLevel > level && level - _mountLevel > 10)
                mod = 0.5; // Штраф на разницу уровней между игроком и петом
            baseSpeed = (int) (mod * speed);
        }
        return super.getSpeed(baseSpeed);
    }

    public int getSponsor() {
        return _clan == null ? 0 : _clan.getClanMember(getObjectId()).getSponsor();
    }

    public int getSubLevel() {
        return isSubClassActive() ? getLevel() : 0;
    }

    public boolean isSubClassActive() {
        return getBaseClassId() != getActiveClassId();
    }

    @Override
    public int getTeam() {
        return _team;
    }

    public int getTeleMode() {
        return _telemode;
    }

    /**
     * Возвращает время в минутах до следующего входа в указанный инстанс.
     */
    public int getTimeToNextEnterInstance(final String name) {
        final String var = getVar(name);
        if (var == null)
            return 0;
        final Calendar last = Calendar.getInstance();
        last.setTimeInMillis(Long.parseLong(var));
        final Calendar now = Calendar.getInstance();
        if (last.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
            unsetVar(name);
            return 0;
        }
        return Math.max(1440 - (now.get(Calendar.HOUR) * 60 + now.get(Calendar.MINUTE)), 1);
    }

    public int getTitleColor() {
        return _titlecolor;
    }

    public int getTradeLimit() {
        return (int) calcStat(Stats.TRADE_LIMIT, 0, null, null);
    }

    public boolean getTradeRefusal() {
        return _tradeRefusal;
    }

    public L2TamedBeastInstance getTrainedBeast() {
        return _tamedBeast;
    }

    public TransactionType getTransactionType() {
        return _currentTransactionType;
    }

    public int getUnstuck() {
        return _unstuck;
    }

    public long getUptime() {
        return System.currentTimeMillis() - _uptime;
    }

    public int getUseSeed() {
        return _useSeed;
    }

    /**
     * Возвращает тип используемого склада.
     *
     * @return null или тип склада:<br> <ul> <li>WarehouseType.PRIVATE <li>WarehouseType.CLAN <li>WarehouseType.CASTLE <li>WarehouseType.FREIGHT </ul>
     */
    public Warehouse.WarehouseType getUsingWarehouseType() {
        return _usingWHType;
    }

    public FastMap<String, String> getVars() {
        return user_variables;
    }

    public L2Object getVisibleObject(final int id) {
        if (getObjectId() == id)
            return this;

        if (getTargetId() == id)
            return getTarget();

        if (_party != null)
            for (final L2Player p : _party.getPartyMembers())
                if (p != null && p.getObjectId() == id)
                    return p;

        return L2World.getAroundObjectById(this, id);
    }

    public int getWaitSitTime() {
        return _waitTimeWhenSit;
    }

    public Warehouse getWarehouse() {
        return _warehouse;
    }

    public ScheduledFuture<?> getWaterTask() {
        return _taskWater;
    }

    public int getexpertisePenalty() {
        return expertisePenalty;
    }

    public long getlastPvpAttack() {
        return _lastPvpAttack;
    }

    public void giveRecom(final L2Player target) {
        final int targetRecom = target.getRecomHave();
        if (targetRecom < 255)
            target.setRecomHave(targetRecom + 1);
        if (_recomLeft > 0)
            _recomLeft--;
        _recomChars.add(target.getName().hashCode());
    }

    public int getRecomHave() {
        return _recomHave;
    }

    public void increaseEventPoints() {
        _eventPoints++;
    }

    private boolean increaseLevel() {
        return increaseLevel(true);
    }

    public boolean isAllyLeader() {
        return getAlliance() != null && getAlliance().getLeader().getLeaderId() == getObjectId();
    }

    public int isAtWar() {
        return _clan == null || _clan.isAtWarOrUnderAttack() <= 0 ? 0 : 1;
    }

    public int isAtWarWith(final Integer id) {
        return _clan == null || !_clan.isAtWarWith(id) ? 0 : 1;
    }

    public boolean isAutoLootEnabled() {
        return AutoLoot;
    }

    public boolean isAutoLootHerbsEnabled() {
        return AutoLootHerbs;
    }

    public boolean isBlockAll() {
        return _blockAll;
    }

    public boolean isCastleLord(final int castleId) {
        return _clan != null && isClanLeader() && _clan.getHasCastle() == castleId;
    }

    public boolean isChecksForTeam() {
        return _checksForTeam;
    }

    public boolean isCombatFlagEquipped() {
        return getActiveWeaponInstance() != null && getActiveWeaponInstance().getItemId() == 9819;
    }

    public boolean isConnected() {
        return _isConnected;
    }

    public boolean isDeleting() {
        return _isDeleting;
    }

    public boolean isEngageRequest() {
        return _engagerequest;
    }

    public boolean isFishing() {
        return _fishing;
    }

    @Override
    public boolean isHero() {
        return _hero;
    }

    public boolean isInBlockList(final L2Player player) {
        return isInBlockList(player.getObjectId());
    }

    public boolean isInBlockList(final int charId) {
        return _blockList.containsKey(charId);
    }

    public boolean isInDuel() {
        return _duel != null;
    }

    /**
     * @return True if the Inventory is disabled.<BR><BR>
     */
    public boolean isInvetoryDisabled() {
        return _inventoryDisable;
    }

    public boolean isMaried() {
        return _maried;
    }

    public boolean isMaryAccepted() {
        return _maryaccepted;
    }

    public boolean isMaryRequest() {
        return _maryrequest;
    }

    public boolean isNotShowBuffAnim() {
        return _notShowBuffAnim;
    }

    public boolean isOlympiadGameStart() {
        final OlympiadGame og = Olympiad.getOlympiadGame(_olympiadGameId);
        if (og == null) {
            _log.warning("OlympiadGameQueue is null, olympiadGameId=" + _olympiadGameId);
            return true;
        }

        if (og.getStarted() == 1)
            return true;
        return false;
    }

    /**
     * Проверка на переполнение инвентаря и перебор в весе для квестов и эвентов
     *
     * @return true если ве проверки прошли успешно
     */
    public boolean isQuestContinuationPossible() {
        if (getWeightPenalty() >= 3 || getInventoryLimit() * 0.8 < getInventory().getSize()) {
            sendPacket(Msg.PROGRESS_IN_A_QUEST_IS_POSSIBLE_ONLY_WHEN_YOUR_INVENTORYS_WEIGHT_AND_VOLUME_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
            return false;
        }
        return true;
    }

    public int getInventoryLimit() {
        return (int) calcStat(Stats.INVENTORY_LIMIT, 0, null, null);
    }

    public int getWeightPenalty() {
        return _curWeightPenalty;
    }

    public boolean isReviveRequested() {
        return _ReviveRequested == 1;
    }

    public boolean isRevivingPet() {
        return _RevivePet;
    }

    /**
     * @return True if a transaction is in progress.<BR><BR>
     */
    public boolean isTransactionInProgress() {
        return (_currentTransactionTimeout < 0 || _currentTransactionTimeout > System.currentTimeMillis()) && _currentTransactionRequester != null;
    }

    public boolean isWearingArmor(final ArmorType armorType) {
        final L2ItemInstance chest = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);

        if (chest == null)
            return armorType == ArmorType.NONE;

        if (chest.getItemType() != armorType)
            return false;

        if (chest.getBodyPart() == L2Item.SLOT_FULL_ARMOR)
            return true;

        final L2ItemInstance legs = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);

        return legs == null ? armorType == ArmorType.NONE : legs.getItemType() == armorType;
    }

    public boolean isWearingFormalWear() {
        return _IsWearingFormalWear;
    }

    public void joinParty(final L2Party party) {
        if (party != null) {
            _party = party;
            party.addPartyMember(this);
        }
    }

    public long lastShift() {
        return _last_shift_append;
    }

    public void leaveMovieMode() {
        setIsInvul(false);
        setImobilised(false);
        sendPacket(new CameraMode(0));
    }

    /**
     * Сохраняет персонажа в бд и запускает необходимые процедуры.
     *
     * @param shutdown тру при шатдауне
     * @param restart  тру при рестарте. Игнорируется шатдаун.
     * @param kicked   Отобразить у клиента табличку с мессагой о закрытии коннекта
     */
    public void logout(final boolean shutdown, final boolean restart, final boolean kicked) {
        if (isLogoutStarted())
            return;

        setLogoutStarted(true);
        prepareToLogout(kicked);

        Log.LogChar(this, Log.Logout, "");
        synchronized (_storeLock) {
            L2GameClient.saveCharToDisk(this);
        }

        // Msg.ExRestartClient - 2 таблички появляется (вторая GG Fail), нажатие ок приводит к закрытию клиента
        // Msg.ServerClose - табличка появляется, после нажатия ок переходит к диалогу ввода логина/пароля
        // Msg.LeaveWorld - молча закрывает клиент (используется при выходе из игры)

        if (restart) {
            // При рестарте просто обнуляем коннект
            deleteMe();
            if (_connection != null)
                _connection.setActiveChar(null);
        } else {
            final L2GameServerPacket sp = shutdown || kicked ? Msg.ServerClose : Msg.LeaveWorld;
            sendPacket(sp);
            if (_connection != null && _connection.getConnection() != null)
                _connection.getConnection().close(sp);
            deleteMe();
        }

        _connection = null;
        setConnected(false);
    }

    public void setLogoutStarted(final boolean logoutStarted) {
        _logoutStarted = logoutStarted;
    }

    public void prepareToLogout(final boolean kicked) {
        if (isFlying() && !checkLandingState())
            teleToClosestTown();

        if (isCastingNow())
            abortCast();

        // При логауте автоматом проигрывается дуэль.
        if (getDuel() != null)
            getDuel().onPlayerDefeat(this);

        if (isFestivalParticipant()) {
            final L2Party playerParty = getParty();

            if (playerParty != null)
                playerParty.broadcastMessageToPartyMembers(getName() + " has been removed from the upcoming festival.");
        }

        if (kicked && Config.ALLOW_CURSED_WEAPONS && Config.DROP_CURSED_WEAPONS_ON_KICK)
            if (isCursedWeaponEquipped()) {
                _pvpFlag = 0;
                CursedWeaponsManager.getInstance().dropPlayer(this);
            }

        CursedWeaponsManager.getInstance().doLogout(this);

        if (inObserverMode())
            if (getOlympiadGameId() == -1)
                leaveObserverMode();
            else
                leaveOlympiadObserverMode();

        if (isInOlympiadMode() || getOlympiadGameId() > -1) {
            Olympiad.logoutPlayer(this);
            Olympiad.unRegisterNoble(this, true);
        }

        // Вызов всех хэндлеров, определенных в скриптах
        final Object[] script_args = new Object[]{this};
        for (final ScriptClassAndMethod handler : Scripts.onPlayerExit)
            Scripts.callScripts(handler.scriptClass, handler.method, this, script_args);

        if (_stablePoint != null) {
            teleToLocation(_stablePoint);
            addAdena(_stablePoint.h);
            unsetVar("wyvern_moneyback");
        }

        // проверка на тюрьму
        if (isInJail()) {
            // срок заключения кончается только когда чар онлайн
            if (_unjailTask != null) {
                _unjailTask.cancel(true);
                _unjailTask = null;
            }
            // посчитать, сколько времени чар посидел в тюрьме; вычесть это время из его срока заключения
            // _jailStartTime устанавливается когда запускается UnJailTask
            long secondsInJail = (System.currentTimeMillis() - _jailStartTime) / 1000;
            long minutesInJail = secondsInJail / 60;
            long srok = Long.parseLong(getVar("jailed")); // "jailed" - срок тюряги в минутах
            srok -= minutesInJail; // "скостим" срок
            if (srok < 1)
                srok = 1; // пусть хоть 1 минута останется... чтобы srok не был = 0
            setVar("jailed", String.valueOf(srok)); // обновим переменную у чара
        }

        if (getPet() != null)
            try {
                getPet().unSummon();
            } catch (final Throwable t) {
                t.printStackTrace();
                _log.log(Level.WARNING, "prepareToLogout()", t);
            }

        if (isInParty())
            try {
                leaveParty();
            } catch (final Throwable t) {
                t.printStackTrace();
                _log.log(Level.WARNING, "prepareToLogout()", t);
            }
    }

    /**
     * Проверяет, можно ли приземлиться в этой зоне.
     *
     * @return можно ли приземлится
     */
    public boolean checkLandingState() {
        if (isInZone(L2Zone.ZoneType.no_landing))
            return false;

        final Siege siege = SiegeManager.getSiege(this, false);
        if (siege != null) {
            final Residence unit = siege.getSiegeUnit();
            if (unit != null && getClan() != null && isClanLeader() && getClan().getHasCastle() == unit.getId())
                return true;
            return false;
        }

        return true;
    }

    public synchronized void leaveObserverMode() {
        if (!_observerMode || _observNeighbor == null || getCurrentRegion() == null) {
            _observerMode = false;
            _observNeighbor = null;
            return;
        }

        // Удаляем фэйк из точки наблюдения и удаляем у чара все обьекты, что там находятся
        for (final L2WorldRegion neighbor : _observNeighbor.getNeighbors()) {
            neighbor.removeObjectsFromPlayer(this);
            neighbor.removeObject(this, false);
        }

        // Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
        setLastClientPosition(null);
        setLastServerPosition(null);

        _observNeighbor = null;
        _observerMode = false;

        setTarget(null);
        unblock();
        standUp();

        // Выходим из режима обсервинга
        sendPacket(new ObserverEnd(this));
    }

    public synchronized void leaveOlympiadObserverMode() {
        if (!_observerMode || _observNeighbor == null || getCurrentRegion() == null) {
            _observerMode = false;
            _observNeighbor = null;
            return;
        }

        // Удаляем фэйк из точки наблюдения и удаляем у чара все обьекты, что там находятся
        for (final L2WorldRegion neighbor : _observNeighbor.getNeighbors()) {
            neighbor.removeObjectsFromPlayer(this);
            neighbor.removeObject(this, false);
        }

        _observNeighbor = null;

        setTarget(null);
        unblock();
        //standUp();

        Olympiad.removeSpectator(_olympiadGameId, this);
        _olympiadGameId = -1;

        // Убираем надпись над головой
        broadcastUserInfo(true);

        // Меняем интерфейс
        sendPacket(new ExOlympiadMode(0));

        // Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
        setLastClientPosition(null);
        setLastServerPosition(null);

        // "Телепортируемся"
        sendPacket(new TeleportToLocation(this, getX(), getY(), getZ()));
        _observerMode = false;
    }

    /**
     * @return true, если игрок на зоне
     */
    public boolean isInJail() {
        return _isInJail;
    }

    public void deleteMe() {
        setMassUpdating(true);
        _isDeleting = true;

        for (final L2Player player : _snoopedPlayer)
            player.removeSnooper(this);

        for (final L2Player player : _snoopListener)
            player.removeSnooped(this);

        // Send friendlists to friends that this player has logged off
        EnterWorld.notifyFriends(this, false);

        if (getTransactionRequester() != null) {
            getTransactionRequester().setTransactionRequester(null);
            setTransactionRequester(null);
        }

        // Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
        try {
            setOnlineStatus(false);
        } catch (final Throwable t) {
            _log.log(Level.WARNING, "deletedMe()", t);
        }

        // Stop the HP/MP/CP Regeneration task (scheduled tasks)
        try {
            stopAllTimers();
        } catch (final Throwable t) {
            _log.log(Level.WARNING, "deletedMe()", t);
        }

        // Cancel Attak or Cast
        try {
            setTarget(null);
        } catch (final Throwable t) {
            _log.log(Level.WARNING, "deletedMe()", t);
        }

        if (_forceBuff != null)
            _forceBuff.delete();

        // Remove the L2Player from the world
        if (isVisible())
            try {
                decayMe();
            } catch (final Throwable t) {
                _log.log(Level.WARNING, "deletedMe()", t);
            }

        try {
            if (getClanId() > 0 && _clan != null && _clan.getClanMember(getObjectId()) != null) {
                final int sponsor = _clan.getClanMember(getObjectId()).getSponsor();
                final int apprentice = getApprentice();
                final PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(this);
                for (final L2Player clanMember : _clan.getOnlineMembers(getObjectId())) {
                    if (clanMember.getObjectId() == getObjectId())
                        continue;
                    clanMember.sendPacket(memberUpdate);
                    if (clanMember.getObjectId() == sponsor)
                        clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_APPRENTICE_HAS_LOGGED_OUT).addString(_name));
                    else if (clanMember.getObjectId() == apprentice)
                        clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_SPONSOR_HAS_LOGGED_OUT).addString(_name));
                }
                _clan.getClanMember(getObjectId()).setPlayerInstance(null);
            }
        } catch (final Throwable t) {
            _log.log(Level.SEVERE, "deletedMe()", t);
        }

        if (CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()) != null)
            CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).setPlayer(null);

        if (getPartyRoom() > 0) {
            final PartyRoom room = PartyRoomManager.getInstance().getRooms().get(getPartyRoom());
            if (room != null)
                if (room.getLeader() == null || room.getLeader().equals(this))
                    PartyRoomManager.getInstance().removeRoom(room.getId());
                else
                    room.removeMember(this, false);
        }

        setPartyRoom(0);

        setEffectList(null);

        // Update database with items in its inventory and remove them from the world
        try {
            getInventory().deleteMe();
        } catch (final Throwable t) {
            _log.log(Level.WARNING, "deletedMe()", t);
        }

        L2World.removeObject(this);
        stopPvPFlag();

        _inventory = PcInventoryDummy.instance;
        _warehouse = null;
        _freight = null;
        _ai = null;
        _summon = null;
        _arrowItem = null;
        _fistsWeaponItem = null;
        _chars = null;
        _enchantScroll = null;
        _lastNpc = null;
        _obsLoc = null;
        _observNeighbor = null;
        setOwner(null);
    }

    public void removeSnooper(final L2Player pci) {
        _snoopListener.remove(pci);
    }

    public void removeSnooped(final L2Player pci) {
        _snoopedPlayer.remove(pci);
    }

    /**
     * @return the L2Player requester of the transaction.<BR><BR>
     */
    public L2Player getTransactionRequester() {
        return _currentTransactionRequester;
    }

    public void setOnlineStatus(final boolean isOnline) {
        _isOnline = isOnline;
        updateOnlineStatus();
    }

    public void updateOnlineStatus() {
        boolean online = isOnline();
        if (isInOfflineMode())
            online = false;

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
            statement.setInt(1, online ? 1 : 0);
            statement.setLong(2, System.currentTimeMillis() / 1000);
            statement.setInt(3, getObjectId());
            statement.execute();
        } catch (final Exception e) {
            _log.warning("could not set char online status:" + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void stopAllTimers() {
        if (_cubics != null && !_cubics.isEmpty())
            for (final L2CubicInstance cubic : _cubics)
                cubic.deleteMe();
        stopWaterTask();
        stopBonusTask();
        stopKickTask();
    }

    public void stopBonusTask() {
        if (_bonusExpiration != null) {
            _bonusExpiration.cancel(true);
            _bonusExpiration = null;
        }
    }

    public void stopKickTask() {
        if (_kickTask != null) {
            _kickTask.cancel(false);
            _kickTask = null;
        }
    }

    public Integer getPartyRoom() {
        return _partyRoom;
    }

    public void setPartyRoom(final Integer partyRoom) {
        _partyRoom = partyRoom;
    }

    public void stopPvPFlag() {
        if (_PvPRegTask != null)
            _PvPRegTask.cancel(true);
        _PvPRegTask = null;
        updatePvPFlag(0);
    }

    public void setConnected(final boolean connected) {
        _isConnected = connected;
    }

    /**
     * Удаляет всю информацию о классе и добавляет новую, только для сабклассов
     */
    public boolean modifySubClass(final int oldClassId, final int newClassId) {
        final L2SubClass originalClass = _classlist.get(oldClassId);
        if (originalClass.isBase())
            return false;

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();

            // Remove all basic info stored about this sub-class.
            statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=? AND class_id=? AND isBase = 0");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
            DatabaseUtils.closeStatement(statement);

            // Remove all skill info stored for this sub-class.
            statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND class_index=? ");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
            DatabaseUtils.closeStatement(statement);

            // Remove all saved skills info stored for this sub-class.
            statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=? ");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
            DatabaseUtils.closeStatement(statement);

            // Remove all saved effects stored for this sub-class.
            statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_id=? AND class_index=? ");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
            DatabaseUtils.closeStatement(statement);

            // Remove all henna info stored for this sub-class.
            statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=? ");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
            DatabaseUtils.closeStatement(statement);

            // Remove all shortcuts info stored for this sub-class.
            statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=? ");
            statement.setInt(1, getObjectId());
            statement.setInt(2, oldClassId);
            statement.execute();
        } catch (final Exception e) {
            _log.warning("Could not delete char sub-class: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
        _classlist.remove(oldClassId);
        return addSubClass(newClassId, false);
    }

    /**
     * Добавить класс, используется только для сабклассов
     *
     * @param storeOld TODO
     */
    public boolean addSubClass(final int classId, final boolean storeOld) {
        if (_classlist.size() >= 4 + Config.ALT_GAME_SUB_ADD)
            return false;

        final ClassId newId = ClassId.values()[classId];

        final L2SubClass newClass = new L2SubClass();
        if (newId.getRace() == null)
            return false;

        newClass.setClassId(classId);
        newClass.setPlayer(this);

        _classlist.put(classId, newClass);

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            // Store the basic info about this new sub-class.
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("INSERT INTO character_subclasses (char_obj_id, class_id, exp, sp, curHp, curMp, curCp, maxHp, maxMp, maxCp, level, active, isBase, death_penalty, skills) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, getObjectId());
            statement.setInt(2, newClass.getClassId());
            statement.setLong(3, Experience.LEVEL[40]);
            statement.setInt(4, 0);
            statement.setDouble(5, getCurrentHp());
            statement.setDouble(6, getCurrentMp());
            statement.setDouble(7, getCurrentCp());
            statement.setDouble(8, getCurrentHp());
            statement.setDouble(9, getCurrentMp());
            statement.setDouble(10, getCurrentCp());
            statement.setInt(11, 40);
            statement.setInt(12, 0);
            statement.setInt(13, 0);
            statement.setInt(14, 0);
            statement.setString(15, "");
            statement.execute();
        } catch (final Exception e) {
            _log.warning("Could not add character sub-class: " + e);
            return false;
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }

        setActiveSubClass(classId, storeOld);

        // Add all the necessary skills up to level 40 for this new class.
        boolean countUnlearnable = true;
        int unLearnable = 0;
        int numSkillsAdded = 0;
        ArrayList<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSkills(this, newId);
        while (skills.size() > unLearnable) {
            for (final L2SkillLearn s : skills) {
                final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
                if (sk == null || !sk.getCanLearn(newId)) {
                    if (countUnlearnable)
                        unLearnable++;
                    continue;
                }
                addSkill(sk, true);
                numSkillsAdded++;
            }
            countUnlearnable = false;
            skills = SkillTreeTable.getInstance().getAvailableSkills(this, newId);
        }

        restoreSkills();
        rewardSkills();
        sendPacket(new SkillList(this));
        setCurrentHpMp(getMaxHp(), getMaxMp(), true);
        setCurrentCp(getMaxCp());

        if (Config.DEBUG)
            _log.info(numSkillsAdded + " skills added for " + getName() + "'s sub-class.");

        return true;
    }

    public void offline() {
        setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
        setOfflineMode(true);
        setVar("offline", String.valueOf(System.currentTimeMillis() / 1000));
        if (Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK > 0)
            startKickTask(Config.SERVICES_OFFLINE_TRADE_SECONDS_TO_KICK * 1000);
        if (isFestivalParticipant()) {
            final L2Party playerParty = getParty();

            if (playerParty != null)
                playerParty.broadcastMessageToPartyMembers(getName() + " has been removed from the upcoming festival.");
        }

        if (getParty() != null)
            getParty().oustPartyMember(this);

        if (getPet() != null)
            getPet().unSummon();

        CursedWeaponsManager.getInstance().doLogout(this);

        if (isInOlympiadMode() || getOlympiadGameId() > -1) {
            Olympiad.logoutPlayer(this);
            Olympiad.unRegisterNoble(this, true);
        }

        sendPacket(Msg.LeaveWorld);
        setConnected(false);
        setOnlineStatus(false);
        // LSConnection.getInstance().removeAccount(getNetConnection());
        // LSConnection.getInstance().sendPacket(new PlayerLogout(getNetConnection().getLoginName()));
        broadcastUserInfo(true);

        store(false);
    }

    public void setOfflineMode(final boolean val) {
        if (!val)
            unsetVar("offline");
        _offline = val;
    }

    /**
     * Через delay миллисекунд выбросит игрока из игры
     */
    public void startKickTask(final long delay) {
        if (_kickTask != null)
            stopKickTask();
        _kickTask = ThreadPoolManager.getInstance().scheduleAi(new KickTask(this), delay, true);
    }

    /**
     * Update L2Player stats in the characters table of the database.
     */
    public void store(final boolean fast) {
        synchronized (_storeLock) {
            storePlayerBuffSchemes();
            storePvPLOG();
            storePlayerShift();

            ThreadConnection con = null;
            FiltredPreparedStatement statement = null;
            try {
                con = L2DatabaseFactory.getInstance().getConnection();
                statement = con.prepareStatement(//
                        "UPDATE characters SET face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?" + //
                                ",karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,deletetime=?," + //
                                "title=?,accesslevel=?,online=?,leaveclan=?,deleteclan=?,nochannel=?," + //
                                "onlinetime=?,noble=?,ketra=?,varka=?,ram=?,pledge_type=?,pledge_rank=?,lvl_joined_academy=?,apprentice=?,pcBangPoints=?,char_name=? WHERE obj_Id=? LIMIT 1");
                statement.setInt(1, getFace());
                statement.setInt(2, getHairStyle());
                statement.setInt(3, getHairColor());
                statement.setInt(4, getHeading() & 0xFFFF);
                if (_stablePoint == null) // если игрок находится в точке в которой его сохранять не стоит (например на виверне) то сохраняются последние координаты
                {
                    statement.setInt(5, getX());
                    statement.setInt(6, getY());
                    statement.setInt(7, getZ());
                } else {
                    statement.setInt(5, _stablePoint.x);
                    statement.setInt(6, _stablePoint.y);
                    statement.setInt(7, _stablePoint.z);
                }
                statement.setInt(8, getKarma());
                statement.setInt(9, getPvpKills());
                statement.setInt(10, getPkKills());
                statement.setInt(11, getRecomHave());
                statement.setInt(12, getRecomLeft());
                statement.setInt(13, getClanId());
                statement.setInt(14, getDeleteTimer());
                statement.setString(15, getTitle());
                statement.setInt(16, _accessLevel);
                statement.setInt(17, isOnline() ? 1 : 0);
                statement.setLong(18, getLeaveClanTime() / 1000);
                statement.setLong(19, getDeleteClanTime() / 1000);
                statement.setLong(20, _NoChannel > 0 ? getNoChannelRemained() / 1000 : _NoChannel);
                statement.setLong(21, _onlineBeginTime > 0 ? (_onlineTime + System.currentTimeMillis() - _onlineBeginTime) / 1000 : _onlineTime / 1000);
                statement.setInt(22, isNoble() ? 1 : 0);
                statement.setInt(23, getKetra());
                statement.setInt(24, getVarka());
                statement.setInt(25, getRam());
                statement.setInt(26, getPledgeType());
                statement.setInt(27, getPowerGrade());
                statement.setInt(28, getLvlJoinedAcademy());
                statement.setInt(29, getApprentice());
                statement.setInt(30, getPcBangPoints());
                statement.setString(31, getName());
                statement.setInt(32, getObjectId());

                statement.executeUpdate();
                L2World.increaseUpdatePlayerBase();

                if (!fast) {
                    storeEffects();
                    storeDisableSkills();
                    storeBlockList();
                }

                storeCharSubClasses();
            } catch (final Exception e) {
                _log.warning("store: could not store char data: " + e);
                e.printStackTrace();
            } finally {
                DatabaseUtils.closeDatabaseCS(con, statement);
            }
        }
    }

    public void storePlayerBuffSchemes() {
        if (_buffs == null || _buffs.isEmpty())
            return;

        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.createStatement();
            statement.executeUpdate("DELETE FROM character_buffs_schemes WHERE char_id = " + getObjectId());

            final SqlBatch b = new SqlBatch("INSERT IGNORE INTO character_buffs_schemes (char_id,s_name,buff_id) VALUES");
            synchronized (_buffs) {
                StringBuilder sb;
                for (Entry<String, GArray<Integer>> effect : _buffs.entrySet()) {
                    for (Integer buff : effect.getValue()) {
                        sb = new StringBuilder("(");
                        sb.append(getObjectId()).append(",").append("'");
                        sb.append(effect.getKey()).append("'").append(",");
                        sb.append(buff).append(")");
                        b.write(sb.toString());
                    }
                }
            }
            if (!b.isEmpty())
                statement.executeUpdate(b.close());
        } catch (final Exception e) {
            _log.warning("Could not store buff schemes data: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void storePvPLOG() {
        if (_lastPvPs == null || _lastPvPs.isEmpty())
            return;

        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.createStatement();
            statement.executeUpdate("DELETE FROM character_pvplog WHERE player = " + getObjectId());

            final SqlBatch b = new SqlBatch("INSERT IGNORE INTO character_pvplog (time,player,oponent,result) VALUES");
            synchronized (_lastPvPs) {
                StringBuilder sb;
                for (PvPinfo data : _lastPvPs) {
                    sb = new StringBuilder("('");
                    sb.append(data.getTime()).append("','");
                    sb.append(getObjectId()).append("','");
                    sb.append(data.getOponent()).append("','");
                    sb.append(data.getResult()).append("')");
                    b.write(sb.toString());
                }
            }
            if (!b.isEmpty())
                statement.executeUpdate(b.close());
        } catch (final Exception e) {
            _log.warning("Could not store pvplog data: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    private void storePlayerShift() {
        generate_shift();
        generate_lastKills();
        PlayerShiftCache.update(getName(), _shift_page, _shift_page_last_kills);
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("INSERT IGNORE character_shift (player,shift_html,kills_html) VALUES (?,?,?)");
            statement.setString(1, getName());
            statement.setString(2, _shift_page);
            statement.setString(3, _shift_page_last_kills);
            statement.executeUpdate();
        } catch (final Exception e) {
            _log.warning("Could not store shift schemes data: " + e);
            e.printStackTrace();
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    public void generate_shift() {
        _last_shift_append = System.currentTimeMillis();
        String dialog = Files.read("data/scripts/actions/player.Stats.htm");

        HashMap<Integer, L2SubClass> subs = getSubClasses();

        dialog = dialog.replaceAll("%rank_name%", getRankName());
        dialog = dialog.replaceAll("%left_to_next_rank%", (getRankKills() - getPvpKills()) + "");

        int barup = calculteBar(280, getPvpKills() - getRankMinus(), getRankKills());
        dialog = dialog.replaceAll("%bar1up%", "" + barup);
        dialog = dialog.replaceAll("%bar2up%", "" + (280 - barup));

        int cc = 0;
        for (L2SubClass sub : subs.values()) {
            cc++;
            dialog = dialog.replaceAll("%sub_name_" + cc + "%", CharTemplateTable.getClassNameById(sub.getClassId()));
            dialog = dialog.replaceAll("%sub_pvp_" + cc + "%", sub.getPvPCount() + "");
        }

        if (cc < 7)
            for (int i = cc; i < 7; i++) {
                dialog = dialog.replaceAll("%sub_name_" + i + "%", "Empty Slot");
                dialog = dialog.replaceAll("%sub_pvp_" + i + "%", "-");
            }

        dialog = dialog.replaceAll("%total_pvp%", getPvpKills() + "");

        dialog = dialog.replaceAll("%class_name%", CharTemplateTable.getClassNameById(getActiveClassId()));

        dialog = dialog.replaceAll("%char_name%", getName());
        dialog = dialog.replaceFirst("%p.atk%", getPAtk(null) + "");
        dialog = dialog.replaceFirst("%m.atk%", getMAtk(null, null) + "");
        dialog = dialog.replaceFirst("%p.def%", getPDef(null) + "");
        dialog = dialog.replaceFirst("%m.def%", getMDef(null, null) + "");

        dialog = dialog.replaceFirst("%acuracy%", getAccuracy() + "");
        dialog = dialog.replaceFirst("%evasion%", getEvasionRate(null) + "");
        dialog = dialog.replaceFirst("%crit_rate%", getCriticalHit(null, null) + "");
        dialog = dialog.replaceFirst("%speed%", getRunSpeed() + "");
        dialog = dialog.replaceFirst("%attack_speed%", getPAtkSpd() + "");
        dialog = dialog.replaceFirst("%casting_speed%", getMAtkSpd() + "");
        dialog = dialog.replaceFirst("%offline_text%", "<center><font color=\"a9a9a2\">Player last login: " + Util.convertDateToString(System.currentTimeMillis() / 1000) + " </font></center>");
        _shift_page = dialog;
        generate_lastKills();
    }

    public HashMap<Integer, L2SubClass> getSubClasses() {
        return _classlist;
    }

    private String getRankName() {
        return RANK_NAMES[getRank()];
    }

    private int calculteBar(int barmax, int buffs, int maxbuffs) {
        int c = barmax * ((buffs == 0 ? 1 : buffs) * 100 / maxbuffs) / 100;
        if (c >= barmax)
            return barmax;
        return c;
    }

    private int getRankMinus() {
        switch (getRank()) {
            case RANK_NOVICE:
                return 0;
            case RANK_INITIATE:
                return 25;
            case RANK_MINOR:
                return 100;
            case RANK_EXPERT:
                return 200;
            case RANK_THUG:
                return 400;
            case RANK_ADEPT:
                return 800;
            case RANK_CONJURER:
                return 1200;
            case RANK_ELITE:
                return 1800;
            case RANK_ARCHON:
                return 2500;
        }
        return 3000;
    }

    private int getRankKills() {
        switch (getRank()) {
            case RANK_NOVICE:
                return 25;
            case RANK_INITIATE:
                return 100;
            case RANK_MINOR:
                return 200;
            case RANK_EXPERT:
                return 400;
            case RANK_THUG:
                return 800;
            case RANK_ADEPT:
                return 1200;
            case RANK_CONJURER:
                return 1800;
            case RANK_ELITE:
                return 2500;
            case RANK_ARCHON:
                return 3000;
        }
        return 20;
    }

    @Override
    public int getPAtk(final L2Character target) {
        double init = getActiveWeaponInstance() == null ? (isMageClass() ? 3 : 4) : 0;
        return Balancer.getModify(bflag.patak, (int) calcStat(Stats.POWER_ATTACK, init, target, null), getClassId().getId());
    }

    public void generate_lastKills() {
        String result = "";
        String dialog = Files.read("data/scripts/actions/player.Stats.lastkills.htm");
        String one = Files.read("data/scripts/actions/player.Stats.lastkills.one.htm");
        dialog = dialog.replaceAll("%rank_name%", getRankName());

        if (_lastPvPs.isEmpty()) {
            result = "<center>Player has no fights yet :(</center>";
        } else {
            for (int i = (_lastPvPs.size() - 1); i >= 0; i--) {
                PvPinfo fight = _lastPvPs.get(i);
                if (fight != null)
                    result += one.replaceAll("%time%", fight.getTime()).replaceAll("%oponent%", fight.getOponent()).replaceAll("%result%", fight.getResultHtml());
            }
        }

        if (_lastPvPs.size() < 15) {
            String brs = "";
            for (int c = 15 - _lastPvPs.size(); c != 0; c--) {
                brs += "<br><br>";
            }
            dialog = dialog.replaceAll("%tabs%", brs);
        }
        dialog = dialog.replaceAll("%tabs%", "");
        _shift_page_last_kills = dialog.replaceAll("%logs%", result).replaceAll("%char_name%", getName());
    }

    public int getRecomLeft() {
        return _recomLeft;
    }

    public boolean isNoble() {
        return _noble;
    }

    public int getKetra() {
        return _ketra;
    }

    public int getVarka() {
        return _varka;
    }

    public int getRam() {
        return _ram;
    }

    private void storeBlockList() {
        ThreadConnection con = null;
        FiltredStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.createStatement();
            statement.executeUpdate("DELETE FROM character_blocklist WHERE obj_Id=" + getObjectId());

            if (_blockList.isEmpty())
                return;

            final SqlBatch b = new SqlBatch("INSERT IGNORE INTO `character_blocklist` (`obj_Id`,`target_Id`,`target_Name`) VALUES");

            synchronized (_blockList) {
                StringBuilder sb;
                for (final Entry<Integer, String> e : _blockList.entrySet()) {
                    sb = new StringBuilder("(");
                    sb.append(getObjectId()).append(",");
                    sb.append(e.getKey()).append(",'");
                    sb.append(e.getValue()).append("')");
                    b.write(sb.toString());
                }
            }
            if (!b.isEmpty())
                statement.executeUpdate(b.close());
        } catch (final Exception e) {
            _log.warning("Can't store player blocklist " + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    @Override
    public void onAction(final L2Player player) {
        if (Events.onAction(player, this))
            return;

        // Check if the L2Player is confused
        if (player.isConfused() || player.isBlocked()) {
            player.sendActionFailed();
            return;
        }

        if (Config.DEBUG)
            _log.info("player.getTarget()=" + player.getTarget() + "; this=" + this);

        // Check if the other player already target this L2Player
        if (player.getTarget() != this) {
            // Set the target of the player
            player.setTarget(this);

            // The color to display in the select window is White
            player.sendPacket(new MyTargetSelected(getObjectId(), 0));
        } else // Check if this L2Player has a Private Store
            if (getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
                if (getDistance(player) > INTERACTION_DISTANCE && getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
                else
                    player.doInteract(this);
            else if (isAutoAttackable(player)) {
                // Player with lvl < 21 can't attack a cursed weapon holder
                // And a cursed weapon holder can't attack players with lvl < 21
                if (isCursedWeaponEquipped() && player.getLevel() < 21 || player.isCursedWeaponEquipped() && getLevel() < 21)
                    player.sendActionFailed();
                else
                    player.getAI().Attack(this, false);
            } else if (player != this)
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this, 100);
            else
                sendActionFailed();

        //if(!player.isAttackingNow())
        //broadcastPacketToOthers(new MoveToPawn(player, player.getTarget(), 100));
    }

    public void doInteract(final L2Object target) {
        if (target == null)
            return;
        if (target.isPlayer()) {
            if (target.getDistance(this) <= INTERACTION_DISTANCE) {
                final L2Player temp = (L2Player) target;

                if (temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_SELL_PACKAGE) {
                    sendPacket(new PrivateStoreListSell(this, temp));
                    sendActionFailed();
                } else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY) {
                    sendPacket(new PrivateStoreListBuy(this, temp));
                    sendActionFailed();
                } else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE) {
                    sendPacket(new RecipeShopSellList(this, temp));
                    sendActionFailed();
                }
                sendActionFailed();
            } else if (getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
                getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
        } else
            target.onAction(this);
    }

    @Override
    public void onForcedAttack(final L2Player player) {
        if (!player.getPlayerAccess().PeaceAttack && (player.isInZonePeace() || isInZonePeace()))
            if (player.getKarma() <= 0 && getKarma() <= 0) {
                player.sendPacket(Msg.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
                player.sendActionFailed();
                return;
            }
        if (player.isInOlympiadMode() && !player.isOlympiadCompStart()) {
            player.sendActionFailed();
            return;
        }
        super.onForcedAttack(player);
    }

    public boolean isOlympiadCompStart() {
        final OlympiadGame og = Olympiad.getOlympiadGame(_olympiadGameId);
        if (og == null) {
            _log.warning("OlympiadGame is null, olympiadGameId=" + _olympiadGameId);
            return true;
        }
        if (og.getStarted() == 2)
            return true;
        return false;
    }

    public void processQuestEvent(final String quest, String event) {
        if (event == null)
            event = "";
        QuestState qs = getQuestState(quest);
        if (qs == null) {
            final Quest q = QuestManager.getQuest(quest);
            if (q == null) {
                System.out.println("Quest " + quest + " not found!!!");
                return;
            }
            qs = q.newQuestState(this);
        }
        if (qs == null || qs.isCompleted())
            return;
        qs.getQuest().notifyEvent(event, qs);
        sendPacket(new QuestList(this));
    }

    public final void reName(final String name) {
        L2World.removeObject(this);
        decayMe();
        setName(name);
        spawnMe();
        broadcastUserInfo(true);
    }

    public int recipesCount() {
        return _commonrecipebook.size() + _recipebook.size();
    }

    /**
     * Забирает адену у игрока.<BR><BR>
     *
     * @param adena -
     *              сколько адены забрать
     * @return L2ItemInstance - остаток адены
     */
    public L2ItemInstance reduceAdena(final int adena) {
        if (adena > 0)
            sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(adena));
        return getInventory().reduceAdena(adena);
    }

    @Override
    public void reduceArrowCount() {
        //sendPacket(Msg.YOU_CAREFULLY_NOCK_AN_ARROW);
        final L2ItemInstance arrows = getInventory().destroyItem(getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, false);
        if (arrows == null || arrows.getCount() == 0) {
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            _arrowItem = null;
        }
    }

    @Override
    public void reduceCurrentHp(double i, final L2Character attacker, final L2Skill skill, final boolean awake, final boolean standUp, final boolean directHp, final boolean canReflect) {
        if (attacker == null || isInvul() || isDead() || attacker.isDead())
            return;

        if (this != attacker && isInOlympiadMode() && !isOlympiadCompStart()) {
            attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
            return;
        }

        if (attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle()) {
            attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
            return;
        }

        double trans = calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null);
        if (trans >= 1)
            if (_summon != null && !_summon.isDead() && _summon.getCurrentHp() > i / 2) {
                if (_summon.isInRange(this, 1200)) {
                    trans *= i / 100;
                    i -= trans;
                    _summon.reduceCurrentHp(trans, attacker, null, false, false, false, false);
                }
            } else
                getEffectList().stopEffect(L2Skill.SKILL_TRANSFER_PAIN);

        if (attacker != this)
            sendPacket(new SystemMessage(SystemMessage.C1_HAS_RECEIVED_DAMAGE_OF_S3_FROM_C2).addName(this).addName(attacker).addNumber((int) i));

        final double hp = directHp ? getCurrentHp() : getCurrentHp() + getCurrentCp();

        if (getDuel() != null)
            if (getDuel() != attacker.getDuel())
                getDuel().setDuelState(this, DuelState.Interrupted);
            else if (getDuel().getDuelState(this) == DuelState.Interrupted) {
                attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
                return;
            } else if (i >= hp) {
                setCurrentHp(1, false);
                getDuel().onPlayerDefeat(this);
                getDuel().stopFighting(attacker.getPlayer());
                return;
            }

        if (isInOlympiadMode()) {
            final OlympiadGame olymp_game = Olympiad.getOlympiadGame(getOlympiadGameId());
            if (olymp_game != null) {
                if (i >= hp) {
                    setCurrentHp(1, false);
                    olymp_game.setWinner(getOlympiadSide());
                    olymp_game.setStarted((byte) 0);
                    Olympiad.startValidateWinner(getOlympiadGameId(), 0);
                    attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
                    attacker.sendActionFailed();
                    return;
                } else if (this != attacker)
                    olymp_game.addDamage(getOlympiadSide(), i);
            } else {
                _log.warning("OlympiadGame id = " + getOlympiadGameId() + " is null");
                Thread.dumpStack();
            }
        }

        // Reduce the current HP of the L2Player
        super.reduceCurrentHp(i, attacker, skill, awake, standUp, directHp, canReflect);
    }

    public void refreshSavedStats() {
        _statsChangeRecorder.refreshSaves();
    }

    public void registerMacro(final L2Macro macro) {
        _macroses.registerMacro(macro);
    }

    public void removeAutoSoulShot(final Integer itemId) {
        _activeSoulShots.remove(itemId);
    }

    public synchronized void removeBuff(String sheme_name, int id) {
        if (_buffs.containsKey(sheme_name) && _buffs.get(sheme_name).contains(id))
            _buffs.get(sheme_name).remove((Integer) id);
    }

    public void removeFromBlockList(final String charName) {
        int charId = 0;
        for (final int blockId : _blockList.keySet())
            if (charName.equalsIgnoreCase(_blockList.get(blockId))) {
                charId = blockId;
                break;
            }
        if (charId == 0) {
            sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DELETE_THE_CHARACTER_FROM_IGNORE_LIST));
            return;
        }
        sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST).addString(_blockList.remove(charId)));
        final L2Player block_target = L2World.getPlayer(charId);
        if (block_target != null)
            block_target.sendMessage(getName() + " has removed you from his/her Ignore List."); // В системных(619 == 620) мессагах ошибка ;)
    }

    /**
     * Remove a Henna of the L2Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2Player.<BR><BR>
     */
    public boolean removeHenna(int slot) {
        if (slot < 1 || slot > 3)
            return false;

        slot--;

        if (_henna[slot] == null)
            return false;

        final L2HennaInstance henna = _henna[slot];
        final short dyeID = henna.getItemIdDye();

        // Added by Tempy - 10 Aug 05
        // Gives amount equal to half of the dyes needed for the henna back.
        final L2ItemInstance hennaDyes = ItemTable.getInstance().createItem(dyeID);
        hennaDyes.setCount(henna.getAmountDyeRequire() / 2);

        _henna[slot] = null;

        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("DELETE FROM character_hennas where char_obj_id=? and slot=? and class_index=?");
            statement.setInt(1, getObjectId());
            statement.setInt(2, slot + 1);
            statement.setInt(3, getActiveClassId());
            statement.execute();
        } catch (final Exception e) {
            _log.warning("could not remove char henna: " + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }

        // Calculate Henna modifiers of this L2Player
        recalcHennaStats();

        // Send Server->Client HennaInfo packet to this L2Player
        sendPacket(new HennaInfo(this));

        // Send Server->Client UserInfo packet to this L2Player
        sendUserInfo(false);

        // Add the recovered dyes to the player's inventory and notify them.
        getInventory().addItem(hennaDyes);
        final SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
        sm.addItemName(henna.getItemIdDye());
        sm.addNumber(henna.getAmountDyeRequire() / 2);
        sendPacket(sm);

        return true;
    }

    public void removeItemFromShortCut(final int objectId) {
        _shortCuts.deleteShortCutByObjectId(objectId);
    }

    public synchronized void removeScheme(String pending_sheme_name) {
        if (_buffs.containsKey(pending_sheme_name))
            _buffs.remove(pending_sheme_name);
    }

    @Override
    public void removeSkillTimeStamp(final Integer skillId) {
        synchronized (skillReuseTimeStamps) {
            skillReuseTimeStamps.remove(skillId);
        }
    }

    public void removeVisibleObject(final L2Object object) {
        if (isLogoutStarted() || object == null || object.getObjectId() == getObjectId())
            return;
        sendPacket(new DeleteObject(object));
        // if(object.isNpc)
        // removeFromHatelist((L2NpcInstance) object);
        getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
    }

    public void resetEventPoints() {
        _eventPoints = 0;
    }

    public void resetSkillsReuse() {
        getSkillReuseTimeStamps().clear();
        sendPacket(new SkillCoolTime(this));
    }

    public void restoreBonus() {
        _bonus = new Bonus(this);
    }

    public void restoreExp() {
        restoreExp(100.);
    }

    public void reviveAnswer(final int answer) {
        if (_ReviveRequested != 1 || !isDead() && !_RevivePet || _RevivePet && getPet() != null && !getPet().isDead())
            return;
        if (answer == 1)
            if (!_RevivePet) {
                if (_RevivePower != 0)
                    doRevive(_RevivePower);
                else
                    doRevive();
            } else if (getPet() != null)
                if (_RevivePower != 0)
                    ((L2PetInstance) getPet()).doRevive(_RevivePower);
                else
                    getPet().doRevive();
        _ReviveRequested = 0;
        _RevivePower = 0;
    }

    public void doRevive(final double percent) {
        restoreExp(percent);
        doRevive();
    }

    public void restoreExp(final double percent) {
        int lostexp = 0;

        final String lostexps = getVar("lostexp");
        if (lostexps != null) {
            lostexp = Integer.parseInt(lostexps);
            unsetVar("lostexp");
        }

        if (lostexp != 0)
            addExpAndSp((long) (lostexp * percent / 100), 0, false, false);
    }

    @Override
    public void doRevive() {
        super.doRevive();
        unsetVar("lostexp");
        updateEffectIcons();
        AutoShot();
        _ReviveRequested = 0;
        _RevivePower = 0;
        if (isInParty() && getParty().isInDimensionalRift())
            if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getLoc()))
                getParty().getDimensionalRift().memberRessurected(this);
    }

    public void AutoShot() {
        synchronized (_activeSoulShots) {
            for (final Integer e : _activeSoulShots) {
                if (e == null)
                    continue;
                final L2ItemInstance item = getInventory().getItemByItemId(e);
                if (item == null) {
                    _activeSoulShots.remove(e);
                    continue;
                }
                final IItemHandler handler = ItemHandler.getInstance().getItemHandler(e);
                if (handler == null)
                    continue;
                handler.useItem(this, item);
            }
        }
    }

    public void reviveRequest(final L2Player Reviver, final double percent, final boolean Pet) {
        if (_ReviveRequested == 1) {
            if (_RevivePet == Pet && _RevivePower >= percent) {
                Reviver.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED);
                return;
            }
            if (Pet && !_RevivePet) {
                Reviver.sendPacket(Msg.SINCE_THE_MASTER_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_THE_PET_HAS_BEEN_CANCELLED);
                return;
            }
            if (Pet && isDead()) {
                Reviver.sendPacket(Msg.WHILE_A_PET_IS_ATTEMPTING_TO_RESURRECT_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER);
                return;
            }
        }
        if (Pet && getPet() != null && getPet().isDead() || !Pet && isDead()) {
            _ReviveRequested = 1;
            _RevivePower = percent;
            _RevivePet = Pet;
            final ConfirmDlg pkt = new ConfirmDlg(SystemMessage.S1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_WITH_$S2_EXPERIENCE_POINTS_DO_YOU_WANT_TO_CONTINUE_WITH_THIS_RESURRECTION, 0, 2);
            pkt.addString(Reviver.getName()).addNumber((int) _RevivePower);
            sendPacket(pkt);
        }
    }

    public void scriptAnswer(final int answer) {
        if (answer == 1 && !_scriptName.equals(""))
            Scripts.callScripts(_scriptName.split(":")[0], _scriptName.split(":")[1],this, _scriptArgs);
        _scriptName = "";
    }

    public void scriptRequest(final String text, final String scriptName, final Object[] args) {
        if (_scriptName.equals("")) {
            _scriptName = scriptName;
            _scriptArgs = args;
            sendPacket(new ConfirmDlg(SystemMessage.S1, 30000, 3).addString(text));
        }
    }

    public void sendMaterialsList() {
        if (getVarB("NoMats"))
            sendPacket(new SystemMessage(2514).addString(msg));
        msg = "";
    }

    public void setAccountAccesslevel(final int level, final String comments, final int banTime) {
        LSConnection.getInstance().sendPacket(new ChangeAccessLevel(getAccountName(), level, comments, banTime));
    }

    public String getAccountName() {
        if (_connection == null)
            return "<not connected>";
        return _connection.getLoginName();
    }

    public void setAutoLoot(final boolean enable) {
        if (Config.AUTO_LOOT_INDIVIDUAL) {
            AutoLoot = enable;
            setVar("AutoLoot", String.valueOf(enable));
        }
    }

    public void setAutoLootHerbs(final boolean enable) {
        if (Config.AUTO_LOOT_INDIVIDUAL) {
            AutoLootHerbs = enable;
            setVar("AutoLootHerbs", String.valueOf(enable));
        }
    }

    public void setBlockAll(final boolean state) {
        _blockAll = state;
        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * @param boat
     */
    public void setBoat(final L2BoatInstance boat) {
        _Boat = boat;
    }

    public void setBuyList(final ConcurrentLinkedQueue<TradeItem> x) {
        _buyList = x;
        saveTradeList();
    }

    public void saveTradeList() {
        String val = "";

        if (_sellList == null || _sellList.isEmpty())
            unsetVar("selllist");
        else {
            for (final TradeItem i : _sellList)
                val += i.getObjectId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
            setVar("selllist", val);
            val = "";
            if (_tradeList != null && _tradeList.getSellStoreName() != null)
                setVar("sellstorename", _tradeList.getSellStoreName());
        }

        if (_buyList == null || _buyList.isEmpty())
            unsetVar("buylist");
        else {
            for (final TradeItem i : _buyList)
                val += i.getItemId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
            setVar("buylist", val);
            val = "";
            if (_tradeList != null && _tradeList.getBuyStoreName() != null)
                setVar("buystorename", _tradeList.getBuyStoreName());
        }

        if (_createList == null || _createList.getList().isEmpty())
            unsetVar("createlist");
        else {
            for (final L2ManufactureItem i : _createList.getList())
                val += i.getRecipeId() + ";" + i.getCost() + ":";
            setVar("createlist", val);
            if (_createList.getStoreName() != null)
                setVar("manufacturename", _createList.getStoreName());
        }
    }

    public void setBuyListId(final int listId) {
        _buyListId = listId;
    }

    public void setCharmOfCourage(final boolean val) {
        _charmOfCourage = val;
        sendPacket(new EtcStatusUpdate(this));
    }

    public void setClassId(final int id) {
        setClassId(id, false);
    }

    @Override
    public void setConsumedSouls(int i, final L2NpcInstance monster) {
        if (i == _consumedSouls)
            return;

        final int max = (int) calcStat(Stats.SOULS_LIMIT, 0, null, null);

        if (i > max)
            i = max;

        if (i <= 0) {
            _consumedSouls = 0;
            sendPacket(new EtcStatusUpdate(this));
            return;
        }

        if (_consumedSouls != i) {
            final int diff = i - _consumedSouls;
            if (diff > 0) {
                final SystemMessage sm = new SystemMessage(SystemMessage.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
                sm.addNumber(diff);
                sm.addNumber(i);
                sendPacket(sm);
                if (monster != null)
                    broadcastPacket(new SpawnEmitter(monster, this));
            }
        } else if (max == i) {
            sendPacket(Msg.SOUL_CANNOT_BE_ABSORBED_ANY_MORE);
            return;
        }

        _consumedSouls = i;
        sendPacket(new EtcStatusUpdate(this));
    }

    public void setCoupleId(final int coupleId) {
        _coupleId = coupleId;
    }

    public void setCreateList(final L2ManufactureList x) {
        _createList = x;
        saveTradeList();
    }

    public void setCursedWeaponEquippedId(final int value) {
        _cursedWeaponEquippedId = value;
    }

    public void setDeathPeanalty(final DeathPenalty dp) {
        getActiveClass().setDeathPenalty(dp);
    }

    public void setDeleteClanCurTime() {
        _deleteClanTime = System.currentTimeMillis();
    }

    public void setDuel(final Duel duel) {
        _duel = duel;
        broadcastPacketToOthers(new RelationChanged(this, true, 0));
    }

    public void setEnchantScroll(final L2ItemInstance scroll) {
        _enchantScroll = scroll;
    }

    public void setExchangeRefusal(final boolean mode) {
        _exchangeRefusal = mode;
    }

    public void setFish(final FishData fish) {
        _fish = fish;
    }

    public void setFishLoc(final Location loc) {
        _fishLoc = loc;
    }

    public void setFishing(final boolean fishing) {
        _fishing = fishing;
    }

    public void setGroundSkillLoc(final Location location) {
        _groundSkillLoc = location;
    }

    public void setInBoatPosition(final Location pt) {
        _inBoatPosition = pt;
    }

    public int setIncorrectValidateCount(final int count) {
        return _incorrectValidateCount;
    }

    public void setInvisible(final boolean vis) {
        _invisible = vis;
    }

    public void setIsInOlympiadMode(final boolean b) {
        _inOlympiadMode = b;
    }

    public void setIsWearingFormalWear(final boolean value) {
        _IsWearingFormalWear = value;
    }

    public void setLastAttackPacket() {
        _lastAttackPacket = System.currentTimeMillis();
    }

    public void setLastBbsOperaion(final String operaion) {
        _lastBBS_script_operation = operaion;
    }

    public void setLastEquipmPacket() {
        _lastEquipmPacket = System.currentTimeMillis();
    }

    public void setLastMovePacket() {
        _lastMovePacket = System.currentTimeMillis();
    }

    /**
     * Set the _lastFolkNpc of the L2Player corresponding to the last Folk witch one the player talked.<BR><BR>
     */
    public void setLastNpc(final L2NpcInstance npc) {
        _lastNpc = npc;
    }

    public void setLastPage(int page) {
        _lastpage = page;
    }

    public void setLeaveClanCurTime() {
        _leaveClanTime = System.currentTimeMillis();
    }

    public final boolean setLevel(final int lvl) {
        if (_activeClass != null)
            _activeClass.setLevel((byte) lvl);
        return lvl == getLevel();
    }

    public void setLoto(final int i, final int val) {
        _loto[i] = val;
    }

    public void setLure(final L2ItemInstance lure) {
        _lure = lure;
    }

    public void setMaried(final boolean state) {
        _maried = state;
    }

    public void setMaryAccepted(final boolean state) {
        _maryaccepted = state;
    }

    public void setMaryRequest(final boolean state) {
        _maryrequest = state;
    }

    public void setMessageRefusal(final boolean mode) {
        _messageRefusal = mode;
        sendPacket(new EtcStatusUpdate(this));
    }

    public void setMount(final int npcId, final int obj_id, final int level) {
        switch (npcId) {
            case 0: // Dismount
                setFlying(false);
                setRiding(false);
                removeSkillById(L2Skill.SKILL_STRIDER_ASSAULT);
                removeSkillById(L2Skill.SKILL_WYVERN_BREATH);
                removeSkillFromShortCut(L2Skill.SKILL_STRIDER_ASSAULT);
                removeSkillFromShortCut(L2Skill.SKILL_WYVERN_BREATH);
                getEffectList().stopEffect(L2Skill.SKILL_HINDER_STRIDER);
                break;
            case PetDataTable.STRIDER_WIND_ID:
            case PetDataTable.STRIDER_STAR_ID:
            case PetDataTable.STRIDER_TWILIGHT_ID:
                setRiding(true);
                if (isNoble())
                    addSkill(SkillTable.getInstance().getInfo(L2Skill.SKILL_STRIDER_ASSAULT, 1), false);
                break;
            case PetDataTable.WYVERN_ID:
                setFlying(true);
                addSkill(SkillTable.getInstance().getInfo(L2Skill.SKILL_WYVERN_BREATH, 1), false);
                break;
        }

        if (npcId > 0 && !isCursedWeaponEquipped()) {
            L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
            if (wpn != null)
                sendDisarmMessage(wpn);
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);

            wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
            if (wpn != null)
                sendDisarmMessage(wpn);
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_RHAND);

            refreshExpertisePenalty();
            abortAttack();
            abortCast();
        }

        _mountNpcId = npcId;
        _mountObjId = obj_id;
        _mountLevel = level;

        broadcastPacket(new Ride(this));
        broadcastUserInfo(true);

        sendPacket(new SkillList(this));
    }

    public void sendDisarmMessage(final L2ItemInstance wpn) {
        if (wpn.getEnchantLevel() > 0) {
            final SystemMessage sm = new SystemMessage(SystemMessage.EQUIPMENT_OF__S1_S2_HAS_BEEN_REMOVED);
            sm.addNumber(wpn.getEnchantLevel());
            sm.addItemName(wpn.getItemId());
            sendPacket(sm);
        } else {
            final SystemMessage sm = new SystemMessage(SystemMessage.S1__HAS_BEEN_DISARMED);
            sm.addItemName(wpn.getItemId());
            sendPacket(sm);
        }
    }

    public void setMultisell(final MultiSellListContainer multisell) {
        _multisell = multisell;
    }

    public void setNameColor(final int red, final int green, final int blue) {
        _nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
        if (_nameColor != Config.NORMAL_NAME_COLOUR && _nameColor != Config.CLANLEADER_NAME_COLOUR && _nameColor != Config.GM_NAME_COLOUR && _nameColor != Config.SERVICES_OFFLINE_TRADE_NAME_COLOR)
            setVar("namecolor", Integer.toHexString(_nameColor));
        else
            unsetVar("namecolor");
    }

    public void setNetConnection(final L2GameClient connection) {
        _connection = connection;
    }

    public void setOlympiadGameId(final int id) {
        _olympiadGameId = id;
    }

    public void setOlympiadSide(final int i) {
        _olympiadSide = i;
    }

    public void setPartnerId(final int partnerid) {
        _partnerId = partnerid;
    }

    public void setParty(final L2Party party) {
        _party = party;
    }

    public void setPartyMatchingLevels(final int levels) {
        _partyMatchingLevels = levels;
    }

    public void setPartyMatchingRegion(final int region) {
        _partyMatchingRegion = region;
    }

    public void setPet(final L2Summon summon) {
        _summon = summon;
        AutoShot();
        if (summon == null)
            getEffectList().stopEffect(4140);
    }

    public void setPledgeClass(final int classId) {
        _pledgeClass = classId;
    }

    public void setQuestState(final QuestState qs) {
        _quests.put(qs.getQuest().getName(), qs);
    }

    public void setRace(final int i, final int val) {
        _race[i] = val;
    }

    public void setSellList(final ConcurrentLinkedQueue<TradeItem> x) {
        _sellList = x;
        saveTradeList();
    }

    /**
     * Устанавливает состояние осады L2Player.<BR> 1 = attacker, 2 = defender, 0 = не учавствует
     */
    public void setSiegeState(final int siegeState) {
        _siegeState = siegeState;
    }

    public void setSkillLearningClassId(final ClassId classId) {
        _skillLearningClassId = classId;
    }

    public void setTeam(final int team, boolean checksForTeam) {
        _checksForTeam = checksForTeam;
        if (_team != team) {
            _team = team;
            broadcastUserInfo(true);

            if (getPet() != null)
                getPet().broadcastPetInfo();
        }
    }

    public void setTeleMode(final int mode) {
        _telemode = mode;
    }

    public void setTitleColor(final int color) {
        _titlecolor = color;
    }

    public void setTitleColor(final int red, final int green, final int blue) {
        _titlecolor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
    }

    public void setTradeRefusal(final boolean mode) {
        _tradeRefusal = mode;
    }

    public void setTrainedBeast(final L2TamedBeastInstance tamedBeast) {
        _tamedBeast = tamedBeast;
    }

    public void setTransactionRequester(final L2Player requestor, final long timeout) {
        _currentTransactionTimeout = timeout;
        _currentTransactionRequester = requestor;
    }

    public void setTransactionType(final TransactionType type) {
        _currentTransactionType = type;
    }

    public void setUnstuck(final int mode) {
        _unstuck = mode;
    }

    public void setUseSeed(final int id) {
        _useSeed = id;
    }

    /**
     * Устанавливает тип используемого склада.
     *
     * @param type тип склада:<BR> <ul> <li>WarehouseType.PRIVATE <li>WarehouseType.CLAN <li>WarehouseType.CASTLE <li>WarehouseType.FREIGHT </ul>
     */
    public void setUsingWarehouseType(final Warehouse.WarehouseType type) {
        _usingWHType = type;
    }

    @Override
    public void setXYZ(final int x, final int y, final int z) {
        super.setXYZ(x, y, z);
    }

    public void setlastPvpAttack(final long time) {
        _lastPvpAttack = time;
    }

    public void specialCamera(final L2Object target, final int dist, final int yaw, final int pitch, final int time, final int duration) {
        sendPacket(new SpecialCamera(target.getObjectId(), dist, yaw, pitch, time, duration));
    }

    public void startBonusTask(long time) {
        time *= 1000;
        time -= System.currentTimeMillis();
        if (_bonusExpiration != null)
            stopBonusTask();
        _bonusExpiration = ThreadPoolManager.getInstance().scheduleAi(new BonusTask(this), time, true);
    }

    public void startFishCombat(final boolean isNoob, final boolean isUpperGrade) {
        _fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
    }

    public void startLookingForFishTask() {
        if (!isDead() && _taskforfish == null) {
            int checkDelay = 0;
            boolean isNoob = false;
            boolean isUpperGrade = false;

            if (_lure != null) {
                final int lureid = _lure.getItemId();
                isNoob = _fish.getGroup() == 0;
                isUpperGrade = _fish.getGroup() == 2;
                if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) // low grade
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.33));
                else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || lureid >= 8505 && lureid <= 8513 || lureid >= 7610 && lureid <= 7613 || lureid >= 7807 && lureid <= 7809 || lureid >= 8484 && lureid <= 8486) // medium
                    // grade,
                    // beginner,
                    // prize-winning
                    // &
                    // quest
                    // special
                    // bait
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.00));
                else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) // high grade
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 0.66));
            }
            _taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
        }
    }

    public void storeHWID(final String HWID) {
        ThreadConnection con = null;
        FiltredPreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("UPDATE characters SET LastHWID=? WHERE obj_id=?");
            statement.setString(1, HWID);
            statement.setInt(2, getObjectId());
            statement.execute();
        } catch (final Exception e) {
            _log.warning("could not store characters HWID:" + e);
        } finally {
            DatabaseUtils.closeDatabaseCS(con, statement);
        }
    }

    /**
     * Обработчик ответа клиента на призыв персонажа.
     *
     * @param answer Идентификатор запроса
     */
    public void summonCharacterAnswer(final int answer) {
        final int summoningCrystallId = 8615;
        if (answer == 1 && _SummonCharacterCoords != null) {
            abortAttack();
            abortCast();
            stopMove();
            if (_SummonConsumeCrystall > 0) {
                final L2ItemInstance ConsumedItem = getInventory().getItemByItemId(summoningCrystallId);
                if (ConsumedItem != null && ConsumedItem.getCount() >= _SummonConsumeCrystall) {
                    getInventory().destroyItemByItemId(summoningCrystallId, _SummonConsumeCrystall, false);
                    sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(summoningCrystallId).addNumber(_SummonConsumeCrystall));
                    teleToLocation(_SummonCharacterCoords);
                } else
                    sendPacket(Msg.INCORRECT_ITEM_COUNT);
            } else
                teleToLocation(_SummonCharacterCoords);
        }
        _SummonCharacterCoords = null;
    }

    /**
     * Отправляет запрос клиенту на призыв персонажа.
     *
     * @param SummonerName Имя призывающего персонажа
     * @param loc          Координаты точки призыва персонажа
     */
    public void summonCharacterRequest(final String SummonerName, final Location loc, final int SummonConsumeCrystall) {
        if (_SummonCharacterCoords == null) {
            _SummonConsumeCrystall = SummonConsumeCrystall;
            _SummonCharacterCoords = loc;
            final ConfirmDlg cd = new ConfirmDlg(SystemMessage.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT, 60000, 1);
            cd.addString(SummonerName).addZoneName(_SummonCharacterCoords.x, _SummonCharacterCoords.y, _SummonCharacterCoords.z);
            sendPacket(cd);
        }
    }

    /**
     * Disable the Inventory and create a new task to enable it after 1.5s.
     */
    public void tempInvetoryDisable() {
        _inventoryDisable = true;

        ThreadPoolManager.getInstance().scheduleAi(new InventoryEnable(), 1500, true);
    }

    public boolean unChargeFishShot() {
        final L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon == null)
            return false;
        weapon.setChargedFishshot(false);
        AutoShot();
        return true;
    }

    @Override
    public boolean unChargeShots(final boolean spirit) {
        final L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon == null)
            return false;

        if (spirit)
            weapon.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
        else
            weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);

        AutoShot();
        return true;
    }

    /**
     * Remove a L2RecipList from the table _recipebook containing all L2RecipeList of the L2Player
     */
    public void unregisterRecipe(final int RecipeID) {
        if (_recipebook.containsKey(RecipeID)) {
            mysql.set("DELETE FROM `character_recipebook` WHERE `char_id`=" + getObjectId() + " AND `id`=" + RecipeID + " LIMIT 1");
            _recipebook.remove(RecipeID);
        } else if (_commonrecipebook.containsKey(RecipeID)) {
            mysql.set("DELETE FROM `character_recipebook` WHERE `char_id`=" + getObjectId() + " AND `id`=" + RecipeID + " LIMIT 1");
            _commonrecipebook.remove(RecipeID);
        } else
            _log.warning("Attempted to remove unknown RecipeList" + RecipeID);
    }

    /**
     * Send a Server->Client packet UserInfo to this L2Player and CharInfo to all L2Player in its _KnownPlayers.
     */
    @Override
    public void updateAbnormalEffect() {
        sendChanges();
    }

    public void updateLastReuseMsg(int skill) {
        _lastReuseMessage = System.currentTimeMillis();
        _lastReuseMessageSkill = skill;
    }

    public void updateWaitSitTime() {
        if (_waitTimeWhenSit < 200)
            _waitTimeWhenSit += 2;
    }

    public static enum TransactionType {
        NONE,
        PARTY,
        PARTY_ROOM,
        CLAN,
        ALLY,
        TRADE,
        FRIEND,
        CHANNEL
    }

    class EndSitDown implements Runnable {
        @Override
        public void run() {
            _sittingTask = false;
            getAI().clearNextAction();
        }
    }

    class EndStandUp implements Runnable {
        @Override
        public void run() {
            _sittingTask = false;
            _isSitting = false;
            if (!getAI().setNextIntention())
                getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
        }
    }

    class BroadcastCharInfoTask implements Runnable {
        @Override
        public void run() {
            broadcastCharInfo();
            _broadcastCharInfoTask = null;
        }
    }

    class UserInfoTask implements Runnable {
        @Override
        public void run() {
            sendPacket(new UserInfo(L2Player.this));
            _userInfoTask = null;
        }
    }

    public class UnJailTask implements Runnable {
        Location _loc;

        public UnJailTask(Location p) {
            _loc = p;
            _isInJail = true;
            _jailStartTime = System.currentTimeMillis();
        }

        public void run() {
            _isInJail = false;
            unsetVar("jailed");
            unsetVar("jailedFrom");
            L2Player.this.teleToLocation(_loc);
            L2Player.this.setReflection(0);
        }
    }

    public class TeleportTask implements Runnable {
        Location _loc;
        int _reflection;

        public TeleportTask(final Location p, final int reflection) {
            _loc = p;
            _reflection = reflection;
        }

        @Override
        public void run() {
            L2Player.this.teleToLocation(_loc);
        }
    }

    class InventoryEnable implements Runnable {
        @Override
        public void run() {
            _inventoryDisable = false;
        }
    }

    class WaterTask implements Runnable {
        @Override
        public void run() {
            if (isDead() || !isInZoneIncludeZ(L2Zone.ZoneType.water)) {
                _taskWater.cancel(false);
                _taskWater = null;
                return;
            }

            double reduceHp = getMaxHp() / 100;

            if (reduceHp < 1)
                reduceHp = 1;

            reduceCurrentHp(reduceHp, L2Player.this, null, false, false, true, false);
            // reduced hp, because not rest
            final SystemMessage sm = new SystemMessage(SystemMessage.YOU_RECEIVED_S1_DAMAGE_BECAUSE_YOU_WERE_UNABLE_TO_BREATHE);
            sm.addNumber((int) reduceHp);
            sendPacket(sm);
        }
    }

    static class KickTask implements Runnable {
        private final WeakReference<L2Player> player_ref;

        public KickTask(final L2Player player) {
            player_ref = new WeakReference<L2Player>(player);
        }

        public void run() {
            final L2Player player = player_ref.get();
            if (player != null) {
                player.setOfflineMode(false);
                player.logout(false, false, true);
            }
        }
    }

    static class BonusTask implements Runnable {
        private final WeakReference<L2Player> player_ref;

        public BonusTask(final L2Player player) {
            player_ref = new WeakReference<L2Player>(player);
        }

        public void run() {
            final L2Player player = player_ref.get();
            if (player != null) {
                player.getNetConnection().setBonus(1);
                player.restoreBonus();
                if (player.getParty() != null)
                    player.getParty().recalculatePartyData();
                final String msg = new CustomMessage("scripts.services.RateBonus.LuckEnded", player).toString();
                player.sendMessage(msg);
            }
        }
    }

    class LookingForFishTask implements Runnable {
        boolean _isNoob, _isUpperGrade;
        int _fishType, _fishGutsCheck, _gutsCheckTime;
        long _endTaskTime;

        protected LookingForFishTask(final int fishWaitTime, final int fishGutsCheck, final int fishType, final boolean isNoob, final boolean isUpperGrade) {
            _fishGutsCheck = fishGutsCheck;
            _endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
            _fishType = fishType;
            _isNoob = isNoob;
            _isUpperGrade = isUpperGrade;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() >= _endTaskTime) {
                endFishing(false);
                return;
            }
            if (_fishType == -1)
                return;
            final int check = Rnd.get(1000);
            if (_fishGutsCheck > check) {
                stopLookingForFishTask();
                startFishCombat(_isNoob, _isUpperGrade);
            }
        }
    }

    class PvPFlag implements Runnable {
        @Override
        public void run() {
            try {
                // _log.fine("Checking pvp time: " + getlastPvpAttack());
                // "lastattack: " _lastAttackTime "currenttime: "
                // System.currentTimeMillis());
                if (Math.abs(System.currentTimeMillis() - getlastPvpAttack()) > Config.PVP_TIME)
                    // _log.fine("Stopping PvP");
                    stopPvPFlag();
                else if (Math.abs(System.currentTimeMillis() - getlastPvpAttack()) > Config.PVP_TIME - 5000)
                    updatePvPFlag(2);
                else
                    updatePvPFlag(1);
                // Start a new PvP timer check
                // checkPvPFlag();
            } catch (final Exception e) {
                _log.log(Level.WARNING, "error in pvp flag task:", e);
            }
        }
    }
}