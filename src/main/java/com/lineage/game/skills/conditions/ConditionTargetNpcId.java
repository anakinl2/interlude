package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;

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