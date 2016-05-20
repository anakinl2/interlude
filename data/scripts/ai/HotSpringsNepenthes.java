package ai;

import javolution.util.FastList;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.SkillTable;
import com.lineage.util.Rnd;

/**
 * AI for Hot Springs Nepenthes (id 21319)
 */
public class HotSpringsNepenthes extends Fighter
{
	private static final int DeBuff = 4552;

	public HotSpringsNepenthes(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor != null && attacker != null && Rnd.chance(5))
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