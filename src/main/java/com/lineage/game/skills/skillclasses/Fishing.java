package com.lineage.game.skills.skillclasses;

import java.util.List;

import com.lineage.game.templates.L2Weapon;
import javolution.util.FastList;
import com.lineage.game.GameTimeController;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.FishData;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ExFishingStart;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.FishTable;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Location;
import com.lineage.util.Rnd;
import com.lineage.util.Util;

public class Fishing extends L2Skill
{
	public Fishing(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		L2Player player = (L2Player) activeChar;

		if(player.getSkillLevel(SKILL_FISHING_MASTERY) == -1)
			return false;

		if(player.isFishing())
		{
			if(player.getFishCombat() != null)
				player.getFishCombat().doDie(false);
			else
				player.endFishing(false);
			player.sendPacket(new SystemMessage(SystemMessage.CANCELS_FISHING));
			return false;
		}

		if(player.isInBoat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_WHILE_YOU_ARE_ON_BOARD));
			return false;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_FISH_WHILE_USING_A_RECIPE_BOOK_PRIVATE_MANUFACTURE_OR_PRIVATE_STORE));
			return false;
		}

		int rnd = Rnd.get(50) + 150;
		double angle = Util.convertHeadingToDegree(player.getHeading());
		double radian = Math.toRadians(angle - 90);
		double sin = Math.sin(radian);
		double cos = Math.cos(radian);
		int x1 = -(int) (sin * rnd);
		int y1 = (int) (cos * rnd);
		int x = player.getX() + x1;
		int y = player.getY() + y1;
		int z = GeoEngine.getHeight(x, y, player.getZ());
		player.setFishLoc(new Location(x, y, z));

		if(!ZoneManager.getInstance().checkIfInZoneFishing(x, y, z))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_HERE));
			return false;
		}

		L2Weapon weaponItem = player.getActiveWeaponItem();
		if(weaponItem == null || weaponItem.getItemType() != L2Weapon.WeaponType.ROD)
		{
			//Fishing poles are not installed
			player.sendPacket(new SystemMessage(SystemMessage.FISHING_POLES_ARE_NOT_INSTALLED));
			return false;
		}

		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(lure == null || lure.getCount() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.BAITS_ARE_NOT_PUT_ON_A_HOOK));
			return false;
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Character> targets)
	{
		if(caster == null || !caster.isPlayer())
			return;

		L2Player player = (L2Player) caster;

		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(lure == null || lure.getCount() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.BAITS_ARE_NOT_PUT_ON_A_HOOK));
			return;
		}

		L2ItemInstance lure2 = player.getInventory().destroyItem(player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, false);
		if(lure2 == null || lure2.getCount() == 0)
			player.sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_BAIT));

		int lvl = FishTable.getInstance().GetRandomFishLvl(player);
		int group = FishTable.getInstance().GetGroupForLure(lure.getItemId());
		int type = FishTable.getInstance().GetRandomFishType(group, lure.getItemId());

		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if(fishs == null || fishs.size() == 0)
		{
			player.sendMessage("Error: Fishes are not definied for lvl " + lvl + " type " + type + " group " + group + ", report admin please.");
			player.endFishing(false);
			return;
		}

		int check = Rnd.get(fishs.size());
		FishData fish = fishs.get(check);

		if(!GameTimeController.getInstance().isNowNight() && lure.isNightLure())
			fish.setType(-1);

		player.stopMove();
		player.setImobilised(true);
		player.setFishing(true);
		player.setFish(fish);
		player.setLure(lure);
		player.broadcastUserInfo(true);
		player.broadcastPacket(new ExFishingStart(player, fish.getType(), player.getFishLoc(), lure.isNightLure()));
		player.sendPacket(new SystemMessage(SystemMessage.STARTS_FISHING));
		player.startLookingForFishTask();
	}
}