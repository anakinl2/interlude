package com.lineage.game.model.listeners;

import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

public final class ItemAugmentationListener implements PaperdollListener
{
	Inventory _inv;

	public ItemAugmentationListener(Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		if(item.isAugmented())
		{
			item.getAugmentation().removeBoni(player);
			player.updateStats();
		}
	}

	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		if(item.isAugmented())
		{
			item.getAugmentation().applyBoni(player);
			player.updateStats();
		}
	}
}