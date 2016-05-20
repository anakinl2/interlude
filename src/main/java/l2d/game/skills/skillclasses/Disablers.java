package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

public class Disablers extends L2Skill
{
	private final boolean _skillInterrupt;

	public Disablers(StatsSet set)
	{
		super(set);
		_skillInterrupt = set.getBool("skillInterrupt", false);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null)
				continue;

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(_skillInterrupt)
			{
				if(target.getCastingSkill() != null && !target.getCastingSkill().isMagic() && !target.isRaid())
					target.abortCast();
				if(!target.isRaid())
					target.abortAttack();
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}