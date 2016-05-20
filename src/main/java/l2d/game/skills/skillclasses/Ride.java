package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;

public class Ride extends L2Skill
{
	public Ride(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!activeChar.isPlayer())
			return false;

		L2Player player = (L2Player) activeChar;
		if(getNpcId() != 0)
		{
			if(player.getDuel() != null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_IN_A_DUEL));
				return false;
			}
			if(player.isInCombat())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_IN_BATTLE));
				return false;
			}
			if(player.isFishing())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_FISHING));
				return false;
			}
			if(player.isSitting())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_SITTING));
				return false;
			}
			if(player.isCursedWeaponEquipped())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_A_CURSED_WEAPON_IS_EQUPPED));
				return false;
			}
			if(player.getPet() != null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_MOUNT_A_STEED_WHILE_A_PET_OR_A_SERVITOR_IS_SUMMONED));
				return false;
			}
			if(player.isMounted())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ALREADY_MOUNTED_ANOTHER_STEED));
				return false;
			}
		}
		else if(getNpcId() == 0 && !player.isMounted())
			return false;

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Character> targets)
	{
		if(!caster.isPlayer())
			return;

		L2Player activeChar = (L2Player) caster;
		activeChar.setMount(getNpcId(), 0, 0);
	}
}