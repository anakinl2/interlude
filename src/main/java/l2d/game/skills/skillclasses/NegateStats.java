package l2d.game.skills.skillclasses;

import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Formulas;
import l2d.game.skills.Stats;
import l2d.game.templates.StatsSet;

public class NegateStats extends L2Skill
{
	private final FastList<Stats> _negateStats;
	private final boolean _negateOffensive;
	private final int _negateCount;

	public NegateStats(StatsSet set)
	{
		super(set);

		String[] negateStats = set.getString("negateStats", "").split(" ");
		_negateStats = new FastList<Stats>(negateStats.length);
		for(String stat : negateStats)
			if(!stat.isEmpty())
				_negateStats.add(Stats.valueOfXml(stat));

		_negateOffensive = set.getBool("negateDebuffs", false);
		_negateCount = set.getInteger("negateCount", 0);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(!_negateOffensive && !Formulas.calcSkillSuccess(activeChar, target, this))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(target.getName()).addSkillName(getId(), getLevel()));
				continue;
			}

			int count = 0;
			ConcurrentLinkedQueue<L2Effect> effects = target.getEffectList().getAllEffects();
			for(Stats stat : _negateStats)
				for(L2Effect e : effects)
				{
					L2Skill skill = e.getSkill();
					if(skill.isOffensive() == _negateOffensive && e.containsStat(stat) && skill.isCancelable())
					{
						target.sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getDisplayLevel()));
						e.exit();
						count++;
					}
					if(_negateCount > 0 && count >= _negateCount)
						break;
				}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}

	@Override
	public boolean isOffensive()
	{
		return !_negateOffensive;
	}

	public FastList<Stats> getNegateStats()
	{
		return _negateStats;
	}
}