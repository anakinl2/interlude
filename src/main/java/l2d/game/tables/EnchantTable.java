package l2d.game.tables;

import java.util.ArrayList;

import javolution.util.FastMap;
import l2d.game.model.base.L2EnchantSkillLearn;

public abstract class EnchantTable
{
	public static FastMap<Integer, ArrayList<L2EnchantSkillLearn>> _enchant = new FastMap<Integer, ArrayList<L2EnchantSkillLearn>>();
}