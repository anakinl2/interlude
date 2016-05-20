package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.serverpackets.NpcHtmlMessage;

/**
 * format ch
 * c: (id) 0xD0
 * h: (subid) 0x2F
 */
public class RequestExOlympiadObserverEnd extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(!activeChar.inObserverMode())
			return;

		String[] matches = Olympiad.getAllTitles();

		NpcHtmlMessage reply = new NpcHtmlMessage(0);
		StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<center><br>Grand Olympiad Game View");
		replyMSG.append("<table width=270 border=0 bgcolor=\"000000\">");
		replyMSG.append("<tr><td fixwidth=30>NO.</td><td>Status &nbsp; &nbsp; Player1 / Player2</td></tr>");

		for(int i = 0; i < matches.length; i++)
			replyMSG.append("<tr><td fixwidth=30><a action=\"bypass -h npc_0_Olympiad 3_" + i + "\">" + i + "</a></td><td>" + matches[i] + "</td></tr>");

		replyMSG.append("</table></center></body></html>");

		reply.setHtml(replyMSG.toString());
		activeChar.sendPacket(reply);
	}
}