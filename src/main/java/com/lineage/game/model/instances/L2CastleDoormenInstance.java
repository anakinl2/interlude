package com.lineage.game.model.instances;

import java.util.StringTokenizer;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.templates.L2NpcTemplate;

public class L2CastleDoormenInstance extends L2NpcInstance
{
	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Castle_Owner = 2;

	public L2CastleDoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();
		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;
		if(condition == Cond_Busy_Because_Of_Siege)
			return;
		if(condition == Cond_Castle_Owner)
			if((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
			{
				if(command.startsWith("open_doors"))
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid
					while(st.hasMoreTokens())
						getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
				}
				else if(command.startsWith("close_doors"))
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid
					if(condition == 2)
						while(st.hasMoreTokens())
							getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
				}
			}
			else
				player.sendMessage(new CustomMessage("common.Privilleges", player));
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/castle/doormen/no.htm";

		int condition = validateCondition(player);

		if(condition == Cond_Busy_Because_Of_Siege)
			filename = "data/html/castle/doormen/busy.htm"; // Busy because of siege

		else if(condition == Cond_Castle_Owner) // Clan owns castle
			filename = "data/html/castle/doormen/" + getTemplate().npcId + ".htm"; // Owner message window

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private int validateCondition(L2Player player)
	{
		if(player.isGM())
			return Cond_Castle_Owner;

		if(player.getClan() != null)
			if(getCastle() != null && getCastle().getId() >= 0)
				if(getCastle().getOwnerId() == player.getClanId())
				{
					if(getCastle().getSiege().isInProgress())
					{
						if(Config.SIEGE_OPERATE_DOORS)
						{
							if(Config.SIEGE_OPERATE_DOORS_LORD_ONLY && !player.isCastleLord(getCastle().getId()))
								return Cond_Busy_Because_Of_Siege;
							return Cond_Castle_Owner;
						}
						return Cond_Busy_Because_Of_Siege;

					}
					return Cond_Castle_Owner;
				}

		return Cond_All_False;
	}
}