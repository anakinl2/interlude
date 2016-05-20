package com.lineage.game.model.entity.olympiad;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.Announcements;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.instancemanager.ServerVariables;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.entity.Hero;
import com.lineage.game.model.instances.L2OlympiadManagerInstance;
import com.lineage.game.serverpackets.CharInfo;
import com.lineage.game.serverpackets.ExOlympiadUserInfoSpectator;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;

public class Olympiad
{
	public static final Logger _log = Logger.getLogger(Olympiad.class.getName());

	public static Map<Integer, OlympiadGameTask> _gamesQueue = new FastMap<Integer, OlympiadGameTask>();
	public static Map<Integer, ScheduledFuture> _gamesQueueScheduled = new FastMap<Integer, ScheduledFuture>();

	public static Map<Integer, StatsSet> _nobles;
	public static FastList<StatsSet> _heroesToBe;
	public static FastList<Integer> _nonClassBasedRegisters = new FastList<Integer>();
	public static Map<Integer, FastList<Integer>> _classBasedRegisters = new FastMap<Integer, FastList<Integer>>();

	public static final int DEFAULT_POINTS = 18;
	private static final int WEEKLY_POINTS = 3;
	public static final int NOBLESSE_GATE_PASS_ID = 6651;

	private static final String OLYMPIAD_DATA_FILE = "config/olympiad.properties";
	public static final String OLYMPIAD_HTML_FILE = "data/html/olympiad/";
	public static final String OLYMPIAD_LOAD_NOBLES = "SELECT * FROM `olympiad_nobles`";
	public static final String OLYMPIAD_SAVE_NOBLES = "REPLACE INTO `olympiad_nobles` (`char_id`, `class_id`, `char_name`, `olympiad_points`, `olympiad_points_past`, `competitions_done`, `competitions_win`, `competitions_loose`) VALUES (?,?,?,?,?,?,?,?)";
	public static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE `olympiad_nobles` SET `olympiad_points` = ?, `olympiad_points_past` = ?, `competitions_done` = ?, `competitions_win` = ?, `competitions_loose` = ? WHERE `char_id` = ?";
	public static final String OLYMPIAD_GET_HEROS = "SELECT `char_id`, `char_name` FROM `olympiad_nobles` WHERE `class_id` = ? AND `competitions_done` >= 9 AND `competitions_win` > 0 ORDER BY `olympiad_points` DESC, `competitions_win` DESC, `competitions_done` DESC";
	public static final String GET_EACH_CLASS_LEADER = "SELECT `char_name` FROM `olympiad_nobles` WHERE `class_id` = ? AND `competitions_done` > 0 ORDER BY `olympiad_points` DESC, `competitions_done` DESC";
	public static final String OLYMPIAD_CLEANUP_NOBLES = "UPDATE `olympiad_nobles` SET `olympiad_points_past` = `olympiad_points`, `olympiad_points` = " + DEFAULT_POINTS + ", `competitions_done` = 0, `competitions_win` = 0, `competitions_loose` = 0";

	private static final int COMP_START = Config.ALT_OLY_START_TIME; // 6PM - 10AM
	private static final int COMP_MIN = Config.ALT_OLY_MIN; // 00 mins
	private static final long COMP_PERIOD = Config.ALT_OLY_CPERIOD; // 6hours
	private static final long BATTLE_PERIOD = Config.ALT_OLY_BATTLE; // 6mins
	private static final long BATTLE_WAIT = Config.ALT_OLY_BWAIT; // 10mins
	private static final long INITIAL_WAIT = Config.ALT_OLY_IWAIT; // 5mins
	public static final long WEEKLY_PERIOD = Config.ALT_OLY_WPERIOD; // 1 week
	public static final long VALIDATION_PERIOD = Config.ALT_OLY_VPERIOD; // 12 hours

