package com.lineage.game.skills.skillclasses;

import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Character.HateInfo;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2NpcInstance;

public class ShiftAggression extends L2Skill
{
	public ShiftAggression(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(activeChar.getPlayer() == null)
			return;

		L2Playable playable = (L2Playable) activeChar;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null || !target.isPlayer())
				continue;

			L2Player player_target = (L2Player) target;

			for(L2NpcInstance npc : L2World.getAroundNpc(activeChar, getSkillRadius(), 200))
			{
				HateInfo hateInfo = playable.getHateList().get(npc);
				if(hateInfo == null || hateInfo.hate <= 0)
					continue;
				player_target.addDamageHate(npc, 0, hateInfo.hate + 100);
				hateInfo.hate = 0;
			}
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
