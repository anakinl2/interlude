package ai;

import javolution.util.FastList;
import l2d.game.ai.Mystic;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.SkillTable;
import l2d.util.Rnd;

/**
 * AI for Hot Springs Atrox (id 21321)
 */
public class HotSpringsAtrox extends Mystic
{
	private static final int DeBuff = 4554;

	public HotSpringsAtrox(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor != null && attacker != null)
			if(Rnd.chance(5))
			{
				FastList<L2Effect> effect = attacker.getEffectList().getEffectsBySkillId(DeBuff);
				if(effect != null)
				{
					int level = effect.getFirst().getSkill().getLevel();
					if(level < 10)
					{
						effect.getFirst().exit();
						L2Skill skill = SkillTable.getInstance().getInfo(DeBuff, level + 1);
						skill.getEffects(actor, attacker, false, false);
					}
				}
				else
				{
					L2Skill skill = SkillTable.getInstance().getInfo(DeBuff, 1);
					skill.getEffects(actor, attacker, false, false);
				}
			}
		super.onEvtAttacked(attacker, damage);
	}
}