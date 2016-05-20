package l2d.game.model.instances;

import l2d.game.templates.L2NpcTemplate;

/**
 * Это алиас L2MonsterInstance используемый для монстров, у которых нестандартные статы
 */
public class L2SpecialMonsterInstance extends L2MonsterInstance
{
	public L2SpecialMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
}