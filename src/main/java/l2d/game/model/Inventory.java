package l2d.game.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2ItemInstance.ItemLocation;
import l2d.game.model.inventory.listeners.ArmorSetListener;
import l2d.game.model.inventory.listeners.BowListener;
import l2d.game.model.inventory.listeners.ChangeRecorder;
import l2d.game.model.inventory.listeners.FormalWearListener;
import l2d.game.model.inventory.listeners.ItemAugmentationListener;
import l2d.game.model.inventory.listeners.ItemSkillsListener;
import l2d.game.model.inventory.listeners.PaperdollListener;
import l2d.game.model.inventory.listeners.StatsListener;
import l2d.game.serverpackets.InventoryUpdate;
import l2d.game.serverpackets.PetInventoryUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.PetDataTable;
import l2d.game.templates.L2EtcItem.EtcItemType;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Weapon.WeaponType;
import com.lineage.util.Log;
import com.lineage.util.Util;

public abstract class Inventory
{
	protected static final Logger _log = Logger.getLogger(Inventory.class.getName());

	public static final byte PAPERDOLL_UNDER = 0;
	public static final byte PAPERDOLL_HEAD = 1;
	public static final byte PAPERDOLL_HAIR = 2;
	public static final byte PAPERDOLL_DHAIR = 3;
	public static final byte PAPERDOLL_NECK = 4;
	public static final byte PAPERDOLL_RHAND = 5;
	public static final byte PAPERDOLL_CHEST = 6;
	public static final byte PAPERDOLL_LHAND = 7;
	public static final byte PAPERDOLL_REAR = 8;
	public static final byte PAPERDOLL_LEAR = 9;
	public static final byte PAPERDOLL_GLOVES = 10;
	public static final byte PAPERDOLL_LEGS = 11;
	public static final byte PAPERDOLL_FEET = 12;
	public static final byte PAPERDOLL_LRHAND = 13;
	public static final byte PAPERDOLL_RFINGER = 14;
	public static final byte PAPERDOLL_LFINGER = 15;
	public static final byte PAPERDOLL_LBRACELET = 16;
	public static final byte PAPERDOLL_RBRACELET = 17;

	public static final byte PAPERDOLL_MAX = 24;

	// Speed percentage mods
	public static final double MAX_ARMOR_WEIGHT = 12000;

	private final L2ItemInstance[] _paperdoll;

	private final CopyOnWriteArrayList<PaperdollListener> _paperdollListeners;

	// protected to be accessed from child classes only
	// Отдельно синхронизировать этот список не надо, ибо ConcurrentLinkedQueue уже синхронизирован
	protected final ConcurrentLinkedQueue<L2ItemInstance> _items;

	private int _totalWeight;

	private boolean refreshingListeners;

	// used to quickly check for using of items of special type
	private int _wearedMask;

	// Castle Lord circlets, WARNING: position == castle.id !
	public static final Short[] _castleLordCirclets = { 0, // no castle - no circlet.. :)
			6838, // Circlet of Gludio
			6835, // Circlet of Dion
			6839, // Circlet of Giran
			6837, // Circlet of Oren
			6840, // Circlet of Aden
			6834, // Circlet of Innadril
			6836, // Circlet of Goddard
			8182, // Circlet of Rune
			8183, // Circlet of Schuttgart
	};

	protected Inventory()
	{
		_paperdoll = new L2ItemInstance[25];
		_items = new ConcurrentLinkedQueue<L2ItemInstance>();
		_paperdollListeners = new CopyOnWriteArrayList<PaperdollListener>();
		addPaperdollListener(new BowListener(this));
		addPaperdollListener(new FormalWearListener(this));
		addPaperdollListener(new ArmorSetListener(this));
		addPaperdollListener(new StatsListener(this));
		addPaperdollListener(new ItemSkillsListener(this));
		addPaperdollListener(new ItemAugmentationListener(this));
	}

	public abstract L2Character getOwner();

	protected abstract ItemLocation getBaseLocation();

	protected abstract ItemLocation getEquipLocation();

	public int getOwnerId()
	{
		return getOwner() == null ? 0 : getOwner().getObjectId();
	}

