package items;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
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