package l2d.game.model.instances;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.CtrlIntention;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2DropData;
import l2d.game.model.L2DropGroup;
import l2d.game.model.L2Manor;
import l2d.game.model.L2Party;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Summon;
import l2d.game.model.base.Experience;
import l2d.game.model.base.ItemToDrop;
import l2d.game.model.instances.L2ItemInstance.ItemLocation;
import l2d.game.model.quest.QuestEventType;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.MinionList;
import com.lineage.util.Rnd;
import com.lineage.util.Util;

/**
 * This class manages all Monsters.
 * L2MonsterInstance :<BR><BR>
 * <li>L2MinionInstance</li>
 * <li>L2RaidBossInstance </li>
 */
public class L2MonsterInstance extends L2NpcInstance
{
	final static Logger _log = Logger.getLogger(L2MonsterInstance.class.getName());

	protected final class RewardInfo
	{
		protected L2Character _attacker;
		protected int _dmg = 0;

		public RewardInfo(final L2Character attacker, final int dmg)
		{
			_attacker = attacker;
			_dmg = dmg;
		}

		public void addDamage(int dmg)
		{
			if(dmg < 0)
				dmg = 0;

			_dmg += dmg;
		}

		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}

	public final class AbsorberInfo
	{
		/** The attacker L2Character concerned by this AbsorberInfo of this L2NpcInstance */
		L2Player _absorber;
		int _crystalId;
		double _absorbedHP;

		AbsorberInfo(final L2Player attacker, final int crystalId, final double absorbedHP)
		{
			_absorber = attacker;
			_crystalId = crystalId;
			_absorbedHP = absorbedHP;
		}

		/**
		 * Return the Identifier of the absorber L2Character.<BR><BR>
		 */
		@Override
		public int hashCode()
		{
			return _absorber.getObjectId();
		}
	}

	private boolean _dead = false;
	private boolean _dying = false;
	private Object _dieLock = new Object();
	private Object _dyingLock = new Object();

	/** Stores the extra (over-hit) damage done to the L2NpcInstance when the attacker uses an over-hit enabled skill */
	private double _overhitDamage;

	/** Stores the attacker who used the over-hit enabled skill on the L2NpcInstance */
	private WeakReference<L2Character> overhitAttacker;

	protected MinionList minionList;

	// protected final MinionList minionList;

	private ScheduledFuture<?> minionMaintainTask;

	private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;

	private HashSet<L2ItemInstance> _inventory;

	/** crops */
	private L2ItemInstance _harvestItem;
	private Object _harvestLock = new Object();
	private L2Item _seeded;
	private WeakReference<L2Player> _seeder, _spoiler;

	/** True if a Soul Crystal was successfully used on the L2NpcInstance */
	private boolean _absorbed;

	/** The table containing all L2Player that successfully absorbed the soul of this L2NpcInstance */
	private ConcurrentHashMap<L2Player, AbsorberInfo> _absorbersList;

	/** Table containing all Items that a Dwarf can Sweep on this L2NpcInstance */
	private L2ItemInstance[] _sweepItems;
	private Object _sweepLock = new Object();

	/** Static tables containing all mobIDs that can have their souls absorbed, divided by max soul crystal level */
	private static final int[] _absorbingMOBS_level4 = {
			20583,
			20584,
			20585,
			20586,
			20587,
			20588,
			20636,
			20637,
			20638,
			20639,
			20640,
			20641,
			20642,
			20643,
			20644,
			20645 };
	private static final int[] _absorbingMOBS_level8 = { 20646, 20647, 20648, 20649, 20650, 21006, 21007, 21008 };
	private static final int[] _absorbingMOBS_level10 = {
			20627,
			20628,
			20629,
			20674,
			20761,
			20762,
			20821,
			20823,
			20826,
			20827,
			20828,
			20829,
			20830,
			20831,
			20858,
			20859,
			20860,
			21009,
			21010,
			21062,
			21063,
			21068,
			21070 };
	private static final int[] _absorbingMOBS_level12 = { 29022, 25163, 25269, 25453, 25328, 25109, 29020, 25283, 25286, 22215, 22216, 22217, 29065 };
	private static final int[] _absorbingMOBS_level13 = { 25338, 29019, 25319, 29028, 29046, 29047 };

	/** Soul Crystal Basic Informations */
	// First ID of each soul crystal
	private static final short _REDCRYSTAL_OFFSET = 4629;
	private static final short _REDCRYSTAL_LVL11 = 5577;
	private static final short _REDCRYSTAL_LVL12 = 5580;
	private static final short _REDCRYSTAL_LVL13 = 5908;
	private static final short _GREENCRYSTAL_OFFSET = 4640;
	private static final short _GREENCRYSTAL_LVL11 = 5578;
	private static final short _GREENCRYSTAL_LVL12 = 5581;
	private static final short _GREENCRYSTAL_LVL13 = 5911;
	private static final short _BLUECRYSTAL_OFFSET = 4651;
	private static final short _BLUECRYSTAL_LVL11 = 5579;
	private static final short _BLUECRYSTAL_LVL12 = 5582;
	private static final short _BLUECRYSTAL_LVL13 = 5914;

	// Max number of levels a soul crystal may reach
	private static final short _MAX_CRYSTALS_LEVEL = 13;
	/** End of Soul Crystal Basic Informations */

	// For ALT_GAME_MATHERIALSDROP
	protected static final L2DropData[] _matdrop = new L2DropData[] {
			// Item Price Chance
			new L2DropData(1864, 1, 1, 50000, 1), // Stem 100 5%
			new L2DropData(1865, 1, 1, 25000, 1), // Varnish 200 2.5%
			new L2DropData(1866, 1, 1, 16666, 1), // Suede 300 1.6666%
			new L2DropData(1867, 1, 1, 33333, 1), // Animal Skin 150 3.3333%
			new L2DropData(1868, 1, 1, 50000, 1), // Thread 100 5%
			new L2DropData(1869, 1, 1, 25000, 1), // Iron Ore 200 2.5%
			new L2DropData(1870, 1, 1, 25000, 1), // Coal 200 2.5%
			new L2DropData(1871, 1, 1, 25000, 1), // Charcoal 200 2.5%
			new L2DropData(1872, 1, 1, 50000, 1), // Animal Bone 150 5%
			new L2DropData(1873, 1, 1, 10000, 1), // Silver Nugget 500 1%
			new L2DropData(1874, 1, 1, 1666, 20), // Oriharukon Ore 3000 0.1666%
			new L2DropData(1875, 1, 1, 1666, 20), // Stone of Purity 3000 0.1666%
			new L2DropData(1876, 1, 1, 5000, 20), // Mithril Ore 1000 0.5%
			new L2DropData(1877, 1, 1, 1000, 20), // Adamantite Nugget 5000 0.1%
			new L2DropData(4039, 1, 1, 833, 40), // Mold Glue 6000 0.0833%
			new L2DropData(4040, 1, 1, 500, 40), // Mold Lubricant 10000 0.05%
			new L2DropData(4041, 1, 1, 217, 40), // Mold Hardener 23000 0.0217%
			new L2DropData(4042, 1, 1, 417, 40), // Enria 12000 0.0417%
			new L2DropData(4043, 1, 1, 833, 40), // Asofe 6000 0.0833%
			new L2DropData(4044, 1, 1, 833, 40) // Thons 6000 0.0833%
	};

