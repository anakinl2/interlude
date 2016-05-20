package items;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ChooseInventoryItem;
import l2d.game.serverpackets.SystemMessage;

public class EnchantScrolls implements IItemHandler, ScriptFile
{
	private static final int[] _itemIds = {
			729,
			730,
			731,
			732,
			947,
			948,
			949,
			950,
			951,
			952,
			953,
			954,
			955,
			956,
			957,
			958,
			959,
			960,
			961,
			962,
			6569,
			6570,
			6571,
			6572,
			6573,
			6574,
			6575,
			6576,
			6577,
			6578 };

	static final SystemMessage SELECT_ITEM_TO_ENCHANT = new SystemMessage(SystemMessage.SELECT_ITEM_TO_ENCHANT);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		player.setEnchantScroll(item);
		player.sendPacket(SELECT_ITEM_TO_ENCHANT);
		player.sendPacket(new ChooseInventoryItem(item.getItemId()));
		return;
	}

	public final int[] getItemIds()
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