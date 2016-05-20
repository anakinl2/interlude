package com.lineage.game.skills.conditions;

import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.skills.Env;

public class ConditionZone extends Condition
{
	private final ZoneType _zoneType;

	public ConditionZone(String zoneType)
	{
		_zoneType = ZoneType.valueOf(zoneType);
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		return env.character.isInZone(_zoneType);
	}
}