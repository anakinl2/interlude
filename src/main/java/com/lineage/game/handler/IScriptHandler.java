package com.lineage.game.handler;

import com.lineage.game.model.L2Player;
import com.lineage.util.Location;

public interface IScriptHandler
{
	public Location onEscape(L2Player player);

	public void onEnterWorld(L2Player player);

}
