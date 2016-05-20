package com.lineage.game.skills.skillclasses;

import com.lineage.game.skills.Stats;
import com.lineage.game.skills.funcs.FuncAdd;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.instances.L2MerchantInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2SummonInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Location;

public class Summon extends L2Skill
{
	private final SummonType _summonType;

	private final float _expPenalty;
	private final int _itemConsumeIdInTime;
	private final int _itemConsumeCountInTime;
	private final int _itemConsumeDelay;
	private final int _lifeTime;

	private static enum SummonType
	{
		PET,
		CUBIC
	}

	public Summon(StatsSet set)
	{
		super(set);

		_summonType = Enum.valueOf(SummonType.class, set.getString("summonType", "PET").toUpperCase());
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_itemConsumeIdInTime = set.getInteger("itemConsumeIdInTime", 0);
		_itemConsumeCountInTime = set.getInteger("itemConsumeCountInTime", 0);
		_itemConsumeDelay = set.getInteger("itemConsumeDelay", 240) * 1000;
		_lifeTime = set.getInteger("lifeTime", 1200) * 1000;
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(_summonType == SummonType.CUBIC && !target.isPlayer())
			return false;

		L2Player player = _summonType == SummonType.CUBIC ? target.getPlayer() : activeChar.getPlayer();
		if(player == null)
			return false;

		switch(_summonType)
		{
			case CUBIC:
				if(_targetType == SkillTargetType.TARGET_SELF)
				{
					int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
					if(mastery < 0)
						mastery = 0;
					if(player.getCubics().size() > mastery && player.getCubic(getNpcId()) == null)
						return false;
				}
				break;
			case PET:
				if(player.inObserverMode())
					return false;
				if(player.getPet() != null || player.isMounted())
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
				break;
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Character> targets)
	{
		L2Player activeChar = caster.getPlayer();

		if(_summonType != SummonType.CUBIC && activeChar == null)
		{
			System.out.println("Non player character has summon skill!!! skill id: " + getId());
			return;
		}

		if(getNpcId() == 0)
		{
			caster.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}

		switch(_summonType)
		{
			case CUBIC:
				for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
				{
					L2Character targ = n.getValue();
					if(!targ.isPlayer())
						continue;
					L2Player target = (L2Player) targ;

					int mastery = target.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
					if(mastery < 0)
						mastery = 0;

					if(target.getCubics().size() > mastery && target.getCubic(getNpcId()) == null)
					{
						target.sendPacket(new SystemMessage(SystemMessage.CUBIC_SUMMONING_FAILED));
						continue;
					}

					if(getNpcId() == 3 && _lifeTime == 3600000) // novice life cubic
						target.addCubic(3, 8, 3600000, false);
					else if(getNpcId() == 3 && _level > 7) // затычка на энчант поскольку один уровень скилла занят на novice
						target.addCubic(getNpcId(), _level + 1, _lifeTime, caster != target);
					else
						target.addCubic(getNpcId(), _level, _lifeTime, caster != target);

					target.broadcastUserInfo(true);
					getEffects(caster, target, getActivateRate() > 0, false);
				}
				break;
			case PET:
				// Удаление трупа, если идет суммон из трупа.
				Location loc = null;
				if(_targetType == SkillTargetType.TARGET_CORPSE)
					for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
					{
						L2Character target = n.getValue();
						if(target != null && target.isDead() && target.isNpc())
						{
							activeChar.getAI().setAttackTarget(null);
							loc = target.getLoc();
							((L2NpcInstance) target).endDecayTask();
						}
					}

				if(activeChar.getPet() != null || activeChar.isMounted())
					return;

				L2NpcTemplate summonTemplate = NpcTable.getTemplate(getNpcId());

				if(summonTemplate == null)
				{
					System.out.println("Null summon template for skill " + this);
					return;
				}

				L2SummonInstance summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, _lifeTime, _itemConsumeIdInTime, _itemConsumeCountInTime, _itemConsumeDelay);

				summon.setTitle(activeChar.getName());
				summon.setExpPenalty(_expPenalty);
				summon.setExp(Experience.LEVEL[Math.min(summon.getLevel(), Experience.LEVEL.length - 1)]);
				summon.setCurrentHp(summon.getMaxHp(), false);
				summon.setCurrentMp(summon.getMaxMp());
				summon.setHeading(activeChar.getHeading());
				summon.setRunning();

				activeChar.setPet(summon);

				L2World.addObject(summon);

				summon.spawnMe(loc == null ? GeoEngine.findPointToStay(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 100, 150) : loc);

				if(summon.getSkillLevel(4140) > 0)
					summon.altUseSkill(SkillTable.getInstance().getInfo(4140, summon.getSkillLevel(4140)), activeChar);

				if(summon.getName().equalsIgnoreCase("Shadow"))
					summon.addStatFunc(new FuncAdd(Stats.ABSORB_DAMAGE_PERCENT, 0x40, this, 15));

				summon.setFollowStatus(true);
				summon.broadcastPetInfo();
				summon.setShowSpawnAnimation(false);
				break;
		}

		if(isSSPossible())
			caster.unChargeShots(isMagic());
	}

	public class DeleteMerchantTask implements Runnable
	{
		L2MerchantInstance _merchant;

		public DeleteMerchantTask(L2MerchantInstance merchant)
		{
			_merchant = merchant;
		}

		@Override
		public void run()
		{
			if(_merchant != null)
				_merchant.deleteMe();
		}
	}

	@Override
	public boolean isOffensive()
	{
		return _targetType == SkillTargetType.TARGET_CORPSE;
	}
}