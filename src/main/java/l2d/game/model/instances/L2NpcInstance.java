package l2d.game.model.instances;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.ext.scripts.Events;
import l2d.ext.scripts.Functions;
import l2d.ext.scripts.Scripts;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.CtrlIntention;
import l2d.game.ai.L2CharacterAI;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.ClanHallManager;
import l2d.game.instancemanager.DimensionalRiftManager;
import l2d.game.instancemanager.LotteryManager;
import l2d.game.instancemanager.QuestManager;
import l2d.game.instancemanager.TownManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.L2WorldRegion;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.base.ClassId;
import l2d.game.model.base.L2EnchantSkillLearn;
import l2d.game.model.base.Race;
import l2d.game.model.entity.Hero;
import l2d.game.model.entity.SevenSigns;
import l2d.game.model.entity.olympiad.Olympiad;
import l2d.game.model.entity.residence.Castle;
import l2d.game.model.entity.residence.ClanHall;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestEventType;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.AcquireSkillList;
import l2d.game.serverpackets.ExEnchantSkillList;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.NpcInfo;
import l2d.game.serverpackets.RadarControl;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.ValidateLocation;
import l2d.game.skills.Stats;
import l2d.game.tables.ClanTable;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import l2d.game.tables.TeleportTable;
import l2d.game.tables.TeleportTable.TeleportLocation;
import l2d.game.taskmanager.DecayTaskManager;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2NpcTemplate;
import l2d.game.templates.L2Weapon;
import l2d.util.GArray;
import l2d.util.Location;
import l2d.util.Log;
import l2d.util.Rnd;
import l2d.util.Strings;

public class L2NpcInstance extends L2Character
{
	private static final Logger _log = Logger.getLogger(L2NpcInstance.class.getName());

	static int[][] _mageBuff = new int[][] {
			// minlevel maxlevel skill skilllevel
			{ 8, 24, 4322, 1 }, // windwalk
			{ 11, 23, 4323, 1 }, // shield
			{ 12, 22, 4328, 1 }, // blessthesoul
			{ 13, 21, 4329, 1 }, // acumen
			{ 14, 20, 4330, 1 }, // concentration
			{ 15, 19, 4331, 1 }, // empower
			{ 16, 19, 4338, 1 }, // life cubic
	};

	static int[][] _warrBuff = new int[][] {
			// minlevel| maxlevel| skill| level|
			{ 8, 24, 4322, 1 }, // windwalk
			{ 11, 23, 4323, 1 }, // shield
			{ 12, 22, 4324, 1 }, // btb
			{ 13, 21, 4325, 1 }, // vampirerage
			{ 14, 20, 4326, 1 }, // regeneration
			{ 15, 19, 4327, 1 }, // haste
			{ 16, 19, 4338, 1 }, // life cubic
	};
	public int minFactionNotifyInterval = 10000;
	public boolean hasChatWindow = true;
	long _dieTime = 0;

	private boolean _unAggred = false;

	private long lastFactionNotifyTime = 0;
	private int _personalAggroRange = -1;

	private int _currentLHandId;
	private int _currentRHandId;

	private double _currentCollisionRadius;
	private double _currentCollisionHeight;

	/** Нужно для отображения анимации спауна, используется в пакете NpcInfo **/
	private boolean _showSpawnAnimation = true;
	private final ClassId[] _classesToTeach;

	/** The delay after witch the attacked is stopped */
	private long _attack_timeout;
	private Location _spawnedLoc = new Location(0, 0, 0);

	private Constructor _ai_constructor;

	private L2Spawn _spawn;

	private boolean _isDecayed = false;

	private int _clanHallId = -1;

	private long _lastSocialAction;

	private boolean _isBusy;
	private String _busyMessage = "";

	static L2Character getTopDamager(final ConcurrentHashMap<L2Playable, AggroInfo> aggroList)
	{
		AggroInfo top = null;
		for(final AggroInfo aggro : aggroList.values())
			if(aggro.attacker != null && (top == null || aggro.damage > top.damage))
				top = aggro;
		return top != null ? top.attacker : null;
	}

	public static boolean canBypassCheck(L2Player player, L2NpcInstance npc)
	{
		if(npc == null || player.isActionsDisabled() || !Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || !npc.isInRange(player, 200))
		{
			player.sendActionFailed();
			return false;
		}
		return true;
	}

