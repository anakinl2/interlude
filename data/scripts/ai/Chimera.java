package ai;

import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

public class Chimera extends Fighter
{
	public Chimera(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{
		if(skill.getId() != 2359)
			return;
		L2NpcInstance actor = getActor();
		if(actor == null || actor.getCurrentHpPercents() > 10) // 10% ХП для использования бутылки
			return;

		// 10% шанс получения Life Force
		L2Player killer = null;
		L2Character MostHated = actor.getMostHated();
		if(MostHated != null && MostHated instanceof L2Playable)
			killer = MostHated.getPlayer();
		actor.dropItem(killer, actor.getNpcId() == 22353 ? 9682 : Rnd.chance(10) ? 9681 : 9680, 1);
		actor.decayMe();
		actor.doDie(actor);
	}
}