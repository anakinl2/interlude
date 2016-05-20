package com.lineage.game.model;

import java.lang.ref.WeakReference;

import com.lineage.game.model.instances.L2ItemInstance.ItemLocation;

public class PcFreight extends Warehouse
{
	private WeakReference<L2Player> _owner; // This is the L2Player that owns this Freight;

	public PcFreight(L2Player owner)
	{
		_owner = new WeakReference<L2Player>(owner);
	}

	/**
	 * Returns an int identifying the owner for this PcFreight instance
	 */
	@Override
	public int getOwnerId()
	{
		L2Player owner = _owner.get();
		return owner == null ? 0 : owner.getObjectId();
	}

	/**
	 * Returns an ItemLocation identifying the freight location type
	 */
	@Override
	public ItemLocation getLocationType()
	{
		return ItemLocation.FREIGHT;
	}
}