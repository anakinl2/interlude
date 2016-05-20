package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

public class ChargeSoul extends L2Skill
{
	private int _numSouls;

	public ChargeSoul(StatsSet set)
	{
		super(set);
		_numSouls = set.getInteger("numSouls", getLevel());
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{

		if(!activeChar.isPlayer())
			return;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(target.isDead() || !target.isPlayer())
				continue;

			target.setConsumedSouls(target.getConsumedSouls() + _numSouls, null);

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}