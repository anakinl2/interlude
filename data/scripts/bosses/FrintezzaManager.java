package bosses;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.ext.listeners.L2ZoneEnterLeaveListener;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlIntention;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2BossInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.funcs.Func;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

/**
 * Менеджер Фринтезы.
 * 
 * @author Darki699, Felixx
 */
public class FrintezzaManager extends Functions implements ScriptFile
{
	private static final Logger _log = Logger.getLogger(FrintezzaManager.class.getName());

	// Must be done this way :( Each of the 11 mobs is an individual with different tasks and values
	// So there is no point to place them in an array
	private static L2Spawn frintezzaSpawn;
	private static L2Spawn scarletSpawnWeak;
	private static L2Spawn scarletSpawnStrong;
	private static L2Spawn portraitSpawn1;
	private static L2Spawn portraitSpawn2;
	private static L2Spawn portraitSpawn3;
	private static L2Spawn portraitSpawn4;
	private static L2Spawn demonSpawn1;
	private static L2Spawn demonSpawn2;
	private static L2Spawn demonSpawn3;
	private static L2Spawn demonSpawn4;

	private static L2BossInstance weakScarlet;
	private static L2BossInstance strongScarlet;
	private static L2MonsterInstance portrait1;
	private static L2MonsterInstance portrait2;
	private static L2MonsterInstance portrait3;
	private static L2MonsterInstance portrait4;
	private static L2BossInstance frintezza;

	// The minions be used as L2MonsterInstance, instead of L2MinionInstance, since they
	// have 3 Bosses: weak scarlet, strong scarlet, and frintezza. All 3 bosses control
	// the minions. Also we do not want the portraits to respawn next to the boss,
	// and we need different respawn intervals for
	// demons and portraits.
	private static L2NpcInstance demon1, demon2, demon3, demon4;

	// Interval time of Monsters.
	private static int _intervalOfBoss;
	private static int _intervalOfDemons;
	private static int _intervalOfRetarget;
	private static int _intervalOfFrintezzaSongs;
	private static int _callForHelpInterval;

	// Delay of appearance time of Boss.
	private static int _appTimeOfBoss;

	// Activity time of Boss.
	private static int _activityTimeOfBoss;

	// lists of last saved positions <objectId, location>
	private static Map<Integer, Location> _lastLocation = new FastMap<Integer, Location>();

	// status in lair.
	private static boolean _respawningDemon1 = false;
	private static boolean _respawningDemon2 = false;
	private static boolean _respawningDemon3 = false;
	private static boolean _respawningDemon4 = false;
	private static boolean _scarletIsWeakest = true;

	private static ScheduledFuture<?> _monsterSpawnTask = null;
	private static ScheduledFuture<?> _activityTimeEndTask = null;
	private static ScheduledFuture<?> _intervalEndTask = null;

	@SuppressWarnings("unused")
	private static Func _DecreaseRegHp = null;
	private static int _debuffPeriod = 0;

	private static EpicBossState _state;
	private static L2Zone _zone;
	private static ZoneListener _zoneListener = new ZoneListener();

	/** ************************************ Initial Functions ************************************* */

	public static boolean isEnableEnterToLair()
	{
		return _state.getState() == EpicBossState.State.NOTSPAWN;
	}

	/**
	 * initialize <b>this</b> Frintezza Manager
	 */
	public static void init()
	{
		_state = new EpicBossState(29045);
		_zone = ZoneManager.getInstance().getZoneById(ZoneType.epic, 702102, false);
		_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);

		_callForHelpInterval = 2000;
		_intervalOfRetarget = 10000;
		_intervalOfFrintezzaSongs = 30000;

		_intervalOfDemons = 2 * 60000;
		_intervalOfBoss = 24 * 60 * 60000;
		_appTimeOfBoss = 5 * 60000;
		_activityTimeOfBoss = 120 * 60000;

		// initialize status in lair.
		_scarletIsWeakest = true;

		// setting spawn data of monsters.
		try
		{
			createMonsterSpawns();
		}
		catch(final RuntimeException e)
		{
			e.printStackTrace();
		}

		_log.info("EpicBossManager : State of Frintezza is " + _state.getState() + ".");
		if( !_state.getState().equals(EpicBossState.State.NOTSPAWN))
			setIntervalEndTask();

