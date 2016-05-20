package l2d.game.model;

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2ItemInstance.ItemLocation;
import l2d.game.tables.ItemTable;

public class PcInventory extends Inventory
{
	private WeakReference<L2Player> _owner;

	public PcInventory(L2Player owner)
	{
		_owner = new WeakReference<L2Player>(owner);
	}

	@Override
	public L2Player getOwner()
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

	public int getAdena()
	{
		L2ItemInstance _adena = getItemByItemId(57);
		if(_adena == null)
			return 0;
		return _adena.getIntegerLimitedCount();
	}

	/**
	 * Get all augmented items
	 */
	public ArrayList<L2ItemInstance> getAugmentedItems()
	{
		ArrayList<L2ItemInstance> list = new ArrayList<L2ItemInstance>();
		for(L2ItemInstance item : getItems())
			if(item != null && item.isAugmented())
				list.add(item);
		return list;
	}

	/**
	 * Добавляет адену игроку.<BR><BR>
	 * 
	 * @param adena
	 *            - сколько адены дать
	 * @return L2ItemInstance - новое количество адены
	 */
	public L2ItemInstance addAdena(int adena)
	{
		L2ItemInstance _adena = ItemTable.getInstance().createItem(57);
		_adena.setCount(adena);
		_adena = addItem(_adena);
		//Log.LogItem(getOwner(), Log.Sys_GetItem, _adena);
		return _adena;
	}

	public L2ItemInstance reduceAdena(int adena)
	{
		return destroyItemByItemId(57, adena, true);
	}

	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[25][3];
		ThreadConnection con = null;
		FiltredPreparedStatement statement2 = null;
		ResultSet invdata = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
			statement2.setInt(1, objectId);
			invdata = statement2.executeQuery();

			while(invdata.next())
			{
				int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				paperdoll[slot][1] = invdata.getInt("item_id");
				paperdoll[slot][2] = invdata.getInt("enchant_level");
			}
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not restore inventory:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement2, invdata);
		}
		return paperdoll;
	}

	public boolean validateCapacity(L2ItemInstance item)
	{
		int slots = getSize();

		if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
			slots++;

		return validateCapacity(slots);
	}

	public boolean validateCapacity(List<L2ItemInstance> items)
	{
		int slots = getSize();

		for(L2ItemInstance item : items)
			if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
				slots++;

		return validateCapacity(slots);
	}

	public boolean validateCapacity(int slots)
	{
		L2Player owner = getOwner();
		if(owner == null)
			return false;
		return slots <= owner.getInventoryLimit();
	}

	public short slotsLeft()
	{
		L2Player owner = getOwner();
		if(owner == null)
			return 0;
		short slots = (short) (owner.getInventoryLimit() - getSize());
		return slots > 0 ? slots : 0;
	}

	public boolean validateWeight(L2ItemInstance item)
	{
		long weight = item.getItem().getWeight() * item.getIntegerLimitedCount();
		return validateWeight(weight);
	}

	public boolean validateWeight(long weight)
	{
		L2Player owner = getOwner();
		if(owner == null)
			return false;
		return getTotalWeight() + weight <= owner.getMaxLoad();
	}

	@Override
	public void restore()
	{
		super.restore();
	}
}