package com.lineage.game.tables;

import java.util.ArrayList;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.SystemMessage;

public class GmListTable
{
	public static ArrayList<L2Player> getAllGMs()
	{
		ArrayList<L2Player> gmList = new ArrayList<L2Player>();
		for(L2Player player : L2World.getAllPlayers())
			if(player.isGM())
				gmList.add(player);

		return gmList;
	}

	public static ArrayList<L2Player> getAllVisibleGMs()
	{
		ArrayList<L2Player> gmList = new ArrayList<L2Player>();
		for(L2Player player : L2World.getAllPlayers())
			if(player.isGM() && !player.isInvisible())
				gmList.add(player);

		return gmList;
	}

	public static void sendListToPlayer(L2Player player)
	{
		ArrayList<L2Player> gmList = getAllVisibleGMs();
		if(gmList.isEmpty())
		{
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NOT_ANY_GMS_THAT_ARE_PROVIDING_CUSTOMER_SERVICE_CURRENTLY));
			return;
		}

		player.sendPacket(new SystemMessage(SystemMessage._GM_LIST_));
		for(L2Player gm : gmList)
			player.sendPacket(new SystemMessage(SystemMessage.GM_S1).addString(gm.getName()));
	}

	public static void broadcastToGMs(L2GameServerPacket packet)
	{
		for(L2Player gm : getAllGMs())
			gm.sendPacket(packet);
	}

	public static void broadcastMessageToGMs(String message)
	{
		for(L2Player gm : getAllGMs())
			gm.sendMessage(message);
	}
}