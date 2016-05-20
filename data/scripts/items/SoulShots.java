package items;

import l2d.ext.scripts.ScriptFile;
import l2d.Config;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExAutoSoulShot;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.L2Weapon.WeaponType;
import l2d.util.Rnd;

public class SoulShots implements IItemHandler, ScriptFile
{
	private static final int[] _itemIds = { 5789, 1835, 1463, 1464, 1465, 1466, 1467 };
	private static final short[] _skillIds = { 2039, 2150, 2151, 2152, 2153, 2154 };

	//static final SystemMessage POWER_OF_THE_SPIRITS_ENABLED = new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_ENABLED);
	static final SystemMessage NOT_ENOUGH_SOULSHOTS = new SystemMessage(SystemMessage.NOT_ENOUGH_SOULSHOTS);
	static final SystemMessage SOULSHOT_DOES_NOT_MATCH_WEAPON_GRADE = new SystemMessage(SystemMessage.SOULSHOT_DOES_NOT_MATCH_WEAPON_GRADE);
	static final SystemMessage CANNOT_USE_SOULSHOTS = new SystemMessage(SystemMessage.CANNOT_USE_SOULSHOTS);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		L2Weapon weaponItem = player.getActiveWeaponItem();

		L2ItemInstance weaponInst = player.getActiveWeaponInstance();
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
			player.sendPacket(CANNOT_USE_SOULSHOTS);
			return;
		}

		// soulshot is already active
		if(weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
			return;

		int grade = weaponItem.getCrystalType().externalOrdinal;
		int soulShotConsumption = weaponItem.getSoulShotCount();
		int count = item.getIntegerLimitedCount();

		if(soulShotConsumption == 0)
		{
			// Can't use soulshots
			if(isAutoSoulShot)
			{
				player.removeAutoSoulShot(SoulshotId);
				player.sendPacket(new ExAutoSoulShot(SoulshotId, false));
				player.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(itemTemplate.getName()));
				return;
			}
			player.sendPacket(CANNOT_USE_SOULSHOTS);
			return;
		}

		if(grade == 0 && SoulshotId != 5789 && SoulshotId != 1835 || grade == 1 && SoulshotId != 1463 || grade == 2 && SoulshotId != 1464 || grade == 3 && SoulshotId != 1465 || grade == 4 && SoulshotId != 1466 || grade == 5 && SoulshotId != 1467)
		{
			// wrong grade for weapon
			if(isAutoSoulShot)
				return;
			player.sendPacket(SOULSHOT_DOES_NOT_MATCH_WEAPON_GRADE);
			return;
		}

		if(weaponItem.getItemType() == WeaponType.BOW)
		{
			int newSS = (int) player.calcStat(Stats.SS_USE_BOW, soulShotConsumption, null, null);
			if(newSS < soulShotConsumption && Rnd.chance(player.calcStat(Stats.SS_USE_BOW_CHANCE, soulShotConsumption, null, null)))
				soulShotConsumption = newSS;
		}

		if(count < soulShotConsumption)
		{
			player.sendPacket(NOT_ENOUGH_SOULSHOTS);
			return;
		}

		weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
		player.getInventory().destroyItem(item, soulShotConsumption, false);
		//player.sendPacket(POWER_OF_THE_SPIRITS_ENABLED);
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