	public ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}

	public int getSize()
	{
		return getItemsList().size();
	}

	public L2ItemInstance[] getItems()
	{
		return getItemsList().toArray(new L2ItemInstance[getItemsList().size()]);
	}

	public ConcurrentLinkedQueue<L2ItemInstance> getItemsList()
	{
		return _items;
	}

	public L2ItemInstance addItem(final int id, final int count, final int source, final String create_type)
	{
		final L2ItemInstance i = ItemTable.getInstance().createItem(id);
		i.setCount(count);
		return addItem(i, true, true);
	}

	public L2ItemInstance addItem(final L2ItemInstance newItem)
	{
		return addItem(newItem, true, true);
	}

	protected L2ItemInstance addItem(final L2ItemInstance newItem, final boolean dbUpdate)
	{
		return addItem(newItem, dbUpdate, true);
	}

	protected L2ItemInstance addItem(final L2ItemInstance newItem, final boolean dbUpdate, final boolean log)
	{
		if(getOwner() == null || newItem == null)
			return null;

		if(newItem.isHerb() && !getOwner().getPlayer().isGM())
		{
			Util.handleIllegalPlayerAction(getOwner().getPlayer(), "tried to pickup herb into inventory", "Inventory[179]", 1);
			return null;
		}

		if(newItem.getCount() < 0)
		{
			_log.warning("AddItem: count < 0 owner:" + getOwner().getName());
			Thread.dumpStack();
			return null;
		}

		L2ItemInstance result = newItem;
		boolean stackableFound = false;

		if(log)
			Log.add("Inventory|" + getOwner().getName() + "|Get item|" + result.getItemId() + "|" + result.getCount() + "|" + result.getObjectId(), "items");

		// If stackable, search item in inventory in order to add to current quantity
		if(newItem.isStackable())
		{
			final int itemId = newItem.getItemId();
			final L2ItemInstance old = getItemByItemId(itemId);
			if(old != null)
			{
				// add new item quantity to existing stack
				old.setCount(old.getCount() + newItem.getCount());
				// reset new item to null
				if(log)
					Log.add("Inventory|" + getOwner().getName() + "|join item from-to|" + result.getItemId() + "|" + newItem.getObjectId() + "|" + old.getObjectId(), "items");
				newItem.setCount(0);
				newItem.setOwnerId(0);
				newItem.setLocation(ItemLocation.VOID);

				L2World.removeObject(newItem);
				IdFactory.getInstance().releaseId(newItem.getObjectId());

				stackableFound = true;

				sendModifyItem(old);

				// If database has to be updated (dbUpdate = true), update old item in inventory and destroy new item
				if(dbUpdate)
				{
					old.updateDatabase();
					newItem.updateDatabase();
				}
				result = old;
			}
		}

		// If item hasn't be found in inventory
		if(!stackableFound)
		{
			// Add item in inventory
			if(getItemByObjectId(newItem.getObjectId()) == null)
				getItemsList().add(newItem);
			else if(log)
				Log.add("Inventory|" + getOwner().getName() + "|add double link to item in inventory list!|" + newItem.getItemId() + "|" + newItem.getObjectId(), "items");

			if(newItem.getOwnerId() != getOwner().getPlayer().getObjectId() || dbUpdate)
			{
				newItem.setOwnerId(getOwner().getPlayer().getObjectId());
				newItem.setLocation(getBaseLocation());
				sendNewItem(newItem);
			}
			// If database wanted to be updated, update item
			if(dbUpdate)
				newItem.updateDatabase();
		}

		// FIXME при чтении инвентаря dbUpdate = false, это происходит только
		// при логине игрока, но ведь могут и сломать... :)
		if(dbUpdate && CursedWeaponsManager.getInstance().isCursed(result.getItemId()) && getOwner().isPlayer())
			CursedWeaponsManager.getInstance().checkPlayer((L2Player) getOwner(), result);

		refreshWeight();
		return result;
	}

	public L2ItemInstance addWearItem(int itemId, L2Player actor)
	{
		L2ItemInstance item = getItemByItemId(itemId);

		if(item != null)
			return item;

		item = ItemTable.getInstance().createItem(itemId);
		item.setWear(true);
		item.setOwnerId(getOwnerId());
		item.setLocation(getBaseLocation());
		item.setLastChange(L2ItemInstance.ADDED);
		addItem(item);

		refreshWeight();
		return item;
	}

	public L2ItemInstance getPaperdollItem(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;
		return _paperdoll[slot];
	}

	public int getPaperdollItemId(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;

		L2ItemInstance item = _paperdoll[slot];
		if(item != null)
			return item.getItemId();
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];
			if(item != null)
				return item.getItemId();
		}
		return 0;
	}

	public int getPaperdollObjectId(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;

		L2ItemInstance item = _paperdoll[slot];
		if(item != null)
			return item.getObjectId();
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];
			if(item != null)
				return item.getObjectId();
		}
		return 0;
	}

	public synchronized void addPaperdollListener(final PaperdollListener listener)
	{
		_paperdollListeners.add(listener);
	}

	public synchronized void removePaperdollListener(final PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}

	public L2ItemInstance setPaperdollItem(final int slot, final L2ItemInstance item)
	{
		final L2ItemInstance old = _paperdoll[slot];
		if(old != item)
		{
			if(old != null)
			{
				_paperdoll[slot] = null;
				old.setLocation(getBaseLocation());
				sendModifyItem(old);
				int mask = 0;
				for(int i = 0; i < PAPERDOLL_MAX; i++)
				{
					final L2ItemInstance pi = _paperdoll[i];
					if(pi != null)
						mask |= pi.getItem().getItemMask();
				}
				_wearedMask = mask;
				for(final PaperdollListener listener : _paperdollListeners)
					listener.notifyUnequipped(slot, old);
				old.updateDatabase();
			}
			if(item != null)
			{
				_paperdoll[slot] = item;
				item.setLocation(getEquipLocation(), slot);
				sendModifyItem(item);
				_wearedMask |= item.getItem().getItemMask();
				for(final PaperdollListener listener : _paperdollListeners)
					listener.notifyEquipped(slot, item);
				item.updateDatabase();
			}
		}
		return old;
	}

	public int getWearedMask()
	{
		return _wearedMask;
	}

	public void unEquipItem(final L2ItemInstance item)
	{
		unEquipItemInBodySlot(item.getBodyPart(), item);
	}

	/**
	 * Снимает предмет, и все зависимые от него, и возвращает отличия.
	 */
	public L2ItemInstance[] unEquipItemInBodySlotAndRecord(final int slot, final L2ItemInstance item)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			unEquipItemInBodySlot(slot, item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	public void unEquipItemInBodySlotAndNotify(final L2Player cha, final int slot, final L2ItemInstance item)
	{
		final L2ItemInstance weapon = cha.getActiveWeaponInstance();
		final L2ItemInstance[] unequipped = cha.getInventory().unEquipItemInBodySlotAndRecord(slot, item);

		for(final L2ItemInstance uneq : unequipped)
		{
			if(uneq == null || uneq.isWear())
				continue;

			cha.sendDisarmMessage(uneq);

			if(weapon != null && uneq == weapon)
			{
				uneq.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				uneq.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				cha.abortAttack();
				cha.abortCast();
			}
		}

		cha.refreshExpertisePenalty();
		cha.broadcastUserInfo(true);
	}

	public L2ItemInstance unEquipItemInSlot(final int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}

	/**
	 * Unequips item in slot (i.e. equips with default value)
	 * 
	 * @param slot
	 *            : int designating the slot
	 */
	public void unEquipItemInBodySlot(final int slot, final L2ItemInstance item)
	{
		byte pdollSlot = -1;

		switch(slot)
		{
			case L2Item.SLOT_NECK:
				pdollSlot = PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_L_EAR:
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:
				pdollSlot = PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_L_FINGER:
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_R_FINGER:
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_HAIR:
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_DHAIR:
				pdollSlot = PAPERDOLL_DHAIR;
				break;
			case L2Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, null); // This should be the same as in DHAIR
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_HEAD:
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_LEGS:
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_FORMAL_WEAR:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_BACK:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_FEET:
				pdollSlot = PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			// TODO: плащ, нужно доработать.
			/*
			 * case L2Item.SLOT_CLOAK: pdollSlot = PAPERDOLL_CLOAK; break;
			 */
			case L2Item.SLOT_LR_HAND:
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null); // this should be the same as in LRHAND
				pdollSlot = PAPERDOLL_RHAND;
				break;
			default:
				_log.warning("Requested invalid body slot!!! " + slot);
				Thread.dumpStack();
		}
		if(pdollSlot >= 0)
			setPaperdollItem(pdollSlot, null);
	}

	public synchronized void equipItem(final L2ItemInstance item)
	{
		final int itemId = item.getItemId();
		final int targetSlot = item.getItem().getBodyPart();
		if(getOwner().isPlayer() && getOwner().getName() != null)
		{
			final L2Player owner = (L2Player) getOwner();
			final L2Clan ownersClan = owner.getClan();
			// Hero items
			if(item.isHeroItem() && !owner.isHero())
			{
				owner.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM));
				return;
			}

			// The Lord's Crown items
			if(itemId == 6841)
				if(ownersClan == null || ownersClan.getHasCastle() == 0 || !owner.isClanLeader())
				{
					owner.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM));
					return;
				}

			if(itemId >= 7850 && itemId <= 7859) // Clan Oath Armor
				if(ownersClan == null || owner.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
				{
					owner.sendPacket(new SystemMessage(SystemMessage.THIS_ITEM_CAN_ONLY_BE_WORN_BY_A_MEMBER_OF_THE_CLAN_ACADEMY));
					return;
				}

			if(item.isClanApellaItem() && owner.getClan() == null)
			{
				owner.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM));
				return;
			}

			if(Arrays.asList(_castleLordCirclets).contains(itemId))
				if(ownersClan == null || itemId != _castleLordCirclets[ownersClan.getHasCastle()])
				{
					owner.sendMessage(new CustomMessage("l2d.game.model.Inventory.CircletWorn", owner).addString(CastleManager.getInstance().getCastleByIndex(Arrays.asList(_castleLordCirclets).indexOf(itemId)).getName()));
					return;
				}
		}

		// Нельзя одевать оружие, если уже одето проклятое оружие
		if(CursedWeaponsManager.getInstance().isCursed(getPaperdollItemId(PAPERDOLL_RHAND)) && (targetSlot == L2Item.SLOT_LR_HAND || targetSlot == L2Item.SLOT_L_HAND || targetSlot == L2Item.SLOT_R_HAND))
			return;

		final L2ItemInstance equipped = getPaperdollItem(PAPERDOLL_RHAND);
		if((targetSlot == L2Item.SLOT_LR_HAND || targetSlot == L2Item.SLOT_L_HAND || targetSlot == L2Item.SLOT_R_HAND) && equipped != null && (equipped.getCustomFlags() & L2ItemInstance.FLAG_NO_UNEQUIP) == L2ItemInstance.FLAG_NO_UNEQUIP)
			return;

		double mp = 0; // при смене робы ману не сбрасываем

		switch(targetSlot)
		{
			case L2Item.SLOT_LR_HAND:
			{
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}

			case L2Item.SLOT_L_HAND:
			{
				final L2ItemInstance slot = getPaperdollItem(PAPERDOLL_RHAND);

				final L2Item oldItem = slot == null ? null : slot.getItem();
				final L2Item newItem = item.getItem();

				if(oldItem != null && newItem.getItemType() == EtcItemType.ARROW && oldItem.getItemType() == WeaponType.BOW && oldItem.getCrystalType() != newItem.getCrystalType())
					return;

				if(newItem.getItemType() != EtcItemType.ARROW && newItem.getItemType() != EtcItemType.BAIT)
				{
					if(oldItem != null && oldItem.getBodyPart() == L2Item.SLOT_LR_HAND)
					{
						setPaperdollItem(PAPERDOLL_RHAND, null);
						setPaperdollItem(PAPERDOLL_LHAND, null);
					}
					else
						setPaperdollItem(PAPERDOLL_LHAND, null);
					setPaperdollItem(PAPERDOLL_LHAND, item);
				}
				else if(oldItem != null && (newItem.getItemType() == EtcItemType.ARROW && oldItem.getItemType() == WeaponType.BOW || newItem.getItemType() == EtcItemType.BAIT && oldItem.getItemType() == WeaponType.ROD))
				{
					setPaperdollItem(PAPERDOLL_LHAND, item);
					if(newItem.getItemType() == EtcItemType.BAIT && getOwner().isPlayer())
					{
						final L2Player owner = (L2Player) getOwner();
						owner.setVar("LastLure", String.valueOf(item.getObjectId()));
					}
				}
				break;
			}

			case L2Item.SLOT_R_HAND:
			{
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L2Item.SLOT_L_EAR:
			case L2Item.SLOT_R_EAR:
			case L2Item.SLOT_L_EAR | L2Item.SLOT_R_EAR:
			{
				if(_paperdoll[PAPERDOLL_LEAR] == null)
				{
					item.setBodyPart(L2Item.SLOT_L_EAR);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if(_paperdoll[PAPERDOLL_REAR] == null)
				{
					item.setBodyPart(L2Item.SLOT_R_EAR);
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					item.setBodyPart(L2Item.SLOT_L_EAR);
					setPaperdollItem(PAPERDOLL_LEAR, null);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case L2Item.SLOT_L_FINGER:
			case L2Item.SLOT_R_FINGER:
			case L2Item.SLOT_L_FINGER | L2Item.SLOT_R_FINGER:
			{
				if(_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					item.setBodyPart(L2Item.SLOT_L_FINGER);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if(_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					item.setBodyPart(L2Item.SLOT_R_FINGER);
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					item.setBodyPart(L2Item.SLOT_L_FINGER);
					setPaperdollItem(PAPERDOLL_LFINGER, null);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case L2Item.SLOT_NECK:
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			case L2Item.SLOT_FULL_ARMOR:
				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			case L2Item.SLOT_CHEST:
				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_CHEST, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			case L2Item.SLOT_LEGS:
			{
				// handle full armor
				final L2ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if(chest != null && chest.getBodyPart() == L2Item.SLOT_FULL_ARMOR)
					setPaperdollItem(PAPERDOLL_CHEST, null);

				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);

				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LEGS, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			}
			case L2Item.SLOT_FEET:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			case L2Item.SLOT_GLOVES:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			case L2Item.SLOT_HEAD:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			case L2Item.SLOT_HAIR:
				final L2ItemInstance slot = getPaperdollItem(PAPERDOLL_DHAIR);
				if(slot != null && slot.getItem().getBodyPart() == L2Item.SLOT_HAIRALL)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_DHAIR, null);
				}
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case L2Item.SLOT_DHAIR:
				final L2ItemInstance slot2 = getPaperdollItem(PAPERDOLL_DHAIR);
				if(slot2 != null && slot2.getItem().getBodyPart() == L2Item.SLOT_HAIRALL)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_DHAIR, null);
				}
				setPaperdollItem(PAPERDOLL_DHAIR, item);
				break;
			case L2Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, item);
				break;
			case L2Item.SLOT_UNDERWEAR:
			case L2Item.SLOT_BACK:
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			case L2Item.SLOT_FORMAL_WEAR:
				// При одевании свадебного платья руки не трогаем
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			default:
				_log.warning("unknown body slot:" + targetSlot + " for item id: " + item.getItemId());
		}

		if(getOwner().isPlayer())
			((L2Player) getOwner()).AutoShot();
	}

	public L2ItemInstance getItemByItemId(final int itemId)
	{
		for(final L2ItemInstance temp : getItemsList())
			if(temp.getItemId() == itemId)
				return temp;
		return null;
	}

	public L2ItemInstance[] getAllItemsById(final int itemId)
	{
		final ArrayList<L2ItemInstance> ar = new ArrayList<L2ItemInstance>();
		for(final L2ItemInstance i : getItemsList())
			if(i.getItemId() == itemId)
				ar.add(i);
		return ar.toArray(new L2ItemInstance[ar.size()]);
	}

	public int getPaperdollAugmentationId(final int slot)
	{
		final L2ItemInstance item = _paperdoll[slot];
		if(item != null && item.getAugmentation() != null)
			return item.getAugmentation().getAugmentationId();
		return 0;
	}

	public L2ItemInstance getItemByObjectId(final Integer objectId)
	{
		for(final L2ItemInstance temp : getItemsList())
			if(temp.getObjectId() == objectId)
				return temp;
		return null;
	}

	public L2ItemInstance destroyItem(final int objectId, final int count, final boolean toLog)
	{
		final L2ItemInstance item = getItemByObjectId(objectId);
		return destroyItem(item, count, toLog);
	}

	/**
	 * Destroy item from inventory and updates database
	 */
	public L2ItemInstance destroyItem(final L2ItemInstance item, final long count, final boolean toLog)
	{
		if(getOwner() == null)
			return null;

		if(count < 0)
		{
			_log.warning("DestroyItem: count < 0 owner:" + getOwner().getName());
			Thread.dumpStack();
			return null;
		}

		if(item != null && toLog)
		{
			Log.LogItem(getOwner(), null, Log.DeleteItem, item, count);
			Log.add("Inventory|" + getOwner().getName() + "|Destroys item|" + item.getItemId() + "|" + count + "|" + item.getObjectId(), "items");
		}

		if(item == null)
			return null;

		if(item.getCount() <= count)
		{
			if(item.getCount() < count && toLog)
				Log.add("!Inventory|" + getOwner().getName() + "|Destroys item|" + item.getItemId() + "|" + count + " but item count " + item.getCount() + "|" + item.getObjectId(), "items");
			removeItemFromInventory(item, true);
			// При удалении ошейника, удалить пета
			if(PetDataTable.isPetControlItem(item))
				PetDataTable.deletePet(item, getOwner());
		}
		else
		{
			item.setCount(item.getCount() - count);
			sendModifyItem(item);
			item.updateDatabase();
		}

		refreshWeight();

		return item;
	}

	private void sendModifyItem(final L2ItemInstance item)
	{
		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addModifiedItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addModifiedItem(item));
	}

	private void sendRemoveItem(final L2ItemInstance item)
	{
		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addRemovedItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addRemovedItem(item));
	}

	private void sendNewItem(final L2ItemInstance item)
	{
		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addNewItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addNewItem(item));
	}

	// we need this one cuz warehouses send itemId only
	/**
	 * Destroy item from inventory by using its <B>itemID</B> and updates
	 * database
	 * 
	 * @param itemId
	 *            : int pointing out the itemID of the item
	 * @param count
	 *            : long designating the quantity of item to destroy
	 * @return L2ItemInstance designating the item up-to-date
	 */
	public L2ItemInstance destroyItemByItemId(final int itemId, final long count, final boolean toLog)
	{
		final L2ItemInstance item = getItemByItemId(itemId);
		Log.LogItem(getOwner(), Log.Sys_DeleteItem, item, count);
		return destroyItem(item, count, toLog);
	}

	/**
	 * Destroy item from inventory and from database.
	 * 
	 * @param item
	 *            : L2ItemInstance designating the item to remove from inventory
	 * @param clearCount
	 *            : boolean : if true, set the item quantity to 0
	 */
	private void removeItemFromInventory(final L2ItemInstance item, final boolean clearCount)
	{
		if(getOwner() == null)
			return;

		if(getOwner().isPlayer())
		{
			final L2Player player = (L2Player) getOwner();
			player.removeItemFromShortCut(item.getObjectId());
			if(item.isEquipped())
				unEquipItem(item);
		}

		getItemsList().remove(item);

		if(clearCount)
		{
			item.setCount(0);
			L2World.removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());
		}

		item.setOwnerId(0);
		item.setLocation(ItemLocation.VOID);
		sendRemoveItem(item);
		item.updateDatabase(true);
	}

	public L2ItemInstance dropItem(final int objectId, final long count)
	{
		final L2ItemInstance item = getItemByObjectId(objectId);

		if(item == null)
		{
			_log.warning("DropItem: item objectId: " + objectId + " does not exist in inventory");
			Thread.dumpStack();
			return null;
		}

		return dropItem(item, count);
	}

	/**
	 * Флаг нужен для определения удалять ли инкрустацию
	 */
	public L2ItemInstance dropItem(final L2ItemInstance item, final int count, final boolean whflag)
	{
		item.setWhFlag(whflag);
		return dropItem(item, count);
	}

	/**
	 * Drop item from inventory by using <B>object L2ItemInstance</B><BR>
	 * <BR>
	 * <U><I>Concept :</I></U><BR>
	 * item equipped are unequipped
	 * <LI>If quantity of items in inventory after drop is negative or null,
	 * change location of item</LI>
	 * <LI>Otherwise, change quantity in inventory and create new object with
	 * quantity dropped</LI>
	 * 
	 * @param oldItem
	 *            : L2ItemInstance designating the item to drop
	 * @param count
	 *            : int designating the quantity of item to drop
	 * @return L2ItemInstance designating the item dropped
	 */
	public L2ItemInstance dropItem(final L2ItemInstance oldItem, final long count)
	{
		if(getOwner() == null)
			return null;

		if(getOwner().isPlayer() && ((L2Player) getOwner()).getPlayerAccess() != null && ((L2Player) getOwner()).getPlayerAccess().BlockInventory)
			return null;

		if(count < 0)
		{
			_log.warning("DropItem: count < 0 owner:" + getOwner().getName());
			return null;
		}

		if(oldItem == null)
		{
			_log.warning("DropItem: item id does not exist in inventory");
			return null;
		}

		Log.LogItem(getOwner(), null, Log.Drop, oldItem, count);

		if(oldItem.getCount() <= count || oldItem.getCount() <= 1)
		{
			Log.add("Inventory|" + getOwner().getName() + "|Drop item|" + oldItem.getItemId() + "|" + count + "|" + oldItem.getObjectId(), "items");
			if(Config.DEBUG)
				_log.fine(" count = count  --> remove");
			removeItemFromInventory(oldItem, false);
			refreshWeight();

			// check drop pet controls items
			if(PetDataTable.isPetControlItem(oldItem))
				PetDataTable.unSummonPet(oldItem, getOwner());
			return oldItem;
		}
		if(Config.DEBUG)
			_log.fine(" count != count  --> reduce");
		oldItem.setCount(oldItem.getCount() - count);
		sendModifyItem(oldItem);
		final L2ItemInstance newItem = ItemTable.getInstance().createItem(oldItem.getItemId());
		newItem.setCount(count);
		oldItem.updateDatabase();
		refreshWeight();
		Log.add("Inventory|" + getOwner().getName() + "|Split item from-to|" + oldItem.getItemId() + "|" + oldItem.getObjectId() + "|" + newItem.getObjectId(), "items");
		Log.add("Inventory|" + getOwner().getName() + "|Drop item|" + newItem.getItemId() + "|" + count + "|" + newItem.getObjectId(), "items");
		return newItem;
	}

	/**
	 * Refresh the weight of equipment loaded
	 */
	private void refreshWeight()
	{
		int weight = 0;

		for(final L2ItemInstance element : getItemsList())
			weight += element.getItem().getWeight() * element.getCount();

		_totalWeight = weight;
		// notify char for overload checking
		if(getOwner().isPlayer())
			((L2Player) getOwner()).refreshOverloaded();
		// Отключено, иначе во время автоматического кормления, мешает писать в чат.
		// При передаче вещей, шлется в другом месте
		// else if(getOwner().isPet)
		// ((L2PetInstance) getOwner()).sendPetInfo();
	}

	public int getTotalWeight()
	{
		return _totalWeight;
	}

	public L2ItemInstance findArrowForBow(final L2Item bow)
	{
		int arrowsId = 0;
		switch(bow.getCrystalType().cry)
		{
			default: // broken weapon.csv ??
			case L2Item.CRYSTAL_NONE:
				arrowsId = 17; // Wooden arrow
				break;
			case L2Item.CRYSTAL_D:
				arrowsId = 1341; // Bone arrow
				break;
			case L2Item.CRYSTAL_C:
				arrowsId = 1342; // Fine steel arrow
				break;
			case L2Item.CRYSTAL_B:
				arrowsId = 1343; // Silver arrow
				break;
			case L2Item.CRYSTAL_A:
				arrowsId = 1344; // Mithril arrow
				break;
			case L2Item.CRYSTAL_S:
				arrowsId = 1345; // Shining arrow
				break;
		}
		return getItemByItemId(arrowsId);
	}

	public L2ItemInstance FindEquippedLure()
	{
		L2ItemInstance res = null;
		int last_lure = 0;
		if(getOwner() != null && getOwner().isPlayer())
			try
			{
				final L2Player owner = (L2Player) getOwner();
				final String LastLure = owner.getVar("LastLure");
				if(LastLure != null && !LastLure.isEmpty())
					last_lure = Integer.valueOf(LastLure);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		for(final L2ItemInstance temp : getItemsList())
			if(temp.getItemType() == EtcItemType.BAIT)
				if(temp.getLocation() == ItemLocation.PAPERDOLL && temp.getEquipSlot() == PAPERDOLL_LHAND)
					return temp;
				else if(last_lure > 0 && res == null && temp.getObjectId() == last_lure)
					res = temp;
		return res;
	}

	/**
	 * Delete item object from world
	 */
	public synchronized void deleteMe()
	{
		for(final L2ItemInstance inst : getItemsList())
		{
			inst.updateInDb();
			L2World.removeObject(inst);
		}
		getItemsList().clear();
	}

	public void updateDatabase(final boolean commit)
	{
		updateDatabase(getItemsList(), commit);
	}

	private void updateDatabase(final ConcurrentLinkedQueue<L2ItemInstance> items, final boolean commit)
	{
		if(getOwner() != null)
			for(final L2ItemInstance inst : items)
				inst.updateDatabase(commit);
	}

	/**
	 * Функция для валидации вещей в инвентаре. Вызывается при загрузке персонажа.
	 */
	public void validateItems()
	{
		for(final L2ItemInstance item : getItemsList())
			// Clan Apella armor
			if(item.isClanApellaItem() && getOwner().isPlayer())
			{
				final L2Player owner = (L2Player) getOwner();
				if(owner.getClan() == null)
					unEquipItem(item);
			}
			// Clan Oath Armor
			else if(item.getItemId() >= 7850 && item.getItemId() <= 7859 && getOwner().isPlayer())
			{
				final L2Player owner = (L2Player) getOwner();
				if(owner.getClan() == null || owner.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
					unEquipItem(item);
			}
			// Hero Items
			else if(item.isHeroItem() && !getOwner().isHero())
			{
				unEquipItem(item);
				destroyItem(item, 1, false);
			}
	}

	public void restore()
	{
		final int OWNER = getOwner().getObjectId();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY object_id DESC");
			statement.setInt(1, OWNER);
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			rset = statement.executeQuery();

			L2ItemInstance item, newItem;
			while(rset.next())
			{
				item = L2ItemInstance.restoreFromDb(rset, con);
				if(item == null)
					continue;
				if(getOwner().isPlayer())
				{
					final L2Player owner = (L2Player) getOwner();
					final int itemId = item.getItemId();
					if(Arrays.asList(_castleLordCirclets).contains(itemId)) // Castle Lord circlets
						if(owner.getClan() == null || itemId != _castleLordCirclets[owner.getClan().getHasCastle()])
						{
							removeItemFromInventory(item, true);
							continue;
						}
				}
				newItem = addItem(item, false, false);
				if(newItem == null)
					continue;
				if(item.isEquipped())
					equipItem(item);
				if(newItem == item)
					L2World.addObject(item); // add this item to the world
				else
				{ // we had another stack before, so update them to remove the duplicate
					item.updateDatabase();
					newItem.updateDatabase();
				}
			}
			refreshWeight(); // TODO нужен ли он тут? ведь в addItem он и так происходит
		}
		catch(final Exception e)
		{
			_log.log(Level.WARNING, "could not restore inventory for player " + getOwner().getName() + ":", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/**
	 * Refresh all listeners
	 * дергать осторожно, если какой-то предмет дает хп/мп то текущее значение будет сброшено
	 */
	public void refreshListeners()
	{
		setRefreshingListeners(true);
		for(int i = 0; i < _paperdoll.length; i++)
		{
			final L2ItemInstance item = getPaperdollItem(i);
			if(item == null)
				continue;
			for(final PaperdollListener listener : _paperdollListeners)
			{
				listener.notifyUnequipped(i, item);
				listener.notifyEquipped(i, item);
			}
		}
		setRefreshingListeners(false);
	}

	public boolean isRefreshingListeners()
	{
		return refreshingListeners;
	}

	public void setRefreshingListeners(final boolean refreshingListeners)
	{
		this.refreshingListeners = refreshingListeners;
	}

	/**
	 * Вызывается из RequestSaveInventoryOrder
	 */
	public void sort(final int[][] order)
	{
		L2ItemInstance _item;
		ItemLocation _itemloc;
		for(final int[] element : order)
		{
			_item = getItemByObjectId(element[0]);
			if(_item == null)
				continue;
			_itemloc = _item.getLocation();
			if(_itemloc != ItemLocation.INVENTORY)
				continue;
			_item.setLocation(_itemloc, element[1]);
		}
	}

	public int getCountOf(final int itemId)
	{
		int result = 0;
		for(final L2ItemInstance item : getItemsList())
			if(item != null && item.getItemId() == itemId)
				result += item.getCount();
		return result;
	}
}