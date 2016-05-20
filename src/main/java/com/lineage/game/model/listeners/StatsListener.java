package com.lineage.game.model.listeners;

import com.lineage.game.model.Inventory;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.skills.funcs.Func;

public final class StatsListener implements PaperdollListener
{
	Inventory _inv;

	public StatsListener(Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		_inv.getOwner().removeStatsOwner(item);
		_inv.getOwner().updateStats();
		if(_inv.getOwner().getPet() != null)
		{
			_inv.getOwner().getPet().removeStatsOwner(item);
			_inv.getOwner().getPet().updateStats();
		}
	}

	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		_inv.getOwner().addStatFuncs(item.getStatFuncs());
		_inv.getOwner().updateStats();
		if(_inv.getOwner().getPet() != null && item.getAttributeFuncTemplate() != null)
		{
			Func f = item.getAttributeFuncTemplate().getFunc(item);
			_inv.getOwner().getPet().addStatFunc(f);
			_inv.getOwner().getPet().updateStats();
		}
	}
}