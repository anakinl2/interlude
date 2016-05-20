package l2d.game.handler;

import l2d.game.model.L2Playable;
import l2d.game.model.instances.L2ItemInstance;

public interface IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item);

	public int[] getItemIds();
}
