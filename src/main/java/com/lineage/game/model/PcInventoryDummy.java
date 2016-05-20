package com.lineage.game.model;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.listeners.PaperdollListener;

public class PcInventoryDummy extends PcInventory
{
	public static final PcInventoryDummy instance = new PcInventoryDummy();
	static final L2ItemInstance[] noitems = new L2ItemInstance[0];
	static final ConcurrentLinkedQueue<L2ItemInstance> noitems_list = new ConcurrentLinkedQueue<L2ItemInstance>();

	public PcInventoryDummy()
	{
		super(null);
	}

	@Override
	public L2ItemInstance[] getItems()
	{
		return noitems;
	}

	@Override
	public ConcurrentLinkedQueue<L2ItemInstance> getItemsList()
	{
		return noitems_list;
	}

	@Override
	protected L2ItemInstance addItem(L2ItemInstance newItem, boolean dbUpdate, boolean log)
	{
		return null;
	}

	@Override
	public L2ItemInstance addAdena(int adena)
	{
		return null;
	}

	@Override
	public synchronized void deleteMe()
	{}

	@Override
	public void updateDatabase(boolean commit)
	{}

	@Override
	public L2ItemInstance destroyItem(int objectId, int count, boolean toLog)
	{
		return null;
	}

	@Override
	public L2ItemInstance destroyItem(L2ItemInstance item, long count, boolean toLog)
	{
		return null;
	}

	@Override
	public L2ItemInstance dropItem(int objectId, long count)
	{
		return null;
	}

	@Override
	public L2ItemInstance dropItem(L2ItemInstance item, int count, boolean whflag)
	{
		return null;
	}

	@Override
	public L2ItemInstance dropItem(L2ItemInstance oldItem, long count)
	{
		return null;
	}

	@Override
	public void restore()
	{}

	@Override
	public L2ItemInstance destroyItemByItemId(int itemId, long count, boolean toLog)
	{
		return null;
	}

	@Override
	public boolean validateCapacity(int slots)
	{
		return false;
	}

	@Override
	public short slotsLeft()
	{
		return 0;
	}

	@Override
	public boolean validateWeight(long weight)
	{
		return false;
	}

	@Override
	public L2ItemInstance addItem(int id, int count, int source, String create_type)
	{
		return null;
	}

	@Override
	public L2ItemInstance getPaperdollItem(int slot)
	{
		return null;
	}

	@Override
	public int getPaperdollItemId(int slot)
	{
		return 0;
	}

	@Override
	public int getPaperdollObjectId(int slot)
	{
		return 0;
	}

	@Override
	public synchronized void addPaperdollListener(PaperdollListener listener)
	{}

	@Override
	public synchronized void removePaperdollListener(PaperdollListener listener)
	{}

	@Override
	public L2ItemInstance setPaperdollItem(int slot, L2ItemInstance item)
	{
		return null;
	}

	@Override
	public void unEquipItemInBodySlotAndNotify(L2Player cha, int slot, L2ItemInstance item)
	{}

	@Override
	public L2ItemInstance unEquipItemInSlot(int pdollSlot)
	{
		return null;
	}

	@Override
	public void unEquipItemInBodySlot(int slot, L2ItemInstance item)
	{}

	@Override
	public synchronized void equipItem(L2ItemInstance item)
	{}

	@Override
	public L2ItemInstance FindEquippedLure()
	{
		return null;
	}

	@Override
	public void validateItems()
	{}

	@Override
	public void sort(int[][] order)
	{}
}