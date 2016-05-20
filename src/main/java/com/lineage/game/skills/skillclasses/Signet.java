package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Location;

public class Signet extends L2Skill
{
	public Signet(final StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(final L2Character cha, final L2Character target, final boolean forceUse, final boolean dontMove, final boolean first)
	{
		if(cha.getEffectList().getEffectByType(EffectType.Signet) != null)
		{
			cha.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(getId(), getLevel()));
			return false;
		}

		if(isOffensive())
		{
			if(cha.isInZonePeace())
			{
				cha.sendPacket(Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
				return false;
			}

			if(cha.isPlayer())
			{
				final L2Player player = (L2Player) cha;
				final Location loc = player.getGroundSkillLoc();
				if(loc != null && ZoneManager.getInstance().checkIfInZone(ZoneType.peace_zone, loc.x, loc.y, loc.z + 45))
				{
					cha.sendPacket(Msg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
					return false;
				}
			}
		}
		return super.checkCondition(cha, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(final L2Character cha, final FastList<L2Character> targets)
	{
		getEffects(cha, cha, false, false);
	}
}