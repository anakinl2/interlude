package events.christmas;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;

public class Seed implements IItemHandler, ScriptFile
{
	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2Spawn spawnedTree = null;

		public DeSpawnScheduleTimerTask(L2Spawn spawn)
		{
			spawnedTree = spawn;
		}

		public void run()
		{
			try
			{
				spawnedTree.getLastSpawn().decayMe();
				spawnedTree.getLastSpawn().deleteMe();
			}
			catch(Throwable t)
			{}
		}
	}

	private static int[] _itemIds =
	{
		5560, // Christmas Tree
		5561 // Special Christmas Tree
	};

	private static int[] _npcIds =
	{
		13006, // Christmas Tree
		13007 // Special Christmas Tree
	};

	private static final int DESPAWN_TIME = 600000; //10 min

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

		for(L2NpcInstance npc : L2World.getAroundNpc(activeChar, 300, 200))
			if(npc.getNpcId() == _npcIds[0] || npc.getNpcId() == _npcIds[1])
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.SINCE_S1_ALREADY_EXISTS_NEARBY_YOU_CANNOT_SUMMON_IT_AGAIN).addItemName(13006));
				return;
			}

		if(template == null)
			return;

		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setId(IdFactory.getInstance().getNextId());
			spawn.setLoc(activeChar.getLoc());
			L2NpcInstance npc = spawn.doSpawn(false);
			npc.setTitle(activeChar.getName()); //FIXME Почему-то не устанавливается
			spawn.respawnNpc(npc);

			// АИ вещающее бафф регена устанавливается только для большой елки
			if(itemId == 5561)
			{
				npc.setAI(new ctreeAI(npc));
				npc.getAI().startAITask();
			}

			ThreadPoolManager.getInstance().scheduleAi(new DeSpawnScheduleTimerTask(spawn), DESPAWN_TIME, true);
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