package com.lineage.game.model;

import java.lang.ref.WeakReference;

import com.lineage.game.model.instances.L2ItemInstance.ItemLocation;

public class PcWarehouse extends Warehouse
{
	private WeakReference<L2Player> _owner;

	public PcWarehouse(L2Player owner)
	{
		_owner = new WeakReference<L2Player>(owner);
	}

	@Override
	public int getOwnerId()
	{
		L2Player owner = _owner.get();
		return owner == null ? 0 : owner.getObjectId();
	}

	@Override
	public ItemLocation getLocationType()
	{
		return ItemLocation.WAREHOUSE;
	}
}