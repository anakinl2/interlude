package l2d.game.model.inventory.listeners;

import l2d.game.model.Inventory;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.templates.L2Weapon.WeaponType;

public final class BowListener implements PaperdollListener
{
	Inventory _inv;

	public BowListener(Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(slot != Inventory.PAPERDOLL_RHAND)
			return;
		if(item.getItemType() == WeaponType.BOW || item.getItemType() == WeaponType.ROD)
			_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
	}

	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(slot != Inventory.PAPERDOLL_RHAND)
			return;
		if(item.getItemType() == WeaponType.BOW)
		{
			L2ItemInstance arrow = _inv.findArrowForBow(item.getItem());
			if(arrow != null)
				_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, arrow);
		}
		if(item.getItemType() == WeaponType.ROD)
		{
			L2ItemInstance bait = _inv.FindEquippedLure();
			if(bait != null)
				_inv.setPaperdollItem(Inventory.PAPERDOLL_LHAND, bait);
		}
	}
}