package l2d.game.model.inventory.listeners;

import java.util.ArrayList;

import l2d.game.model.Inventory;
import l2d.game.model.instances.L2ItemInstance;

/**
 * Recorder of alterations in inventory
 */
public final class ChangeRecorder implements PaperdollListener
{
	private final ArrayList<L2ItemInstance> _changed;

	/**
	 * Constructor of the ChangeRecorder
	 * @param inventory inventory to watch
	 */
	public ChangeRecorder(Inventory inventory)
	{
		_changed = new ArrayList<L2ItemInstance>();
		inventory.addPaperdollListener(this);
	}

	/**
	 * Add alteration in inventory when item equipped
	 */
	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!_changed.contains(item))
			_changed.add(item);
	}

	/**
	 * Add alteration in inventory when item unequipped
	 */
	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!_changed.contains(item))
			_changed.add(item);
	}

	/**
	 * Returns alterations in inventory
	 * @return L2ItemInstance[] : array of alterated items
	 */
	public L2ItemInstance[] getChangedItems()
	{
		return _changed.toArray(new L2ItemInstance[_changed.size()]);
	}
}
