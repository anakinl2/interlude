package events.donate;

import javolution.util.FastList;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.instancemanager.ServerVariables;
import com.lineage.Config;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.SocialAction;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Item.Grade;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Files;
import com.lineage.util.Log;

public class Shop extends Functions implements ScriptFile
{
	private static int hmsg;
	private static boolean _active = false;
	@SuppressWarnings("unused")
	private L2NpcInstance shop = npc;

	private static boolean isActive()
	{
		return ServerVariables.getString("Donate", "off").equalsIgnoreCase("on");
	}

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			System.out.println("Loaded Event: Donate [state: activated]");
		}
		else
			System.out.println("Loaded Event: Donate [state: deactivated]");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if( !player.getPlayerAccess().IsEventGm)
			return;

		if( !isActive())
		{
			ServerVariables.set("Donate", "on");
			_active = true;
			System.out.println("Event: Donate started.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.donate.Shop.AnnounceEventStarted", null);
		}
		else
			player.sendMessage("Event 'Donate' already started.");
		_active = true;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if( !player.getPlayerAccess().IsEventGm)
			return;
		if(isActive())
		{
			ServerVariables.unset("Donate");
			System.out.println("Event: Donate stopped.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.donate.Shop.AnnounceEventStoped", null);
		}
		else
			player.sendMessage("Event 'Donate' not started.");
		_active = false;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public static void Chalange(String[] var)
	{
		L2Player player = (L2Player) self;

		if( !_active)
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
			{
				player.sendMessage("Евент временно отключен.");
			}
			else
			{
				player.sendMessage("Event temporarily disabled.");
			}
			return;
		}

		try
		{
			hmsg = Integer.valueOf(var[0]);
		}
		catch(Exception e)
		{
			show(Files.read("data/scripts/events/donate/NothingToSay.htm", player), player);
		}

		if(hmsg == 1)
			show(Files.read("data/scripts/events/donate/EnchWeapon.htm", player), player);
		else if(hmsg == 2)
			show(Files.read("data/scripts/events/donate/EnchArmor.htm", player), player);
		else if(hmsg == 3)
			show(Files.read("data/scripts/events/donate/EnchJewels.htm", player), player);
		else if(hmsg == 4)
			show(Files.read("data/scripts/events/donate/HeroStatus.htm", player), player);
		else if(hmsg == 5)
			show(Files.read("data/scripts/events/donate/RaidJewels.htm", player), player);
		else if(hmsg == 6)
			show(Files.read("data/scripts/events/donate/Nobless.htm", player), player);
		else if(hmsg == 7)
			show(Files.read("data/scripts/events/donate/Money.htm", player), player);
		else if(hmsg == 8)
			show(Files.read("data/scripts/events/donate/Unjail.htm", player), player);
		else if(hmsg == 9)
			show(Files.read("data/scripts/events/donate/Unbun.htm", player), player);
		else if(hmsg == 10 && _active)
			show(Files.read("data/scripts/events/donate/Main.htm", player), player);
		else
			show(Files.read("data/scripts/events/donate/NothingToSay.htm", player), player);
	}

	public static void enchantWeapon(String[] var)
	{
		L2Player player = (L2Player) self;

		int EnchVal = Config.ENCHANT_WEAPON_NUMBER_LIST[Integer.valueOf(var[0])];
		int CoinCount = Config.ENCHANT_WEAPON_COAL_LIST[Integer.valueOf(var[0])];

		if( !checkCondition(player, CoinCount))
			return;

		L2ItemInstance WeaponInSlot = null;

		L2ItemInstance weaponType = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

		if(weaponType != null && weaponType.getEquipSlot() == Inventory.PAPERDOLL_RHAND)
			WeaponInSlot = weaponType;
		else
		{
			// for bows and double handed weapons
			weaponType = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
			if(weaponType != null && weaponType.getEquipSlot() == Inventory.PAPERDOLL_LRHAND)
				WeaponInSlot = weaponType;
		}

		if(WeaponInSlot != null)
		{
			removeItem(player, Config.DONATE_ID_PRICE, CoinCount);

			player.getInventory().unEquipItemInSlot(WeaponInSlot.getEquipSlot());
			WeaponInSlot.setEnchantLevel(EnchVal);
			player.getInventory().equipItem(WeaponInSlot);

			player.sendPacket(new InventoryUpdate().addModifiedItem(WeaponInSlot));
			player.sendChanges();

			// сообщение
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("Ваше оружие было заточено до " + EnchVal + ".");
			else
				player.sendMessage("Your weapon has been enchanted to " + EnchVal + ".");

			Log.add(player + "Has by weapon enchant for" + WeaponInSlot + "weapon", "wmzSeller");
		}

	}

	public void enchantArmor()
	{
		L2Player player = (L2Player) self;

		if( !checkCondition(player, Config.ENCHANT_ARMOR_PRICE))
			return;

		int SetArm = 0;
		for(L2Skill setSkill : player.getAllSkills())
		{
			if(setSkill.getName().equalsIgnoreCase("Equipped with Shield"))
			{}
			if( !(setSkill.getName().endsWith("Set") && setSkill.getId() >= 700))
				continue;
			else
				SetArm = setSkill.getId();
		}

		L2ItemInstance armorInChestSlot = null;
		L2ItemInstance armor = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);

		if(SetArm == 0)
		{
			if(armor.getItem().getName().contains("Dynasty"))
				SetArm = 1;
		}
		if(SetArm == 0)
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("Вам необходимо одеть полный сет для заточки");
			else
				player.sendMessage("You must equip full set for enchant");
			player.sendActionFailed();
			return;
		}

		if(armor != null && armor.getEquipSlot() == Inventory.PAPERDOLL_CHEST)
			armorInChestSlot = armor;
		if(armorInChestSlot.getItem().getItemGrade().ordinal() < Grade.A.ordinal())
			player.sendMessage("Your Armor Must Be A,S or S80 grade");

		removeItem(player, Config.DONATE_ID_PRICE, Config.ENCHANT_ARMOR_PRICE);

		for(L2ItemInstance item : player.getInventory().getItems())
		{
			if(item.isEquipped() && item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR)
			{
				player.getInventory().unEquipItemInSlot(item.getEquipSlot());
				item.setEnchantLevel(10);
				// сообщение
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
					player.sendMessage("Ваша броня была заточена до +10. Спасибо.");
				else
					player.sendMessage("Your armor has been enachant to +10 Thx");
				player.getInventory().equipItem(item);
				player.sendPacket(new InventoryUpdate().addModifiedItem(item));
			}
		}
		player.sendChanges();

		// сообщение
		// if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
		// player.sendMessage("Ваша броня была заточена до +10. Спасибо.");
		// else
		// player.sendMessage("Your armor has been enachant to +10 Thx");

		Log.add(player + "Has by armor enchant for" + armorInChestSlot + "and equiped full armor set for this", "wmzSeller");
	}

	public void enchantJewel()
	{
		L2Player player = (L2Player) self;

		if( !checkCondition(player, Config.ENCHANT_JEWEL_PRICE))
			return;
		FastList<L2ItemInstance> equiped = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : player.getInventory().getItems())
			if(item.isEquipped() && item.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
			{
				equiped.add(item);
			}

		int eqiptotal = equiped.size();

		if(eqiptotal < 5)
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("Вы должны одеть весь сет для заточки");
			else
				player.sendMessage("You must equip full set for enchant");
			return;
		}
		else
		{
			removeItem(player, Config.DONATE_ID_PRICE, Config.ENCHANT_JEWEL_PRICE);
			for(L2ItemInstance jewel : equiped)
			{
				player.getInventory().unEquipItemInSlot(jewel.getEquipSlot());
				jewel.setEnchantLevel(10);
				Log.add(player + "Has by jewel enchant for" + jewel, "wmzSeller");
				player.getInventory().equipItem(jewel);
				player.sendPacket(new InventoryUpdate().addModifiedItem(jewel));
			}
			equiped.clear();
		}
		player.sendChanges();

		// сообщение
		if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
			player.sendMessage("Ваша бижутерия была заточена до +10. Спасибо.");
		else
			player.sendMessage("Your jewels has been enachant to +10 Thx");
	}

	public void Noble()
	{
		L2Player player = (L2Player) self;
		if( !player.isNoble())
		{
			if( !checkCondition(player, Config.NOBLE_PRICE))
				return;
			removeItem(player, Config.DONATE_ID_PRICE, Config.NOBLE_PRICE);

			player.setNoble(true);
			player.addSkill(SkillTable.getInstance().getInfo(1323, 1));
			player.addSkill(SkillTable.getInstance().getInfo(325, 1));
			player.addSkill(SkillTable.getInstance().getInfo(326, 1));
			player.addSkill(SkillTable.getInstance().getInfo(327, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1324, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1325, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1326, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1327, 1));

			if(player.isNoble())
			{
				player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
					player.sendMessage("Теперь ты Дворянин!");
				else
					player.sendMessage("Now you are Noblesse!");
			}
			player.sendChanges();
			Log.add(player + "Has by Noble status", "wmzSeller");
		}
		else
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("Тебе это вовсе ненужно!");
			else
				player.sendMessage("This is not for you!");
		}
	}

	public static boolean checkCondition(L2Player player, int CoinCount)
	{
		synchronized(player)
		{
			if( !_active || player == null)
				return false;
			L2ItemInstance Coin = player.getInventory().getItemByItemId(Config.DONATE_ID_PRICE);
			String name = ItemTable.getInstance().getTemplate(Config.DONATE_ID_PRICE).getName();
			if(Coin == null)
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				{
					player.sendMessage("Недостаточно " + name);
				}
				else
				{
					player.sendMessage("You have not enough " + name);
				}
			if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
				return false;
			if(player.getLastNpc().getNpcId() != npc.getNpcId())
				return false;

			if(CoinCount != 0 && Coin.getCount() < CoinCount)
			{
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				{
					player.sendMessage("Недостаточно " + name + ".");
				}
				else
				{
					player.sendMessage("You have not enough " + name + ".");
				}
				player.sendActionFailed();
				return false;
			}

			return true;
		}
	}
}