package ai;

import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.SkillTable;

public class Quest421FairyTree extends Fighter
{
	public Quest421FairyTree(L2Character actor)
	{
		super(actor);
		actor.setImobilised(true);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor != null && attacker.isPlayer())
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5423, 12);
			skill.getEffects(actor, attacker, false, false);
			return;
		}
		if(attacker.isPet())
		{
			super.onEvtAttacked(attacker, damage);
			return;
		}
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}