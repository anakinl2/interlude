package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.util.Util;

public class RequestGiveNickName extends L2GameClientPacket
{
	//Format: cSS
	static Logger _log = Logger.getLogger(RequestGiveNickName.class.getName());

	private String _target;
	private String _title;

	@Override
	public void readImpl()
	{
		_target = readS();
		_title = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(!_title.equals("") && !Util.isMatchingRegexp(_title, Config.CLAN_TITLE_TEMPLATE))
		{
			activeChar.sendMessage("Incorrect title.");
			return;
		}

		// Дворяне могут устанавливать/менять себе title
		if(activeChar.isNoble() && _target.matches(activeChar.getName()))
		{
			activeChar.setTitle(_title);
			activeChar.sendPacket(Msg.TITLE_HAS_CHANGED);
			activeChar.sendChanges();
			return;
		}
		// Can the player change/give a title?
		else if((activeChar.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) != L2Clan.CP_CL_GIVE_TITLE)
			return;

		if(activeChar.getClan().getLevel() < 3)
		{
			activeChar.sendPacket(Msg.TITLE_ENDOWMENT_IS_ONLY_POSSIBLE_WHEN_CLANS_SKILL_LEVELS_ARE_ABOVE_3);
			return;
		}

		L2ClanMember member = activeChar.getClan().getClanMember(_target);
		if(member != null)
		{
			member.setTitle(_title);
			if(member.isOnline())
			{
				member.getPlayer().sendPacket(Msg.TITLE_HAS_CHANGED);
				member.getPlayer().sendChanges();
			}
		}
		else
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestGiveNickName.NotInClan", activeChar));

	}
}