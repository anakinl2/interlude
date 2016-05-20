package com.lineage.game.model.instances;

import java.util.concurrent.Future;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Summon;
import com.lineage.game.serverpackets.SetSummonRemainTime;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.taskmanager.DecayTaskManager;
import com.lineage.game.templates.L2NpcTemplate;

public class L2SummonInstance extends L2Summon
{
	public final int CYCLE = 5000; // in millis
	private float _expPenalty = 0;
	private int _itemConsumeIdInTime;
	private int _itemConsumeCountInTime;
	private int _itemConsumeDelay;

	private Future _disappearTask;

	private int _consumeCountdown;
	private int _lifetimeCountdown;
	private int _maxLifetime;

	public L2SummonInstance(int objectId, L2NpcTemplate template, L2Player owner, int lifetime, int consumeid, int consumecount, int consumedelay)
	{
		super(objectId, template, owner);
		setName(template.name);
		_lifetimeCountdown = _maxLifetime = lifetime;
		_itemConsumeIdInTime = consumeid;
		_itemConsumeCountInTime = consumecount;
		_consumeCountdown = _itemConsumeDelay = consumedelay;
		_disappearTask = ThreadPoolManager.getInstance().scheduleEffect(new Lifetime(this), CYCLE);
	}

	@Override
	public final byte getLevel()
	{
		return getTemplate() != null ? getTemplate().level : 0;
	}

	@Override
	public int getSummonType()
	{
		return 1;
	}

	@Override
	public int getCurrentFed()
	{
		return _lifetimeCountdown;
	}

	@Override
	public int getMaxFed()
	{
		return _maxLifetime;
	}

	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}

	@Override
	public float getExpPenalty()
	{
		return _expPenalty;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill, boolean awake, boolean standUp, boolean directHp, boolean canReflect)
	{
		if(attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle())
		{
			attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
			return;
		}

		super.reduceCurrentHp(damage, attacker, skill, awake, standUp, directHp, canReflect);

		L2Player owner = getPlayer();
		if(owner == null)
			return;

		owner.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RECEIVED_DAMAGE_OF_S3_FROM_C2).addName(this).addName(attacker).addNumber((int) damage));
	}

	class Lifetime implements Runnable
	{
		private L2SummonInstance _summon;

		Lifetime(L2SummonInstance summon)
		{
			_summon = summon;
		}

		@Override
		public void run()
		{
			L2Player owner = getPlayer();
			if(owner == null)
			{
				_disappearTask = null;
				unSummon();
				return;
			}

			int usedtime = _summon.isInCombat() ? CYCLE : CYCLE / 4;
			_lifetimeCountdown -= usedtime;

			if(_lifetimeCountdown <= 0)
			{
				owner.sendPacket(new SystemMessage(SystemMessage.SERVITOR_DISAPPEASR_BECAUSE_THE_SUMMONING_TIME_IS_OVER));
				_disappearTask = null;
				unSummon();
				return;
			}

			_consumeCountdown -= usedtime;
			if(_itemConsumeIdInTime > 0 && _itemConsumeCountInTime > 0 && _consumeCountdown <= 0)
			{
				L2ItemInstance item = owner.getInventory().getItemByItemId(_summon.getItemConsumeIdInTime());
				if(item != null && item.getCount() >= _summon.getItemConsumeCountInTime())
				{
					_consumeCountdown = _itemConsumeDelay;
					L2ItemInstance dest = owner.getInventory().destroyItemByItemId(_summon.getItemConsumeIdInTime(), _summon.getItemConsumeCountInTime(), true);
					owner.sendPacket(new SystemMessage(SystemMessage.A_SUMMONED_MONSTER_USES_S1).addItemName(dest.getItemId()));
				}
				else
				{
					owner.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITORS_STAY_THE_SERVITOR_WILL_DISAPPEAR));
					_summon.unSummon();
				}
			}

			owner.sendPacket(new SetSummonRemainTime(_summon));

			_disappearTask = ThreadPoolManager.getInstance().scheduleEffect(new Lifetime(_summon), CYCLE);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);

		if(_disappearTask != null)
		{
			_disappearTask.cancel(false);
			_disappearTask = null;
		}

		DecayTaskManager.getInstance().addDecayTask(this);
	}

	public int getItemConsumeIdInTime()
	{
		return _itemConsumeIdInTime;
	}

	public int getItemConsumeCountInTime()
	{
		return _itemConsumeCountInTime;
	}

	public int getItemConsumeDelay()
	{
		return _itemConsumeDelay;
	}

	protected synchronized void stopDisappear()
	{
		if(_disappearTask != null)
		{
			_disappearTask.cancel(true);
			_disappearTask = null;
		}
	}

	@Override
	public synchronized void unSummon()
	{
		stopDisappear();
		super.unSummon();
	}

	@Override
	public void displayHitMessage(L2Character target, int damage, boolean crit, boolean miss)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		if(crit)
			owner.sendPacket(Msg.SUMMONED_MONSTERS_CRITICAL_HIT);
		if(miss)
			owner.sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(this));
		else
			owner.sendPacket(new SystemMessage(SystemMessage.C1_HAS_GIVEN_C2_DAMAGE_OF_S3).addName(this).addName(target).addNumber(damage));
	}
}