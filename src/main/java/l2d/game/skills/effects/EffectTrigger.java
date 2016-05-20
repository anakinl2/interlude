package l2d.game.skills.effects;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.MagicSkillLaunched;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.tables.SkillTable;

public class EffectTrigger extends L2Effect
{
	public EffectTrigger(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		final double mpConsume = getSkill().getMpConsume();
		final L2Character _actor = getEffector();
		final L2Skill _skill = SkillTable.getInstance().getInfo(getSkill().getTriggerEffectId(), getSkill().getTriggerEffectLevel());
		final boolean offensive = false;

		if(_skill == null)
			return;

		if(_actor == null || _actor.getPlayer() == null)
			return;

		if(_actor.isDead())
			return;

		if(mpConsume > _actor.getCurrentMp())
		{
			_actor.sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_MP));
			return;
		}

		if(_skill == null)
		{
			_log.severe("Not implemented trigger skill, id = " + getSkill().getId());
			exit();
			return;
		}

		for(final L2Character cha : _skill.getTargets(_actor, _actor.getPlayer(), false))
			// TODO Надо ли проверку && _skill.checkTarget(_actor, cha, cha, false, false) == null ?
			if(cha.getEffectList().getEffectsBySkill(_skill) == null && getSkill().checkCondition(_actor, cha, false, false, false) && _skill.checkTarget(_actor, cha, cha, false, false) == null)
			{
				final FastList<L2Character> targets = new FastList<L2Character>();
				targets.add(cha);
				_actor.broadcastPacket(new MagicSkillLaunched(_actor.getObjectId(), _skill.getDisplayId(), _skill.getDisplayLevel(), targets, offensive));
				_actor.broadcastPacket(new MagicSkillLaunched(_actor.getObjectId(), getSkill().getDisplayId(), getSkill().getDisplayLevel(), targets, offensive));
				_actor.broadcastPacket(new MagicSkillUse(_actor, cha, _skill.getId(), _skill.getLevel(), 0, 0));
				_actor.callSkill(_skill, targets, false);
			}
	}

	@Override
	public void onExit()
	{
		super.onExit();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}