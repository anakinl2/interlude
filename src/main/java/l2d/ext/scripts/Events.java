package l2d.ext.scripts;

import l2d.ext.scripts.Scripts.ScriptClassAndMethod;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.util.Strings;

public final class Events
{
	public static boolean onAction(L2Player player, L2Object obj)
	{
		ScriptClassAndMethod handler = Scripts.onAction.get(obj.getL2ClassShortName());
		if(handler == null)
			return false;
		return Strings.parseBoolean(Scripts.callScripts(handler.scriptClass, handler.method,player, new Object[] { player, obj }));
	}

	public static boolean onActionShift(L2Player player, L2Object obj)
	{
		ScriptClassAndMethod handler = Scripts.onActionShift.get(obj.getL2ClassShortName());
		if(handler == null && obj.isNpc())
			handler = Scripts.onActionShift.get("L2NpcInstance");
		if(handler == null && obj.isSummon())
			handler = Scripts.onActionShift.get("L2SummonInstance");
		if(handler == null && obj.isPet())
			handler = Scripts.onActionShift.get("L2PetInstance");
		if(handler == null)
			return false;
		return Strings.parseBoolean(Scripts.callScripts(handler.scriptClass, handler.method,player, new Object[] { player, obj }));
	}
}