	protected static final GArray<L2DropGroup> _herbs = new GArray<L2DropGroup>(3);

	static
	{
		L2DropGroup d = new L2DropGroup(0);
		d.addDropItem(new L2DropData(8600, 1, 1, 120000, 1)); // of Life 15%
		d.addDropItem(new L2DropData(8603, 1, 1, 120000, 1)); // of Mana 15%
		d.addDropItem(new L2DropData(8601, 1, 1, 40000, 1)); // Greater of Life 5%
		d.addDropItem(new L2DropData(8604, 1, 1, 40000, 1)); // Greater of Mana 5%
		d.addDropItem(new L2DropData(8602, 1, 1, 12000, 1)); // Superior of Life 1.6%
		d.addDropItem(new L2DropData(8605, 1, 1, 12000, 1)); // Superior of Mana 1.6%
		d.addDropItem(new L2DropData(8614, 1, 1, 3000, 1)); // of Recovery 0.3%
		_herbs.add(d);
		d = new L2DropGroup(0);
		d.addDropItem(new L2DropData(8611, 1, 1, 50000, 1)); // of Speed 5%
		d.addDropItem(new L2DropData(8606, 1, 1, 50000, 1)); // of Power 5%
		d.addDropItem(new L2DropData(8608, 1, 1, 50000, 1)); // of Atk. Spd. 5%
		d.addDropItem(new L2DropData(8610, 1, 1, 50000, 1)); // of Critical Attack 5%
		d.addDropItem(new L2DropData(8607, 1, 1, 50000, 1)); // of Magic 5%
		d.addDropItem(new L2DropData(8609, 1, 1, 50000, 1)); // of Casting Speed 5%
		d.addDropItem(new L2DropData(8612, 1, 1, 10000, 1)); // of Warrior 1%
		d.addDropItem(new L2DropData(8613, 1, 1, 10000, 1)); // of Mystic 1%
		_herbs.add(d);
	}

/*	protected static final L2DropData[] _toplifestones = new L2DropData[] 
	{
			new L2DropData(7917, 1, 1, 100000, 73, 75), // Top-Grade Life Stone: level 70
			new L2DropData(8762, 1, 2, 1000000, 76, 90), // Top-Grade Life Stone: level 76
	};*/
	
	/*protected static final L2DropData[] _clanEgg = new L2DropData[] 
		{ 
		new L2DropData(8753, 1, 1, 100000, 44, 46), // Top-Grade Life Stone: level 46
		new L2DropData(8754, 1, 1, 100000, 47, 49), // Top-Grade Life Stone: level 49
		new L2DropData(8755, 1, 1, 100000, 50, 52), // Top-Grade Life Stone: level 52
		new L2DropData(8756, 1, 1, 100000, 53, 55), // Top-Grade Life Stone: level 55
		new L2DropData(8757, 1, 1, 100000, 56, 58), // Top-Grade Life Stone: level 58
		new L2DropData(8758, 1, 1, 100000, 59, 61), // Top-Grade Life Stone: level 61
		new L2DropData(8759, 1, 1, 100000, 62, 66), // Top-Grade Life Stone: level 64
		new L2DropData(8760, 1, 1, 100000, 67, 72), // Top-Grade Life Stone: level 67
		new L2DropData(8761, 1, 1, 100000, 73, 75), // Top-Grade Life Stone: level 70
		new L2DropData(8762, 1, 1, 1000000, 76, 90), // Top-Grade Life Stone: level 76
		};*/

	/**
	 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            to apply to the NPC
	 */
	public L2MonsterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isMovementDisabled()
	{
		// Невозможность ходить для этих мобов
		return getNpcId() == 29045 || getNpcId() == 18344 || getNpcId() == 18345 || super.isMovementDisabled();
	}

	@Override
	public boolean isLethalImmune()
	{
		return _isChampion > 0 || getNpcId() == 22215 || getNpcId() == 22216 || getNpcId() == 22217 || super.isLethalImmune();
	}

	@Override
	public boolean isFearImmune()
	{
		return _isChampion > 0 || super.isFearImmune();
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return _isChampion > 0 || super.isParalyzeImmune();
	}

