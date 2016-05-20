package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Summon;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2StaticObjectInstance;
import com.lineage.game.serverpackets.CharInfo;
import com.lineage.game.serverpackets.DoorInfo;
import com.lineage.game.serverpackets.DoorStatusUpdate;
import com.lineage.game.serverpackets.NpcInfo;
import com.lineage.game.serverpackets.Ride;
import com.lineage.game.serverpackets.SpawnItem;
import com.lineage.game.serverpackets.StaticObject;
import com.lineage.game.serverpackets.VehicleInfo;

public class RequestReload extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getActiveChar();

		if(player == null)
			return;

		player.sendUserInfo(false);
		for(L2Object obj : L2World.getAroundObjects(player))
		{
			if(obj == null)
				continue;
			if(obj.isNpc())
				player.sendPacket(new NpcInfo((L2NpcInstance) obj, player));
			else if(obj instanceof L2Summon)
				((L2Summon) obj).broadcastPetInfo();
			else if(obj.isPlayer())
			{
				L2Player targetPlayer = (L2Player) obj;
				if(player.getObjectId() != targetPlayer.getObjectId() && !targetPlayer.isInvisible())
				{
					if(targetPlayer.isMounted())
					{
						player.sendPacket(new CharInfo(targetPlayer, player, false));
						player.sendPacket(new Ride(targetPlayer));
					}
					player.sendPacket(new CharInfo(targetPlayer, player, true));
				}
			}
			else if(obj instanceof L2DoorInstance)
			{
				player.sendPacket(new DoorInfo((L2DoorInstance) obj));
				player.sendPacket(new DoorStatusUpdate((L2DoorInstance) obj));
			}
			else if(obj instanceof L2BoatInstance)
				player.sendPacket(new VehicleInfo((L2BoatInstance) obj));
			else if(obj instanceof L2ItemInstance)
				player.sendPacket(new SpawnItem((L2ItemInstance) obj));
			else if(obj instanceof L2StaticObjectInstance)
				player.sendPacket(new StaticObject((L2StaticObjectInstance) obj));
		}
	}
}