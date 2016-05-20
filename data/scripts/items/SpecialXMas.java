package items;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ShowXMasSeal;

public class SpecialXMas implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = { 5555 };

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!playable.isPlayer())
			return;
		L2Player activeChar = (L2Player) playable;
		int itemId = item.getItemId();

		if(itemId == 5555) // Token of Love
		{
			ShowXMasSeal SXS = new ShowXMasSeal(5555);
			activeChar.broadcastPacket(SXS);
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