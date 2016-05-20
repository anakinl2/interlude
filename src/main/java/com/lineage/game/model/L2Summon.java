package com.lineage.game.model;

import com.lineage.Config;
import com.lineage.ext.scripts.Events;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.ai.L2SummonAI;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.entity.Duel;
import com.lineage.game.model.instances.L2CubicInstance;
import com.lineage.game.model.instances.L2CubicInstance.CubicType;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.NpcInfo;
import com.lineage.game.serverpackets.PartySpelled;
import com.lineage.game.serverpackets.PetDelete;
import com.lineage.game.serverpackets.PetInfo;
import com.lineage.game.serverpackets.PetStatusShow;
import com.lineage.game.serverpackets.PetStatusUpdate;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.skills.Stats;
import com.lineage.game.taskmanager.DecayTaskManager;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.game.templates.L2Weapon;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public abstract class L2Summon extends L2Playable
{
	//private static final Logger _log = Logger.getLogger(L2Summon.class.getName());

	protected long _exp = 0;
	protected int _sp = 0;
	private int _attackRange = 36; //Melee range
	private boolean _follow = true;
	private int _maxLoad;
	private boolean _posessed = false;
	private boolean _showSpawnAnimation = true;

	private boolean _ssCharged = false;
	private int _spsCharged = 0;

	private static final int SIEGE_GOLEM_ID = 14737;
	private static final int SIEGE_CANNON_ID = 14768;
	private static final int SWOOP_CANNON_ID = 14839;

	private static final int SUMMON_DISAPPEAR_RANGE = 2500;

	public L2Summon(final int objectId, final L2NpcTemplate template, final L2Player owner)
	{
		super(objectId, template, owner);
		setOwner(owner);

		L2ItemInstance weapon = owner.getActiveWeaponInstance();
		if(weapon != null && weapon.getAttributeFuncTemplate() != null)
			addStatFunc(weapon.getAttributeFuncTemplate().getFunc(weapon));

		setXYZInvisible(owner.getX() + Rnd.get(-100, 100), owner.getY() + Rnd.get(-100, 100), owner.getZ());
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();
		onSpawn();
	}

	@Override
	public void onSpawn()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
	}

	@Override
	public L2SummonAI getAI()
	{
		if(_ai == null)
			_ai = new L2SummonAI(this);
		return (L2SummonAI) _ai;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) _template;
	}

	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	// this defines the action buttons, 1 for Summon, 2 for Pets
	public abstract int getSummonType();

	@Override
	public void updateAbnormalEffect()
	{
		broadcastPetInfo();
	}

	/**
	 * @return Returns the mountable.
	 */
	public boolean isMountable()
	{
		return false;
	}

	@Override
	public void onAction(final L2Player player)
	{
		L2Player owner = getPlayer();
		if(owner == null)
		{
			player.sendActionFailed();
			return;
		}

		if(Events.onAction(player, this))
			return;

		// Check if the L2Player is confused
		if(player.isConfused() || player.isBlocked())
			player.sendActionFailed();

		if(player.getTarget() != this)
		{
			// Set the target of the player
			player.setTarget(this);
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			//if(isSummon)
			//{
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			//su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			//su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
			player.sendPacket(su);
			//}
		}
		else if(player == owner)
		{
			player.sendPacket(new PetInfo(this));
			player.sendPacket(new PetStatusShow(this));
			player.sendActionFailed();
		}
		else if(isAutoAttackable(player))
		{
			// Player with lvl < 21 can't attack a cursed weapon holder
			// And a cursed weapon holder  can't attack players with lvl < 21
			if(owner.isCursedWeaponEquipped() && player.getLevel() < 21 || player.isCursedWeaponEquipped() && owner.getLevel() < 21)
				player.sendActionFailed();
			else
				player.getAI().Attack(this, false);
		}
		else if(player != owner)
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this, 100);
		else
			sendActionFailed();
	}

	public long getExpForThisLevel()
	{
		if(getLevel() >= Experience.LEVEL.length)
			return 0;
		return Experience.LEVEL[getLevel()];
	}

	public long getExpForNextLevel()
	{
		if(getLevel() + 1 >= Experience.LEVEL.length)
			return 0;
		return Experience.LEVEL[getLevel() + 1];
	}

	@Override
	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	public final long getExp()
	{
		return _exp;
	}

	public final void setExp(final long exp)
	{
		_exp = exp;
	}

	public final int getSp()
	{
		return _sp;
	}

	public void setSp(final int sp)
	{
		_sp = sp;
	}

	public int getMaxLoad()
	{
		return _maxLoad;
	}

	public void setMaxLoad(final int maxLoad)
	{
		_maxLoad = maxLoad;
	}

	@Override
	public int getBuffLimit()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return Config.ALT_BUFF_LIMIT;
		return (int) calcStat(Stats.BUFF_LIMIT, owner.getBuffLimit(), null, null);
	}

	public abstract int getCurrentFed();

	public abstract int getMaxFed();

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);

		L2Player owner = getPlayer();
		if(owner == null)
			return;

		if(killer == null || killer == owner || killer.getObjectId() == _objectId || isInZoneBattle() || killer.isInZoneBattle())
			return;

		if(killer instanceof L2Summon)
			killer = killer.getPlayer();

		if(killer == null)
			return;

		if(killer.isPlayer())
		{
			L2Player pk = (L2Player) killer;

			if(isInZone(L2Zone.ZoneType.Siege))
				return;

			if(owner.getPvpFlag() > 0 || owner.atMutualWarWith(pk))
				pk.setPvpKills(pk.getPvpKills() + 1);
			else if((getDuel() == null || getDuel() != pk.getDuel()) && getKarma() <= 0)
			{
				int pkCountMulti = Math.max(pk.getPkKills() / 2, 1);
				pk.increaseKarma(Config.KARMA_MIN_KARMA * pkCountMulti);
			}

			// Send a Server->Client UserInfo packet to attacker with its PvP Kills Counter
			pk.sendChanges();
		}
	}

	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	@Override
	public void onDecay()
	{
		deleteMe();
	}

	@Override
	public void broadcastStatusUpdate()
	{
		super.broadcastStatusUpdate();

		L2Player owner = getPlayer();
		if(owner == null)
			return;

		if(isVisible())
			owner.sendPacket(new PetStatusUpdate(this));

	}

	public void deleteMe()
	{
		setTarget(null);
		stopMove();
		decayMe();
		detachAI();

		L2Player owner = getPlayer();
		if(owner != null)
		{
			owner.sendPacket(new PetDelete(getObjectId(), 2));
			owner.setPet(null);
			setOwner(null);
		}

		L2World.removeObject(this);
	}

	public synchronized void unSummon()
	{
		deleteMe();
	}

	public int getAttackRange()
	{
		return _attackRange;
	}

	public void setAttackRange(int range)
	{
		if(range < 36)
			range = 36;
		_attackRange = range;
	}

	@Override
	public void setFollowStatus(boolean state)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;

		_follow = state;
		if(_follow)
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, owner, 100);
		else
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
	}

	public boolean getFollowStatus()
	{
		return _follow;
	}

	@Override
	public void updateEffectIcons()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;

		broadcastNpcInfo();

		PartySpelled ps = new PartySpelled(this, true);
		L2Party party = owner.getParty();
		if(party != null)
			party.broadcastToPartyMembers(ps);
		else
			owner.sendPacket(ps);
	}

	/**
	 * @return Returns the showSpawnAnimation.
	 */
	public boolean isShowSpawnAnimation()
	{
		return _showSpawnAnimation;
	}

	/**
	 * Sets showSpawnAnimation.
	 */
	public void setShowSpawnAnimation(boolean showSpawnAnimation)
	{
		_showSpawnAnimation = showSpawnAnimation;
	}

	public int getControlItemObjId()
	{
		return 0;
	}

	public L2Weapon getActiveWeapon()
	{
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return null;
	}

	@Override
	public void doPickupItem(final L2Object object)
	{}

	/**
	 * Return null.<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	public abstract void displayHitMessage(L2Character target, int damage, boolean crit, boolean miss);

	@Override
	public boolean unChargeShots(final boolean spirit)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return false;

		if(spirit)
		{
			if(_spsCharged != 0)
			{
				_spsCharged = 0;
				owner.AutoShot();
				return true;
			}
		}
		else if(_ssCharged)
		{
			_ssCharged = false;
			owner.AutoShot();
			return true;
		}

		return false;
	}

	@Override
	public boolean getChargedSoulShot()
	{
		return _ssCharged;
	}

	@Override
	public int getChargedSpiritShot()
	{
		return _spsCharged;
	}

	public void chargeSoulShot()
	{
		_ssCharged = true;
	}

	public void chargeSpiritShot(final int state)
	{
		_spsCharged = state;
	}

	public int getSoulshotConsumeCount()
	{
		return getLevel() / 27 + 1;
	}

	public int getSpiritshotConsumeCount()
	{
		return getLevel() / 58 + 1;
	}

	@Override
	public void doAttack(final L2Character target)
	{
		super.doAttack(target);
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		for(L2CubicInstance cubic : owner.getCubics())
			if(cubic.getType() != CubicType.LIFE_CUBIC)
				cubic.doAction(target);
	}

	@Override
	public void doCast(final L2Skill skill, final L2Character target, boolean forceUse)
	{
		super.doCast(skill, target, forceUse);
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		if(skill.isOffensive() && target != null)
			for(L2CubicInstance cubic : owner.getCubics())
				if(cubic.getType() != CubicType.LIFE_CUBIC)
					cubic.doAction(target);
	}

	public boolean isPosessed()
	{
		return _posessed;
	}

	public void setPossessed(final boolean possessed)
	{
		_posessed = possessed;
	}

	public boolean isInRange()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return false;
		return getDistance(owner) < SUMMON_DISAPPEAR_RANGE;
	}

	public void teleportToOwner()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		setLoc(Location.getAroundPosition(owner, owner, 50, 150, 10));
		if(_follow)
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, owner, 100);
		updateEffectIcons();
	}

	public void broadcastNpcInfo()
	{
		for(L2Player player : L2World.getAroundPlayers(this))
			if(player != null)
				player.sendPacket(new NpcInfo(this, player, _showSpawnAnimation));
	}

	public void broadcastPetInfo()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		for(L2Player player : L2World.getAroundPlayers(this))
			if(player != null)
				if(player == owner)
					player.sendPacket(new PetInfo(this, _showSpawnAnimation));
				else
					player.sendPacket(new NpcInfo(this, player, _showSpawnAnimation));
	}

	@Override
	public void startPvPFlag(L2Character target)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		owner.startPvPFlag(target);
	}

	@Override
	public int getPvpFlag()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return 0;
		return owner.getPvpFlag();
	}

	@Override
	public Duel getDuel()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return null;
		return owner.getDuel();
	}

	@Override
	public int getTeam()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return 0;
		return owner.getTeam();
	}

	public boolean isSiegeWeapon()
	{
		return getNpcId() == SIEGE_GOLEM_ID || getNpcId() == SIEGE_CANNON_ID || getNpcId() == SWOOP_CANNON_ID;
	}

	public abstract float getExpPenalty();
}