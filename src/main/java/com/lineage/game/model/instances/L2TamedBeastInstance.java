package com.lineage.game.model.instances;

import java.util.HashMap;
import java.util.concurrent.Future;

import javolution.util.FastList;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.NpcInfo;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

// While a tamed beast behaves a lot like a pet (ingame) and does have
// an owner, in all other aspects, it acts like a mob.
// In addition, it can be fed in order to increase its duration.
// This class handles the running tasks, AI, and feed of the mob.
public final class L2TamedBeastInstance extends L2FeedableBeastInstance
{
	private int _foodSkillId;
	private static final int MAX_DISTANCE_FROM_OWNER = 2000;
	private static final int MAX_DURATION = 1200000; // 20 minutes
	private static final int DURATION_CHECK_INTERVAL = 60000; // 1 minute
	private static final int DURATION_INCREASE_INTERVAL = 20000; // 20 secs

	// (gained upon feeding)
	private static final int BUFF_INTERVAL = 5000; // 5 seconds
	private int _remainingTime = MAX_DURATION;
	private Location _homeLoc;
	private L2Player _owner;

	private Future<?> _buffTask = null;
	private Future<?> _durationCheckTask = null;

	public L2TamedBeastInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setHome(this);
	}

	public L2TamedBeastInstance(int objectId, L2NpcTemplate template, L2Player owner, int foodSkillId, Location loc)
	{
		super(objectId, template);

		setCurrentHp(getMaxHp(), false);
		setCurrentMp(getMaxMp());
		setFoodType(foodSkillId);
		setHome(loc);
		setRunning();
		spawnMe(loc);

		setOwner(owner);
	}

	public void onReceiveFood()
	{
		// Eating food extends the duration by 20secs, to a max of 20minutes
		_remainingTime = _remainingTime + DURATION_INCREASE_INTERVAL;
		if(_remainingTime > MAX_DURATION)
			_remainingTime = MAX_DURATION;
	}

	public Location getHome()
	{
		return _homeLoc;
	}

	public void setHome(Location loc)
	{
		_homeLoc = loc;
	}

	public void setHome(L2Character c)
	{
		setHome(c.getLoc());
	}

	public int getRemainingTime()
	{
		return _remainingTime;
	}

	public void setRemainingTime(int duration)
	{
		_remainingTime = duration;
	}

	public int getFoodType()
	{
		return _foodSkillId;
	}

	public void setFoodType(int foodItemId)
	{
		if(foodItemId > 0)
		{
			_foodSkillId = foodItemId;

			// start the duration checks start the buff tasks
			if(_durationCheckTask != null)
				_durationCheckTask.cancel(true);
			_durationCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckDuration(this), DURATION_CHECK_INTERVAL, DURATION_CHECK_INTERVAL);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		stopMove();

		if(_buffTask != null)
		{
			_buffTask.cancel(true);
			_buffTask = null;
		}

		if(_durationCheckTask != null)
		{
			_durationCheckTask.cancel(true);
			_durationCheckTask = null;
		}

		if(_owner != null)
		{
			_owner.setTrainedBeast(null);
			_owner = null;
		}

		_foodSkillId = 0;
		_remainingTime = 0;
	}

	@Override
	public L2Player getPlayer()
	{
		return _owner;
	}

	public void setOwner(L2Player owner)
	{
		if(owner != null)
		{
			_owner = owner;
			setTitle(owner.getName());
			owner.setTrainedBeast(this);

			for(L2Player player : L2World.getAroundPlayers(this))
				if(player != null && _objectId != player.getObjectId())
					player.sendPacket(new NpcInfo(this, player));

			// always and automatically follow the owner.
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _owner, 100);

			// instead of calculating this value each time, let's get this now and pass it on
			int totalBuffsAvailable = 0;
			for(L2Skill skill : getTemplate().getSkills().values())
				// if the skill is a buff, check if the owner has it already
				if(skill.getSkillType() == L2Skill.SkillType.BUFF)
					totalBuffsAvailable++;

			// start the buff tasks
			if(_buffTask != null)
				_buffTask.cancel(true);
			_buffTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckOwnerBuffs(this, totalBuffsAvailable), BUFF_INTERVAL, BUFF_INTERVAL);
		}
		else
			doDespawn(); // despawn if no owner
	}

	public void doDespawn()
	{
		// stop running tasks
		stopMove();
		_buffTask.cancel(true);
		_durationCheckTask.cancel(true);

		// clean up variables
		if(_owner != null)
			_owner.setTrainedBeast(null);
		setTarget(null);
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;

		// remove the spawn
		onDecay();
	}

	// notification triggered by the owner when the owner is attacked.
	// tamed mobs will heal/recharge or debuff the enemy according to their skills
	public void onOwnerGotAttacked(L2Character attacker)
	{
		// check if the owner is no longer around...if so, despawn
		if(_owner == null || !_owner.isOnline())
		{
			doDespawn();
			return;
		}
		// if the owner is too far away, stop anything else and immediately run towards the owner.
		if(!isInRange(_owner, MAX_DISTANCE_FROM_OWNER))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _owner, 100);
			return;
		}
		// if the owner is dead, do nothing...
		if(_owner.isDead())
			return;

		double HPRatio = _owner.getCurrentHpRatio();

		// if the owner has a lot of HP, then debuff the enemy with a random debuff among the available skills
		// use of more than one debuff at this moment is acceptable
		if(HPRatio >= 0.8 && attacker != null)
		{
			HashMap<Integer, L2Skill> skills = getTemplate().getSkills();
			for(L2Skill skill : skills.values())
				// if the skill is a debuff, check if the attacker has it already [ attacker.getEffect(L2Skill skill) ]
				if(skill.isOffensive() && attacker.getEffectList().getEffectsBySkill(skill) == null && Rnd.nextBoolean())
				{
					setTarget(attacker);
					doCast(skill, attacker, true);
				}
		}
		// for HP levels between 80% and 50%, do not react to attack events (so that MP can regenerate a bit)
		// for lower HP ranges, heal or recharge the owner with 1 skill use per attack.
		else if(HPRatio < 0.5)
		{
			int chance = 1;
			if(HPRatio < 0.25)
				chance = 2;

			// if the owner has a lot of HP, then debuff the enemy with a random debuff among the available skills
			HashMap<Integer, L2Skill> skills = getTemplate().getSkills();
			for(L2Skill skill : skills.values())
				if(!skill.isOffensive() && _owner.getEffectList().getEffectsBySkill(skill) == null && Rnd.chance(chance * 20))
				{
					setTarget(_owner);
					doCast(skill, _owner, true);
					return;
				}
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _owner, 100);
	}

	private class CheckDuration implements Runnable
	{
		private L2TamedBeastInstance _tamedBeast;

		CheckDuration(L2TamedBeastInstance tamedBeast)
		{
			_tamedBeast = tamedBeast;
		}

		@Override
		public void run()
		{
			int foodTypeSkillId = _tamedBeast.getFoodType();
			L2Player owner = _tamedBeast.getPlayer();
			_tamedBeast.setRemainingTime(_tamedBeast.getRemainingTime() - DURATION_CHECK_INTERVAL);

			// I tried to avoid this as much as possible...but it seems I can't avoid hardcoding
			// ids further, except by carrying an additional variable just for these two lines...
			// Find which food item needs to be consumed.
			L2ItemInstance item = null;
			if(foodTypeSkillId == 2188)
				item = owner.getInventory().getItemByItemId(6643);
			else if(foodTypeSkillId == 2189)
				item = owner.getInventory().getItemByItemId(6644);

			// if the owner has enough food, call the item handler (use the food and triffer all necessary actions)
			if(item != null && item.getCount() >= 1)
			{
				L2Object oldTarget = owner.getTarget();
				owner.setTarget(_tamedBeast);
				FastList<L2Character> targets = new FastList<L2Character>();
				targets.add(_tamedBeast);

				// emulate a call to the owner using food, but bypass all checks for range, etc
				// this also causes a call to the AI tasks handling feeding, which may call onReceiveFood as required.
				owner.callSkill(SkillTable.getInstance().getInfo(foodTypeSkillId, 1), targets, true);
				owner.setTarget(oldTarget);
			}
			else // if the owner has no food, the beast immediately despawns, except when it was only
			// newly spawned. Newly spawned beasts can last up to 5 minutes
			if(_tamedBeast.getRemainingTime() < MAX_DURATION - 300000)
				_tamedBeast.setRemainingTime(-1);

			if(_tamedBeast.getRemainingTime() <= 0)
				_tamedBeast.doDespawn();
		}
	}

	private class CheckOwnerBuffs implements Runnable
	{
		private L2TamedBeastInstance _tamedBeast;

		private int _numBuffs;

		CheckOwnerBuffs(L2TamedBeastInstance tamedBeast, int numBuffs)
		{
			_tamedBeast = tamedBeast;
			_numBuffs = numBuffs;
		}

		@Override
		public void run()
		{
			L2Player owner = _tamedBeast.getPlayer();

			// check if the owner is no longer around...if so, despawn
			if(owner == null || !owner.isOnline())
			{
				doDespawn();
				return;
			}

			setRunning();

			// if the owner is too far away, stop anything else and immediately run towards the owner.
			if(!isInRange(owner, MAX_DISTANCE_FROM_OWNER))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _tamedBeast.getPlayer(), 100);
				return;
			}
			// if the owner is dead, do nothing...
			if(owner.isDead())
				return;

			int totalBuffsOnOwner = 0;
			int i = 0;
			int rand = Rnd.get(_numBuffs);
			L2Skill buffToGive = null;

			HashMap<Integer, L2Skill> skills = _tamedBeast.getTemplate().getSkills();

			for(L2Skill skill : skills.values())
				// if the skill is a buff, check if the owner has it already
				if(skill.getSkillType() == L2Skill.SkillType.BUFF)
				{
					if(i == rand)
						buffToGive = skill;
					i++;
					if(owner.getEffectList().getEffectsBySkill(skill) != null)
						totalBuffsOnOwner++;
				}
			// if the owner has less than 60% of this beast's available buff, cast a random buff
			if(_numBuffs * 2 / 3 > totalBuffsOnOwner)
			{
				_tamedBeast.setTarget(owner);
				_tamedBeast.doCast(buffToGive, owner, true);
			}
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _tamedBeast.getPlayer(), 100);
		}
	}
}