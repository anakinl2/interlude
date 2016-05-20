package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

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
