package items;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.MagicSkillUse;

public class CharChangePotions implements IItemHandler, ScriptFile
{
	private static final int[] _itemIds = {
			5235,	// Facelifting Potion - A
			5236,	// Facelifting Potion - B
			5237,	// Facelifting Potion - C
			5238,	// Dye Potion - A
			5239,	// Dye Potion - B
			5240,	// Dye Potion - C
			5241,	// Dye Potion - D
			5242,	// Hair Style Change Potion - A
			5243,	// Hair Style Change Potion - B
			5244,	// Hair Style Change Potion - C
			5245,	// Hair Style Change Potion - D
			5246,	// Hair Style Change Potion - E
			5247,	// Hair Style Change Potion - F
			5248	// Hair Style Change Potion - G
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		int itemId = item.getItemId();

		if(player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}

		switch(itemId)
		{
			case 5235:
				player.setFace((byte) 0);
				break;
			case 5236:
				player.setFace((byte) 1);
				break;
			case 5237:
				player.setFace((byte) 2);
				break;
			case 5238:
				player.setHairColor((byte) 0);
				break;
			case 5239:
				player.setHairColor((byte) 1);
				break;
			case 5240:
				player.setHairColor((byte) 2);
				break;
			case 5241:
				player.setHairColor((byte) 3);
				break;
			case 5242:
				player.setHairStyle((byte) 0);
				break;
			case 5243:
				player.setHairStyle((byte) 1);
				break;
			case 5244:
				player.setHairStyle((byte) 2);
				break;
			case 5245:
				player.setHairStyle((byte) 3);
				break;
			case 5246:
				player.setHairStyle((byte) 4);
				break;
			case 5247:
				player.setHairStyle((byte) 5);
				break;
			case 5248:
				player.setHairStyle((byte) 6);
				break;
		}

		player.getInventory().destroyItem(item, 1, true);
		player.broadcastPacket(new MagicSkillUse(player, player, 2003, 1, 1, 0));
		player.broadcastUserInfo(true);
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