package items;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ExAutoSoulShot;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Weapon;

public class BlessedSpiritShot implements IItemHandler, ScriptFile
{
	// all the items ids that this handler knowns
	private static final int[] _itemIds = {
			3947,	// Blessed Spiritshot: No Grade
			3948,	// Blessed Spiritshot: D-Grade
			3949,	// Blessed Spiritshot: C-Grade
			3950,	// Blessed Spiritshot: B-Grade
			3951,	// Blessed Spiritshot: A-Grade
			3952	// Blessed Spiritshot: S Grade
	};
	private static final short[] _skillIds = {
			2061,	// Blessed Spiritshot: No Grade
			2160,	// Blessed Spiritshot: D-Grade
			2161,	// Blessed Spiritshot: C-Grade
			2162,	// Blessed Spiritshot: B-Grade
			2163,	// Blessed Spiritshot: A-Grade
			2164	// Blessed Spiritshot: S Grade
	};

	//static final SystemMessage POWER_OF_MANA_ENABLED = new SystemMessage(SystemMessage.POWER_OF_MANA_ENABLED);
	static final SystemMessage NOT_ENOUGH_SPIRITSHOTS = new SystemMessage(SystemMessage.NOT_ENOUGH_SPIRITSHOTS);
	static final SystemMessage SPIRITSHOT_DOES_NOT_MATCH_WEAPON_GRADE = new SystemMessage(SystemMessage.SPIRITSHOT_DOES_NOT_MATCH_WEAPON_GRADE);
	static final SystemMessage CANNOT_USE_SPIRITSHOTS = new SystemMessage(SystemMessage.CANNOT_USE_SPIRITSHOTS);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(player.isInOlympiadMode())
		{
			player.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		L2ItemInstance weaponInst = player.getActiveWeaponInstance();
		L2Weapon weaponItem = player.getActiveWeaponItem();
		int SoulshotId = item.getItemId();
		boolean isAutoSoulShot = false;
		L2Item itemTemplate = ItemTable.getInstance().getTemplate(item.getItemId());

		if(player.getAutoSoulShot().contains(SoulshotId))
			isAutoSoulShot = true;

		if(weaponInst == null)
		{
			if(isAutoSoulShot)
			{
				player.removeAutoSoulShot(SoulshotId);
				player.sendPacket(new ExAutoSoulShot(SoulshotId, false));
				player.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(itemTemplate.getName()));
				return;
			}
			player.sendPacket(CANNOT_USE_SPIRITSHOTS);
			return;
		}

		if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			// already charged by blessed spirit shot
			// btw we cant charge only when bsps is charged
			return;

		int spiritshotId = item.getItemId();
		int grade = weaponItem.getCrystalType().externalOrdinal;
		int blessedsoulSpiritConsumption = weaponItem.getSpiritShotCount();
		int count = item.getIntegerLimitedCount();

		if(blessedsoulSpiritConsumption == 0)
		{
			// Can't use Spiritshots
			if(isAutoSoulShot)
			{
				player.removeAutoSoulShot(SoulshotId);
				player.sendPacket(new ExAutoSoulShot(SoulshotId, false));
				player.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(itemTemplate.getName()));
				return;
			}
			player.sendPacket(CANNOT_USE_SPIRITSHOTS);
			return;
		}

		if(grade == 0 && spiritshotId != 3947 || grade == 1 && spiritshotId != 3948 || grade == 2 && spiritshotId != 3949 || grade == 3 && spiritshotId != 3950 || grade == 4 && spiritshotId != 3951 || grade == 5 && spiritshotId != 3952)
		{
			if(isAutoSoulShot)
				return;
			player.sendPacket(SPIRITSHOT_DOES_NOT_MATCH_WEAPON_GRADE);
			return;
		}

		if(count < blessedsoulSpiritConsumption)
		{
			if(isAutoSoulShot)
			{
				player.removeAutoSoulShot(SoulshotId);
				player.sendPacket(new ExAutoSoulShot(SoulshotId, false));
				player.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(itemTemplate.getName()));
				return;
			}
			player.sendPacket(NOT_ENOUGH_SPIRITSHOTS);
			return;
		}

		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
		player.getInventory().destroyItem(item, blessedsoulSpiritConsumption, false);
		//player.sendPacket(POWER_OF_MANA_ENABLED);
		player.broadcastPacket(new MagicSkillUse(player, player, _skillIds[grade], 1, 0, 0));
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