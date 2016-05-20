package items;

import com.lineage.ext.scripts.ScriptFile;
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

public class SpiritShot implements IItemHandler, ScriptFile
{
	// all the items ids that this handler knowns
	private static final int[] _itemIds = {
			5790,	// Spiritshot: No Grade for Beginners
			2509,	// Spiritshot: No Grade
			2510,	// Spiritshot: D-grade
			2511,	// Spiritshot: C-grade
			2512,	// Spiritshot: B-grade
			2513,	// Spiritshot: A-grade
			2514	// Spiritshot: S-grade
	};
	private static final short[] _skillIds = {
			2061,	// Blessed Spiritshot: No Grade
			2155,	// Spiritshot: D-Grade
			2156,	// Spiritshot: C-Grade
			2157,	// Spiritshot: B-Grade
			2158,	// Spiritshot: A-Grade
			2159	// Spiritshot: S-Grade
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

		// spiritshot is already active
		if(weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			return;

		int SpiritshotId = item.getItemId();
		int grade = weaponItem.getCrystalType().externalOrdinal;
		int soulSpiritConsumption = weaponItem.getSpiritShotCount();
		int count = item.getIntegerLimitedCount();

		if(soulSpiritConsumption == 0)
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

		if(grade == 0 && SpiritshotId != 5790 && SpiritshotId != 2509 || grade == 1 && SpiritshotId != 2510 || grade == 2 && SpiritshotId != 2511 || grade == 3 && SpiritshotId != 2512 || grade == 4 && SpiritshotId != 2513 || grade == 5 && SpiritshotId != 2514)
		{
			// wrong grade for weapon
			if(isAutoSoulShot)
				return;
			player.sendPacket(SPIRITSHOT_DOES_NOT_MATCH_WEAPON_GRADE);
			return;
		}

		if(!(count >= soulSpiritConsumption))
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

		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
		player.getInventory().destroyItem(item, soulSpiritConsumption, false);
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