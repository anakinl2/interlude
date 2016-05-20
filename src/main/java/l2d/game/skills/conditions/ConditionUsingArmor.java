package l2d.game.skills.conditions;

import l2d.game.model.L2Player;
import l2d.game.skills.Env;
import l2d.game.templates.L2Armor.ArmorType;

public class ConditionUsingArmor extends Condition
{
	private final ArmorType _armor;

	public ConditionUsingArmor(ArmorType armor)
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
