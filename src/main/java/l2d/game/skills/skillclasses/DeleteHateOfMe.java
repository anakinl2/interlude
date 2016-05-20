package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.ai.CtrlIntention;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Formulas;
import l2d.game.templates.StatsSet;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class DeleteHateOfMe extends L2Skill
{
	private final boolean _cancelSelfTarget;
	private final boolean _turner;

	public DeleteHateOfMe(final StatsSet set)
	{
		super(set);
		_turner = set.getBool("turner", false);
		_cancelSelfTarget = set.getBool("cancelSelfTarget", false);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			final L2Character target = n.getValue();

			final boolean success = _id == SKILL_BLUFF ? false : Rnd.chance(getActivateRate());

			if(_id != SKILL_BLUFF && Config.SKILLS_SHOW_CHANCE && activeChar.isPlayer() && !((L2Player) activeChar).getVarB("SkillsHideChance"))
				activeChar.sendMessage(new CustomMessage("l2d.game.skills.Formulas.Chance", activeChar).addString(getName()).addNumber(getActivateRate()));

			if(_id == SKILL_BLUFF ? Formulas.calcSkillSuccess(activeChar, target, this) : success)
			{
				if(target.isNpc())
				{
					final L2NpcInstance npc = (L2NpcInstance) target;
					activeChar.removeFromHatelist(npc, true);
					npc.getAI().clearTasks();
					npc.getAI().setAttackTarget(null);
					if(npc.noTarget())
					{
						npc.getAI().setGlobalAggro(-10);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					}
				}

				if(_cancelSelfTarget)
					activeChar.setTarget(null);
			}

			if(_turner && !target.isInvul())
			{
				final int posX = target.getX();
				final int posY = target.getY();
				int signx = -1;
				int signy = -1;
				if(target.getX() > target.getX())
					signx = 1;
				if(target.getY() > target.getY())
					signy = 1;

				target.stopMove();
				target.setHeading(target, false);

				if(!target.isMonster())
					target.setTarget(null);
				target.setRunning();

				final Location loc = GeoEngine.moveCheck(target.getX(), target.getY(), target.getZ(), posX + signx * 40, posY + signy * 40);
				target.moveToLocation(loc, 0, false);

				target.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(_displayId, _displayLevel));
			}

			// Для Bluff шанс прохождения эффекта скила расчитывается как для шоковых атак, причем отдельно
			if(success || _id == SKILL_BLUFF)
				getEffects(activeChar, target, _id == SKILL_BLUFF, false);
		}
	}
}