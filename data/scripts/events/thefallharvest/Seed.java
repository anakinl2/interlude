package events.thefallharvest;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;

public class Seed implements IItemHandler, ScriptFile
{
	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2Spawn spawnedPlant = null;

		public DeSpawnScheduleTimerTask(L2Spawn spawn)
		{
			spawnedPlant = spawn;
		}

		public void run()
		{
			try
			{
				spawnedPlant.getLastSpawn().decayMe();
				spawnedPlant.getLastSpawn().deleteMe();
			}
			catch(Throwable t)
			{}
		}
	}

	private static int[] _itemIds =
	{
		6389, // small seed
		6390 // large seed
	};

	private static int[] _npcIds =
	{
		12774, // Young Pumpkin
		12777 // Large Young Pumpkin
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2Player activeChar = (L2Player) playable;
		L2NpcTemplate template = null;

		int itemId = item.getItemId();
		for(int i = 0; i < _itemIds.length; i++)
			if(_itemIds[i] == itemId)
			{
				template = NpcTable.getTemplate(_npcIds[i]);
				break;
			}

		if(template == null)
			return;

		L2Object target = activeChar.getTarget();
		if(target == null)
			target = activeChar;

		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setConstructor(SquashInstance.class.getConstructors()[0]);
			spawn.setId(IdFactory.getInstance().getNextId());
			spawn.setLoc(GeoEngine.findPointToStay(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 30, 70));
			L2NpcInstance npc = spawn.doSpawn(true);
			npc.setAI(new SquashAI(npc));
			((SquashInstance) npc).setSpawner(activeChar);

			ThreadPoolManager.getInstance().scheduleAi(new DeSpawnScheduleTimerTask(spawn), 180000, true);
			activeChar.getInventory().destroyItem(item.getObjectId(), 1, false);
		}
		catch(Exception e)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOUR_TARGET_CANNOT_BE_FOUND));
		}
	}

	public int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}