	public L2NpcInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);

		if(template == null)
		{
			_log.warning("No template for Npc. Please check your datapack is setup correctly.");
			throw new IllegalArgumentException();
		}

		_classesToTeach = template.getTeachInfo();
		setName(template.name);
		setTitle(template.title);

		final String implementationName = template.ai_type;

		try
		{
			if(!implementationName.equalsIgnoreCase("npc"))
				_ai_constructor = Class.forName("l2d.game.ai." + implementationName).getConstructors()[0];
		}
		catch(final Exception e)
		{
			try
			{
				_ai_constructor = Scripts.getInstance().getClasses().get("ai." + implementationName).getRawClass().getConstructors()[0];
			}
			catch(final Exception e1)
			{
				_log.warning("AI type " + template.ai_type + " not found!");
				e1.printStackTrace();
			}
		}

		// Т.к. у нас не создаются инстансы при каждом спавне, то все ок.
		// Анимируем здесь только для тех, у кого дефолтный AI. Монстров тоже исключаем.
		if(hasRandomAnimation() && _ai_constructor == null && !isMonster())
			startRandomAnimation();

		// инициализация параметров оружия
		_currentLHandId = getTemplate()._lhand;
		_currentRHandId = getTemplate()._rhand;
		// инициализация коллизий
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return Config.MAX_NPC_ANIMATION > 0;
	}

	public void startRandomAnimation()
	{
		final int interval = 1000 * Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION);
		ThreadPoolManager.getInstance().scheduleAi(new RandomAnimationTask(), interval, false);
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) _template;
	}

	protected int getMinFactionNotifyInterval()
	{
		return minFactionNotifyInterval;
	}

	@Override
	public String toString()
	{
		return "NPC " + getName() + " [" + getNpcId() + "]";
	}

	@Override
	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	public GArray<L2NpcInstance> ActiveFriendTargets(final boolean check_canSeeTarget)
	{
		final GArray<L2NpcInstance> ActiveFriends = new GArray<L2NpcInstance>();
		final L2WorldRegion region = L2World.getRegion(this);
		if(region != null && region.getObjectsSize() > 0)
			for(final L2NpcInstance obj : region.getNpcsList(new GArray<L2NpcInstance>(), getObjectId(), getReflection()))
				if(obj != null && obj.hasAI() && !obj.isDead())
					if(!check_canSeeTarget || GeoEngine.canSeeTarget(this, obj, false))
						ActiveFriends.add(obj);
		return ActiveFriends;
	}

	public GArray<L2NpcInstance> ActiveFriendTargets(final boolean active, final boolean attack)
	{
		final GArray<L2NpcInstance> ActiveFriends = new GArray<L2NpcInstance>();
		for(final L2NpcInstance obj : ActiveFriendTargets(true))
			if(attack && obj.getAI().getIntention() == AI_INTENTION_ATTACK || active && obj.getAI().getIntention() == AI_INTENTION_ACTIVE)
				ActiveFriends.add(obj);
		return ActiveFriends;
	}

	public int calculateLevelDiffForDrop(final int charLevel)
	{
		if(!Config.DEEPBLUE_DROP_RULES)
			return 0;

		final int mobLevel = getLevel();
		// According to official data (Prima), deep blue mobs are 9 or more levels below players
		final int deepblue_maxdiff = isRaid() ? Config.DEEPBLUE_DROP_RAID_MAXDIFF : Config.DEEPBLUE_DROP_MAXDIFF;

		return Math.max(charLevel - mobLevel - deepblue_maxdiff, 0);
	}

	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public byte getLevel()
	{
		return getTemplate().level;
	}

	public void callFriends(final L2Character attacker)
	{
		callFriends(attacker, 0);
	}

	public void callFriends(final L2Character attacker, final int damage)
	{
		if(isMonster())
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			if(this instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance) this).getLeader();
				if(!master.isInCombat() && !master.isDead())
					master.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, new Object[] { attacker, Rnd.get(1, 100) });
			}
			else
				master.callMinionsToAssist(attacker);
		}

		// call friend's
		if(getFactionId() != null && !getFactionId().isEmpty() && System.currentTimeMillis() - lastFactionNotifyTime > getMinFactionNotifyInterval())
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyFaction(attacker, damage), 100, false);
			lastFactionNotifyTime = System.currentTimeMillis();
		}
	}

	/**
	 * Возвращает группу социальности или пустой String (не null)
	 */
	public String getFactionId()
	{
		return getTemplate().factionId;
	}

	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object </li>
	 * <li>Remove L2Object object from _allObjects of L2World </li><BR><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
	 */
	public void deleteMe()
	{
		decayMe();
		detachAI();
		L2World.removeObject(this);
		setSpawn(null);
	}

	public void setSpawn(final L2Spawn spawn)
	{
		_spawn = spawn;
	}

	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<BR><BR>
	 */
	@Override
	public void doDie(final L2Character killer)
	{
		_dieTime = System.currentTimeMillis();
		setDecayed(false);

		if(isMonster() && (((L2MonsterInstance) this).isSeeded() || ((L2MonsterInstance) this).isSpoiled()))
			DecayTaskManager.getInstance().addDecayTask(this, 20000);
		else
			DecayTaskManager.getInstance().addDecayTask(this);

		// установка параметров оружия и коллизий по умолчанию
		_currentLHandId = getTemplate()._lhand;
		_currentRHandId = getTemplate()._rhand;
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;

		clearAggroList(false);

		super.doDie(killer);
	}

	/**
	 * Set the decayed state of this L2NpcInstance<BR><BR>
	 */
	public final void setDecayed(final boolean mode)
	{
		_isDecayed = mode;
	}

	public void clearAggroList(final boolean onlyHate)
	{
		for(final L2Playable playable : L2World.getAroundPlayables(this))
			if(playable != null)
				playable.removeFromHatelist(this, onlyHate);
	}

	public void dropItem(final L2Player lastAttacker, final L2ItemInstance item)
	{
		if(item.getCount() == 0)
			return;

		if(Config.DEBUG)
			_log.fine("Item id to drop: " + item + " amount: " + item.getCount());

		if(isRaid())
		{
			SystemMessage sm;
			if(item.getItemId() == 57)
			{
				sm = new SystemMessage(SystemMessage.S1_DIED_AND_HAS_DROPPED_S2_ADENA);
				sm.addString(getName());
				sm.addNumber(item.getCount());
			}
			else
			{
				sm = new SystemMessage(SystemMessage.S1_DIED_AND_DROPPED_S3_S2);
				sm.addString(getName());
				sm.addItemName(item.getItemId());
				sm.addNumber(item.getCount());
			}
			broadcastPacket(sm);
		}

		lastAttacker.doAutoLootOrDrop(item, this);
	}

	public void dropItem(final L2Player lastAttacker, final int itemId, final int itemCount)
	{
		if(itemCount == 0 || lastAttacker == null)
			return;

		L2ItemInstance item;

		for(int i = 1; i <= itemCount; i++)
		{
			item = ItemTable.getInstance().createItem(itemId);

			// Set the Item quantity dropped if L2ItemInstance is stackable
			if(item.isStackable())
			{
				i = itemCount; // Set so loop won't happent again
				item.setCount(itemCount); // Set item count
			}
			if(Config.DEBUG)
				_log.fine("Item id to drop: " + itemId + " amount: " + item.getCount());

			if(isRaid())
			{
				SystemMessage sm;
				if(itemId == 57)
				{
					sm = new SystemMessage(SystemMessage.S1_DIED_AND_HAS_DROPPED_S2_ADENA);
					sm.addString(getName());
					sm.addNumber(item.getCount());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.S1_DIED_AND_DROPPED_S3_S2);
					sm.addString(getName());
					sm.addItemName(itemId);
					sm.addNumber(item.getCount());
				}
				broadcastPacket(sm);
			}

			lastAttacker.doAutoLootOrDrop(item, this);
		}
	}

	public void endDecayTask()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
		onDecay();
	}

	@Override
	public synchronized void onDecay()
	{
		if(isDecayed())
			return;

		setDecayed(true);

		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();

		// Decrease its spawn counter
		if(_spawn != null)
			_spawn.decreaseCount(this);
	}

	/**
	 * Return the decayed status of this L2NpcInstance<BR><BR>
	 */
	public final boolean isDecayed()
	{
		return _isDecayed;
	}

	@Override
	public synchronized L2CharacterAI getAI()
	{
		if(_ai == null)
		{
			if(_ai_constructor != null)
				try
				{
					_ai = (L2CharacterAI) _ai_constructor.newInstance(new Object[] { this });
				}
				catch(final Exception e)
				{
					e.printStackTrace();
				}
			if(_ai == null)
				_ai = new L2CharacterAI(this);
		}
		return _ai;
	}

	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equipped in the right hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the L2NpcInstance
		final int weaponId = getTemplate()._rhand;

		if(weaponId < 1)
			return null;

		// Get the weapon item equipped in the right hand of the L2NpcInstance
		final L2Item item = ItemTable.getInstance().getTemplate(getTemplate()._rhand);

		if(!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	public long getAttackTimeout()
	{
		return _attack_timeout;
	}

	/**
	 * Return the busy message of this L2NpcInstance.<BR><BR>
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	@Override
	public boolean getChargedSoulShot()
	{
		switch(getTemplate().shots)
		{
			case SOUL:
			case SOUL_SPIRIT:
			case SOUL_BSPIRIT:
				return true;
			default:
				return false;
		}
	}

	@Override
	public int getChargedSpiritShot()
	{
		switch(getTemplate().shots)
		{
			case SPIRIT:
			case SOUL_SPIRIT:
				return 1;
			case BSPIRIT:
			case SOUL_BSPIRIT:
				return 2;
			default:
				return 0;
		}
	}

	/** Return the L2ClanHall this L2NpcInstance belongs to. */
	public ClanHall getClanHall()
	{
		if(_clanHallId < 0)
			_clanHallId = ClanHallManager.getInstance().findNearestClanHallIndex(getX(), getY(), 32768);
		return ClanHallManager.getInstance().getClanHall(_clanHallId);
	}

	@Override
	public float getColHeight()
	{
		return (float) getCollisionHeight();
	}

	/**
	 * @return Returns the zOffset.
	 */
	public double getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	@Override
	public float getColRadius()
	{
		return (float) getCollisionRadius();
	}

	/**
	 * @return Returns the collisionRadius.
	 */
	public double getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	public long getDeadTime()
	{
		if(_dieTime <= 0)
			return 0;
		return System.currentTimeMillis() - _dieTime;
	}

	public long getExpReward()
	{
		return (long) calcStat(Stats.EXP, getTemplate().revardExp, null, null);
	}

	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}

	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	public L2Character getMostHated()
	{
		final L2Character target = getAI().getAttackTarget();
		if(target != null && target.isNpc() && target.isVisible() && target != this && !target.isDead() && target.isInRange(this, 2000))
			return target;

		ConcurrentHashMap<L2Playable, AggroInfo> aggroList = getAggroList();

		final ConcurrentHashMap<L2Playable, AggroInfo> activeList = new ConcurrentHashMap<L2Playable, AggroInfo>();
		final ConcurrentHashMap<L2Playable, AggroInfo> passiveList = new ConcurrentHashMap<L2Playable, AggroInfo>();

		for(final AggroInfo ai : aggroList.values())
			if(ai.hate > 0)
			{
				final L2Playable cha = ai.attacker;
				if(cha != null)
					if(!cha.isSummon() && (cha.isStunned() || cha.isSleeping() || cha.isParalyzed() || cha.isAfraid() || cha.isBlocked()))
						passiveList.put(cha, ai);
					else
						activeList.put(cha, ai);
			}

		if(!activeList.isEmpty())
			aggroList = activeList;
		else
			aggroList = passiveList;

		AggroInfo mosthated = null;

		for(final AggroInfo ai : aggroList.values())
			if(mosthated == null)
				mosthated = ai;
			else if(mosthated.hate < ai.hate)
				mosthated = ai;

		return mosthated != null && mosthated.hate > 0 ? mosthated.attacker : null;
	}

	public ConcurrentHashMap<L2Playable, AggroInfo> getAggroList()
	{
		final ConcurrentHashMap<L2Playable, AggroInfo> temp = new ConcurrentHashMap<L2Playable, AggroInfo>();
		for(final L2Playable playable : L2World.getAroundPlayables(this))
			if(playable != null)
			{
				final HateInfo hateInfo = playable.getHateList().get(this);
				if(hateInfo != null)
				{
					final AggroInfo aggroInfo = new AggroInfo(playable);
					aggroInfo.hate = hateInfo.hate;
					aggroInfo.damage = hateInfo.damage;
					temp.put(playable, aggroInfo);
				}
			}
		return temp;
	}

	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	/**
	 * Return null (regular NPCs don't have weapons instances).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instances
		return null;
	}

	/**
	 * Return the weapon item equipped in the left hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the L2NpcInstance
		final int weaponId = getTemplate()._lhand;

		if(weaponId < 1)
			return null;

		// Get the weapon item equipped in the right hand of the L2NpcInstance
		final L2Item item = ItemTable.getInstance().getTemplate(getTemplate()._lhand);

		if(!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	public long getSpReward()
	{
		return (long) calcStat(Stats.SP, getTemplate().revardSp, null, null);
	}

	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	/**
	 * Return the position of the spawned point.<BR><BR>
	 * Может возвращать случайную точку, поэтому всегда следует кешировать результат вызова!
	 */
	public Location getSpawnedLoc()
	{
		return _spawnedLoc;
	}

	public String getTypeName()
	{
		return getClass().getSimpleName().replaceFirst("L2", "").replaceFirst("Instance", "");
	}

	/**
	 * Return True if the L2NpcInstance is aggressive (ex : L2MonsterInstance in function of aggroRange).<BR><BR>
	 */
	public boolean isAggressive()
	{
		return getAggroRange() > 0;
	}

	public int getAggroRange()
	{
		if(_unAggred)
			return 0;

		if(_personalAggroRange >= 0)
			return _personalAggroRange;

		return getTemplate().aggroRange;
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}

	/**
	 * Return the busy status of this L2NpcInstance.<BR><BR>
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	public boolean isSevenSignsMonster()
	{
		return getName().startsWith("Lilim ") || getName().startsWith("Nephilim ") || getName().startsWith("Lith ") || getName().startsWith("Gigant ");
	}

	/**
	 * Возвращает режим NPC: свежезаспавненный или нормальное состояние
	 *
	 * @return true, если NPC свежезаспавненный
	 */
	public boolean isShowSpawnAnimation()
	{
		return _showSpawnAnimation;
	}

	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	public boolean noTarget()
	{
		return getAggroList().size() == 0;
	}

	@Override
	public void onAction(final L2Player player)
	{
		if(player.getTarget() != this)
		{
			if(Config.DEBUG)
				_log.fine("new target selected:" + getObjectId());
			player.setTarget(this);
			if(player.getTarget() == this)
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				if(isAutoAttackable(player))
				{
					final StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
			}
			player.sendPacket(new ValidateLocation(this));
			player.sendActionFailed();
			return;
		}

		//if(!player.isAttackingNow())
		//player.broadcastPacket(new MoveToPawn(player, player.getTarget(), 100));

		if(Events.onAction(player, this))
			return;

		if(isAutoAttackable(player))
		{
			player.getAI().Attack(this, false);
			return;
		}

		if(!isInRange(player, INTERACTION_DISTANCE))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			return;
		}

		// С NPC нельзя разговаривать мертвым и сидя
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
			return;

		if(System.currentTimeMillis() - _lastSocialAction > 10000)
		{
			broadcastPacket(new SocialAction(getObjectId(), 2));
			_lastSocialAction = System.currentTimeMillis();
		}

		player.sendActionFailed();
		player.stopMove(false);

		if(_isBusy)
			showBusyWindow(player);
		else if(hasChatWindow)
		{
			final Quest[] qlst = getTemplate().getEventQuests(QuestEventType.NPC_FIRST_TALK);
			if(qlst == null || qlst.length == 0 || player.getQuestState(qlst[0].getName()) == null || player.getQuestState(qlst[0].getName()).isCompleted() || !qlst[0].notifyFirstTalk(this, player))
				showChatWindow(player, 0);
		}
	}

	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}

	public void showBusyWindow(final L2Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile("data/html/npcbusy.htm");
		html.replace("%npcname%", getName());
		html.replace("%playername%", player.getName());
		html.replace("%busymessage%", _busyMessage);
		player.sendPacket(html);
	}

	public void showChatWindow(final L2Player player, final int val)
	{
		final int npcId = getTemplate().npcId;
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

		if(!player.isInRangeZ(this, 200))
			return;

		if(player.getKarma() > 0 && !(this instanceof L2WarehouseInstance || this instanceof L2ResidenceManager || this instanceof L2ClanHallDoormenInstance || this instanceof L2CastleDoormenInstance))
		{
			Functions.show("*Please leave before I call the guards.*", player);
			player.sendActionFailed();
			return;
		}

		switch(npcId)
		{
			case 31111: // Gatekeeper Spirit (Disciples)
				final int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
				final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
				if(playerCabal == sealAvariceOwner && playerCabal == compWinner)
					switch(sealAvariceOwner)
					{
						case SevenSigns.CABAL_DAWN:
							filename += "spirit_dawn.htm";
							break;
						case SevenSigns.CABAL_DUSK:
							filename += "spirit_dusk.htm";
							break;
						case SevenSigns.CABAL_NULL:
							filename += "spirit_null.htm";
							break;
					}
				else
					filename += "spirit_null.htm";
				break;
			case 31112: // Gatekeeper Spirit (Disciples)
				filename += "spirit_exit.htm";
				break;
			case 31688:
				if(player.isNoble())
					filename = Olympiad.OLYMPIAD_HTML_FILE + "noble_main.htm";
				else
					filename = getHtmlPath(npcId, val);
				break;
			case 31690:
			case 31769:
			case 31770: // Monument of Heroes
			case 31771:
			case 31772:
				if(player.isHero() || Hero.getInstance().isInactiveHero(player.getObjectId()))
					filename = Olympiad.OLYMPIAD_HTML_FILE + "hero_main.htm";
				else
					filename = getHtmlPath(npcId, val);
				break;
			default:
				if(npcId >= 31093 && npcId <= 31094 || npcId >= 31172 && npcId <= 31201 || npcId >= 31239 && npcId <= 31254)
					return;
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = getHtmlPath(npcId, val);
				break;
		}

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	public String getHtmlPath(final int npcId, final int val)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		String temp = "data/html/default/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/trainer/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/lottery/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/instance/kamaloka/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}

	@Override
	public void onActionShift(final L2Player player)
	{
		if(this != player.getTarget())
		{
			if(Config.DEBUG)
				_log.fine("new target selected:" + getObjectId());
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(isAutoAttackable(player))
			{
				final StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			player.sendPacket(new ValidateLocation(this));
			player.sendActionFailed();
			Events.onActionShift(player, this);
			return;
		}

		player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

		if(Events.onActionShift(player, this))
			return;

		player.sendActionFailed();
	}

	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR><BR>
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : RequestBypassToServer</li><BR><BR>
	 *
	 * @param command
	 *            The command string received from client
	 */
	public void onBypassFeedback(final L2Player player, final String command)
	{
		if(!isInRange(player, INTERACTION_DISTANCE))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			player.sendActionFailed();
		}
		else
			try
			{
				if(command.equalsIgnoreCase("TerritoryStatus"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/merchant/territorystatus.htm");
					html.replace("%npcname%", getName());

					if(getCastle() != null && getCastle().getId() > 0)
					{
						html.replace("%castlename%", getCastle().getName());
						html.replace("%taxpercent%", String.valueOf(getCastle().getTaxPercent()));

						if(getCastle().getOwnerId() > 0)
						{
							final L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
							if(clan != null)
							{
								html.replace("%clanname%", clan.getName());
								html.replace("%clanleadername%", clan.getLeaderName());
							}
							else
							{
								html.replace("%clanname%", "unexistant clan");
								html.replace("%clanleadername%", "None");
							}
						}
						else
						{
							html.replace("%clanname%", "NPC");
							html.replace("%clanleadername%", "None");
						}
					}
					else
					{
						html.replace("%castlename%", "Open");
						html.replace("%taxpercent%", "0");

						html.replace("%clanname%", "No");
						html.replace("%clanleadername%", getName());
					}

					player.sendPacket(html);
				}
				else if(command.startsWith("Quest"))
				{
					final String quest = command.substring(5).trim();
					if(quest.length() == 0)
						showQuestWindow(player);
					else
						showQuestWindow(player, quest);
				}
				else if(command.startsWith("Chat"))
					try
					{
						final int val = Integer.parseInt(command.substring(5));
						showChatWindow(player, val);
					}
					catch(final NumberFormatException nfe)
					{
						final String filename = command.substring(5).trim();
						if(filename.length() == 0)
							showChatWindow(player, "data/html/npcdefault.htm");
						else
							showChatWindow(player, filename);
					}
				else if(command.startsWith("Loto"))
				{
					final int val = Integer.parseInt(command.substring(5));
					showLotoWindow(player, val);
				}
				else if(command.startsWith("CPRecovery"))
					makeCPRecovery(player);
				else if(command.startsWith("NpcLocationInfo"))
				{
					final int val = Integer.parseInt(command.substring(16));
					final L2NpcInstance npc = L2World.findNpcByNpcId(val);
					// Убираем флажок на карте и стрелку на компасе
					player.sendPacket(new RadarControl(2, 2, npc.getLoc()));
					// Ставим флажок на карте и стрелку на компасе
					player.sendPacket(new RadarControl(0, 1, npc.getLoc()));
				}
				else if(command.startsWith("SupportMagic"))
					makeSupportMagic(player);
				else if(command.startsWith("ProtectionBlessing"))
				{
					// Не выдаём блессиг протекшена ПКшникам.
					if(player.getKarma() > 0)
						return;
					if(player.getLevel() > 39 || player.getClassId().getLevel() >= 2)
					{
						final String content = "<html><body>Blessing of protection not available for characters whose level more than 39 or completed second class transfer.</body></html>";
						final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
						html.setHtml(content);
						player.sendPacket(html);
						return;
					}
					doCast(SkillTable.getInstance().getInfo(5182, 1), player, true);
				}
				else if(command.startsWith("Multisell") || command.startsWith("multisell"))
				{
					final String listId = command.substring(9).trim();
					L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(listId), player, getCastle() != null ? getCastle().getTaxRate() : 0);
				}
				else if(command.startsWith("EnterRift"))
				{
					final StringTokenizer st = new StringTokenizer(command);
					st.nextToken(); // no need for "enterRift"

					final Integer b1 = Integer.parseInt(st.nextToken()); // type

					DimensionalRiftManager.getInstance().start(player, b1, this);
				}
				else if(command.startsWith("ChangeRiftRoom"))
				{
					if(player.isInParty() && player.getParty().isInDimensionalRift())
						player.getParty().getDimensionalRift().manualTeleport(player, this);
					else
						DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
				}
				else if(command.startsWith("ExitRift"))
				{
					if(player.isInParty() && player.getParty().isInDimensionalRift())
						player.getParty().getDimensionalRift().manualExitRift(player, this);
					else
						DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
				}
				else if(command.equalsIgnoreCase("SkillList"))
					showSkillList(player);
				else if(command.equalsIgnoreCase("ClanSkillList"))
					showClanSkillList(player);
				else if(command.equalsIgnoreCase("FishingSkillList"))
					showFishingSkillList(player);
				else if(command.equalsIgnoreCase("EnchantSkillList"))
					showEnchantSkillList(player);
				else if(command.startsWith("Augment"))
				{
					final int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
					if(cmdChoice == 1)
					{
						player.sendPacket(new SystemMessage(SystemMessage.SELECT_THE_ITEM_TO_BE_AUGMENTED));
						player.sendPacket(Msg.ExShowVariationMakeWindow);
					}
					else if(cmdChoice == 2)
					{
						player.sendPacket(new SystemMessage(SystemMessage.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
						player.sendPacket(Msg.ExShowVariationCancelWindow);
					}
				}
				else if(command.startsWith("Link"))
					showChatWindow(player, "data/html/" + command.substring(5));
				else if(command.startsWith("Teleport"))
				{
					final int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
					final TeleportLocation[] list = TeleportTable.getInstance().getTeleportLocationList(getNpcId(), cmdChoice);
					if(list != null)
						showTeleportList(player, list);
					else
						player.sendMessage("Ссылка неисправна, сообщите администратору.");
				}
			}
			catch(final StringIndexOutOfBoundsException sioobe)
			{
				_log.info("Incorrect htm bypass! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
			catch(final NumberFormatException nfe)
			{
				_log.info("Invalid bypass to Server command parameter! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
	}

	public void showQuestWindow(final L2Player player)
	{
		// collect awaiting quests and start points
		final ArrayList<Quest> options = new ArrayList<Quest>();

		final ArrayList<QuestState> awaits = player.getQuestsForEvent(this, QuestEventType.QUEST_TALK);
		final Quest[] starts = getTemplate().getEventQuests(QuestEventType.QUEST_START);

		if(awaits != null)
			for(final QuestState x : awaits)
				if(!options.contains(x.getQuest()))
					if(x.getQuest().getQuestIntId() > 0)
						options.add(x.getQuest());

		if(starts != null)
			for(final Quest x : starts)
				if(!options.contains(x))
					if(x.getQuestIntId() > 0)
						options.add(x);

		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if(options.size() > 1)
			showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
		else if(options.size() == 1)
			showQuestWindow(player, options.get(0).getName());
		else
			showQuestWindow(player, "");
	}

	public void showQuestChooseWindow(final L2Player player, final Quest[] quests)
	{
		final StringBuffer sb = new StringBuffer();

		sb.append("<html><body><title>Talk about:</title><br>");

		for(final Quest q : quests)
			if(player.getQuestState(q.getName()) == null)
				sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\">[").append(q.getDescr()).append("]</a><br>");
			else if(player.getQuestState(q.getName()).isCompleted())
				sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\">[").append(q.getDescr()).append(" (completed)").append("]</a><br>");
			else
				sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\">[").append(q.getDescr()).append(" (in progress)").append("]</a><br>");

		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	public void showQuestWindow(final L2Player player, final String questId)
	{
		if(!player.isQuestContinuationPossible())
			return;

		try
		{
			// Get the state of the selected quest
			QuestState qs = player.getQuestState(questId);
			if(qs != null)
			{
				if(qs.isCompleted())
				{
					Functions.show(new CustomMessage("quests.QuestAlreadyCompleted", player), player);
					return;
				}
				if(qs.getQuest().notifyTalk(this, qs))
					return;
			}
			else
			{
				final Quest q = QuestManager.getQuest(questId);
				if(q != null)
				{
					// check for start point
					final Quest[] qlst = getTemplate().getEventQuests(QuestEventType.QUEST_START);
					if(qlst != null && qlst.length > 0)
						for(final Quest element : qlst)
							if(element == q)
							{
								qs = q.newQuestState(player);
								if(qs.getQuest().notifyTalk(this, qs))
									return;
								break;
							}
				}
			}

			final String content = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setHtml(content);
			player.sendPacket(html);
		}
		catch(final Exception e)
		{
			_log.warning("problem with npc text " + e);
			e.printStackTrace();
		}

		player.sendActionFailed();
	}

	/** For Lottery Manager **/
	public void showLotoWindow(final L2Player player, final int val)
	{
		final int npcId = getTemplate().npcId;
		String filename;
		SystemMessage sm;
		final NpcHtmlMessage html = new NpcHtmlMessage(player, this);

		// if loto
		if(val == 0)
		{
			filename = getHtmlPath(npcId, 1);
			html.setFile(filename);
		}

		else if(val >= 1 && val <= 21)
		{
			if(!LotteryManager.getInstance().isStarted())
			{
				/** LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD **/
				player.sendPacket(new SystemMessage(SystemMessage.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD));
				return;
			}
			if(!LotteryManager.getInstance().isSellableTickets())
			{
				/** TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE **/
				player.sendPacket(new SystemMessage(SystemMessage.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE));
				return;
			}

			filename = getHtmlPath(npcId, 5);
			html.setFile(filename);

			int count = 0;
			int found = 0;

			// counting buttons and unsetting button if found
			for(int i = 0; i < 5; i++)
				if(player.getLoto(i) == val)
				{
					// unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if(player.getLoto(i) > 0)
					count++;

			// if not rearched limit 5 and not unseted value
			if(count < 5 && found == 0 && val <= 20)
				for(int i = 0; i < 5; i++)
					if(player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}

			// setting pusshed buttons
			count = 0;
			for(int i = 0; i < 5; i++)
				if(player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if(player.getLoto(i) < 10)
						button = "0" + button;
					final String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					final String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}
			if(count == 5)
			{
				player.getVar("lang@").equalsIgnoreCase("en");
				final String search = "0\">Return";
				final String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
			else
			{
				player.getVar("lang@").equalsIgnoreCase("ru");
				final String search = "0\">Назад";
				final String replace = "22\">Вы выбрали свои цифры победитель.";
				html.replace(search, replace);
			}
			player.sendPacket(html);
		}

		if(val == 22)
		{
			if(!LotteryManager.getInstance().isStarted())
			{
				/** LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD **/
				player.sendPacket(new SystemMessage(SystemMessage.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD));
				return;
			}
			if(!LotteryManager.getInstance().isSellableTickets())
			{
				/** TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE **/
				player.sendPacket(new SystemMessage(SystemMessage.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE));
				return;
			}

			final int price = Config.LOTTERY_PRICE;
			final int lotonumber = LotteryManager.getInstance().getId();
			int enchant = 0;
			int type2 = 0;
			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) == 0)
					return;
				if(player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if(player.getAdena() < price)
			{
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}
			player.reduceAdena(price);
			sm = new SystemMessage(SystemMessage.ACQUIRED__S1_S2);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);
			final L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), (short) 4442);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem(item);
			Log.LogItem(player, Log.BuyItem, item);
			player.getInventory().getItemByItemId(57);
			if(player.getVar("lang@").equalsIgnoreCase("en"))
				html.setHtml("<html><body>Lottery Ticket Seller:<br>Thank you for playing the lottery<br>The winners will be announced at 7:00 pm <br><center><a action=\"bypass -h npc_%objectId%_Chat 0\">Back</a></center></body></html>");
			else
				html.setHtml("<html><body>Продавец лотерейных билетов:<br>Благодарим вас за участие в лотереи<br>Победители будут объявлены в 7.00 вечера <br><center><a action=\"bypass -h npc_%objectId%_Chat 0\">Назад</a></center></body></html>");
		}
		else if(val == 23) // 23 - current lottery jackpot
		{
			filename = getHtmlPath(npcId, 3);
			html.setFile(filename);
		}
		else if(val == 24)
		{
			filename = getHtmlPath(npcId, 4);
			html.setFile(filename);

			final int lotonumber = LotteryManager.getInstance().getId();
			String message = "";

			for(final L2ItemInstance item : player.getInventory().getItems())
			{
				if(item == null)
					continue;
				if(item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					final int[] numbers = LotteryManager.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for(int i = 0; i < 5; i++)
						message += numbers[i] + " ";
					final int[] check = LotteryManager.getInstance().checkTicket(item);
					if(check[0] > 0)
					{
						switch(check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if(message == "" && player.getVar("lang@").equalsIgnoreCase("en"))
				message += "There is no winning lottery ticket...<br>";
			else
				message += "Нет победителя лотерейных билетов...<br>";
			html.replace("%result%", message);
		}
		else if(val == 25)
		{
			filename = getHtmlPath(npcId, 2);
			html.setFile(filename);
		}
		else if(val > 25)
		{
			final int lotonumber = LotteryManager.getInstance().getId();
			final L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if(item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			final int[] check = LotteryManager.getInstance().checkTicket(item);

			sm = new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED);
			sm.addItemName(4442);
			player.sendPacket(sm);

			final int adena = check[1];
			if(adena > 0)
				player.addAdena(adena);
			player.getInventory().destroyItem(item, 1, true);
			return;
		}

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + LotteryManager.getInstance().getId());
		html.replace("%adena%", "" + LotteryManager.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + Config.LOTTERY_5_NUMBER_RATE * 100);
		html.replace("%prize4%", "" + Config.LOTTERY_4_NUMBER_RATE * 100);
		html.replace("%prize3%", "" + Config.LOTTERY_3_NUMBER_RATE * 100);
		html.replace("%prize2%", "" + Config.LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(LotteryManager.getInstance().getEndDate()));

		player.sendPacket(html);
		player.sendActionFailed();
	}

	public void makeCPRecovery(final L2Player player)
	{
		if(getNpcId() != 31225 && getNpcId() != 31226)
			return;
		final int neededmoney = 100;
		final int currentmoney = player.getAdena();
		if(neededmoney > currentmoney)
		{
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}
		player.reduceAdena(neededmoney);
		player.setCurrentCp(getCurrentCp() + 5000);
		player.sendPacket(new SystemMessage(SystemMessage.S1_CPS_WILL_BE_RESTORED).addString(player.getName()));
	}

	public void makeSupportMagic(final L2Player player)
	{
		// Prevent a cursed weapon weilder of being buffed
		if(player.isCursedWeaponEquipped())
			return;
		final int lvl = player.getLevel();

		if(lvl < 6)
		{
			showChatWindow(player, "data/html/default/newbie_nosupport6.htm");
			return;
		}
		if(lvl > 24)
		{
			showChatWindow(player, "data/html/default/newbie_nosupport62.htm");
			return;
		}

		FastList<L2Character> target = new FastList<L2Character>();
		target.add(player);

		if(!player.isMageClass() || player.getRace() == Race.orc && player.getClassId().getLevel() < 3)
		{
			for(final int[] buff : _warrBuff)
				if(lvl >= buff[0] && lvl <= buff[1])
				{
					broadcastPacket(new MagicSkillUse(this, player, buff[2], buff[3], 0, 0));
					callSkill(SkillTable.getInstance().getInfo(buff[2], buff[3]), target, true);
				}
		}
		else
			for(final int[] buff : _mageBuff)
				if(lvl >= buff[0] && lvl <= buff[1])
				{
					broadcastPacket(new MagicSkillUse(this, player, buff[2], buff[3], 0, 0));
					callSkill(SkillTable.getInstance().getInfo(buff[2], buff[3]), target, true);
				}

		if(Config.ALT_BUFF_SUMMON && player.getPet() != null && !player.getPet().isDead())
		{
			target.clear();
			target = new FastList<L2Character>();
			target.add(player.getPet());

			int[][] buffs;
			if(player.getPet() instanceof L2PetBabyInstance)
				buffs = _mageBuff;
			else
				buffs = _warrBuff;

			for(final int[] buff : buffs)
				if(lvl >= buff[0] && lvl <= buff[1])
				{
					broadcastPacket(new MagicSkillUse(this, player.getPet(), buff[2], buff[3], 0, 0));
					callSkill(SkillTable.getInstance().getInfo(buff[2], buff[3]), target, true);
				}
		}
	}

	public Castle getCastle()
	{
		if(Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && (getReflection().getId() != 0 || isInZone(ZoneType.offshore)))
			return null;
		return TownManager.getInstance().getClosestTown(this).getCastle();
	}

	/**
	 * this displays SkillList to the player.
	 *
	 * @param player
	 */
	public void showSkillList(final L2Player player)
	{
		if(Config.DEBUG)
			_log.fine("SkillList activated on: " + getObjectId());

		final ClassId classId = player.getClassId();

		if(classId == null)
			return;

		final int npcId = getTemplate().npcId;

		if(_classesToTeach == null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);

			return;
		}

		if(!(getTemplate().canTeach(classId) || getTemplate().canTeach(classId.getParent(player.getSex()))))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append(new CustomMessage("l2d.game.model.instances.L2NpcInstance.WrongTeacherClass", player));
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);

			return;
		}

		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.USUAL);
		int counts = 0;

		final ArrayList<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		for(final L2SkillLearn s : skills)
		{
			if(s.getItemCount() == -1)
				continue;
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null || !sk.getCanLearn(player.getClassId()) || !sk.canTeachBy(npcId))
				continue;
			final int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if(counts == 0)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);

			if(minlevel > 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN__COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
			{
				final TextBuilder sb = new TextBuilder();
				sb.append("<html><head><body>");
				sb.append("You've learned all skills for your class.");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}
		else
			player.sendPacket(asl);

		player.sendActionFailed();
	}

	public void showClanSkillList(final L2Player player)
	{
		if(Config.DEBUG)
			_log.fine("SkillList activated on: " + getObjectId());

		if(player.getClan() == null || !player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			player.sendActionFailed();
			return;
		}

		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.CLAN);
		int counts = 0;

		final L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableClanSkills(player.getClan());
		for(final L2SkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			final int cost = s.getRepCost();
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if(counts == 0)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setHtml("<html><head><body>You've learned all skills.</body></html>");
			player.sendPacket(html);
		}
		else
			player.sendPacket(asl);

		player.sendActionFailed();
	}

	public void showFishingSkillList(final L2Player player)
	{
		if(Config.DEBUG)
			_log.fine("SkillList activated on: " + getObjectId());

		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.FISHING);
		int counts = 0;

		final L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableFishingSkills(player);
		for(final L2SkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			final int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if(counts == 0)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("You've learned all skills.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else
			player.sendPacket(asl);

		player.sendActionFailed();
	}

	public void showEnchantSkillList(final L2Player player)
	{
		if(!enchantChecks(player))
			return;

		ExEnchantSkillList esl = new ExEnchantSkillList();
		int counts = 0;
		Collection<L2Skill> supers = player.getAllSkills();

		for(L2Skill zaidejoskilas : supers)
		{
			ArrayList<L2EnchantSkillLearn> enchants = SkillTreeTable.getFirstEnchantsForSkill(zaidejoskilas.getId());

			if(enchants != null)
				for(L2EnchantSkillLearn es : enchants)
				{
					counts++;
					if(zaidejoskilas.getDisplayLevel() > 100 && zaidejoskilas.getDisplayLevel() < 140 || zaidejoskilas.getDisplayLevel() > 140 && zaidejoskilas.getDisplayLevel() < 171)
						esl.addSkill(zaidejoskilas.getId(), zaidejoskilas.getDisplayLevel() + 1);
					else
						esl.addSkill(es.getId(), es.getLevel());
				}
		}

		if(counts == 0)
			player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
		else
			player.sendPacket(esl);
	}

	private boolean enchantChecks(final L2Player player)
	{
		final int npcId = getTemplate().npcId;

		if(_classesToTeach == null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return false;
		}

		if(!(getTemplate().canTeach(player.getClassId()) || getTemplate().canTeach(player.getClassId().getParent(player.getSex()))))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append(new CustomMessage("l2d.game.model.instances.L2NpcInstance.WrongTeacherClass", player));
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return false;
		}

		if(player.getClassId().getLevel() < 4)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			final TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("You must have 3rd class change quest completed.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return false;
		}

		if(player.getLevel() < 76)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
			return false;
		}

		return true;
	}

	public void showChatWindow(final L2Player player, final String filename)
	{
		player.sendPacket(new NpcHtmlMessage(player, this, filename, 0));
	}

	public void showTeleportList(final L2Player player, final TeleportLocation[] list)
	{
		final StringBuffer sb = new StringBuffer();

		sb.append("!Gatekeeper ").append(_name).append(":<br>\n");

		if(list != null)
		{
			for(final TeleportLocation tl : list)
				if(tl._item.getItemId() == 57)
				{
					float pricemod = player.getLevel() <= Config.GATEKEEPER_FREE ? 0f : Config.GATEKEEPER_MODIFIER;
					if(tl._price > 0 && pricemod > 0)
					{
						final int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
						final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						if(day != 1 && day != 7 && (hour <= 12 || hour >= 22))
							pricemod /= 2;
					}
					sb.append("[scripts_Util:Gatekeeper ").append(tl._target).append(" ").append((int) (tl._price * pricemod)).append(" @811;").append(tl._name).append("|").append(tl._name);
					if(tl._price > 0)
						sb.append(" - ").append((int) (tl._price * pricemod)).append(" Adena");
					sb.append("]<br1>\n");
				}
				else
					sb.append("[scripts_Util:QuestGatekeeper ").append(tl._target).append(" ").append(tl._price).append(" ").append(tl._item.getItemId()).append(" @811;").append(tl._name).append("|").append(tl._name).append(" - ").append(tl._price).append(" ").append(tl._item.getName()).append("]<br1>\n");
		}
		else
			sb.append("No teleports available.");

		final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setHtml(Strings.bbParse(sb.toString()));
		player.sendPacket(html);
	}

	private void onClanAttacked(final L2NpcInstance attacked_member, final L2Character attacker, final int damage)
	{
		final String my_name = getName();
		final String attacked_name = attacked_member.getName();

		if(my_name.startsWith("Lilim ") && attacked_name.startsWith("Nephilim "))
			return;
		if(my_name.startsWith("Nephilim ") && attacked_name.startsWith("Lilim "))
			return;
		if(my_name.startsWith("Lith ") && attacked_name.startsWith("Gigant "))
			return;
		if(my_name.startsWith("Gigant ") && attacked_name.startsWith("Lith "))
			return;

		getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, new Object[] { attacked_member, attacker, damage });
	}

	// У NPC всегда 2
	public void onRandomAnimation()
	{
		broadcastPacket(new SocialAction(getObjectId(), 2));
	}

	@Override
	public void onSpawn()
	{
		setDecayed(false);
		_showSpawnAnimation = false;
		_dieTime = 0;
	}

	/**
	 * Устанавливает данному npc новый aggroRange.
	 * Если установленый aggroRange < 0, то будет братся аггрорейндж с темплейта.
	 *
	 * @param aggroRange
	 *            новый agrroRange
	 */
	public void setAggroRange(final int aggroRange)
	{
		_personalAggroRange = aggroRange;
	}

	public void setAttackTimeout(final long time)
	{
		_attack_timeout = time;
	}

	/**
	 * Set the busy status of this L2NpcInstance.<BR><BR>
	 */
	public void setBusy(final boolean isBusy)
	{
		_isBusy = isBusy;
	}

	/**
	 * Set the busy message of this L2NpcInstance.<BR><BR>
	 */
	public void setBusyMessage(final String message)
	{
		_busyMessage = message;
	}

	/**
	 * @param offset
	 *            The zOffset to set.
	 */
	public void setCollisionHeight(final double offset)
	{
		_currentCollisionHeight = offset;
	}

	/**
	 * @param collisionRadius
	 *            The collisionRadius to set.
	 */
	public void setCollisionRadius(final double collisionRadius)
	{
		_currentCollisionRadius = collisionRadius;
	}

	public void setLHandId(final int newWeaponId)
	{
		_currentLHandId = newWeaponId;
	}

	public void setRHandId(final int newWeaponId)
	{
		_currentRHandId = newWeaponId;
	}

	public void setSpawnedLoc(final Location loc)
	{
		_spawnedLoc = loc;
	}

	public void setUnAggred(final boolean state)
	{
		_unAggred = state;
	}

	/**
	 * Затычка, просто рассылает пакет.
	 */
	@Override
	public boolean unChargeShots(final boolean spirit)
	{
		broadcastPacket(new MagicSkillUse(this, spirit ? 2061 : 2039, 1, 0, 0));
		return true;
	}

	/**
	 * Send a packet NpcInfo with state of abnormal effect to all visible L2Player<BR><BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		for(final L2Player _cha : L2World.getAroundPlayers(this))
			_cha.sendPacket(new NpcInfo(this, _cha));
	}

	public class NotifyFaction implements Runnable
	{
		private final L2Character _attacker;
		private final int _damage;

		NotifyFaction(final L2Character attacker, final int damage)
		{
			_attacker = attacker;
			_damage = damage;
		}

		public void run()
		{
			if(_attacker == null)
				return;
			try
			{
				final String faction_id = getFactionId();
				if(faction_id == null || faction_id.isEmpty())
					return;
				for(final L2NpcInstance npc : ActiveFriendTargets(false))
					if(npc != null && faction_id.equalsIgnoreCase(npc.getFactionId()))
						npc.onClanAttacked(L2NpcInstance.this, _attacker, _damage);
			}
			catch(final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}

	public class AggroInfo
	{
		public L2Playable attacker;
		public int hate;
		public int damage;

		AggroInfo(final L2Playable attacker)
		{
			this.attacker = attacker;
		}
	}

	public class RandomAnimationTask implements Runnable
	{
		private final int interval;

		public RandomAnimationTask()
		{
			interval = 1000 * Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION);
		}

		public void run()
		{
			if(!isDead() && !isMoving)
				onRandomAnimation();

			ThreadPoolManager.getInstance().scheduleAi(this, interval, false);
		}
	}
}