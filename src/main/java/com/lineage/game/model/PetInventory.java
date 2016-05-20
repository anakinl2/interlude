package com.lineage.game.model;

import java.lang.ref.WeakReference;

import com.lineage.game.model.instances.L2ItemInstance.ItemLocation;
import com.lineage.game.model.instances.L2PetInstance;

public class PetInventory extends Inventory
{
	private WeakReference<L2PetInstance> _owner;

	public PetInventory(L2PetInstance owner)
	{
		_owner = new WeakReference<L2PetInstance>(owner);
	}

	@Override
	public L2PetInstance getOwner()
	{
		return _owner.get();
	}

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
}