	/**
	 * Return True if the attacker is not another L2MonsterInstance.<BR><BR>
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return !attacker.isMonster();
	}

	private int _isChampion;

	public int getChampion()
	{
		return _isChampion;
	}

	public void setChampion(final int level)
	{
		if(level == 0)
		{
			removeSkillById(4407);
			_isChampion = 0;
		}
		else
		{
			addSkill(SkillTable.getInstance().getInfo(4407, level));
			_isChampion = level;
			setCurrentHp(getMaxHp(), false);
		}
	}

	@Override
	public int getTeam()
	{
		return getChampion();
	}

	@Override
	public void onSpawn()
	{
		_dead = false;
		_dying = false;
		overhitAttacker = null;
		setChampion(0);
		if(!isRaid() && getReflection().canChampions() && !(this instanceof L2ReflectionBossInstance) && !(this instanceof L2MinionInstance) && !(this instanceof L2ChestInstance) && getTemplate().revardExp > 0)
		{
			final double random = Rnd.nextDouble();
			if(Config.CHAMPION_CHANCE2 / 100 >= random)
				setChampion(2);
			else if((Config.CHAMPION_CHANCE1 + Config.CHAMPION_CHANCE2) / 100 >= random)
				setChampion(1);
			setCurrentHpMp(getMaxHp(), getMaxMp(), false);
		}
		super.onSpawn();
		spawnMinions();

		// Clear mob spoil, absorbs, seed
		setSpoiled(false, null);
		_sweepItems = null;
		_absorbed = false;
		_absorbersList = null;
		_seeded = null;
		_seeder = null;
		_spoiler = null;
	}

	protected int getMaintenanceInterval()
	{
		return MONSTER_MAINTENANCE_INTERVAL;
	}

	public MinionList getMinionList()
	{
		return minionList;
	}

	static class MinionMaintainTask implements Runnable
	{
		private final WeakReference<L2MonsterInstance> monster_ref;

		public MinionMaintainTask(final L2MonsterInstance monster)
		{
			monster_ref = new WeakReference<L2MonsterInstance>(monster);
		}

		@Override
		public void run()
		{
			final L2MonsterInstance monster = monster_ref.get();
			if(monster == null)
				return;
			try
			{
				if(monster.minionList == null)
					monster.minionList = new MinionList(monster);
				monster.minionList.maintainMinions();
			}
			catch(final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	public void spawnMinions()
	{
		if(getTemplate().getMinionData().size() > 0)
		{
			if(minionMaintainTask != null)
			{
				minionMaintainTask.cancel(true);
				minionMaintainTask = null;
			}
			try
			{
				minionMaintainTask = ThreadPoolManager.getInstance().scheduleAi(new MinionMaintainTask(this), getMaintenanceInterval(), false);
			}
			catch(final NullPointerException e)
			{}
		}
	}

	public Location getMinionPosition()
	{
		return Location.getAroundPosition(this, this, 100, 150, 10);
	}

	@Override
	public void callMinionsToAssist(final L2Character attacker)
	{
		if(minionList != null && minionList.hasMinions())
			for(final L2MinionInstance minion : minionList.getSpawnedMinions())
				if(minion != null && minion.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK && !minion.isDead())
					minion.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
	}

	public void setDead(final boolean dead)
	{
		_dead = dead;
	}

	public void removeMinions()
	{
		if(minionMaintainTask != null)
		{
			minionMaintainTask.cancel(true);
			minionMaintainTask = null;
		}
		if(minionList != null)
			minionList.maintainLonelyMinions();
		minionList = null;
	}

	public int getTotalSpawnedMinionsInstances()
	{
		return minionList == null ? 0 : minionList.countSpawnedMinions();
	}

	public void notifyMinionDied(final L2MinionInstance minion)
	{
		if(minionList != null)
			minionList.removeSpawnedMinion(minion);
	}

	public void notifyMinionSpawned(final L2MinionInstance minion)
	{
		if(minionList != null)
			minionList.addSpawnedMinion(minion);
	}

	@Override
	public boolean hasMinions()
	{
		return minionList != null && minionList.hasMinions();
	}

	@Override
	public void deleteMe()
	{
		if(hasMinions())
			removeMinions();
		super.deleteMe();
	}

	@Override
	public void doDie(final L2Character killer)
	{
		if(minionMaintainTask != null)
		{
			minionMaintainTask.cancel(true);
			minionMaintainTask = null;
		}

		if(_dead)
			return;

		synchronized (_dieLock)
		{
			if(_dead)
				return;
			_dieTime = System.currentTimeMillis();
			_dead = true;

			if(this instanceof L2ChestInstance && !((L2ChestInstance) this).isFake())
			{
				super.doDie(killer);
				return;
			}

			try
			{
				calculateRewards(killer);
			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, "", e);
				e.printStackTrace();
			}
		}
		
		if(isRaid() || isBoss())
		{
			L2Player kill = killer.getPlayer();			
			broadcastPacket(new Earthquake(getLoc(), 40, 5));
			if(kill.getClan() != null)
				Announcements.getInstance().announceToAll(getName() + " killed by " + kill.getClan().getName() + " clan.");
			else if(kill.isInParty())
				Announcements.getInstance().announceToAll(getName() + " killed by " + kill.getParty().getPartyLeader().getName() + " party.");
			else
				Announcements.getInstance().announceToAll(getName() + " killed by " + kill.getName());
			_lastmessage = 0;
		}
		super.doDie(killer);
	}

	public void calculateRewards(L2Character lastAttacker)
	{
		synchronized (_dyingLock)
		{
			_dying = true;
		}
		final ConcurrentHashMap<L2Playable, AggroInfo> aggroList = getAggroList();
		L2Character topDamager = getTopDamager(aggroList);
		if(lastAttacker == null && topDamager != null)
			lastAttacker = topDamager;
		if(lastAttacker == null || aggroList.isEmpty())
			return;
		final L2Player killer = lastAttacker.getPlayer();
		if(killer == null)
			return;

		if(topDamager == null)
			topDamager = lastAttacker;

		// Notify the Quest Engine of the L2NpcInstance death if necessary
		try
		{
			// Get the L2Player that killed the L2NpcInstance
			final L2Player playerKiller = killer.getPlayer();
			if(playerKiller == null) // маловероятно но бывает
				return;

			getTemplate().killscount++;

			if(getTemplate().hasQuestEvents())
			{
				ArrayList<L2Player> players = null;
				if(isRaid() && Config.ALT_NO_LASTHIT)
				{
					players = new ArrayList<L2Player>();
					for(final L2Playable p : aggroList.keySet())
						if(p.isPlayer())
							players.add((L2Player) p);
				}
				else if(playerKiller.getParty() != null)
					players = new ArrayList<L2Player>(playerKiller.getParty().getPartyMembers());

				if(players != null)
				{
					for(final L2Player pl : players)
						if(!pl.isDead() && (pl.isInRange(this, Config.ALT_PARTY_DISTRIBUTION_RANGE) || pl.isInRange(playerKiller, Config.ALT_PARTY_DISTRIBUTION_RANGE)))
						{
							final ArrayList<QuestState> ql = pl.getQuestsForEvent(this, QuestEventType.MOBKILLED);
							if(ql != null)
								for(final QuestState qs : ql)
									if(qs.getQuest().isParty() || pl.equals(playerKiller))
										qs.getQuest().notifyKill(this, qs);
						}
				}
				else
				{
					final ArrayList<QuestState> ql = playerKiller.getQuestsForEvent(this, QuestEventType.MOBKILLED);
					if(ql != null)
						for(final QuestState qs : ql)
							qs.getQuest().notifyKill(this, qs);
				}
			}
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}

		final int npcID = getTemplate().npcId;

		// Distribute Exp and SP rewards to L2Player (including Summon owner) that hit the L2NpcInstance and to their Party members
		final FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().setShared(true);

		for(final AggroInfo info : aggroList.values())
		{
			final L2Character attacker = info.attacker;
			L2Character owner = null;

			if(attacker.isSummon())
				owner = attacker.getPlayer();

			int damage = info.damage;

			if(npcID <= 0 || damage <= 1)
				continue;

			RewardInfo reward = rewards.get(attacker);
			if(attacker.isPlayer() || attacker.isPet())
			{
				if(reward == null)
					rewards.put(attacker, new RewardInfo(attacker, damage));
				else
					reward.addDamage(damage);
			}
			else if(owner != null)
			{
				// TODO Проверить нужно ли нам учитывать разницу в уровне суммона и хозяина.
				int levelsDiff = owner.getLevel() - attacker.getLevel();
				if(levelsDiff < 0)
					levelsDiff = 0;
				damage = damage - levelsDiff * 50;
				if(damage < 0)
					damage = 0;
				reward = rewards.get(owner);
				if(reward == null)
					rewards.put(owner, new RewardInfo(owner, damage));
				else
					reward.addDamage(damage);
			}
		}

		for(FastMap.Entry<L2Character, RewardInfo> e = rewards.head(), end = rewards.tail(); e != null && (e = e.getNext()) != end && e != null;)
		{
			final RewardInfo reward = e.getValue();
			if(reward == null)
				continue;

			final L2Character attacker = reward._attacker;

			if(attacker == null)
				continue;

			if(attacker.isDead())
				continue;

			L2Party attackerParty = null;
			if(attacker.isPlayer())
				attackerParty = ((L2Player) attacker).getParty();

			final int maxHp = getMaxHp();

			if(attackerParty == null)
			{
				int damage = reward._dmg;
				if(damage > maxHp)
					damage = maxHp;

				if(damage > 0)
				{
					int diff = attacker.getLevel() - getLevel();

					// kamael exp penalty
					if(attacker.getLevel() > 77 && diff > 3 && diff <= 5)
						diff += 3;

					double xp = 0;
					double sp = 0;

					if(isInRange(attacker, Config.ALT_PARTY_DISTRIBUTION_RANGE) && Math.abs(attacker.getZ() - getZ()) < 200)
					{
						final long[] tmp = calculateExpAndSp(diff, damage);
						xp = tmp[0];
						sp = tmp[1];
					}

					if(xp > 0)
						xp *= (isRaid() ? Config.RATE_DROP_RAIDBOSS : Config.RATE_XP) * attacker.getPlayer().getRateExp();
					if(sp > 0)
						sp *= (isRaid() ? Config.RATE_DROP_RAIDBOSS : Config.RATE_SP) * attacker.getPlayer().getRateSp();

					if(xp > 0 && getOverhitAttacker() != null && killer == getOverhitAttacker())
					{
						final int overHitExp = calculateOverhitExp(xp);
						killer.sendPacket(new SystemMessage(SystemMessage.OVER_HIT));
						killer.sendPacket(new SystemMessage(SystemMessage.ACQUIRED_S1_BONUS_EXPERIENCE_THROUGH_OVER_HIT).addNumber(overHitExp));
						xp += overHitExp;
					}

					attacker.addExpAndSp((long) xp, (long) sp, false, true);

					// Начисление душ камаэлянам
					final double neededExp = attacker.calcStat(Stats.SOULS_CONSUME_EXP, 0, null, null);
					if(neededExp > 0 && xp > neededExp)
						attacker.setConsumedSouls(attacker.getConsumedSouls() + 1, this);
				}
				rewards.remove(attacker);
			}
			else
			{
				int partyDmg = 0;
				float partyMul = 1.f;
				int partylevel = 1;

				final ArrayList<L2Player> rewardedMembers = new ArrayList<L2Player>();

				for(final L2Player partyMember : attackerParty.getPartyMembers())
				{
					if(partyMember == null || partyMember.isDead())
						continue;

					final RewardInfo ai = rewards.remove(partyMember);
					if(ai != null)
						partyDmg += ai._dmg;

					final L2Summon pet = partyMember.getPet();
					if(pet != null)
						rewards.remove(pet);

					rewardedMembers.add(partyMember);

					if(partyMember.isInRange(lastAttacker.getPlayer(), Config.ALT_PARTY_DISTRIBUTION_RANGE) && partyMember.getLevel() > partylevel)
						partylevel = partyMember.getLevel();
				}

				if(partyDmg < maxHp)
					partyMul = (float) partyDmg / maxHp;
				else
					partyDmg = maxHp;

				if(partyDmg > 0)
				{
					long xp = 0;
					long sp = 0;

					if(attacker.knowsObject(this))
					{
						int diff = partylevel - getLevel();

						// kamael exp penalty
						if(partylevel > 77 && diff > 3 && diff <= 5)
							diff += 3;

						final long[] tmp = calculateExpAndSp(diff, partyDmg);
						xp = tmp[0];
						sp = tmp[1];
					}

					xp *= partyMul * (isRaid() ? Config.RATE_DROP_RAIDBOSS : Config.RATE_XP);
					sp *= partyMul * (isRaid() ? Config.RATE_DROP_RAIDBOSS : Config.RATE_SP);

					// Check for an over-hit enabled strike
					// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
					if(getOverhitAttacker() != null && killer == getOverhitAttacker())
					{
						final int overHitExp = calculateOverhitExp(xp);
						killer.sendPacket(new SystemMessage(SystemMessage.OVER_HIT));
						killer.sendPacket(new SystemMessage(SystemMessage.ACQUIRED_S1_BONUS_EXPERIENCE_THROUGH_OVER_HIT).addNumber(overHitExp));
						xp += overHitExp;
					}

					attackerParty.distributeXpAndSp(xp, sp, rewardedMembers, lastAttacker, this);
				}
			}
		}

		// Check the drop of a cursed weapon
		CursedWeaponsManager.getInstance().dropAttackable(this, killer);

		// Manage Base, Quests and Special Events drops of the L2NpcInstance
		doItemDrop(topDamager);

		// Manage Sweep drops of the L2NpcInstance
		if(isSpoiled())
			doSweepDrop(lastAttacker, topDamager);

		if(!isRaid() && !(this instanceof L2MinionInstance)) // С рейдов падают только топовые лайфстоны, с миньонов вообще ничего дополнительно не падает
		{
			final double chancemod = ((L2NpcTemplate) _template).rateHp * Experience.penaltyModifier(calculateLevelDiffForDrop(topDamager.getLevel()), 9);

			// Дополнительный дроп материалов
			if(Config.ALT_GAME_MATHERIALSDROP && chancemod > 0 && (!isSeeded() || _seeded.isAltSeed()))
				for(final L2DropData d : _matdrop)
					if(getLevel() >= d.getMinLevel())
					{
						final int count = Util.rollDrop(d.getMinDrop(), d.getMaxDrop(), d.getChance() * chancemod * Config.RATE_DROP_ITEMS * killer.getRateItems(), true);
						if(count > 0)
							dropItem(killer, d.getItemId(), count);
					}

			// Хербы
			if(((L2NpcTemplate) _template).isDropHerbs && chancemod > 0)
				for(final L2DropGroup h : _herbs)
				{
					final Collection<ItemToDrop> itdl = h.rollFixedQty(0, this, killer, chancemod);
					if(itdl != null)
						for(final ItemToDrop itd : itdl)
							dropItem(killer, itd.itemId, 1);
				}
		}
		else if(isBoss())
		{
			//Sancity Crystals
			dropItem(killer, 7917, 1);
			dropItem(killer, 7917, 1);
			dropItem(killer, 7917, 1);
			dropItem(killer, 7917, 1);
			
			//Few Lifestones
			dropItem(killer, 8762, 1);
			dropItem(killer, 8762, 1);
		}
		// Enhance soul crystals of the attacker if this L2NpcInstance had its soul absorbed
		try
		{
			levelSoulCrystals(killer, aggroList);
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
		_dying = false;
	}

	/**
	 * Моб уже формально мертв, но его труп еще нельзя использовать поскольку не закончен подсчет наград
	 */
	public boolean isDying()
	{
		return _dying;
	}

