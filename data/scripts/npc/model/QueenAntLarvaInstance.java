package npc.model;

import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.templates.L2NpcTemplate;

public class QueenAntLarvaInstance extends L2MonsterInstance
{
	public QueenAntLarvaInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		setImobilised(true);
	}

	@Override
	public void reduceCurrentHp(final double i, final L2Character attacker, final L2Skill skill, final boolean awake, final boolean standUp, final boolean directHp, final boolean canReflect)
	{
		final double damage = getCurrentHp() - i > 1 ? i : getCurrentHp() - 1;
		super.reduceCurrentHp(damage, attacker, skill, awake, standUp, directHp, canReflect);
	}
}