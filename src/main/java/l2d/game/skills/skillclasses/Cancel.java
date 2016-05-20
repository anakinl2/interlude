package l2d.game.skills.skillclasses;

import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;
import l2d.util.Rnd;

public class Cancel extends L2Skill
{
	private final String _dispelType;
	private final int _cancelRate;
	private final int _negateCount;

	public Cancel(StatsSet set)
	{
		super(set);
		_dispelType = set.getString("dispelType", "");
		_cancelRate = set.getInteger("cancelRate", 0);
		_negateCount = set.getInteger("negateCount", 5);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(_cancelRate <= 0 || Rnd.chance(_cancelRate))
			{
				byte counter = 0;

				if(_dispelType.equals(""))
				{
					byte antiloop = 24;
					while(counter < _negateCount && antiloop > 0)
					{
						ConcurrentLinkedQueue<L2Effect> eff = target.getEffectList().getAllEffects();
						if(eff.size() == 0)
							break;
						L2Effect e = eff.toArray(new L2Effect[eff.size()])[Rnd.get(eff.size())];
						if(e.getSkill().isCancelable())
						{
							e.exit();
							counter++;
						}
						antiloop--;
					}
				}
				else
				{
					counter = 0;
					if(_dispelType.contains("negative"))
						for(L2Effect e : target.getEffectList().getAllEffects())
							if(counter < _negateCount && e.getSkill().isOffensive())
							{
								e.exit();
								counter++;
							}

					counter = 0;
					if(_dispelType.contains("positive"))
						for(L2Effect e : target.getEffectList().getAllEffects())
							if(counter < _negateCount && !e.getSkill().isOffensive())
							{
								e.exit();
								counter++;
							}
				}
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}

	@Override
	public boolean isOffensive()
	{
		return !_dispelType.contains("negative");
	}
}