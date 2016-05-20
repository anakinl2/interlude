package events.thefallharvest;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.SkillTable;

public class Nectar implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = { 6391 };

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2Player player = (L2Player) playable;
		L2Character target = (L2Character) player.getTarget();

		if(!(target instanceof SquashInstance))
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(2005, 1);
		if(skill != null) // && skill.checkCondition(player, target, true, false, true))
			player.getAI().Cast(skill, target);
	}

	public int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
