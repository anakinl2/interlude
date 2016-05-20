package l2d.game.skills.effects;

import l2d.game.cache.Msg;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;

public class EffectRelax extends L2Effect
{
	public EffectRelax(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(((L2Player) _effected).isMounted())
		{
			exit();
			return;
		}
		setRelax(true);
		((L2Player) _effected).sitDown();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		setRelax(false);
	}

	@Override
	public boolean onActionTime()
	{
		boolean retval = true;
		if(getEffected().isDead())
			retval = false;

		if(!((L2Player) _effected).isSitting())
			retval = false;

		if(_effected.isCurrentHpFull() && getSkill().isToggle())
		{
			getEffected().sendPacket(new SystemMessage(SystemMessage.HP_WAS_FULLY_RECOVERED_AND_SKILL_WAS_REMOVED));
			retval = false;
		}

		double manaDam = calc();
		if(getSkill().isMagic())
			manaDam = _effected.calcStat(Stats.MP_MAGIC_SKILL_CONSUME, manaDam, null, getSkill());
		else
			manaDam = _effected.calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, manaDam, null, getSkill());

		if(manaDam > _effected.getCurrentMp())
			if(getSkill().isToggle())
			{
				_effected.sendPacket(Msg.NOT_ENOUGH_MP);
				_effected.sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(getSkill().getId(), getSkill().getDisplayLevel()));
				retval = false;
			}

		if(!retval)
			setRelax(retval);
		else
			_effected.reduceCurrentMp(manaDam, null);

		return retval;
	}

	private void setRelax(boolean val)
	{
		((L2Player) _effected).setRelax(val);
	}
}