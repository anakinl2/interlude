package com.lineage.game.skills.skillclasses;

import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;

public class Default extends L2Skill
{
	public Default(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Default.NotImplemented", activeChar).addNumber(getId()).addString("" + getSkillType()));
		activeChar.sendActionFailed();
	}
}
