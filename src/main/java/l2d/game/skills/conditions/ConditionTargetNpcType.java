package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public class ConditionTargetNpcType extends Condition
{

	private final String[] _npcType;

	public ConditionTargetNpcType(final String[] type)
	{
		_npcType = type;
	}

	@Override
	public boolean testImpl(final Env env)
	{
		if(env.target == null)
			return false;
		boolean mt;
		for(int i = 0; i < _npcType.length; i++)
		{
			mt = env.target.getClass().getName().endsWith(_npcType[i] + "Instance");
			if(mt)
				return true;
		}
		return false;
	}
}