	/*	  private static final int COMP_START = 13; // 1PM - 2PM
		  private static final int COMP_MIN = 15; // 20mins
		  private static final long COMP_PERIOD = 7200000; // 2hours
		  private static final long BATTLE_PERIOD = 180000; // 3mins
		  private static final long BATTLE_WAIT = 600000; // 10mins
		  private static final long INITIAL_WAIT = 300000; // 5mins
		  static final long WEEKLY_PERIOD = 7200000; // 2 hours
		  static final long VALIDATION_PERIOD = 3600000; // 1 hour*/

	public static final String CHAR_ID = "char_id";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String POINTS_PAST = "olympiad_points_past";
	public static final String COMP_DONE = "competitions_done";
	public static final String COMP_WIN = "competitions_win";
	public static final String COMP_LOOSE = "competitions_loose";

	public static long _olympiadEnd;
	public static long _validationEnd;
	public static int _period;
	public static long _nextWeeklyChange;
	public static int _currentCycle;
	private static long _compEnd;
	private static Calendar _compStart;
	public static boolean _inCompPeriod;
	public static boolean _isOlympiadEnd;
	public static boolean _battleStarted;
	public static boolean _cycleTerminated;

	private static ScheduledFuture _scheduledOlympiadEnd;
	public static ScheduledFuture _scheduledManagerTask;
	public static ScheduledFuture _scheduledWeeklyTask;
	public static ScheduledFuture _scheduledValdationTask;

	public static final Stadia[] STADIUMS = new Stadia[22];

	public static OlympiadManager _manager;
	private static ArrayList<L2OlympiadManagerInstance> _npcs = new ArrayList<L2OlympiadManagerInstance>();

	public static void load()
	{
		_nobles = new FastMap<Integer, StatsSet>();
		_currentCycle = ServerVariables.getInt("Olympiad_CurrentCycle", -1);
		_period = ServerVariables.getInt("Olympiad_Period", -1);
		_olympiadEnd = ServerVariables.getLong("Olympiad_End", -1);
		_validationEnd = ServerVariables.getLong("Olympiad_ValdationEnd", -1);
		_nextWeeklyChange = ServerVariables.getLong("Olympiad_NextWeeklyChange", -1);

		Properties OlympiadProperties = new Properties();
		InputStream is;
		try
		{
			is =Config.class.getClassLoader().getResourceAsStream(OLYMPIAD_DATA_FILE);
			OlympiadProperties.load(is);
			is.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(_currentCycle == -1)
			_currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
		if(_period == -1)
			_period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
		if(_olympiadEnd == -1)
			_olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
		if(_validationEnd == -1)
			_validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValdationEnd", "0"));
		if(_nextWeeklyChange == -1)
			_nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));

		initStadiums();

		switch(_period)
		{
			case 0:
				if(_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
					OlympiadDatabase.setNewOlympiadEnd();
				else
					_isOlympiadEnd = false;
				break;
			case 1:
				if(_validationEnd > Calendar.getInstance().getTimeInMillis())
				{
					_isOlympiadEnd = true;
					_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationTask(), getMillisToValidationEnd());
				}
				else
				{
					OlympiadDatabase.sortHerosToBe();
					giveHeroBonus();
					OlympiadDatabase.saveNobleData(); // Сохраняем героев-ноблесов, получивших бонус в виде очков
					if(Hero.getInstance().computeNewHeroes(_heroesToBe))
						_log.warning("Olympiad: Error while computing new heroes!");
					_currentCycle++;
					_period = 0;
					OlympiadDatabase.cleanupNobles();
					OlympiadDatabase.setNewOlympiadEnd();
					init();
				}
				break;
			default:
				_log.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				return;
		}

		OlympiadDatabase.loadNobles();

		_log.info("[Olympiad System]: Loading Olympiad System....");
		if(_period == 0)
			_log.info("[Olympiad System]: Currently in Olympiad Period");
		else
			_log.info("[Olympiad System]: Currently in Validation Period");

		_log.info("[Olympiad System]: Period Ends....");

		long milliToEnd;
		if(_period == 0)
			milliToEnd = getMillisToOlympiadEnd();
		else
			milliToEnd = getMillisToValidationEnd();

