package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.instancemanager.RaidBossSpawnManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.SpawnTable;
import com.lineage.util.Log;

public class AdminDelete implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_delete
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditNPC)
			return false;

		switch(command)
		{
			case admin_delete:
				L2Object obj = activeChar.getTarget();
				if(obj != null && obj.isNpc())
				{
					L2NpcInstance target = (L2NpcInstance) obj;

					L2Spawn spawn = target.getSpawn();
					if(spawn != null)
					{
						spawn.stopRespawn();
						if(RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
							RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
						else
							SpawnTable.getInstance().deleteSpawn(spawn, true);
					}

					target.deleteMe();
					Log.add("deleted NPC" + target.getObjectId(), "gm_ext_actions", activeChar);
				}
				else
					activeChar.sendPacket(Msg.INVALID_TARGET);
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}