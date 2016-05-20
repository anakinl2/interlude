package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.skills.Env;

public class ConditionTargetNpcId extends Condition
{
	private final String[] _npcIds;

	public ConditionTargetNpcId(String[] ids)
	{
		_npcIds = ids;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character target = env.target;
		if(target == null)
			return false;

		for(String id : _npcIds)
			if(target instanceof L2NpcInstance && target.getNpcId() == Integer.parseInt(id))
				return true;

		return false;
	}
}