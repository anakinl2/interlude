package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;
import com.lineage.game.templates.L2Armor;
import com.lineage.game.model.L2Player;

public class ConditionUsingArmor extends Condition
{
	private final L2Armor.ArmorType _armor;

	public ConditionUsingArmor(L2Armor.ArmorType armor)
	{
		_armor = armor;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer() && ((L2Player) env.character).isWearingArmor(_armor))
			return true;

		return false;
	}
}
