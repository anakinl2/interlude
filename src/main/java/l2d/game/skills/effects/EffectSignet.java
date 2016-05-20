package l2d.game.skills.effects;

import javolution.util.FastList;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.instances.L2EffectPointInstance;
import l2d.game.serverpackets.MagicSkillLaunched;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;

public class EffectSignet extends L2Effect
{
	private L2Skill _skill;
	private L2EffectPointInstance _actor;
	private int _count = 0;
	private boolean _force;

	public EffectSignet(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.Signet;
	}

	@Override
	public void onStart()
	{
		_skill = SkillTable.getInstance().getInfo(getSkill().getTriggerEffectId(), getSkill().getTriggerEffectLevel());

		if(_skill == null)
			return;

		if(getEffector().isDead() || getEffector() == null || !getEffector().getPlayer().isOnline())
		{
			onExit();
			return;
		}

		final L2NpcTemplate template = NpcTable.getTemplate(getSkill().getEffectNpcId());

		if(template == null)
			return;

		try
		{
			Location loc = getEffector().getLoc();
			if(getEffector().isPlayer() && ((L2Player) getEffector()).getGroundSkillLoc() != null)
			{
				loc = ((L2Player) getEffector()).getGroundSkillLoc();
				((L2Player) getEffector()).setGroundSkillLoc(null);
			}

			final L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, getEffector().getPlayer());
			effectPoint.setCurrentHp(effectPoint.getMaxHp(), false);
			effectPoint.setCurrentMp(effectPoint.getMaxMp());

			effectPoint.setIsInvul(true);
			effectPoint.spawnMe(loc);
			_actor = effectPoint;
			_force = getEffector().getPlayer().getAI().isForceCast();
			_actor.broadcastPacket(new MagicSkillUse(_actor, _skill.getDisplayId(), _skill.getDisplayLevel(), 0, _skill.getReuseDelay()));

		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onActionTime()
	{
		if(_skill == null)
			return false;

		if(_actor == null || _actor.getPlayer() == null)
			return false;

		if(getSkill().isOffensive() && _count++ < 2)
			return true;

		if(getEffector().isDead() || getEffector() == null || !getEffector().getPlayer().isOnline())
		{
			onExit();
			return false;
		}

		final FastList<L2Character> targets = _skill.getTargets(_actor, _actor.getPlayer(), _force);

		if(targets.size() > 0)
		{
			double mpConsume2 = _skill.getMpConsume2();
			mpConsume2 = _skill.isMagic() ? _actor.getPlayer().calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume2, null, _skill) : _actor.getPlayer().calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume2, null, _skill);
			if(mpConsume2 >= 0)
			{
				if(_actor.getPlayer().getCurrentMp() < mpConsume2)
				{
					_actor.getPlayer().sendPacket(new SystemMessage(SystemMessage.YOUR_SKILL_WAS_REMOVED_DUE_TO_A_LACK_OF_MP));
					_actor.getPlayer().abortCast();
					return false;
				}
				_actor.getPlayer().reduceCurrentMp(mpConsume2, null);
			}

			boolean offensive = false;
			if(_skill.getSkillType() != SkillType.BUFF || _skill.getSkillType() != SkillType.EFFECT)
				offensive = true;

			for(final L2Character cha : _skill.getTargets(_actor, _actor.getPlayer(), false))
				if(cha.getEffectList().getEffectsBySkill(_skill) == null && _skill.checkTarget(_actor, cha, cha, false, false) == null)
				{
					_actor.broadcastPacket(new MagicSkillLaunched(_actor.getObjectId(), _skill.getDisplayId(), _skill.getDisplayLevel(), targets, offensive));
					_actor.broadcastPacket(new MagicSkillLaunched(_actor.getObjectId(), getSkill().getDisplayId(), getSkill().getDisplayLevel(), targets, offensive));
					_actor.getPlayer().callSkill(_skill, targets, false);
				}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		super.onExit();

		if(_actor != null)
			_actor.deleteMe();
	}
}