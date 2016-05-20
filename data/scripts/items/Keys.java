package items;

import java.util.ArrayList;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.DoorTable;

public class Keys implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = null;

	public Keys()
	{
		ArrayList<Integer> keys = new ArrayList<Integer>();
		for(L2DoorInstance door : DoorTable.getInstance().getDoors())
			if(door != null && door.key > 0)
				keys.add(door.key);
		_itemIds = new int[keys.size()];
		int i = 0;
		for(int id : keys)
		{
			_itemIds[i] = id;
			i++;
		}
	}

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(item == null || item.getIntegerLimitedCount() < 1)
		{
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}
		
		L2Object target = player.getTarget();
		if(target == null || !(target instanceof L2DoorInstance))
		{
			player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		L2DoorInstance door = (L2DoorInstance) target;

		if(door.isOpen())
		{
			player.sendPacket(new SystemMessage(SystemMessage.IT_IS_NOT_LOCKED));
			return;
		}
		
		if(door.key <= 0) // ключ не подходит к двери
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_UNABLE_TO_UNLOCK_THE_DOOR));
			return;
		}
		
		if(item.getItemId() != door.key) // ключ не подходит к двери
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_UNABLE_TO_UNLOCK_THE_DOOR));
			return;
		}

		L2ItemInstance ri = player.getInventory().destroyItem(item, 1, true);
		player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(ri.getItemId()));
		player.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Unlock.Success", player));
		door.openMe();
		door.onOpen();
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