		final Date dt = new Date(_state.getRespawnDate());
		_log.info("EpicBossManager : Next spawn date of Frintezza is " + dt + ".");
	}

	/** ***************************** Spawn Manager Creates all spawns ******************************** */

	/**
	 * Makes a spawn of all monsters, but doe not spawn them yet
	 */
	private static void createMonsterSpawns()
	{
		// The boss of all bosses ofcourse
		frintezzaSpawn = createNewSpawn(29045, 174240, -89805, -5022, 16048, _intervalOfBoss);

		// weak Scarlet Van Halisha.
		scarletSpawnWeak = createNewSpawn(29046, 174234, -88015, -5116, 48028, _intervalOfBoss);

		// Strong Scarlet Van Halisha -> x , y , z , heading, and Hp are set when the morph actually happens.
		scarletSpawnStrong = createNewSpawn(29047, 173203, -88484, -3513, 48028, _intervalOfBoss);

		// Portrait spawns - 4 portraits = 4 spawns
		portraitSpawn1 = createNewSpawn(29048, 175880, -88696, -5104, 35048, _intervalOfBoss);

		portraitSpawn2 = createNewSpawn(29049, 175816, -87160, -5104, 28205, _intervalOfBoss);

		portraitSpawn3 = createNewSpawn(29048, 172648, -87176, -5104, 64817, _intervalOfBoss);

		portraitSpawn4 = createNewSpawn(29049, 172600, -88664, -5104, 57730, _intervalOfBoss);

		// Demon spawns - 4 portraits = 4 demons (?)
		demonSpawn1 = createNewSpawn(29050, 175880, -88696, -5104, 35048, _intervalOfDemons);

		demonSpawn2 = createNewSpawn(29049, 175816, -87160, -5104, 28205, _intervalOfDemons);

		demonSpawn3 = createNewSpawn(29048, 172648, -87176, -5104, 64817, _intervalOfDemons);

		demonSpawn4 = createNewSpawn(29049, 172600, -88664, -5104, 57730, _intervalOfDemons);
	}

	/**
	 * Creates a single spawn from the parameter values and returns the L2Spawn created
	 * 
	 * @param templateId
	 *            int value of the monster template id number
	 * @param x
	 *            int value of the X position
	 * @param y
	 *            int value of the Y position
	 * @param z
	 *            int value of the Z position
	 * @param heading
	 *            int value of where is the monster facing to...
	 * @param respawnDelay
	 *            int value of the respawn of this L2Spawn
	 * @return L2Spawn created
	 */
	private static L2Spawn createNewSpawn(final int templateId, final int x, final int y, final int z, final int heading, final int respawnDelay)
	{
		L2Spawn tempSpawn = null;

		L2NpcTemplate template;

		try
		{
			template = NpcTable.getTemplate(templateId);
			tempSpawn = new L2Spawn(template);
			tempSpawn.setLocx(x);
			tempSpawn.setLocy(y);
			tempSpawn.setLocz(z);
			tempSpawn.setHeading(heading);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(respawnDelay);
			tempSpawn.setReflection(0);
			tempSpawn.stopRespawn();

			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		return tempSpawn;
	}

	/** ************************ Starting the battle with Frintezza + co. ***************************** */

	/**
	 * setting Scarlet Van Halisha spawn task which also starts the whole Frintezza battle.
	 * @param object 
	 */
	public static void setScarletSpawnTask(L2Object object)
	{
		if(_state.getState().equals(EpicBossState.State.NOTSPAWN) && _monsterSpawnTask == null)
		{
			Functions.npcShoutInRange(object,"Frintezza", "I will spawn in 5 minutes.", 5000);
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(1), _appTimeOfBoss);
		}
	}

	/**
	 * Shows a movie to the players in the lair.
	 * 
	 * @param target
	 *            - L2NpcInstance target is the center of this movie
	 * @param dist
	 *            - int distance from target
	 * @param yaw
	 *            - angle of movie (north = 90, south = 270, east = 0 , west = 180)
	 * @param pitch
	 *            - pitch > 0 looks up / pitch < 0 looks down
	 * @param time
	 *            - fast ++ or slow -- depends on the value
	 * @param duration
	 *            - How long to watch the movie
	 * @param socialAction
	 *            - 1,2,3 social actions / other values do nothing
	 */

	private static void showSocialActionMovie(final L2NpcInstance target, final int dist, final int yaw, final int pitch, final int time, final int duration, final int socialAction)
	{
		if(target == null)
			return;

		// set camera.
		for(final L2Player pc : getPlayersInside())
		{

			setIdle(pc);

			pc.setTarget(null);

			if(pc.getDistance(target) <= 2500)
			{
				pc.enterMovieMode();
				pc.specialCamera(target, dist, yaw, pitch, time, duration);
			}
			else
			{
				pc.leaveMovieMode();
			}
		}

		// do social.
		if(socialAction > 0 && socialAction < 5)
		{
			target.broadcastPacket(new SocialAction(target.getObjectId(), socialAction));
		}
	}

	/**
	 * I noticed that if the players do not stand at a certain position, they can not watch the entire movie, so I set them to the center during the entire movie.
	 */

	private static void teleportToStart()
	{
		if(_lastLocation == null)
			_lastLocation = new FastMap<Integer, Location>();

		final Location p = new Location(174233, -88212, -5116);

		for(final L2Player pc : getPlayersInside())
		{
			if(pc.getX() != p.x && pc.getY() != p.y && pc.getZ() != p.z)
			{
				if( !_lastLocation.containsKey(pc.getObjectId()))
					_lastLocation.put(pc.getObjectId(), new Location(pc.getX(), pc.getY(), pc.getZ()));

				pc.teleToLocation(p.x, p.y, p.z);
			}
		}
	}

	/**
	 * Teleports the players back to their last positions before the movie started
	 */

	private static void teleportToFinish()
	{
		if(_lastLocation == null || _lastLocation.isEmpty())
			return;

		for(final L2Player pc : getPlayersInside())
		{
			if(_lastLocation.containsKey(pc.getObjectId()))
			{
				final Location loc = _lastLocation.get(pc.getObjectId());
				pc.teleToLocation(loc.x, loc.y, loc.z);
			}
		}
		_lastLocation.clear();
	}

	/** ************************** Initialize a movie and spawn the monsters *********************** */

	/**
	 * Spawns Frintezza, the weak version of Scarlet Van Halisha, the minions, and all that is shown in a movie to the observing players.
	 */

	private static class ScarletWeakSpawn implements Runnable
	{
		private int _taskId = 0;

		public ScarletWeakSpawn(final int taskId)
		{
			_taskId = taskId;
		}

		public void run()
		{
			switch(_taskId)
			{
				case 1: // spawn.
					frintezza = (L2BossInstance) frintezzaSpawn.doSpawn(false);
					frintezza.setImobilised(true);

					_state.setRespawnDate(_intervalOfBoss);
					_state.setState(EpicBossState.State.ALIVE);
					_state.update();

					teleportToStart();

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(2), 1000);
					break;

				case 2:
					// show movie
					showSocialActionMovie(frintezza, 1000, 90, 30, 0, 5000, 0);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(200), 3000);
					break;

				case 200:
					// show movie
					showSocialActionMovie(frintezza, 1000, 90, 30, 0, 5000, 0);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(3), 3000);
					break;

				case 3:
					// show movie
					showSocialActionMovie(frintezza, 140, 90, 0, 6000, 6000, 2);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(5), 5990);
					break;

				case 5:
					// show movie
					showSocialActionMovie(frintezza, 240, 90, 3, 22000, 6000, 3);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(6), 5800);
					break;

				case 6:
					// show movie
					showSocialActionMovie(frintezza, 240, 90, 3, 300, 6000, 0);
					frintezza.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5006, 1, _intervalOfFrintezzaSongs, 0));

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(7), 5800);

					weakScarlet = (L2BossInstance) scarletSpawnWeak.doSpawn(true);
					weakScarlet.setImobilised(true);

					portrait1 = (L2MonsterInstance) portraitSpawn1.doSpawn(true);
					portrait1.setImobilised(true);

					portrait2 = (L2MonsterInstance) portraitSpawn2.doSpawn(true);
					portrait2.setImobilised(true);

					portrait3 = (L2MonsterInstance) portraitSpawn3.doSpawn(true);
					portrait3.setImobilised(true);

					portrait4 = (L2MonsterInstance) portraitSpawn4.doSpawn(true);
					portrait4.setImobilised(true);

					demon1 = demonSpawn1.doSpawn(true);
					demon1.setImobilised(true);

					demon2 = demonSpawn2.doSpawn(true);
					demon2.setImobilised(true);

					demon3 = demonSpawn3.doSpawn(true);
					demon3.setImobilised(true);

					demon4 = demonSpawn4.doSpawn(true);
					demon4.setImobilised(true);

					final Earthquake eq = new Earthquake(weakScarlet.getLoc(), 50, 6);

					for(final L2Player pc : getPlayersInside())
						pc.broadcastPacket(eq);

					break;

				case 7:

					showSocialActionMovie(demon1, 140, 0, 3, 22000, 3000, 1);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(8), 2800);
					break;

				case 8:

					showSocialActionMovie(demon2, 140, 0, 3, 22000, 3000, 1);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(9), 2800);
					break;

				case 9:

					showSocialActionMovie(demon3, 140, 180, 3, 22000, 3000, 1);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(10), 2800);
					break;

				case 10:

					showSocialActionMovie(demon4, 140, 180, 3, 22000, 3000, 1);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(17), 2800);

					weakScarlet.broadcastPacket(new SocialAction(weakScarlet.getObjectId(), 2));
					break;

				case 17:

					// show movie
					for(final L2Player pc : getPlayersInside())
					{
						pc.setTarget(null);

						if(pc.getDistance(weakScarlet) <= 2500)
						{
							pc.enterMovieMode();
							pc.specialCamera(weakScarlet, 1700, 180, 90, 0, 4000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(18), 3800);
					break;

				case 18:
					// show movie
					showSocialActionMovie(weakScarlet, 1500, 45, 70, 6000, 7000, 2);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(19), 6900);
					break;

				case 19:
					// show movie
					showSocialActionMovie(weakScarlet, 1500, 180, 60, 0, 5000, 2);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(20), 4900);
					break;

				case 20:
					// show movie
					showSocialActionMovie(weakScarlet, 1220, 360, 70, 300, 2000, 0);

					// set next task.
					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(21), 1900);
					break;

				case 21:

					weakScarlet.abortCast();

					ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(weakScarlet), 16);

					// weakScarlet.teleToLocation(174234, -88015, -5116);

					showSocialActionMovie(weakScarlet, 1000, 45, 19, 300, 3000, 2);

					ThreadPoolManager.getInstance().scheduleGeneral(new ScarletWeakSpawn(32), 3100);

					teleportToFinish();

					// set delete task.
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(), _activityTimeOfBoss);
					break;

				case 32:

					// reset camera.
					for(final L2Player pc : getPlayersInside())
					{
						pc.leaveMovieMode();
					}

					frintezza.abortCast();

					final L2Skill skill = SkillTable.getInstance().getInfo(1086, 1);

					demon1.setImobilised(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(demon1, skill, _intervalOfFrintezzaSongs, 1000), 4000);

					demon2.setImobilised(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(demon2, skill, _intervalOfFrintezzaSongs, 1000), 4100);

					demon3.setImobilised(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(demon3, skill, _intervalOfFrintezzaSongs, 1000), 4200);

					demon4.setImobilised(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(demon4, skill, _intervalOfFrintezzaSongs, 1000), 4300);

					ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(frintezza), 16);

					// Start random attacks on players for Frintezza
					ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(frintezza), _intervalOfRetarget);

					// Start random attacks on players for Scarlet
					ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(weakScarlet), _intervalOfRetarget + 16);

					ThreadPoolManager.getInstance().scheduleGeneral(new Music(), Rnd.get(_intervalOfFrintezzaSongs));

					startAttackListeners();
					break;
			}
		}
	}

	/** ******************************************************************************************** */

	/***********************************************************************************************************************************************************
	 * ****** M M PPPPPPP TTTTTTTTT Y Y /* MM MM P P T Y Y /* M M M M P P T Y Y /******* M M M M P P T Y /* M M M PPPPPPP T Y /* M M P T Y /* M M P T Y /******** M M P T Y
	 * /*********************************************************************************************** /** Frintezza's songs, needed special implementation since core doesn't support 3 stage skills *
	 * /
	 **********************************************************************************************************************************************************/

	/**
	 * Three stages of casts: 1. Song, cast on Frintezza, for the music to play (skill 5007, levels 1-5) Each level is a different tune =) 2. Visual Effect, cast on targets, to show the effect (skill
	 * 5008, levels 1-5) Each level has a different
	 * animation effect and different targets and purpose 3. Actual skill, which is different since NCSoft has a different skill system. I used other skill Ids to implement these effects: song
	 * effects: 1. skill 1217 - Greater Heal (5007,1 -> 5008,1
	 * -> 1217,33) 2. skill 1204 - Wind Walk (5007,2 -> 5008,2 -> 1204,2) 3. skill 1086 - Haste Buff (5007,3 -> 5008,3 -> 1086,2) 4. skill 406 - Angelic Icon (5007,4 -> 5008,4 -> 406,3 only gainHp*0.2
	 * func added) 5. no skill only immobilize (5007,5
	 * -> 5008,5 -> dance+stun animation + Immobilizes)
	 */

	private static class Music implements Runnable
	{
		public void run()
		{
			if(frintezza == null)
				return;

			int song = getSong();
			if(song < 1)
				song = 1;
			else if(song > 5)
				song = 5;

			frintezza.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5007, song, _intervalOfFrintezzaSongs, 0));

			final int currentHp = (int) frintezza.getCurrentHp();

			// Launch the song's effects (they start about 10 seconds after he starts to play)
			ThreadPoolManager.getInstance().scheduleGeneral(new SongEffectLaunched(getSongTargets(song), song, currentHp, 10000), 10000);

			// Schedule a new song to be played in 30-40 seconds...
			ThreadPoolManager.getInstance().scheduleGeneral(new Music(), _intervalOfFrintezzaSongs + Rnd.get(10000));
		}

		/**
		 * Depending on the song, returns the song's targets (either mobs or players)
		 * 
		 * @param songId
		 *            (1-5 songs)
		 * @return L2Object[] targets
		 */
		private static L2Object[] getSongTargets(final int songId)
		{

			final List<L2Object> targets = new FastList<L2Object>();

			if(songId < 4) // Target is the minions
			{

				if(weakScarlet != null && !weakScarlet.isDead())
					targets.add(weakScarlet);

				if(strongScarlet != null && !strongScarlet.isDead())
					targets.add(strongScarlet);

				if(portrait1 != null && !portrait1.isDead())
					targets.add(portrait1);

				if(portrait2 != null && !portrait2.isDead())
					targets.add(portrait2);

				if(portrait3 != null && !portrait3.isDead())
					targets.add(portrait3);

				if(portrait4 != null && !portrait4.isDead())
					targets.add(portrait4);

				if(demon1 != null && !demon1.isDead())
					targets.add(demon1);

				if(demon2 != null && !demon2.isDead())
					targets.add(demon2);

				if(demon3 != null && !demon3.isDead())
					targets.add(demon3);

				if(demon4 != null && !demon4.isDead())
					targets.add(demon4);

				targets.add(frintezza);
			}
			else
			// Target is the players
			{

				for(final L2Player pc : getPlayersInside())
				{
					if( !pc.isDead())
						targets.add(pc);
				}
			}

			return targets.toArray(new L2Object[targets.size()]);
		}

		/**
		 * returns the chosen symphony for Frintezza to play If the minions are injured he has 40% to play a healing song If they are all dead, he will only play harmful player symphonies
		 * 
		 * @return
		 */

		private static int getSong()
		{
			if(minionsNeedHeal())
				return 1;
			else if(minionsAreDead())
				return Rnd.get(4, 6);
			return Rnd.get(2, 6);
		}

		/**
		 * Checks if the main minions are dead (not including demons, only Scarlet and Portraits)
		 * 
		 * @return boolean true if all main minions are dead
		 */

		private static boolean minionsAreDead()
		{
			if(weakScarlet != null && !weakScarlet.isDead())
				return false;
			else if(strongScarlet != null && !strongScarlet.isDead())
				return false;
			else if(portrait1 != null && !portrait1.isDead())
				return false;
			else if(portrait2 != null && !portrait2.isDead())
				return false;
			else if(portrait3 != null && !portrait3.isDead())
				return false;
			else if(portrait4 != null && !portrait4.isDead())
				return false;
			return true;
		}

		/**
		 * Checks if Frintezza's minions need heal (only major minions are checked) Return a "need heal" = true only 40% of the time
		 * 
		 * @return boolean value true if need to play a healing minion song
		 */
		private static boolean minionsNeedHeal()
		{
			boolean returnValue = false;

			if(weakScarlet != null && !weakScarlet.isDead() && (int) weakScarlet.getCurrentHp() < weakScarlet.getMaxHp() * 2 / 3)
				returnValue = true;
			else if(strongScarlet != null && !strongScarlet.isDead() && (int) strongScarlet.getCurrentHp() < strongScarlet.getMaxHp() * 2 / 3)
				returnValue = true;
			else if((portrait1 != null && !portrait1.isDead() && (int) portrait1.getCurrentHp() < portrait1.getMaxHp() / 3) || (portrait2 != null && !portrait2.isDead() && (int) portrait2.getCurrentHp() < portrait2.getMaxHp() / 3) || (portrait3 != null && !portrait3.isDead() && (int) portrait3.getCurrentHp() < portrait3.getMaxHp() / 3) || (portrait4 != null && !portrait4.isDead() && (int) portrait4.getCurrentHp() < portrait4.getMaxHp() / 3))
				returnValue = true;

			if(returnValue && Rnd.chance(40)) // 40% to heal minions when needed.
				return false;

			return returnValue;
		}
	}

	/**
	 * The song was played, this class checks it's affects (if any)
	 * 
	 * @author Darki699, Felixx
	 */
	private static class SongEffectLaunched implements Runnable
	{
		private static L2Object[] _targets;

		private static int _song, _previousHp, _currentTime;

		/**
		 * Constructor
		 * 
		 * @param targets
		 *            - song's targets L2Object[]
		 * @param song
		 *            - song id 1-5
		 * @param previousHp
		 *            - Frintezza's HP when he started to play
		 * @param currentTimeOfSong
		 *            - skills during music play are consecutive, repeating
		 */
		public SongEffectLaunched(final L2Object[] targets, final int song, final int previousHp, final int currentTimeOfSong)
		{
			_targets = targets;
			_song = song;
			_previousHp = previousHp;
			_currentTime = currentTimeOfSong;
		}

		public void run()
		{
			if(frintezza == null)
				return;
			// If the song time is over stop this loop
			else if(_currentTime > _intervalOfFrintezzaSongs)
				return;

			// Skills are consecutive, so call them again
			final SongEffectLaunched songLaunched = new SongEffectLaunched(_targets, _song, (int) frintezza.getCurrentHp(), _currentTime + _intervalOfFrintezzaSongs / 10);
			ThreadPoolManager.getInstance().scheduleGeneral(songLaunched, _intervalOfFrintezzaSongs / 10);

			// If Frintezza got injured harder than his regen rate, do not launch the song.
			if(_previousHp > frintezza.getCurrentHp())
			{
				final L2Object frintezzaTarget = frintezza.getTarget();

				if(frintezzaTarget != null && frintezzaTarget.isPlayable())
					callMinionsToAssist((L2Playable) frintezzaTarget, 200);

				return;
			}

			for(final L2Object target : _targets)
			{
				if(target == null || !(target instanceof L2Character))
					continue;

				final L2Character cha = (L2Character) target;

				if(cha.isDead() || cha.isInvul())
					continue;

				// show the magic effect on the target - visual effect
				cha.broadcastPacket(new MagicSkillUse(frintezza, cha, 5008, _song, 2000, 0));

				// calculate the song's damage
				calculateSongEffects(cha);
			}
		}

		/**
		 * Calculates the music damage according to the current song played
		 * 
		 * @param target
		 *            - L2Character affected by the music
		 */
		private static void calculateSongEffects(final L2Character target)
		{
			if(target == null)
				return;
			try
			{
				L2Skill skill;

				switch(_song)
				{
					case 1: // Consecutive Heal : Greater Heal - on the monsters
						skill = SkillTable.getInstance().getInfo(1217, 33);
						frintezza.doCast(skill, target, false);
						break;

					case 2: // Consecutive Dash : Wind Walk - monsters run faster
						skill = SkillTable.getInstance().getInfo(1204, 2);
						frintezza.doCast(skill, target, false);
						break;

					case 3: // Affecting Atk Spd : Haste Buff - monsters attack faster
						skill = SkillTable.getInstance().getInfo(1086, 2);
						frintezza.doCast(skill, target, false);
						break;

					case 4: // Offensive Skill: Decreases the effect of HP reg. on the players

						if(Rnd.get(100) < 80) // 80% success not considering m.def + p.def ???
						{
							// Launch the skill on the target to decrease it's HP
							decreaseEffectOfHpReg(target);
						}
						break;

					case 5: // Offensive Skill: Immoblizes - dance+stun. Player is immoblized
						skill = SkillTable.getInstance().getInfo(5008, 5);
						if(Rnd.get(100) < 80) // 80% success not considering m.def + p.def ???
						{
							new startStunDanceEffect(target, skill).run();
						}
						break;
				}
			}
			catch(final RuntimeException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Decreases the HP Regeneration of the <b>target</b>
	 * 
	 * @param target
	 *            L2Character who's HP Regeneration is decreased.
	 */
	private static void decreaseEffectOfHpReg(final L2Character target)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(5008, 4);

		frintezza.doCast(skill, target, false);

		// send target the message, the skill was launched
		if(target instanceof L2Player)
			target.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(skill.getId(), 1));

		// Add stat funcs to the target
		// target.addStatFunc(getDecreaseRegHpFunc());

		// Set exit timer for these stats
		// ThreadPoolManager.getInstance().scheduleGeneral(new exitDecreaseRegHp(target), getDebuffPeriod(skill, target));
	}

	/**
	 * Returns the duration of the <b>skill</b> on the <b>target</b>.
	 * 
	 * @param skill
	 *            L2Skill to calculate it's duration
	 * @param target
	 *            L2Character to calculate the duration on it
	 * @return int value of skill duration before exit
	 */
	private static int getDebuffPeriod(final L2Skill skill, final L2Character target)
	{
		// This is usually returned, unless _debuffPeriod needs initialization
		if(_debuffPeriod != 0)
			return _debuffPeriod;

		// Initialize _debuffPeriod
		if(skill == null || target == null)
		{
			_debuffPeriod = 15000;

			return _debuffPeriod;
		}

		if(_debuffPeriod == 0)
			_debuffPeriod = 15000;

		// return _debuffPeriod which is now initialized
		return _debuffPeriod;
	}

	/**
	 * Further implementation into the core is needed. But this will do for now ;] Class needed to implement the start Frintezza dance+stun effect on a target.
	 * 
	 * @author Darki699, Felixx
	 */

	private static class startStunDanceEffect implements Runnable
	{
		private static L2Character _effected;

		private static L2Skill _skill;

		public startStunDanceEffect(final L2Character target, final L2Skill skill)
		{
			_effected = target;
			_skill = skill;
		}

		public void run()
		{
			try
			{
				if(_effected == null)
					return;

				// stun dance can be cast on L2Player only
				if( !(_effected instanceof L2Player))
					return;

				if(_effected.getEffectList().getEffectsBySkill(_skill) != null)
					return;

				final L2Player effected = (L2Player) _effected;
				if(effected.isInvul() || effected.isInvisible())
					return;

				// stop all actions
				setIdle(_effected);

				_effected.setTarget(null);

				// start the animation
				_effected.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_DANCE_STUNNED);

				// add the effect icon
				_effected.doCast(_skill, _effected, false);

				// send target the message
				if(_effected instanceof L2Player)
					_effected.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(_skill.getId(), 1));

				// set the cancel task for this effect
				ThreadPoolManager.getInstance().scheduleGeneral(new exitStunDanceEffect(_effected), getDebuffPeriod(_skill, _effected));
			}
			catch(final RuntimeException e)
			{
				if(_effected != null && !_effected.isAlikeDead())
				{
					_effected.setImobilised(false);
				}

				e.printStackTrace();
			}
		}
	}

	/**
	 * Ends the dance+stun effect on the target
	 * 
	 * @author Darki699, Felixx
	 */
	private static class exitStunDanceEffect implements Runnable
	{
		private static L2Character _effected;

		public exitStunDanceEffect(final L2Character target)
		{
			_effected = target;
		}

		public void run()
		{
			if(_effected == null)
				return;

			_effected.setImobilised(false);

			_effected.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_DANCE_STUNNED);
		}
	}

	/** ************************** End of Frintezza's Musical effects ******************************** */

	/***********************************************************************************************************************************************************
	 * ****** M M PPPPPPP TTTTTTTTT Y Y /* MM MM P P T Y Y /* M M M M P P T Y Y /******* M M M M P P T Y /* M M M PPPPPPP T Y /* M M P T Y /* M M P T Y /******** M M P T Y /************************
	 * Minion Control Attack + Respawn + Polymorph
	 **********************************************************************************************************************************************************/

	/**
	 * Initializes the Attack Listeners for <b>all</b> monsters in this zone. Sends a thread loop (tasks canceled ofcourse) with the mob to be listened to, and the amount of hate it sends to all other
	 * mobs regarding it's attacker.
	 */

	private static void startAttackListeners()
	{
		// Set listeners for the Demons.
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(demon1, 1), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(demon2, 1), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(demon3, 1), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(demon4, 1), Rnd.get(2000));

		// Set listeners for the Portraits.
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(portrait1, 50), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(portrait2, 50), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(portrait3, 50), Rnd.get(2000));

		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(portrait4, 50), Rnd.get(2000));

		// Set a listener for Frintezza.
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(frintezza, 200), Rnd.get(2000));

		// Set a listener for the weaker version of Scarlet Van Halisha.
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(weakScarlet, 100), Rnd.get(2000));

		// Note that Scarlet's strong version isn't spawned yet. An attack listener for it will
		// be added when and if he's spawned.
	}

	/**
	 * Class is recalled at an interval for a monster's life time. Once the monster is <b>deleted</b> (null), this class is not called anymore If the monster is <b>dead</b>, this class is still called
	 * at the interval, but it does nothing until next
	 * respawn. If the monster is <b>alive</b> and is being attacked, it "tells" the other monsters that it's attacked.
	 */
	private static class attackerListener implements Runnable
	{
		private static L2NpcInstance _mob;

		private static int _aggroDamage;

		public attackerListener(final L2NpcInstance controller, final int hate)
		{
			_mob = controller;
			_aggroDamage = hate;
		}

		public void run()
		{
			// If the monster is deleted, return.
			if(_mob == null)
				return;

			// Set next listener.
			ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(_mob, _aggroDamage), _callForHelpInterval + Rnd.get(500));

			// If the mob is dead, we do nothing until next respawn
			if(_mob.isDead())
			{
				try
				{
					// if this is a demon, decay it until next respawn.
					if( !(_mob instanceof L2BossInstance))
						_mob.decayMe();
					else
						// if this is a boss, we need to do a few checks:
						// portraits - unspawn demons, set both to null
						// Scarlet, Frintezza - check if all bosses are dead.
						bossDeadCheck(_mob);
				}
				catch(final RuntimeException e)
				{
					e.printStackTrace();
				}
				return;
			}
			// Added a check, since mobs sometimes move toward their targets (teleport)
			// He shouldn't move from his place
			else if(_mob == frintezza)
			{
				if(_mob.getX() != frintezzaSpawn.getLocx() || _mob.getY() != frintezzaSpawn.getLocy())
				{
					final boolean[] targeted = getTargeted(_mob);
					final L2Object target = _mob.getTarget();

					_mob.setXYZ(frintezzaSpawn.getLocx(), frintezzaSpawn.getLocy(), frintezzaSpawn.getLocz());

					_mob.decayMe();
					_mob.spawnMe();

					_mob.setTarget(target);
					setTargeted(_mob, targeted);
				}
			}

			// Tell the other mobs "I'm attacked"
			final L2Object target = _mob.getTarget();

			if(target == null || (target instanceof L2Character && ((L2Character) target).isDead()))
				return;

			if(target.isPlayable())
				callMinionsToAssist((L2Playable) target, _aggroDamage);

			// Now set the mob's Target to the most hated:
			if(_mob instanceof L2NpcInstance)
			{
				final L2Character mostHated = _mob.getMostHated();

				if(mostHated != null)
				{
					_mob.setTarget(mostHated);
				}
			}
		}
	}

	/**
	 * If the dead boss is a Portrait, we delete it from the world, and it's demon as well If the dead boss is Scarlet or Frintezza, we do a bossesAreDead() check to see if both Frintezza and Scarlet
	 * are dead.
	 * 
	 * @param mob
	 *            - L2BossInstance that is (or is set as) dead.
	 */

	public static void bossDeadCheck(L2NpcInstance mob)
	{
		if(mob == null)
			return;

		// !!! Frintezza or Scarlet should NEVER be called from setUnspawn() to this function !!!
		// It will cause a deadlock.
		if(mob == weakScarlet || mob == strongScarlet)
		{
			if(bossesAreDead())
				doUnspawn();
			return;
		}

		L2NpcInstance demon = null;

		if(mob == portrait1)
		{
			portrait1 = null;
			demon = demon1;
			demon1 = null;
		}
		else if(mob == portrait2)
		{
			portrait2 = null;
			demon = demon2;
			demon2 = null;
		}
		else if(mob == portrait3)
		{
			portrait3 = null;
			demon = demon3;
			demon3 = null;
		}
		else if(mob == portrait4)
		{
			portrait4 = null;
			demon = demon4;
			demon4 = null;
		}

		// Try to delete the portrait.
		try
		{
			mob.decayMe();
			mob.deleteMe();
			mob = null;
		}
		catch(final RuntimeException e)
		{
			e.printStackTrace();
		}

		// Try to delete the portraits demon.
		if(demon != null)
		{
			try
			{
				demon.decayMe();
				demon.deleteMe();
				demon = null;
			}
			catch(final RuntimeException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * controls the assistance for all 3 bosses: 1. if Frintezza needs help, all (including Scarlet van Halisha) help him 2. if Scarlet needs help, all (including Frintezza) come to his help 3. if
	 * Strong Scarlet is already spawned, then he teleports
	 * to help Frintezza
	 * 
	 * @param attacker
	 *            - The player that attacked the boss
	 * @param hate
	 *            - Damage hate to add to the attacker 1. Frintezza adds 200 hate 2. Weak Scarlet adds 100 hate 3. Stronger Scarlet adds 125 hate 4. Strongest Scarlet adds 150 hate 5. Portraits adds
	 *            50 hate 6. Demons adds 1 hate
	 */
	public static void callMinionsToAssist(final L2Playable attacker, final int hate)
	{
		if(attacker == null)
			return;

		if(demon1 != null && !demon1.isDead())
			attacker.addDamageHate(demon1, 0, hate);
		else
			checkRespawnTime(demon1);

		if(demon2 != null && !demon2.isDead())
			attacker.addDamageHate(demon2, 0, hate);
		else
			checkRespawnTime(demon2);

		if(demon3 != null && !demon3.isDead())
			attacker.addDamageHate(demon3, 0, hate);
		else
			checkRespawnTime(demon3);

		if(demon4 != null && !demon4.isDead())
			attacker.addDamageHate(demon4, 0, hate);
		else
			checkRespawnTime(demon4);

		if(weakScarlet != null && !weakScarlet.isDead())
		{
			weakScarletHpListener();
			if(weakScarlet != null && !weakScarlet.isDead())
				attacker.addDamageHate(weakScarlet, 0, hate);
		}
		else
			bossesAreDead();

		if(strongScarlet != null && !strongScarlet.isDead())
			attacker.addDamageHate(strongScarlet, 0, hate);
		else
			bossesAreDead();
	}

	/**
	 * Checks on a killed demon to set it's respawn time (only done if the <b>Portrait</b> of <b>Demon</b> was not killed)
	 * 
	 * @param mob
	 *            - L2MonsterInstance mob of the Demon that was killed
	 * @return int value of the demon that should be respawned. -1 is returned if the demon should not respawn now.
	 */
	public static int checkRespawnTime(final L2NpcInstance mob)
	{
		if(mob == null)
			return -1;

		if(mob == demon1)
		{
			if(portrait1 == null || portrait1.isDead())
				return -1;
			else if(_respawningDemon1)
				return 1;

			_respawningDemon1 = true;
		}

		else if(mob == demon2)
		{
			if(portrait2 == null || portrait2.isDead())
				return -1;
			else if(_respawningDemon2)
				return 2;

			_respawningDemon2 = true;
		}

		else if(mob == demon3)
		{
			if(portrait3 == null || portrait3.isDead())
				return -1;
			else if(_respawningDemon3)
				return 3;

			_respawningDemon3 = true;
		}

		else if(mob == demon4)
		{
			if(portrait4 == null || portrait4.isDead())
				return -1;
			else if(_respawningDemon4)
				return 4;

			_respawningDemon4 = true;
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new respawnDemon(mob), _intervalOfDemons);
		return -1;
	}

	/**
	 * Class respawns a demon if it's portrait is not dead.
	 * 
	 * @author Darki699, Felixx
	 */

	private static class respawnDemon implements Runnable
	{
		private static L2NpcInstance _mob;

		public respawnDemon(final L2NpcInstance mob)
		{
			_mob = mob;
		}

		public void run()
		{
			final int demonId = checkRespawnTime(_mob);

			switch(demonId)
			{
				case -1: // If it's portrait is dead do not respawn the demon.
					try
					{
						_mob.decayMe();
						_mob.deleteMe();
					}
					catch(final RuntimeException e)
					{
						e.printStackTrace();
					}

					return;

				case 1: // Demon #1 is respawned
					_mob.setXYZ(demonSpawn1.getLocx(), demonSpawn1.getLocy(), demonSpawn1.getLocz());
					_respawningDemon1 = false;
					break;

				case 2: // Demon #2 is respawned
					_mob.setXYZ(demonSpawn2.getLocx(), demonSpawn2.getLocy(), demonSpawn2.getLocz());
					_respawningDemon2 = false;
					break;

				case 3: // Demon #3 is respawned
					_mob.setXYZ(demonSpawn3.getLocx(), demonSpawn3.getLocy(), demonSpawn3.getLocz());
					_respawningDemon3 = false;
					break;

				case 4: // Demon #4 is respawned
					_mob.setXYZ(demonSpawn4.getLocx(), demonSpawn4.getLocy(), demonSpawn4.getLocz());
					_respawningDemon4 = false;
					break;
			}

			_mob.doRevive();
			_mob.setCurrentHp(_mob.getMaxHp(), true);
			_mob.setCurrentMp(_mob.getMaxMp());
			_mob.spawnMe();

			final L2Character target = getRandomPlayer();
			_mob.setTarget(target);

			final Location pos = new Location(target.getX(), target.getY(), target.getZ());
			new MoveToPos(_mob, pos);
		}
	}

	/**
	 * Listens to the weaker Scarlet Van Halisha HP status. If it's weakened enough, we spawn the Stronger version of Scarlet and delete the weaker one.
	 */

	public static void weakScarletHpListener()
	{
		if(weakScarlet == null || weakScarlet.isDead())
			return;

		final double curHp = weakScarlet.getCurrentHp(), maxHp = weakScarlet.getMaxHp();

		if(_scarletIsWeakest)
		{
			// Morph Scarlet Van Halisha into a Stronger one ;]
			if(curHp < maxHp * 2 / 3)
			{
				doSecondMorph();
			}
		}
		else
		{
			if(curHp < maxHp * 1 / 3)
			{
				// Do 3rd Morph, Scarlet Van Halisha now changes templates
				doThirdMorph();
			}
		}
	}

	/**
	 * Does the 3rd and last polymorph for Scarlet Van Halisha. Now he looks entirely different... (he is different)
	 */
	private static void doThirdMorph()
	{
		setIdle(weakScarlet);

		weakScarlet.setIsInSocialAction(true);

		// animation
		weakScarlet.setPolyInfo(L2Object.POLY_NPC, 29047);
		final MagicSkillUse msk = new MagicSkillUse(weakScarlet, 1008, 1, 4000, 0);
		weakScarlet.broadcastPacket(msk);

		// set Strong Scarlet's position and heading
		scarletSpawnStrong.setLocx(weakScarlet.getX());
		scarletSpawnStrong.setLocy(weakScarlet.getY());
		scarletSpawnStrong.setLocz(weakScarlet.getZ());
		scarletSpawnStrong.setHeading(weakScarlet.getHeading());
		scarletSpawnStrong.setRespawnDelay(_intervalOfBoss);
		scarletSpawnStrong.stopRespawn();

		// spawn Strong Scarlet and set his HP to the weaker version:
		strongScarlet = (L2BossInstance) scarletSpawnStrong.doSpawn(true);
		final double newHp = weakScarlet.getCurrentHp();
		strongScarlet.setCurrentHp(newHp, true);

		// Immobilize Strong Scarlet
		setIdle(strongScarlet);
		strongScarlet.setIsInSocialAction(true);

		// do a social action "hello" ;]
		strongScarlet.broadcastPacket(new SocialAction(strongScarlet.getObjectId(), 2));

		// update his target
		strongScarlet.setTarget(weakScarlet.getTarget());

		// restore the original weapon into the template
		weakScarlet.getTemplate().setRhand(8204);

		// get weakScarlet's list of attackers (or players that targeted it).
		final boolean[] targeted = getTargeted(weakScarlet);

		// set the list of players to target strongScarlet
		setTargeted(strongScarlet, targeted);

		// delete the weakScarlet from the world
		scarletSpawnWeak.stopRespawn();
		scarletSpawnWeak.decreaseCount(weakScarlet);

		weakScarlet.setPolyInfo(0, 0);
		weakScarlet.decayMe();
		weakScarlet.deleteMe();
		weakScarlet = null;

		// add Attack Listener
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(strongScarlet, 150), Rnd.get(4000) + 1000);

		// add retarget Listener
		ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(strongScarlet), _intervalOfRetarget);

		// mobilize Strong Scarlet
		ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(strongScarlet), 4000);

		// set teleport speed
		final L2Skill skill = SkillTable.getInstance().getInfo(1086, 1);
		ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(strongScarlet, skill, _intervalOfRetarget, 300), 4016);
	}

	/**
	 * Receives a target and a list of players in _playersInLair that should target this target
	 * 
	 * @param target
	 *            L2Character
	 * @param targeted
	 *            boolean[]
	 * @return void
	 */

	private static void setTargeted(final L2Character target, final boolean[] targeted)
	{
		int count = 0;

		// Server->Client packet StatusUpdate of the L2NpcInstance to the L2Player to update its HP bar
		final StatusUpdate su = new StatusUpdate(target.getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());

		// set the target again on the players that targeted this _caster
		for(final L2Player pc : getPlayersInside())
		{
			if(pc != null && targeted[count])
			{
				pc.setTarget(target);

				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2Player to update its HP bar
				pc.sendPacket(su);
			}
			count++;
		}
	}

	/**
	 * Receives a target and returns the _playersInLair that target this target
	 * 
	 * @param target
	 *            L2Object
	 * @return boolean[] targeted players (true = target , false = not target)
	 */

	private static boolean[] getTargeted(final L2Object target)
	{
		final boolean[] targeted = new boolean[getPlayersInside().size()];
		int count = 0;

		// get the players that targeted this _caster
		for(final L2Player pc : getPlayersInside())
		{
			targeted[count] = !(pc == null || pc.getTarget() != target);
			count++;
		}
		return targeted;
	}

	/**
	 * Sets a L2Character to idle state. Disables all skills, aborts attack and cast, immoblizies
	 * 
	 * @param target
	 *            L2Character
	 */
	public static void setIdle(final L2Character target)
	{
		target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		target.abortAttack();
		target.abortCast();
		target.setImobilised(true);
	}

	/**
	 * Does the 2nd Morph for Scarlet Van Halisha. Now he's bigger and he teleports to his targets
	 */

	private static void doSecondMorph()
	{
		_scarletIsWeakest = false;

		weakScarlet.getTemplate().setRhand(7903);

		final L2Spawn scarletSpawnTemp = createNewSpawn(29046, weakScarlet.getX(), weakScarlet.getY(), weakScarlet.getZ(), weakScarlet.getHeading(), _intervalOfBoss);

		final L2BossInstance tempScarlet = (L2BossInstance) scarletSpawnTemp.doSpawn(true);
		tempScarlet.setCurrentHp(weakScarlet.getCurrentHp(), true);
		tempScarlet.setTarget(weakScarlet.getTarget());
		final boolean[] targeted = getTargeted(weakScarlet);

		weakScarlet.decayMe();
		weakScarlet.deleteMe();
		weakScarlet = tempScarlet;

		setTargeted(weakScarlet, targeted);

		setIdle(weakScarlet);

		weakScarlet.setIsInSocialAction(true);
		weakScarlet.broadcastPacket(new SocialAction(weakScarlet.getObjectId(), 2));

		// add a NEW Attack Listener
		ThreadPoolManager.getInstance().scheduleGeneral(new attackerListener(weakScarlet, 125), Rnd.get(4000) + 1000);

		// add a NEW retarget Listener
		ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(weakScarlet), _intervalOfRetarget * 2 / 3);

		// start teleporting fast
		L2Skill skill = SkillTable.getInstance().getInfo(1086, 1);
		ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(weakScarlet, skill, _intervalOfRetarget, 200), 50);

		skill = SkillTable.getInstance().getInfo(1068, 3);
		weakScarlet.doCast(skill, weakScarlet, false);

		ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(weakScarlet), 1100);

		// reset camera.
		for(final L2Player pc : getPlayersInside())
		{
			pc.leaveMovieMode();
		}
	}

	/**
	 * Starts the skill effects for Scarlet Van Halisha. He moves like the wind ;] Continuous skills cast at _intervalOfRetarget
	 * 
	 * @author Darki699, Felixx
	 */

	private static class doSkill implements Runnable
	{
		private static L2Character _caster;
		private static L2Skill _skill;
		private static int _interval, _range;

		/**
		 * Shows skill animation effect and teleports to the target if it's out of range
		 * 
		 * @param caster
		 *            - the monster
		 * @param skill
		 *            - the skill to animate
		 * @param interval
		 *            - the time between leaps
		 * @param range
		 *            - the range minimum to teleport
		 */

		public doSkill(final L2Character caster, final L2Skill skill, final int interval, final int range)
		{
			_caster = caster;
			_skill = skill;
			_interval = interval;
			_range = range;
		}

		public void run()
		{
			if(_caster == null || _caster.isDead())
				return;

			try
			{

				L2Object tempTarget = _caster.getTarget();

				if(tempTarget == null || !(tempTarget instanceof L2Character))
					tempTarget = _caster;

				final int x = tempTarget.getX() + Rnd.get(_range) - _range / 2, y = tempTarget.getY() + Rnd.get(_range) - _range / 2, z = tempTarget.getZ();

				if( !_caster.isInsideRadius(x, y, _range, false) && tempTarget instanceof L2Player && _zone.checkIfInZone(tempTarget))
				{
					// Returns a list of the players that targeted the _caster
					final boolean[] targeted = getTargeted(_caster);

					_caster.broadcastPacket(new MagicSkillUse(_caster, ((L2Character) tempTarget), _skill.getId(), _skill.getLevel(), 0, 0));
					_caster.decayMe();
					_caster.setXYZ(x, y, z);
					_caster.spawnMe(new Location(x, y, z));
					_caster.setTarget(tempTarget);

					// retarget all the players that targeted this _caster
					setTargeted(_caster, targeted);
				}
			}
			catch(final RuntimeException e)
			{
				e.printStackTrace();
			}

			ThreadPoolManager.getInstance().scheduleGeneral(new doSkill(_caster, _skill, _interval, _range), _interval + Rnd.get(500));
		}
	}

	/** * Re-Target Class to update a monster's known list and to re-target it again at an interval ** */

	private static class ReTarget implements Runnable
	{
		private static L2NpcInstance _mob;

		public ReTarget(final L2NpcInstance mob)
		{
			_mob = mob;
		}

		public void run()
		{
			if(_mob == null || _mob.isDead() || getPlayersInside().isEmpty())
			{
				if(bossesAreDead() || getPlayersInside().isEmpty())
				{
					// Unspawn in 20 seconds...
					doUnspawn();
				}
				return;
			}

			_mob.setTarget(getRandomPlayer());

			if(_mob.getTarget() == null)
				ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(_mob), 1000);
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new ReTarget(_mob), _intervalOfRetarget);
		}
	}

	/** *********************************** End of re-target class *************************************** */

	/**
	 * starts the unspawn in 20 seconds.
	 */
	private static void doUnspawn()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Unspawn(), 20000);
	}

	/**
	 * Unspawns class calls setUnspawn() function to end this battle scene
	 */
	private static class Unspawn implements Runnable
	{
		public void run()
		{
			setUnspawn();
		}
	}

	/**
	 * Checks if Frintezza and Scarlet Van Halisha are <b>both</b> dead
	 * 
	 * @return boolean true if <b>all</b> bosses are dead.
	 */

	private static boolean bossesAreDead()
	{
		if(weakScarlet == null && strongScarlet == null)
			return true;

		int deadCount = 0;

		if((weakScarlet != null && weakScarlet.isDead()) || weakScarlet == null)
			deadCount++;

		if((strongScarlet != null && strongScarlet.isDead()) || strongScarlet == null)
			deadCount++;

		return deadCount == 2;
	}

	/**
	 * Class ends the activity of the Bosses after a interval of time Exits the battle field in any way ...
	 * 
	 * @author Darki699, Felixx
	 */
	private static class ActivityTimeEnd implements Runnable
	{
		public void run()
		{
			setUnspawn();
		}
	}

	/**
	 * Clean Frintezza's lair.
	 */
	public static void setUnspawn()
	{
		NpcTable.getTemplate(29046).setRhand(8204);

		// delete monsters.
		bossDeadCheck(portrait1); // Deletes portrait and demon

		bossDeadCheck(portrait2); // Deletes portrait and demon

		bossDeadCheck(portrait3); // Deletes portrait and demon

		bossDeadCheck(portrait4); // Deletes portrait and demon

		try
		{
			if(frintezza != null)
			{
				frintezza.decayMe();
				frintezza.deleteMe();
			}

			if(strongScarlet != null)
			{
				strongScarlet.decayMe();
				strongScarlet.deleteMe();
			}

			if(weakScarlet != null)
			{
				weakScarlet.decayMe();
				weakScarlet.deleteMe();
			}
		}
		catch(final RuntimeException e)
		{
			e.printStackTrace();
		}

		frintezza = strongScarlet = weakScarlet = null;
		_state.setState(EpicBossState.State.DEAD);

		// delete spawns
		if(frintezzaSpawn != null)
			frintezzaSpawn.stopRespawn();
		if(scarletSpawnWeak != null)
			scarletSpawnWeak.stopRespawn();
		if(scarletSpawnStrong != null)
			scarletSpawnStrong.stopRespawn();
		if(portraitSpawn1 != null)
			portraitSpawn1.stopRespawn();
		if(portraitSpawn2 != null)
			portraitSpawn2.stopRespawn();
		if(portraitSpawn3 != null)
			portraitSpawn3.stopRespawn();
		if(portraitSpawn4 != null)
			portraitSpawn4.stopRespawn();
		if(demonSpawn1 != null)
			demonSpawn1.stopRespawn();
		if(demonSpawn2 != null)
			demonSpawn2.stopRespawn();
		if(demonSpawn3 != null)
			demonSpawn3.stopRespawn();
		if(demonSpawn4 != null)
			demonSpawn4.stopRespawn();

		frintezzaSpawn = scarletSpawnWeak = scarletSpawnStrong = null;

		portraitSpawn1 = portraitSpawn2 = portraitSpawn3 = portraitSpawn4 = null;

		demonSpawn1 = demonSpawn2 = demonSpawn3 = demonSpawn4 = null;

		// not executed tasks are canceled.
		if(_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		if(_intervalEndTask != null)
		{
			_intervalEndTask.cancel(true);
			_intervalEndTask = null;
		}
		if(_activityTimeEndTask != null)
		{
			_activityTimeEndTask.cancel(true);
			_activityTimeEndTask = null;
		}

		// interval begin.... Count until Frintezza is ready to respawn again.
		setIntervalEndTask();
	}

	/**
	 * Creates a thread to initialize Frintezza again... until this loops ends, no one can enter the lair.
	 */
	public static void setIntervalEndTask()
	{
		if( !_state.getState().equals(EpicBossState.State.INTERVAL))
		{
			_state.setRespawnDate(_intervalOfBoss);
			_state.setState(EpicBossState.State.INTERVAL);
			_state.update();
		}
		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	/**
	 * Calls for a re-initialization when time comes, only then can players enter the lair.
	 * 
	 * @author Darki699, Felixx
	 */

	private static class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(EpicBossState.State.NOTSPAWN);
			_state.update();
		}
	}

	/**
	 * Class used to make the monster/boss mobile again.
	 * 
	 * @author Darki699, Felixx
	 */
	private static class SetMobilised implements Runnable
	{
		private final L2NpcInstance _boss;

		public SetMobilised(final L2NpcInstance boss)
		{
			_boss = boss;
		}

		public void run()
		{
			_boss.setImobilised(false);
			if(_boss instanceof L2BossInstance)
				((L2BossInstance) _boss).setIsInSocialAction(false);
		}
	}

	/**
	 * Moves an L2NpcInstance to a new Position.
	 * 
	 * @author Darki699, Felixx
	 */
	private static class MoveToPos implements Runnable
	{
		private final L2NpcInstance _npc;
		private final Location _pos;

		public MoveToPos(final L2NpcInstance npc, final Location pos)
		{
			_npc = npc;
			_pos = pos;
		}

		public void run()
		{
			_npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_npc.moveToLocation(_pos, 0, true);
		}
	}

	private static List<L2Player> getPlayersInside()
	{
		return getZone().getInsidePlayers();
	}

	private static L2Player getRandomPlayer()
	{
		final List<L2Player> lst = getPlayersInside();
		if( !lst.isEmpty())
		{
			return lst.get(Rnd.get(lst.size()));
		}
		return null;
	}

	public static L2Zone getZone()
	{
		return _zone;
	}

	public void onLoad()
	{
		init();
	}

	public void onReload()
	{
		getZone().getListenerEngine().removeMethodInvokedListener(_zoneListener);
	}

	public void onShutdown()
	{}

	public static class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(final L2Zone zone, final L2Object object)
		{
			if(object.isPlayable())
			{
				setScarletSpawnTask(object);
			}
		}

		@Override
		public void objectLeaved(final L2Zone zone, final L2Object object)
		{}
	}
}