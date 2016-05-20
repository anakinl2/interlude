package items;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;

public class Scrolls implements IItemHandler, ScriptFile
{
	private static final int[] _itemIds = { 8515, 8516, 8517, 8518, 8519, 8520 // charms of courage
	};

	static final SystemMessage INCOMPATIBLE_ITEM_GRADE_THIS_ITEM_CANNOT_BE_USED = new SystemMessage(SystemMessage.INCOMPATIBLE_ITEM_GRADE_THIS_ITEM_CANNOT_BE_USED);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(player.isActionsDisabled() || player.isSitting())
		{
			player.sendActionFailed();
			return;
		}

		int itemId = item.getItemId();

		if(!validateScrollGrade(player.getLevel(), itemId))
		{
			player.sendPacket(INCOMPATIBLE_ITEM_GRADE_THIS_ITEM_CANNOT_BE_USED);
			return;
		}
		player.getInventory().destroyItem(item, 1, true);
		player.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addItemName(itemId));
		player.setCharmOfCourage(true);
	}

	public boolean validateScrollGrade(byte playerLvl, int itemId)
	{
		switch(itemId)
		{
			case 8515:
				return playerLvl <= 19;
			case 8516:
				return playerLvl >= 20 && playerLvl <= 39;
			case 8517:
				return playerLvl >= 40 && playerLvl <= 51;
			case 8518:
				return playerLvl >= 52 && playerLvl <= 60;
			case 8519:
				return playerLvl >= 61 && playerLvl <= 75;
			case 8520:
				return playerLvl >= 76;
		}
		return false;
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