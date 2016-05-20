package items;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.L2Weapon.WeaponType;

public class FishShots implements IItemHandler, ScriptFile
{
	// All the item IDs that this handler knows.
	private static int[] _itemIds = { 6535, 6536, 6537, 6538, 6539, 6540 };
	private static int[] _skillIds = { 2181, 2182, 2183, 2184, 2185, 2186 };

	static final SystemMessage THIS_FISHING_SHOT_IS_NOT_FIT_FOR_THE_FISHING_POLE_CRYSTAL = new SystemMessage(SystemMessage.THIS_FISHING_SHOT_IS_NOT_FIT_FOR_THE_FISHING_POLE_CRYSTAL);
	static final SystemMessage POWER_OF_MANA_ENABLED = new SystemMessage(SystemMessage.POWER_OF_MANA_ENABLED);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;
		int FishshotId = item.getItemId();

		boolean isAutoSoulShot = false;
		if(player.getAutoSoulShot().contains(FishshotId))
			isAutoSoulShot = true;

		L2ItemInstance weaponInst = player.getActiveWeaponInstance();
		L2Weapon weaponItem = player.getActiveWeaponItem();

		if(weaponInst == null || weaponItem.getItemType() != WeaponType.ROD)
		{
			if(isAutoSoulShot)
				player.removeAutoSoulShot(FishshotId);
			return;
		}
		if(item.getIntegerLimitedCount() < 1)
		{
			if(isAutoSoulShot)
				player.removeAutoSoulShot(FishshotId);
			return;
		}

		// spiritshot is already active
		if(weaponInst.getChargedFishshot())
			return;

		int grade = weaponItem.getCrystalType().externalOrdinal;

		if(grade == 0 && FishshotId != 6535 || grade == 1 && FishshotId != 6536 || grade == 2 && FishshotId != 6537 || grade == 3 && FishshotId != 6538 || grade == 4 && FishshotId != 6539 || grade == 5 && FishshotId != 6540)
		{
			if(isAutoSoulShot)
				return;
			player.sendPacket(THIS_FISHING_SHOT_IS_NOT_FIT_FOR_THE_FISHING_POLE_CRYSTAL);
			return;
		}

		weaponInst.setChargedFishshot(true);
		player.getInventory().destroyItem(item.getObjectId(), 1, false);
		player.sendPacket(POWER_OF_MANA_ENABLED);
		player.broadcastPacket(new MagicSkillUse(player, player, _skillIds[grade], 1, 0, 0));
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