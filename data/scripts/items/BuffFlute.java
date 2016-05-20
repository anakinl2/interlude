package items;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import services.NPCBuffer.buffflute;

public class BuffFlute implements IItemHandler, ScriptFile
{
	private static final int[] ITEM_IDS = {1770};

	@Override
	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;
		buffflute.main_page(player);
	}

	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
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