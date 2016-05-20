package l2d.game.handler;

import l2d.game.model.L2Player;
import l2d.util.Location;

public interface IScriptHandler
{
	public Location onEscape(L2Player player);

	public void onEnterWorld(L2Player player);

}
