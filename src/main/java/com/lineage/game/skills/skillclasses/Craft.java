package com.lineage.game.skills.skillclasses;

import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.game.RecipeController;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;

public class Craft extends L2Skill
{
	private final boolean _dwarven;

	public Craft(StatsSet set)
	{
		super(set);
		_dwarven = set.getBool("isDwarven");
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!activeChar.isPlayer() || activeChar.isOutOfControl() || activeChar.getDuel() != null)
			return false;
		return true;
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		RecipeController.getInstance().requestBookOpen((L2Player) activeChar, _dwarven);
	}
}
