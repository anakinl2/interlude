package items;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.MagicSkillUse;

public class CrystalCarol implements IItemHandler, ScriptFile
{
	private final static int[] _itemIds = {
			5562,	// Echo Crystal - 1st Carol
			5563,	// Echo Crystal - 2nd Carol
			5564,	// Echo Crystal - 3rd Carol
			5565,	// Echo Crystal - 4th Carol
			5566,	// Echo Crystal - 5th Carol
			5583,	// Echo Crystal - 6th Carol
			5584,	// Echo Crystal - 7th Carol
			5585,	// Echo Crystal - 8th Carol
			5586,	// Echo Crystal - 9th Carol
			5587,	// Echo Crystal - 10th Carol
			4411,	// Echo Crystal - Theme of Journey
			4412,	// Echo Crystal - Theme of Battle
			4413,	// Echo Crystal - Theme of Love
			4414,	// Echo Crystal - Theme of Solitude
			4415,	// Echo Crystal - Theme of the Feast
			4416,	// Echo Crystal - Theme of Celebration
			4417,	// Echo Crystal - Theme of Comedy
			5010,	// Echo Crystal - Theme of Victory
			7061,	// Echo Crystal - Theme of Birthday
			7062,	// Echo Crystal - Theme of Wedding
			6903,	// Music Box M
			8555	// Echo Crystal - Viva Victory Korea
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		int itemId = item.getItemId();
		int skillId;
		switch(itemId)
		{
			default:
			case 5562:
				skillId = 2140;
				break;
			case 5563:
				skillId = 2141;
				break;
			case 5564:
				skillId = 2142;
				break;
			case 5565:
				skillId = 2143;
				break;
			case 5566:
				skillId = 2144;
				break;
			case 5583:
				skillId = 2145;
				break;
			case 5584:
				skillId = 2146;
				break;
			case 5585:
				skillId = 2147;
				break;
			case 5586:
				skillId = 2148;
				break;
			case 5587:
				skillId = 2149;
				break;
			case 4411:
				skillId = 2069;
				break;
			case 4412:
				skillId = 2068;
				break;
			case 4413:
				skillId = 2070;
				break;
			case 4414:
				skillId = 2072;
				break;
			case 4415:
				skillId = 2071;
				break;
			case 4416:
				skillId = 2073;
				break;
			case 4417:
				skillId = 2067;
				break;
			case 5010:
				skillId = 2066;
				break;
			case 7061:
				skillId = 2073;
				break;
			case 7062:
				skillId = 2230;
				break;
			case 8555:
				skillId = 2272;
				break;
			case 6903:
				skillId = 2187;
				break;
		}

		player.getInventory().destroyItem(item, 1, true);
		player.broadcastPacket(new MagicSkillUse(player, player, skillId, 1, 1, 0));
	}

	public final int[] getItemIds()
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