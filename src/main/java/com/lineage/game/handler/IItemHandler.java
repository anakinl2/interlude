package com.lineage.game.handler;

import com.lineage.game.model.L2Playable;
import com.lineage.game.model.instances.L2ItemInstance;

public interface IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item);

	public int[] getItemIds();
}
