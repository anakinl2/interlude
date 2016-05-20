package l2d.game.clientpackets;

import l2d.ext.multilang.CustomMessage;
import l2d.game.model.L2Clan;
import l2d.game.model.L2ClanMember;
import l2d.game.model.L2Player;

public class RequestPledgeSetMemberPowerGrade extends L2GameClientPacket
{
	// format: (ch)Sd
	private int _powerGrade;
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
		_powerGrade = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeSetMemberPowerGrade");

		L2Clan clan = activeChar.getClan();
		if(clan == null)
			return;

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) == L2Clan.CP_CL_MANAGE_RANKS)
		{
			L2ClanMember member = activeChar.getClan().getClanMember(_name);
			if(member != null)
			{
				if(clan.isAcademy(member.getPledgeType()))
				{
					activeChar.sendMessage("You cannot change academy member grade");
					return;
				}
				if(_powerGrade > 5 && clan.getAffiliationRank(member.getPledgeType()) != _powerGrade)
					member.setPowerGrade(clan.getAffiliationRank(member.getPledgeType()));
				else
					member.setPowerGrade(_powerGrade);
				if(member.isOnline())
					member.getPlayer().sendUserInfo(false);
			}
			else
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestPledgeSetMemberPowerGrade.NotBelongClan", activeChar));
		}
		else
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestPledgeSetMemberPowerGrade.HaveNotAuthority", activeChar));
	}
}