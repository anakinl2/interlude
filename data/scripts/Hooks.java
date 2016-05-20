import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IScriptHandler;
import com.lineage.game.handler.ScriptHandler;
import com.lineage.game.model.L2Player;
import com.lineage.util.Location;

public class Hooks implements IScriptHandler, ScriptFile
{
	public Location onEscape(L2Player player)
	{
/*		Location loc;
		loc = DionArena.getInstance().onEscape(player);
		if(loc != null)
			return loc;
		loc = GludinArena.getInstance().onEscape(player);
		if(loc != null)
			return loc;
		loc = GiranArena.getInstance().onEscape(player);
		if(loc != null)
			return loc;
		loc = Tournament_battle.onEscape(player);
		if(loc != null)
			return loc;
		loc = LastHero.onEscape(player);
		if(loc != null)
			return loc;
		loc = TvT.onEscape(player);
		if(loc != null)
			return loc;*/
		return null;
	}
	
	public void onEnterWorld(L2Player player)
	{
	}
	
	public void onLoad()
	{
		ScriptHandler.getInstance().registerScriptHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}