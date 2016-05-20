package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;
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