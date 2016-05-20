package items;

import java.util.List;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.FishDropData;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.FishTable;
import l2d.game.tables.ItemTable;
import com.lineage.util.Util;

public class FishItem implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = null;

	public FishItem()
	{
		FishTable ft = FishTable.getInstance();
		_itemIds = new int[ft.GetFishItemCount()];
		for(int i = 0; i < ft.GetFishItemCount(); i++)
			_itemIds[i] = ft.getFishIdfromList(i);
	}

	public synchronized void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		List<FishDropData> rewards = FishTable.getInstance().getFishReward(item.getItemId());
		int count = 0;
		player.getInventory().destroyItem(item, 1, true);
		for(FishDropData d : rewards)
		{
			int roll = Util.rollDrop(d.getMinCount(), d.getMaxCount(), d.getChance() * Config.RATE_FISH_DROP_COUNT * Config.RATE_DROP_ITEMS * player.getRateItems() * 10000, false);
			if(roll > 0)
			{
				giveItems(player, d.getRewardItemId(), roll);
				count++;
			}
		}
		if(count == 0)
			player.sendMessage(new CustomMessage("scripts.items.FishItem.Nothing", player));
	}

	public void giveItems(L2Player activeChar, short itemId, int count)
	{
		L2ItemInstance item = ItemTable.getInstance().createItem(itemId);
		item.setCount(count);
		activeChar.getInventory().addItem(item);
		if(count > 1)
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(itemId).addNumber((int) count));
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(itemId));
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