package l2d.game.skills;

import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ItemInstance;

/**
 * An Env object is just a class to pass parameters to a calculator such as L2Player,
 * L2ItemInstance, Initial value.
 */
public final class Env
{
	public L2Character character;
	public L2Character target;
	public L2ItemInstance item;
	public L2Skill skill;
	public double value;

	public Env()
	{}

	public Env(final L2Character cha, final L2Character tar, final L2Skill sk)
	{
		character = cha;
		target = tar;
		skill = sk;
	}
}
