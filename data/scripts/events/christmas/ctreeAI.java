package events.christmas;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.SkillTable;

public class ctreeAI extends DefaultAI
{
	public ctreeAI(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return true;

		int skillId = 2139;
		for(L2Player player : L2World.getAroundPlayers(actor, 200, 200))
			if(player != null && !player.isInZonePeace() && player.getEffectList().getEffectsBySkillId(skillId) == null)
				actor.doCast(SkillTable.getInstance().getInfo(skillId, 1), player, true);
		return false;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}