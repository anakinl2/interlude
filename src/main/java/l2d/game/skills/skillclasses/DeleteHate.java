package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.templates.StatsSet;
import com.lineage.util.Rnd;

public class DeleteHate extends L2Skill
{
	public DeleteHate(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(target.isRaid())
				continue;

			if(getActivateRate() > 0)
			{
				if(Config.SKILLS_SHOW_CHANCE && activeChar.isPlayer() && !((L2Player) activeChar).getVarB("SkillsHideChance"))
					activeChar.sendMessage(new CustomMessage("l2d.game.skills.Formulas.Chance", activeChar).addString(getName()).addNumber(getActivateRate()));

				if(!Rnd.chance(getActivateRate()))
					return;
			}

			if(target.isNpc())
			{
				L2NpcInstance npc = (L2NpcInstance) target;
				npc.clearAggroList(true);
				npc.getAI().clearTasks();
				npc.getAI().setGlobalAggro(-10);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}

			getEffects(activeChar, target, false, false);
		}
	}
}