	public void giveItem(final L2ItemInstance item, final boolean store)
	{
		if(_inventory == null)
			_inventory = new HashSet<L2ItemInstance>();

		_inventory.add(item);

		if(store)
		{
			item.setOwnerId(getNpcId());
			item.setLocation(ItemLocation.MONSTER);
			item.updateDatabase();
		}
	}

	/**
	 * Может вернуть null если инвентарь пуст
	 */
	public HashSet<L2ItemInstance> getInventory()
	{
		return _inventory;
	}

	public void truncateInventory()
	{
		_inventory.clear();
		_inventory = null;
	}

	@Override
	public void onRandomAnimation()
	{
		// Action id для живности 1-3
		broadcastPacket(new SocialAction(getObjectId(), Rnd.get(1, 3)));
	}

	@Override
	public int getKarma()
	{
		return 0;
	}

	/**
	 * Activate the absorbed soul condition on the L2NpcInstance.<BR><BR>
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}

	/**
	 * Return True if the L2NpcInstance had his soul absorbed.<BR><BR>
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}

	/**
	 * Adds an attacker that successfully absorbed the soul of this L2NpcInstance into the _absorbersList.<BR><BR>
	 * params: attacker - a valid L2Player
	 * condition - an integer indicating the event when mob dies. This should be:
	 * = 0 - "the crystal scatters";
	 * = 1 - "the crystal failed to absorb. nothing happens";
	 * = 2 - "the crystal resonates because you got more than 1 crystal on you";
	 * = 3 - "the crystal cannot absorb the soul because the mob level is too low";
	 * = 4 - "the crystal successfuly absorbed the soul";
	 */
	public void addAbsorber(final L2Player attacker, final int crystalId)
	{
		// The attacker must not be null
		if(attacker == null)
			return;

		// This L2NpcInstance must be of one type in the _absorbingMOBS_levelXX tables.
		// OBS: This is done so to avoid triggering the absorbed conditions for mobs that can't be absorbed.
		boolean validTarget = false;
		for(final int mobId : _absorbingMOBS_level4)
			if(getNpcId() == mobId)
			{
				validTarget = true;
				break;
			}
		if(!validTarget)
			for(final int mobId : _absorbingMOBS_level8)
				if(getNpcId() == mobId)
				{
					validTarget = true;
					break;
				}
		if(!validTarget)
			for(final int mobId : _absorbingMOBS_level10)
				if(getNpcId() == mobId)
				{
					validTarget = true;
					break;
				}
		if(!validTarget)
			for(final int mobId : _absorbingMOBS_level12)
				if(getNpcId() == mobId)
				{
					validTarget = true;
					break;
				}
		if(!validTarget)
			for(final int mobId : _absorbingMOBS_level13)
				if(getNpcId() == mobId)
				{
					validTarget = true;
					break;
				}
		if(!validTarget)
			return;

		// If we have no _absorbersList initiated, do it
		AbsorberInfo ai = null;
		if(_absorbersList == null)
			_absorbersList = new ConcurrentHashMap<L2Player, AbsorberInfo>();
		else
			ai = _absorbersList.get(attacker);

		// If the L2Character attacker isn't already in the _absorbersList of this L2NpcInstance, add it
		if(ai == null)
		{
			ai = new AbsorberInfo(attacker, crystalId, getCurrentHp());
			_absorbersList.put(attacker, ai);
		}
		else
		{
			ai._absorber = attacker;
			ai._crystalId = crystalId;
			ai._absorbedHP = getCurrentHp();
		}

		absorbSoul();
	}

