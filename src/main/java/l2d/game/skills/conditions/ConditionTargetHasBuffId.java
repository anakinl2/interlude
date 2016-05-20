package l2d.game.skills.conditions;

import javolution.util.FastList;
import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public final class ConditionTargetHasBuffId extends Condition
{
	private final int _id;
	private final int _level;

	public ConditionTargetHasBuffId(int id, int level)
	{
		_id = id;
		_level = level;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.target == null)
			return false;
		if(_level == -1)
			return env.target.getEffectList().getEffectsBySkillId(_id) != null;
		FastList<L2Effect> el = env.target.getEffectList().getEffectsBySkillId(_id);
		if(el == null)
			return false;
		for(L2Effect effect : el)
			if(effect != null && effect.getSkill().getLevel() >= _level)
				return true;
		return false;
	}
}
