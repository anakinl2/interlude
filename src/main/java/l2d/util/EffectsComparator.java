package l2d.util;

import java.util.Comparator;

import l2d.game.model.L2Effect;

public class EffectsComparator implements Comparator<L2Effect>
{
	private static final EffectsComparator instance = new EffectsComparator();

	public static final EffectsComparator getInstance()
	{
		return instance;
	}

	@Override
	public int compare(L2Effect o1, L2Effect o2)
	{
		if(o1 == null || o2 == null || o1.getSkill().isToggle() && o2.getSkill().isToggle())
			return 0;

		if(o1.getSkill().isToggle())
			return 1;

		if(o2.getSkill().isToggle())
			return -1;

		if(o1.getPeriodStartTime() > o2.getPeriodStartTime())
			return 1;

		if(o1.getPeriodStartTime() < o2.getPeriodStartTime())
			return -1;

		return 0;
	}
}