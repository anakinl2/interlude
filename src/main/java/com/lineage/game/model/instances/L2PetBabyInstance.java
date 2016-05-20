package com.lineage.game.model.instances;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Rnd;

public final class L2PetBabyInstance extends L2PetInstance
{
	boolean _thinking = false;

	public L2PetBabyInstance(int objectId, L2NpcTemplate template, L2Player owner, L2ItemInstance control, byte _currentLevel, long exp)
	{
		super(objectId, template, owner, control, _currentLevel, exp);
	}

	public L2PetBabyInstance(int objectId, L2NpcTemplate template, L2Player owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);
	}

	// heal
	private static final int HealTrick = 4717;
	private static final int GreaterHealTrick = 4718;

	public L2Skill onActionTask()
	{
		try
		{
			L2Player owner = getPlayer();
			if(owner != null && !owner.isDead() && !owner.isInvul())
			{
				L2Skill skill = null;
				if(Rnd.chance(30))
				{
					// проверка лечения
					int maxHp = owner.getMaxHp();
					double curHP = owner.getCurrentHp();
					if(curHP <= maxHp * .8)
						if(curHP < maxHp * .33) // экстренная ситуация, сильный хил
							skill = SkillTable.getInstance().getInfo(GreaterHealTrick, getHealLevel());
						else if(Rnd.chance(20)) // обычная ситуация, слабый хил, Improved Kookaburra этого скилла не имеет
							skill = SkillTable.getInstance().getInfo(HealTrick, getHealLevel());

				}

				if(skill != null && skill.checkCondition(L2PetBabyInstance.this, owner, false, !getFollowStatus(), true))
				{
					setTarget(owner);
					getAI().Cast(skill, owner, false, !getFollowStatus());
					return skill;
				}

				if(owner.isInOfflineMode() || owner.getEffectList().getEffectsCountForSkill(5771) > 0)
					return null;
			}
		}
		catch(Throwable e)
		{
			_log.warning("Pet [#" + getObjectId() + "] a buff task error has occurred: " + e);
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
	}

	@Override
	public synchronized void unSummon()
	{
		super.unSummon();
	}

	public int getHealLevel()
	{
		return Math.min(Math.max((getLevel() - 25) / 3, 1), 12);
	}

	public int getRechargeLevel()
	{
		return Math.min(Math.max((getLevel() - 55) / 3, 1), 8);
	}

	public int getBuffLevel()
	{
		return Math.min(Math.max((getLevel() - 55) / 5, 0), 5);
	}

	@Override
	public int getSoulshotConsumeCount()
	{
		return 1;
	}

	@Override
	public int getSpiritshotConsumeCount()
	{
		return 1;
	}

	@Override
	public float getExpPenalty()
	{
		return .05f;
	}

	// debuff (unused)
	@SuppressWarnings("unused")
	private static final int WindShackle = 5196, Hex = 5197, Slow = 5198, CurseGloom = 5199;

}