		double numSecs = milliToEnd / 1000 % 60;
		double countDown = (milliToEnd / 1000 - numSecs) / 60;
		int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		int numHours = (int) Math.floor(countDown % 24);
		int numDays = (int) Math.floor((countDown - numHours) / 24);

		_log.info("[Olympiad System]: In " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

		if(_period == 0)
		{
			_log.info("[Olympiad System]: Next Weekly Change is in....");

			milliToEnd = getMillisToWeekChange();

			double numSecs2 = milliToEnd / 1000 % 60;
			double countDown2 = (milliToEnd / 1000 - numSecs2) / 60;
			int numMins2 = (int) Math.floor(countDown2 % 60);
			countDown2 = (countDown2 - numMins2) / 60;
			int numHours2 = (int) Math.floor(countDown2 % 24);
			int numDays2 = (int) Math.floor((countDown2 - numHours2) / 24);

			_log.info("[Olympiad System]: In " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
		}

		_log.info("[Olympiad System]: Loaded " + _nobles.size() + " Noblesses");

		if(_period == 0)
			init();
	}

	private static void initStadiums()
	{
		for(int id = 1; id < 23; id++)
		{
			Stadia s = STADIUMS[id - 1];
			if(s == null)
				s = new Stadia();
			STADIUMS[id - 1] = s;
		}
	}

	public static void init()
	{
		if(_period == 1)
			return;

		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

		if(_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), getMillisToOlympiadEnd());

		updateCompStatus();

		if(_scheduledWeeklyTask != null)
			_scheduledWeeklyTask.cancel(true);
		_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WeeklyTask(), getMillisToWeekChange(), WEEKLY_PERIOD);
	}

	public static synchronized boolean registerNoble(L2Player noble, boolean classBased)
	{
		SystemMessage sm;
		if(!_inCompPeriod || _isOlympiadEnd)
		{
			sm = new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}

		if(getMillisToOlympiadEnd() <= 600 * 1000)
		{
			sm = new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}

		if(getMillisToCompEnd() <= 600 * 1000)
		{
			sm = new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}

		if(noble.isCursedWeaponEquipped())
		{
			noble.sendMessage(new CustomMessage("com.lineage.game.model.entity.Olympiad.Cursed", noble));
			return false;
		}

		if(!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessage.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			noble.sendPacket(sm);
			return false;
		}

		if(noble.getBaseClassId() != noble.getClassId().getId())
		{
			sm = new SystemMessage(SystemMessage.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
			noble.sendPacket(sm);
			return false;
		}

		if(noble.getOlympiadGameId() > -1)
		{
			sm = new SystemMessage(SystemMessage.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME);
			noble.sendPacket(sm);
			return false;
		}

		if(!_nobles.containsKey(noble.getObjectId()))
		{
			StatsSet statDat = new StatsSet();
			statDat.set(CLASS_ID, noble.getClassId().getId());
			statDat.set(CHAR_NAME, noble.getName());
			statDat.set(POINTS, DEFAULT_POINTS);
			statDat.set(POINTS_PAST, 0);
			statDat.set(COMP_DONE, 0);
			statDat.set(COMP_WIN, 0);
			statDat.set(COMP_LOOSE, 0);
			statDat.set("to_save", true);
			_nobles.put(noble.getObjectId(), statDat);
		}

		if(getNoblePoints(noble.getObjectId()) < 3)
		{
			noble.sendMessage(new CustomMessage("com.lineage.game.model.entity.Olympiad.LessPoints", noble));
			return false;
		}

		if(classBased)
		{
			FastList<Integer> classed = _classBasedRegisters.get(noble.getClassId().getId());
			if(classed == null)
			{
				classed = new FastList<Integer>();
				_classBasedRegisters.put(noble.getClassId().getId(), classed);
			}
			else if(classed.contains(noble.getObjectId()))
			{
				noble.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS));
				return false;
			}

			classed.add(noble.getObjectId());
			noble.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES));
		}
		else
		{
			if(_nonClassBasedRegisters.contains(noble.getObjectId()))
			{
				noble.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME));
				return false;
			}

			_nonClassBasedRegisters.add(noble.getObjectId());
			noble.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES));
		}

		return true;
	}

	public static synchronized void logoutPlayer(L2Player player)
	{
		List<Integer> classed = _classBasedRegisters.get(player.getClassId().getId());
		if(classed != null)
			classed.remove(new Integer(player.getObjectId()));
		_nonClassBasedRegisters.remove(new Integer(player.getObjectId()));

		int gameId = player.getOlympiadGameId();
		if(gameId == -1 || getOlympiadGame(gameId) == null)
			return;
		try
		{
			if(player.getOlympiadSide() == 1)
				getOlympiadGame(gameId)._playerOne = null;
			else
				getOlympiadGame(gameId)._playerTwo = null;
			if(!getOlympiadGame(gameId).validated)
				startValidateWinner(gameId, 2);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static synchronized boolean unRegisterNoble(L2Player noble, boolean disconnect)
	{
		SystemMessage sm;
		if(!disconnect)
		{
			if(!_inCompPeriod || _isOlympiadEnd)
			{
				sm = new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
				noble.sendPacket(sm);
				return false;
			}

			if(!noble.isNoble())
			{
				sm = new SystemMessage(SystemMessage.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
				noble.sendPacket(sm);
				return false;
			}

			if(!isRegistered(noble))
			{
				sm = new SystemMessage(SystemMessage.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
				noble.sendPacket(sm);
				return false;
			}
		}

		try
		{
			if(noble.getOlympiadGameId() > -1)
			{
				startValidateWinner(noble.getOlympiadGameId(), noble.getObjectId());
				return true;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		_nonClassBasedRegisters.remove(new Integer(noble.getObjectId()));
		List<Integer> classed = _classBasedRegisters.get(noble.getClassId().getId());
		if(classed == null)
			return true;
		classed.remove(new Integer(noble.getObjectId()));

		if(!disconnect)
			noble.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME));

		return true;
	}

	private static synchronized void updateCompStatus()
	{
		long milliToStart = getMillisToCompBegin();
		double numSecs = milliToStart / 1000 % 60;
		double countDown = (milliToStart / 1000 - numSecs) / 60;
		int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		int numHours = (int) Math.floor(countDown % 24);
		int numDays = (int) Math.floor((countDown - numHours) / 24);

		_log.info("[Olympiad System]: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		_log.info("[Olympiad System]: Event starts/started : " + _compStart.getTime());

		ThreadPoolManager.getInstance().scheduleGeneral(new CompStartTask(), getMillisToCompBegin());
	}

	private static long getMillisToOlympiadEnd()
	{
		return _olympiadEnd - System.currentTimeMillis();
	}

	static long getMillisToValidationEnd()
	{
		if(_validationEnd > System.currentTimeMillis())
			return _validationEnd - System.currentTimeMillis();
		return 10L;
	}

	public static boolean isOlympiadEnd()
	{
		return _isOlympiadEnd;
	}

	public static boolean inCompPeriod()
	{
		return _inCompPeriod;
	}

	private static long getMillisToCompBegin()
	{
		if(_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis())
			return 10L;
		if(_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
			return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
		return setNewCompBegin();
	}

	private static long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

		_log.info("Olympiad System: New Schedule @ " + _compStart.getTime());

		return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}

	public static long getMillisToCompEnd()
	{
		return _compEnd - Calendar.getInstance().getTimeInMillis();
	}

	private static long getMillisToWeekChange()
	{
		if(_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
			return _nextWeeklyChange - Calendar.getInstance().getTimeInMillis();
		return 10L;
	}

	public static synchronized void addWeeklyPoints()
	{
		if(_period == 1)
			return;
		for(Integer nobleId : _nobles.keySet())
		{
			StatsSet nobleInfo = _nobles.get(nobleId);
			if(nobleInfo != null)
				nobleInfo.set(POINTS, nobleInfo.getInteger(POINTS) + WEEKLY_POINTS);
		}
	}

	public static String[] getAllTitles()
	{
		String[] msg = new String[STADIUMS.length];
		for(int i = 0; i < STADIUMS.length; i++)
			if(_manager != null && _manager.getOlympiadInstance(i) != null && _manager.getOlympiadInstance(i).getStarted() > 0)
				msg[i] = i + 1 + "_In Progress_" + _manager.getOlympiadInstance(i).getTitle();
			else
				msg[i] = i + 1 + "_Initial State";
		return msg;
	}

	public static int getCurrentCycle()
	{
		return _currentCycle;
	}

	public static synchronized void addSpectator(int id, L2Player spectator)
	{
		if(spectator.getOlympiadGameId() != -1 || Olympiad.isRegisteredInComp(spectator))
		{
			spectator.sendPacket(new SystemMessage(SystemMessage.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME));
			return;
		}

		if(_manager == null || _manager.getOlympiadInstance(id) == null)
		{
			spectator.sendPacket(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS));
			return;
		}

		int[] coords = new int[3];
		int[] c = ZoneManager.getInstance().getZoneById(L2Zone.ZoneType.OlympiadStadia, 3001 + id, false).getSpawns().get(0);
		coords[0] = c[0];
		coords[1] = c[1];
		coords[2] = c[2];
		spectator.enterOlympiadObserverMode(coords, id);

		_manager.getOlympiadInstance(id).addSpectator(spectator);

		L2Player[] players = _manager.getOlympiadInstance(id).getPlayers();

		if(players.length == 0)
			return;

		spectator.sendPacket(new CharInfo(players[0], spectator, true));
		spectator.sendPacket(new CharInfo(players[1], spectator, true));
		spectator.sendPacket(new ExOlympiadUserInfoSpectator(players[0], 1));
		spectator.sendPacket(new ExOlympiadUserInfoSpectator(players[1], 2));
	}

	public static synchronized void removeSpectator(int id, L2Player spectator)
	{
		if(_manager == null || _manager.getOlympiadInstance(id) == null)
			return;

		_manager.getOlympiadInstance(id).removeSpectator(spectator);
	}

	public static List<L2Player> getSpectators(int id)
	{
		if(_manager == null || _manager.getOlympiadInstance(id) == null)
			return null;
		return _manager.getOlympiadInstance(id).getSpectators();
	}

	public static OlympiadGame getOlympiadGame(int gameId)
	{
		return _manager.getOlympiadGames().get(gameId);
	}

	public static synchronized void startValidateWinner(int id, int count)
	{
		OlympiadGameTask task;
		ScheduledFuture sf;
		if(_gamesQueueScheduled.get(id) != null)
			_gamesQueueScheduled.get(id).cancel(true);
		task = new OlympiadGameTask(_manager.getOlympiadGames().get(id), BattleStatus.ValidateWinner, count);
		sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 200);
		_gamesQueue.put(id, task);
		_gamesQueueScheduled.put(id, sf);
	}

	public static synchronized int[] getWaitingList()
	{
		if(!inCompPeriod())
			return null;

		int classCount = 0;
		if(_classBasedRegisters.size() != 0)
			for(List<Integer> classed : _classBasedRegisters.values())
				classCount += classed.size();

		int[] array = new int[2];
		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();

		return array;
	}

	/**
	 * Выдает всем героям бонус в размере 300 очков
	 */
	public static synchronized void giveHeroBonus()
	{
		if(_heroesToBe == null || _heroesToBe.size() == 0)
			return;

		for(StatsSet hero : _heroesToBe)
		{
			StatsSet noble = _nobles.get(hero.getInteger(CHAR_ID));
			if(noble != null)
				noble.set(POINTS, noble.getInteger(POINTS) + 300);
		}
	}

	public static synchronized int getNoblessePasses(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		int points = noble.getInteger(POINTS_PAST);
		if(points <= 50)
			return 0;
		noble.set(POINTS_PAST, 0);
		return points * 1000;
	}

	public static synchronized boolean isRegistered(L2Player noble)
	{
		if(_nonClassBasedRegisters.contains(noble.getObjectId()))
			return true;
		List<Integer> classed = _classBasedRegisters.get(noble.getClassId().getId());
		if(classed == null)
			return false;
		if(!classed.contains(noble.getObjectId()))
			return false;
		return true;
	}

	public static synchronized boolean isRegisteredInComp(L2Player player)
	{
		if(isRegistered(player))
			return true;
		if(_manager == null || _manager.getOlympiadGames() == null)
			return false;
		for(OlympiadGame g : _manager.getOlympiadGames().values())
			if(g != null)
				for(L2Player pl : g.getPlayers())
					if(pl.getObjectId() == player.getObjectId())
						return true;
		return false;
	}

	/**
	 * Возвращает олимпийские очки за текущий период
	 * 
	 * @param objId
	 * @return
	 */
	public static synchronized int getNoblePoints(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger(POINTS);
	}

	/**
	 * Возвращает олимпийские очки за прошлый период
	 * 
	 * @param objId
	 * @return
	 */
	public static synchronized int getNoblePointsPast(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger(POINTS_PAST);
	}

	public static synchronized int getCompetitionDone(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger(COMP_DONE);
	}

	public static synchronized int getCompetitionWin(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger(COMP_WIN);
	}

	public static synchronized int getCompetitionLoose(int objId)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger(COMP_LOOSE);
	}

	public static Stadia[] getStadiums()
	{
		return STADIUMS;
	}

	public static ArrayList<L2OlympiadManagerInstance> getNpcs()
	{
		return _npcs;
	}

	public static void addOlympiadNpc(L2OlympiadManagerInstance npc)
	{
		_npcs.add(npc);
	}

	public static Map<Integer, OlympiadGameTask> getGamesQueue()
	{
		return _gamesQueue;
	}

	public static Map<Integer, ScheduledFuture> getGamesQueueScheduled()
	{
		return _gamesQueueScheduled;
	}

	public static boolean manualStartOlympiad()
	{
		if(_inCompPeriod)
			return false;
		Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_STARTED));
		_log.info("[Olympiad System]: Olympiad Game Started");
		_inCompPeriod = true;
		OlympiadManager om = new OlympiadManager();
		Thread olyCycle = new Thread(om);
		olyCycle.start();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
			@Override
			public void run()
			{
				_scheduledManagerTask.cancel(true);
				_inCompPeriod = false;
				Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_ENDED));
				_log.info("[Olympiad System]: Olympiad Game Ended");
			}
		}, COMP_PERIOD);
		return true;
	}

	public static void manualSelectHeroes()
	{
		SystemMessage sm = new SystemMessage(SystemMessage.OLYMPIAD_PERIOD_S1_HAS_ENDED);
		sm.addNumber(_currentCycle);

		Announcements.getInstance().announceToAll(sm);
		Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

		_isOlympiadEnd = true;
		if(_scheduledManagerTask != null)
			_scheduledManagerTask.cancel(true);
		if(_scheduledWeeklyTask != null)
			_scheduledWeeklyTask.cancel(true);
		if(_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);

		OlympiadDatabase.saveNobleData();

		_period = 1;

		Hero.getInstance().clearHeroes();

		try
		{
			OlympiadDatabase.save();
		}
		catch(Exception e)
		{
			_log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
		}

		OlympiadDatabase.sortHerosToBe();
		giveHeroBonus();
		OlympiadDatabase.saveNobleData(); // Сохраняем героев-ноблесов, получивших бонус в виде очков
		_log.info("[Olympiad]: Sorted " + _heroesToBe.size() + " new heroes.");
		if(Hero.getInstance().computeNewHeroes(_heroesToBe))
			_log.warning("[Olympiad]: Error while computing new heroes!");
		Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
		_period = 0;
		_currentCycle++;
		OlympiadDatabase.cleanupNobles();
		OlympiadDatabase.setNewOlympiadEnd();
		init();
	}

	public static void manualSetNoblePoints(int objId, int points)
	{
		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return;
		noble.set(POINTS, points);
		noble.set("to_save", true);
	}
}