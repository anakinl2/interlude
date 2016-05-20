package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.entity.residence.Castle;
import l2d.game.model.entity.siege.Siege;
import l2d.game.model.entity.siege.SiegeClan;
import l2d.game.model.instances.L2ArtefactInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;

public class TakeCastle extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!super.checkCondition(activeChar, target, forceUse, dontMove, first))
			return false;

		if(activeChar == null || !activeChar.isPlayer())
			return false;

		L2Player player = (L2Player) activeChar;
		if(player.getClan() == null || !player.isClanLeader())
			return false;

		Siege siege = SiegeManager.getSiege(activeChar, true);
		if(siege == null || !(siege.getSiegeUnit() instanceof Castle))
			return false;
		if(!siege.isInProgress())
			return false;
		if(siege.getAttackerClan(player.getClan()) == null)
			return false;

		if(first)
			for(SiegeClan sc : siege.getDefenderClans().values())
			{
				L2Clan clan = sc.getClan();
				if(clan != null)
					for(L2Player pl : clan.getOnlineMembers(0))
						if(pl != null)
							pl.sendPacket(new SystemMessage(SystemMessage.THE_OPPONENT_CLAN_HAS_BEGUN_TO_ENGRAVE_THE_RULER));
			}

		return true;
	}

	public TakeCastle(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(!(target instanceof L2ArtefactInstance))
				continue;
			L2Player player = (L2Player) activeChar;
			Siege siege = SiegeManager.getSiege(activeChar, true);
			if(siege != null && siege.isInProgress())
			{
				Siege.announceToPlayer(new SystemMessage(SystemMessage.CLAN_S1_HAS_SUCCEEDED_IN_ENGRAVING_THE_RULER).addString(player.getClan().getName()), false);
				siege.Engrave(player.getClan(), target.getObjectId());
			}
		}
	}
}