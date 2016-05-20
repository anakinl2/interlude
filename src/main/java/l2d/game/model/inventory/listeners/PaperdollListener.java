package l2d.game.model.inventory.listeners;

import l2d.game.model.instances.L2ItemInstance;

public interface PaperdollListener
{
	public void notifyEquipped(int slot, L2ItemInstance inst);

	public void notifyUnequipped(int slot, L2ItemInstance inst);
}