	/**
	 * Calculate the leveling chance of Soul Crystals based on the attacker that killed this L2NpcInstance
	 * 
	 * @param attacker
	 *            The player that last hitted (killed) this L2NpcInstance
	 */
	private void levelSoulCrystals(final L2Character attacker, final ConcurrentHashMap<L2Playable, AggroInfo> aggroList)
	{
		// Only L2Player can absorb a soul
		if(attacker == null || !attacker.isPlayer())
		{
			_absorbed = false;
			_absorbersList = null;
			return;
		}

		// Init some useful vars
		boolean success = false;
		boolean levelPartyCrystals = false;
		final L2Player killer = (L2Player) attacker;

		// Check if this L2NpcInstance isn't within any of the groups of mobs that can be absorbed
		int minCrystalLevel = 0;
		int maxCrystalLevel = 0;
		for(final int mobId : _absorbingMOBS_level4)
			if(getNpcId() == mobId)
			{
				maxCrystalLevel = 4;
				success = true;
				break;
			}
		if(!success)
			for(final int mobId : _absorbingMOBS_level8)
				if(getNpcId() == mobId)
				{
					maxCrystalLevel = 8;
					success = true;
					break;
				}
		if(!success)
			for(final int mobId : _absorbingMOBS_level10)
				if(getNpcId() == mobId)
				{
					maxCrystalLevel = 10;
					success = true;
					break;
				}
		if(!success)
			for(final int mobId : _absorbingMOBS_level12)
				if(getNpcId() == mobId)
				{
					minCrystalLevel = 10;
					maxCrystalLevel = 12;
					success = true;
					levelPartyCrystals = true;
					break;
				}
		if(!success)
			for(final int mobId : _absorbingMOBS_level13)
				if(getNpcId() == mobId)
				{
					minCrystalLevel = 12;
					maxCrystalLevel = 13;
					success = true;
					levelPartyCrystals = true;
					break;
				}

		// If this is not a valid L2NpcInstance, clears the _absorbersList and just return
		if(!success)
		{
			_absorbed = false;
			_absorbersList = null;
			return;
		}

		// If this mob is a boss, then skip some checkings
		success = true;
		if(!levelPartyCrystals)
		{
			// Fail if this L2NpcInstance isn't absorbed or there's no one in its _absorbersList
			if(!_absorbed || _absorbersList == null)
			{
				_absorbed = false;
				_absorbersList = null;
				return;
			}

			// Fail if the killer isn't in the _absorbersList of this L2NpcInstance and mob is not boss
			final AbsorberInfo ai = _absorbersList.get(killer);
			if(ai == null || ai._absorber.getObjectId() != killer.getObjectId())
				success = false;

			// Check if the soul crystal was used when HP of this L2NpcInstance wasn't higher than half of it
			if(ai != null && ai._absorbedHP > getMaxHp() / 2)
				success = false;

			if(!success)
			{
				_absorbed = false;
				_absorbersList = null;
				return;
			}
		}

		_absorbed = false;
		_absorbersList = null;

		// Now we got four choices:
		// 1- The Monster level is too low for the crystal. Nothing happens.
		// 2- Everything is correct, but it failed. Nothing happens. (57.5%)
		// 3- Everything is correct, but it failed. The crystal scatters. A sound event is played. (10%)
		// 4- Everything is correct, the crystal level up. A sound event is played. (32.5%)

		GArray<L2Player> players = null;
		if(levelPartyCrystals)
			if(Config.ALT_NO_LASTHIT)
			{
				players = new GArray<L2Player>();
				for(final L2Playable p : aggroList.keySet())
					if(p.isPlayer())
						players.add((L2Player) p);
			}
			else if(killer.isInParty())
				players = killer.getParty().getPartyMembers();

		if(players == null)
		{
			players = new GArray<L2Player>();
			players.add(killer);
		}

		for(final L2Player player : players)
		{
			if(!player.isInRange(this, Config.ALT_PARTY_DISTRIBUTION_RANGE) && !player.isInRange(killer, Config.ALT_PARTY_DISTRIBUTION_RANGE))
				continue;
			int oldCrystalId = 0;
			int newCrystalId = 0;
			short crystalsCount = 0;
			byte crystalLevel = 0;
			boolean canIncreaseCrystal = false;
			boolean resonated = false;

			// Check how many soul crystals the player has in his inventory and which soul crystal he has
			for(final L2ItemInstance item : player.getInventory().getItems())
			{
				final int itemId = item.getItemId();
				if(!(itemId == _REDCRYSTAL_LVL11 || itemId == _BLUECRYSTAL_LVL11 || itemId == _GREENCRYSTAL_LVL11 || itemId == _REDCRYSTAL_LVL12 || itemId == _BLUECRYSTAL_LVL12 || itemId == _GREENCRYSTAL_LVL12 || itemId == _REDCRYSTAL_LVL13 || itemId == _BLUECRYSTAL_LVL13 || itemId == _GREENCRYSTAL_LVL13))
					if(!(itemId >= _REDCRYSTAL_OFFSET || itemId >= _GREENCRYSTAL_OFFSET || itemId >= _BLUECRYSTAL_OFFSET) || !(itemId <= _REDCRYSTAL_OFFSET + 10 || itemId <= _GREENCRYSTAL_OFFSET + 10 || itemId <= _BLUECRYSTAL_OFFSET + 10))
						continue;

				if(++crystalsCount > 1)
				{
					resonated = true;
					break;
				}

				int crystalOffset = 0;
				for(crystalLevel = 0; crystalLevel <= _MAX_CRYSTALS_LEVEL; crystalLevel++)
				{
					// Get a crystal offset to calculate the new crystal ID
					// If the crystal level is way too high for this mob, say that we can't increase it
					// Define crystals' IDs based on the crystalOffset found

					crystalOffset = getNextLevelCrystalId(itemId, crystalLevel);
					if(crystalOffset == 0)
						continue;

					canIncreaseCrystal = crystalLevel >= minCrystalLevel && crystalLevel < maxCrystalLevel;
					oldCrystalId = itemId;
					newCrystalId = crystalOffset;
					break;
				}
			}

			if(crystalsCount < 1)
				continue;

			// The player has more than one soul crystal with him, the crystal resonates and we skip this player
			if(resonated)
			{
				// player.sendPacket(new PlaySound("ItemSound2.broken_key"));
				player.sendPacket(new SystemMessage(SystemMessage.THE_SOUL_CRYSTALS_CAUSED_RESONATION_AND_FAILED_AT_ABSORBING_A_SOUL));
				continue;
			}

			// The soul crystal stage of the player is way too high, refuse to increase it
			if(!canIncreaseCrystal)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_SOUL_CRYSTAL_IS_REFUSING_TO_ABSORB_A_SOUL));
				continue;
			}

			// На анаказле, вон хелльмане, эмбере и тиранозаврах кристаллы качаются с шансом
			if(getNpcId() == 25338 || getNpcId() == 22215 || getNpcId() == 22216 || getNpcId() == 22217 || getNpcId() == 25328 || getNpcId() == 25319)
				levelPartyCrystals = false;

			// If the killer succeeds or it is a boss mob, level up the crystal
			if(levelPartyCrystals || Rnd.chance(Config.SOUL_RATE_CHANCE))
			{
				final L2ItemInstance oldCrystal = player.getInventory().getItemByItemId(oldCrystalId);
				if(oldCrystal == null)
					continue;

				final L2ItemInstance newCrystal = ItemTable.getInstance().createItem(newCrystalId);
				player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(oldCrystal.getItemId()));
				player.getInventory().destroyItem(oldCrystal, 1, true);
				player.sendPacket(new SystemMessage(SystemMessage.THE_SOUL_CRYSTAL_SUCCEEDED_IN_ABSORBING_A_SOUL));
				player.getInventory().addItem(newCrystal);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(newCrystalId));

				// извещаем окружающих если получен кристал выше 10-ого уровня
				if(!player.isInvisible())
				{
					String newCrystalColor = null;
					switch(newCrystalId)
					{
						case _REDCRYSTAL_LVL11:
						case _REDCRYSTAL_LVL12:
						case _REDCRYSTAL_LVL13:
							newCrystalColor = "red";
							break;
						case _BLUECRYSTAL_LVL11:
						case _BLUECRYSTAL_LVL12:
						case _BLUECRYSTAL_LVL13:
							newCrystalColor = "blue";
							break;
						case _GREENCRYSTAL_LVL11:
						case _GREENCRYSTAL_LVL12:
						case _GREENCRYSTAL_LVL13:
							newCrystalColor = "green";
							break;
					}
					if(newCrystalColor != null)
					{
						int newCrystalLvl = 0;
						switch(newCrystalId)
						{
							case _REDCRYSTAL_LVL11:
							case _BLUECRYSTAL_LVL11:
							case _GREENCRYSTAL_LVL11:
								newCrystalLvl = 11;
								break;
							case _REDCRYSTAL_LVL12:
							case _BLUECRYSTAL_LVL12:
							case _GREENCRYSTAL_LVL12:
								newCrystalLvl = 12;
								break;
							case _REDCRYSTAL_LVL13:
							case _BLUECRYSTAL_LVL13:
							case _GREENCRYSTAL_LVL13:
								newCrystalLvl = 13;
								break;
						}
						final CustomMessage cm = new CustomMessage("l2d.game.model.instances.L2MonsterInstance.levelSoulCrystals", player);
						cm.addCharName(player).addString(newCrystalColor).addNumber(newCrystalLvl);
						player.broadcastPacketToOthers(SystemMessage.sendString(cm.toString()));
					}
				}
				continue;
			}

			player.sendPacket(new SystemMessage(SystemMessage.THE_SOUL_CRYSTAL_IS_REFUSING_TO_ABSORB_A_SOUL));
		}
	}

	private int getNextLevelCrystalId(int crystalId, final int crystalLevel)
	{
		switch(crystalLevel)
		{
			case 10:
				if(crystalId == _REDCRYSTAL_OFFSET + 10)
					return _REDCRYSTAL_LVL11;
				if(crystalId == _GREENCRYSTAL_OFFSET + 10)
					return _GREENCRYSTAL_LVL11;
				if(crystalId == _BLUECRYSTAL_OFFSET + 10)
					return _BLUECRYSTAL_LVL11;
				break;
			case 11:
				if(crystalId == _REDCRYSTAL_LVL11)
					return _REDCRYSTAL_LVL12;
				if(crystalId == _GREENCRYSTAL_LVL11)
					return _GREENCRYSTAL_LVL12;
				if(crystalId == _BLUECRYSTAL_LVL11)
					return _BLUECRYSTAL_LVL12;
				break;
			case 12:
				if(crystalId == _REDCRYSTAL_LVL12)
					return _REDCRYSTAL_LVL13;
				if(crystalId == _GREENCRYSTAL_LVL12)
					return _GREENCRYSTAL_LVL13;
				if(crystalId == _BLUECRYSTAL_LVL12)
					return _BLUECRYSTAL_LVL13;
				break;
		}
		if(crystalId == _REDCRYSTAL_OFFSET + crystalLevel || crystalId == _GREENCRYSTAL_OFFSET + crystalLevel || crystalId == _BLUECRYSTAL_OFFSET + crystalLevel)
			return ++crystalId;

		return 0;
	}

	public L2ItemInstance takeHarvest()
	{
		synchronized (_harvestLock)
		{
			final L2ItemInstance harvest = _harvestItem;
			_harvestItem = null;
			_seeded = null;
			_seeder = null;
			return harvest;
		}
	}

	public void setSeeded(final L2Item seed, final L2Player player)
	{
		if(player == null)
			return;

		_seeded = seed;
		_seeder = new WeakReference<L2Player>(player);

		synchronized (_harvestLock)
		{
			_harvestItem = ItemTable.getInstance().createItem(L2Manor.getInstance().getCropType(seed.getItemId()));
			// Количество всходов от xHP до (xHP + xHP/2)
			if(getTemplate().rateHp <= 1)
				_harvestItem.setCount(1);
			else
				_harvestItem.setCount(Rnd.get((int) Math.round(getTemplate().rateHp * Config.RATE_MANOR), (int) Math.round(1.5 * getTemplate().rateHp * Config.RATE_MANOR)));
		}
	}

	public boolean isSeeded(final L2Player seeder)
	{
		return _seeder != null && (_seeder.get() != null && seeder == _seeder.get() || _dieTime + 10000 < System.currentTimeMillis());
	}

	public boolean isSeeded()
	{
		return _seeded != null;
	}

	/** True if a Dwarf has used Spoil on this L2NpcInstance */
	private boolean _isSpoiled;

	/**
	 * Return True if this L2NpcInstance has drops that can be sweeped.<BR><BR>
	 */
	public boolean isSpoiled()
	{
		synchronized (_sweepLock)
		{
			return _isSpoiled;
		}
	}

	public boolean isSpoiled(final L2Player spoiler)
	{
		synchronized (_sweepLock)
		{
			if(!_isSpoiled) // если не заспойлен то false
				return false;
			if(_spoiler == null || _spoiler.get() == null) // если определить заспойлившего невозможно то true
				return true;
			if(spoiler.getObjectId() == _spoiler.get().getObjectId()) // если id совпадают о true
				return true;
			if(_dieTime + 10000 < System.currentTimeMillis()) // Если прошло больше 10 секунд то можно
				return true;
			if(this.getDistance(_spoiler.get()) > Config.ALT_PARTY_DISTRIBUTION_RANGE) // если спойлер слишком далеко разрешать
				return true;
			if(spoiler.getParty() != null && spoiler.getParty().containsMember(_spoiler.get())) // сопартийцам тоже можно
				return true;
		}
		return false;
	}

	/**
	 * Set the spoil state of this L2NpcInstance.<BR><BR>
	 * 
	 * @param spoiler
	 */
	public void setSpoiled(final boolean isSpoiled, final L2Player spoiler)
	{
		synchronized (_sweepLock)
		{
			_isSpoiled = isSpoiled;
			if(spoiler != null)
				_spoiler = new WeakReference<L2Player>(spoiler);
			else
				_spoiler = null;
		}
	}

	public void doItemDrop(final L2Character topDamager)
	{
		final L2Player player = topDamager.getPlayer();

		if(player == null)
			return;

		final double mod = calcStat(Stats.DROP, 1., null, null);

		HashMap<Integer, Integer> d2 = new HashMap<Integer, Integer>();

		if(getTemplate().getDropData() != null)
		{
			final FastList<ItemToDrop> drops = getTemplate().getDropData().rollDrop(calculateLevelDiffForDrop(topDamager.getLevel()), this, player, mod);
			for(final ItemToDrop drop : drops)
			{
				// Если в моба посеяно семя, причем не альтернативное - не давать никакого дропа, кроме адены.
				if(_seeded != null && !_seeded.isAltSeed() && !drop.isAdena)
					continue;

				if(ItemTable.getInstance().createDummyItem(drop.itemId).isStackable())
				{
					if(d2.containsKey(drop.itemId))
						d2.put((int) drop.itemId, d2.get(drop.itemId) + drop.count);
					else
						d2.put((int) drop.itemId, drop.count);
				}
				else
				{
					d2.put((int) drop.itemId, drop.count);
				}
			}
		}

		if(d2.size() > 0)
		{
			for(int id : d2.keySet())
			{
				dropItem(player, id, d2.get(id));
			}
			player.sendMaterialsList();
		}

		if(_inventory != null)
		{
			for(final L2ItemInstance drop : _inventory)
				if(drop != null)
				{
					player.sendMessage(new CustomMessage("l2d.game.model.instances.L2MonsterInstance.ItemBelongedToOther", player).addString(drop.getItem().getName()));
					dropItem(player, drop);
				}
			truncateInventory();
		}
	}

	private void doSweepDrop(final L2Character lastAttacker, final L2Character topDamager)
	{
		final L2Player player = lastAttacker.getPlayer();

		if(player == null)
			return;

		final int levelDiff = calculateLevelDiffForDrop(topDamager.getLevel());

		final ArrayList<L2ItemInstance> spoiled = new ArrayList<L2ItemInstance>();

		if(getTemplate().getDropData() != null)
		{
			final double mod = calcStat(Stats.DROP, 1., null, null);
			final FastList<ItemToDrop> spoils = getTemplate().getDropData().rollSpoil(levelDiff, this, player, mod);
			for(final ItemToDrop spoil : spoils)
			{
				final L2ItemInstance dropit = ItemTable.getInstance().createItem(spoil.itemId);
				dropit.setCount(spoil.count);
				spoiled.add(dropit);
			}
		}

		if(spoiled.size() > 0)
			_sweepItems = spoiled.toArray(new L2ItemInstance[spoiled.size()]);
	}

	private long[] calculateExpAndSp(final long diff, final long damage)
	{
		long xp = getExpReward() * damage / getMaxHp();
		long sp = getSpReward() * damage / getMaxHp();

		if(diff > 5)
		{
			final double mod = Math.pow(.83, diff - 5);
			xp *= mod;
			sp *= mod;
		}

		if(xp < 0)
			xp = 0;
		if(sp < 0)
			sp = 0;

		return new long[] { xp, sp };
	}

	public L2Character getOverhitAttacker()
	{
		if(overhitAttacker == null)
			return null;
		final L2Character c = overhitAttacker.get();
		if(c == null)
			overhitAttacker = null;
		return c;
	}

	@Override
	public void setOverhitAttacker(final L2Character overhitAttacker)
	{
		this.overhitAttacker = new WeakReference<L2Character>(overhitAttacker);
	}

	public double getOverhitDamage()
	{
		return _overhitDamage;
	}

	@Override
	public void setOverhitDamage(final double damage)
	{
		_overhitDamage = damage;
	}

	public int calculateOverhitExp(final double normalExp)
	{
		double overhitPercentage = getOverhitDamage() * 100 / getMaxHp();
		if(overhitPercentage > 25)
			overhitPercentage = 25;
		final double overhitExp = overhitPercentage / 100 * normalExp;
		overhitAttacker = null;
		setOverhitDamage(0);
		return (int) Math.round(overhitExp);
	}

	/**
	 * Return True if a Dwarf use Sweep on the L2NpcInstance and if item can be spoiled.<BR><BR>
	 */
	public boolean isSweepActive()
	{
		synchronized (_dyingLock)
		{
			return _sweepItems != null && _sweepItems.length > 0;
		}
	}

	/**
	 * Return table containing all L2ItemInstance that can be spoiled.<BR><BR>
	 */
	public L2ItemInstance[] takeSweep()
	{
		synchronized (_sweepLock)
		{
			if(_sweepItems == null || _sweepItems.length == 0)
				return null;
			final L2ItemInstance[] sweep = _sweepItems.clone();
			_sweepItems = null;
			return sweep;
		}
	}

	/**
	 * Check Raidboss hp
	 */
	
	private long _lastmessage = 0;
	public void checkmyHpTask()
	{
		if(!isBoss())
			return;
		
		int hp = (int)getCurrentHpPercents();
		if(hp <= 90 && getMostHated() != null)
		{
			if(_lastmessage + 300000 > System.currentTimeMillis())
				return;
			_lastmessage = System.currentTimeMillis();
			L2Player mostHated = ((L2Player) getMostHated());
			if(mostHated.getClan() != null)
				Announcements.getInstance().announceToAll(mostHated.getClan().getName() + " clan attacking " + getName() + " boss " + hp + "% hp left!");
			else if(mostHated.isInParty())
				Announcements.getInstance().announceToAll(mostHated.getParty().getPartyLeader().getName() + " party attacking " + getName() + " boss " + hp + "% hp left!");
			else
				Announcements.getInstance().announceToAll(mostHated.getName() + " attacking a " + getName() + " boss " + hp + "% hp left!");
		}
	}
	
	@Override
	public boolean isInvul()
	{
		return _isInvul;
	}

	@Override
	public boolean isAggressive()
	{
		return (Config.CHAMPION_CAN_BE_AGGRO || getChampion() == 0) && super.isAggressive();
	}

	@Override
	public String getFactionId()
	{
		return Config.CHAMPION_CAN_BE_SOCIAL || getChampion() == 0 ? super.getFactionId() : "";
	}

	@Override
	public String toString()
	{
		return "Mob " + getName() + " [" + getNpcId() + "] / " + getObjectId();
	}
}