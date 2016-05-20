package com.lineage.game.model.instances;

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Events;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.instancemanager.MercTicketManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.base.L2Augmentation;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.ItemList;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.funcs.Func;
import com.lineage.game.skills.funcs.FuncTemplate;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.tables.PetDataTable;
import com.lineage.game.taskmanager.ItemsAutoDestroy;
import com.lineage.game.templates.L2EtcItem;
import com.lineage.game.templates.L2EtcItem.EtcItemType;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Item.Grade;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.Rnd;

public final class L2ItemInstance extends L2Object
{
	private static final Logger _log = Logger.getLogger(L2ItemInstance.class.getName());

	/** Enumeration of locations for item */
	public static enum ItemLocation
	{
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		FREIGHT,
		MONSTER
	}

	/** Item types to select */
	public static enum ItemClass
	{
		/** List all deposited items */
		ALL,
		/** Weapons, Armor, Jevels, Arrows, Baits */
		EQUIPMENT,
		/** Soul/Spiritshot, Potions, Scrolls */
		CONSUMABLE,
		/** Common craft matherials */
		MATHERIALS,
		/** Special (item specific) craft matherials */
		PIECES,
		/** Crafting recipies */
		RECIPIES,
		/** Skill learn books */
		SPELLBOOKS,
		/** Dyes, lifestones */
		MISC,
		/** All other */
		OTHER
	}

	/** ID of the owner */
	private int _owner_id;

	/** Время жизни призрачных вещей **/
	LifeTimeTask _itemLifeTimeTask;
	private int _lifeTimeRemaining;

	/** Quantity of the item */
	private long _count;

	/** ID of the item */
	private int _itemId;

	/** Object L2Item associated to the item */
	private L2Item _itemTemplate;

	/** Location of the item */
	private ItemLocation _loc;

	/** Slot where item is stored */
	private int _loc_data;

	/** Level of enchantment of the item */
	private int _enchantLevel;

	/** Price of the item for selling */
	private int _price_sell;

	private int _count_sell;

	/** Wear Item */
	private boolean _wear;

	private L2Augmentation _augmentation = null;

	/** Custom item types (used loto, race tickets) */
	private int _type1;
	private int _type2;

	/** Item drop time for autodestroy task */
	private long _dropTime;

	/** Item drop time */
	private long _dropTimeOwner;

	/** owner of the dropped item */
	private WeakReference<L2Player> itemDropOwner;

	public static final byte CHARGED_NONE = 0;
	public static final byte CHARGED_SOULSHOT = 1;
	public static final byte CHARGED_SPIRITSHOT = 1;
	public static final byte CHARGED_BLESSED_SPIRITSHOT = 2;

	private byte _chargedSoulshot = CHARGED_NONE;
	private byte _chargedSpiritshot = CHARGED_NONE;

	private boolean _chargedFishtshot = false;

	public static final byte UNCHANGED = 0;
	public static final byte ADDED = 1;
	public static final byte REMOVED = 3;
	public static final byte MODIFIED = 2;
	private byte _lastChange = 2; // 1 ??, 2 modified, 3 removed
	private boolean _existsInDb; // if a record exists in DB.
	private boolean _storedInDb; // if DB data is up-to-date.

	/**
	 * Спецфлаги для конкретного инстанса
	 */
	private int _customFlags = 0;

	public static final int FLAG_NO_DROP = 1;
	public static final int FLAG_NO_TRADE = 2;
	public static final int FLAG_NO_TRANSFER = 4;
	public static final int FLAG_NO_CRYSTALLIZE = 8;
	public static final int FLAG_NO_ENCHANT = 16;
	public static final int FLAG_NO_DESTROY = 32;
	public static final int FLAG_NO_UNEQUIP = 64;
	public static final int FLAG_ALWAYS_DROP_ON_DIE = 128;
	public static final int FLAG_EQUIP_ON_PICKUP = 256;
	public static final int FLAG_NO_RIDER_PICKUP = 512;
	public static final int FLAG_PET_EQUIPPED = 1024;

	private Future<?> _lazyUpdateInDb;

	/** Task of delayed update item info in database */
	private class LazyUpdateInDb implements Runnable
	{
		final WeakReference<L2ItemInstance> _item;

		public LazyUpdateInDb(L2ItemInstance item)
		{
			_item = new WeakReference<L2ItemInstance>(item);
		}

		@Override
		public void run()
		{
			try
			{
				if(_item.get() != null)
					_item.get().updateInDb();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(_item.get() != null)
					_item.get().stopLazyUpdateTask(false);
			}
		}
	}

	// Для магазинов с ограниченным количеством предметов
	private int _maxCountToSell;
	private int _lastRechargeTime;
	private int _rechargeTime;

	private int _bodypart;

	private boolean _whflag = false;

	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 * 
	 * @param objectId
	 *            : int designating the ID of the object in the world
	 * @param itemId
	 *            : int designating the ID of the item
	 */
	public L2ItemInstance(int objectId, int itemId)
	{
		this(objectId, ItemTable.getInstance().getTemplate(itemId));
	}

	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 * 
	 * @param objectId
	 *            : int designating the ID of the object in the world
	 * @param item
	 *            : L2Item containing informations of the item
	 */
	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		if(item == null)
		{
			_log.warning("Not found template for item id: " + _itemId);
			throw new IllegalArgumentException();
		}

		_itemId = item.getItemId();
		_itemTemplate = item;
		_count = 1;
		_loc = ItemLocation.VOID;

		_dropTime = 0;
		_dropTimeOwner = 0;
		setItemDropOwner(null, 0);

		_lifeTimeRemaining = _itemTemplate.isTemporal() ? (int) (System.currentTimeMillis() / 1000) + _itemTemplate.getDurability() * 60 : _itemTemplate.getDurability();
		if(_itemTemplate.isTemporal())
			notifyEquipped(true);

		_bodypart = _itemTemplate.getBodyPart();
	}

	public int getBodyPart()
	{
		return _bodypart;
	}

	public void setBodyPart(int bodypart)
	{
		_bodypart = bodypart;
	}

	/**
	 * Sets the ownerID of the item
	 * 
	 * @param owner_id
	 *            : int designating the ID of the owner
	 */
	public void setOwnerId(int owner_id)
	{
		if(owner_id == _owner_id)
			return;
		_owner_id = owner_id;
		_storedInDb = false;
	}

	/**
	 * Returns the ownerID of the item
	 * 
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _owner_id;
	}

	/**
	 * Sets the location of the item
	 * 
	 * @param loc
	 *            : ItemLocation (enumeration)
	 */
	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, 0);
	}

	/**
	 * Sets the location of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * 
	 * @param loc
	 *            : ItemLocation (enumeration)
	 * @param loc_data
	 *            : int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(ItemLocation loc, int loc_data)
	{
		if(loc == _loc && loc_data == _loc_data)
			return;
		_loc = loc;
		_loc_data = loc_data;
		_storedInDb = false;
	}

	public ItemLocation getLocation()
	{
		return _loc;
	}

	/**
	 * Возвращает количество предметов в int, использовать только для отсылки клиенту
	 * В прочих случаях использовать getCount
	 * 
	 * @return int
	 */
	public int getIntegerLimitedCount()
	{
		return (int) Math.min(_count, Integer.MAX_VALUE);
	}

	/**
	 * Возвращает количество предметов без приведения к int
	 * По возможности следует использовать именно его
	 * 
	 * @return long
	 */
	public long getCount()
	{
		return _count;
	}

	/**
	 * Sets the quantity of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * 
	 * @param count
	 *            : long
	 */
	public void setCount(long count)
	{
		if(count == 0)
			return;

		if(count > 0 && count > Integer.MAX_VALUE)
			count = Integer.MAX_VALUE;

		if(count < 0)
			count = 0;
		if(!isStackable() && count > 1)
		{
			_count = 1;
			Log.IllegalPlayerAction(getPlayer(), "tried to stack unstackable item " + getItemId(), 0);
			return;
		}
		if(_count == count)
			return;
		_count = count;
		_storedInDb = false;
	}

	public boolean isMaterial()
	{
		return _itemTemplate.getItemType() == EtcItemType.MATERIAL;
	}

	/**
	 * Returns if item is equipable
	 * 
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return _itemTemplate.getItemType() == EtcItemType.BAIT || _itemTemplate.getItemType() == EtcItemType.ARROW || !(getBodyPart() == 0 || _itemTemplate instanceof L2EtcItem);
	}

	/**
	 * Returns if item is equipped
	 * 
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL;
	}

	/**
	 * Returns the slot where the item is stored
	 * 
	 * @return int
	 */
	public int getEquipSlot()
	{
		return _loc_data;
	}

	/**
	 * Returns the characteristics of the item
	 * 
	 * @return L2Item
	 */
	public L2Item getItem()
	{
		return _itemTemplate;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}

	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}

	public void setDropTime(long time)
	{
		_dropTime = time;
	}

	public long getDropTime()
	{
		return _dropTime;
	}

	public long getDropTimeOwner()
	{
		return _dropTimeOwner;
	}

	public void setItemDropOwner(L2Player owner, long time)
	{
		if(owner != null)
		{
			itemDropOwner = new WeakReference<L2Player>(owner);
			_dropTimeOwner = time + System.currentTimeMillis();
		}
		else
		{
			itemDropOwner = null;
			_dropTimeOwner = 0;
		}
	}

	public L2Player getItemDropOwner()
	{
		if(itemDropOwner == null)
			return null;

		L2Player p = itemDropOwner.get();
		if(p == null)
			itemDropOwner = null;

		return p;
	}

	public boolean isWear()
	{
		return _wear;
	}

	public void setWear(boolean newwear)
	{
		_wear = newwear;
	}

	/**
	 * Returns the type of item
	 * 
	 * @return Enum
	 */
	public Enum getItemType()
	{
		return _itemTemplate.getItemType();
	}

	/**
	 * Returns the ID of the item
	 * 
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * Returns the reference price of the item
	 * 
	 * @return int
	 */
	public int getReferencePrice()
	{
		return _itemTemplate.getReferencePrice();
	}

	/**
	 * Returns the price of the item for selling
	 * 
	 * @return int
	 */
	public int getPriceToSell()
	{
		return _price_sell;
	}

	/**
	 * Sets the price of the item for selling
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * 
	 * @param price
	 *            : int designating the price
	 */
	public void setPriceToSell(int price)
	{
		_price_sell = price;
	}

	public void setCountToSell(int count)
	{
		_count_sell = count;
	}

	public int getCountToSell() // TODO: long
	{
		return _count_sell;
	}

	public void setMaxCountToSell(int count)
	{
		_maxCountToSell = count;
	}

	public int getMaxCountToSell()
	{
		return _maxCountToSell;
	}

	/**
	 * Устанавливает время последнего респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 * 
	 * @param lastRechargeTime
	 *            : unixtime в минутах
	 */
	public void setLastRechargeTime(int lastRechargeTime)
	{
		_lastRechargeTime = lastRechargeTime;
	}

	/**
	 * Возвращает время последнего респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 * 
	 * @return unixtime в минутах
	 */
	public int getLastRechargeTime()
	{
		return _lastRechargeTime;
	}

	/**
	 * Устанавливает время респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 * 
	 * @param rechargeTime
	 *            : unixtime в минутах
	 */
	public void setRechargeTime(int rechargeTime)
	{
		_rechargeTime = rechargeTime;
	}

	/**
	 * Возвращает время респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 * 
	 * @return unixtime в минутах
	 */
	public int getRechargeTime()
	{
		return _rechargeTime;
	}

	/**
	 * Возвращает ограничен ли этот предмет в количестве, используется в NPC магазинах с ограниченным количеством.
	 * 
	 * @return true, если ограничен
	 */
	public boolean isCountLimited()
	{
		return _maxCountToSell > 0;
	}

	/**
	 * Returns the last change of the item
	 * 
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}

	/**
	 * Sets the last change of the item
	 * 
	 * @param lastChange
	 *            : int
	 */
	public void setLastChange(byte lastChange)
	{
		_lastChange = lastChange;
	}

	/**
	 * Returns if item is stackable
	 * 
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _itemTemplate.isStackable();
	}

	@Override
	public void onAction(L2Player player)
	{
		if(Events.onAction(player, this))
			return;

		if(player.isCursedWeaponEquipped() && CursedWeaponsManager.getInstance().isCursed(_itemId))
			return;

		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner.

		int _castleId = MercTicketManager.getInstance().getTicketCastleId(_itemId);

		if(_castleId > 0)
		{
			if((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES || player.isGM())
			{
				if(player.isInParty())
					player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ItemInstance.NoMercInParty", player));
				else
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_AUTHORITY_TO_CANCEL_MERCENARY_POSITIONING));

			player.setTarget(this);
			player.sendActionFailed();
		}
		else
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this, null);
	}

	/**
	 * Returns the level of enchantment of the item
	 * 
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	/**
	 * Sets the level of enchantment of the item
	 * 
	 * @param enchantLevel
	 *            level of enchant
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		if(_enchantLevel == enchantLevel)
			return;
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}

	/**
	 * Returns false cause item can't be attacked
	 * 
	 * @return boolean false
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	public boolean isAugmented()
	{
		return _augmentation == null ? false : true;
	}

	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}

	public int getAugmentationId()
	{
		return _augmentation == null ? 0 : _augmentation.getAugmentationId();
	}

	public boolean setAugmentation(L2Augmentation augmentation)
	{
		if(_augmentation != null)
			return false;
		_augmentation = augmentation;
		updateItemAttributes();
		return true;
	}

	public void removeAugmentation()
	{
		_augmentation = null;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ? LIMIT 1");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.info("Could not remove augmentation for item: " + getObjectId() + " from DB:");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void restoreAttributes()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT augAttributes,augSkillId,augSkillLevel FROM item_attributes WHERE itemId=? LIMIT 1");
			statement.setInt(1, getObjectId());
			ResultSet rs = statement.executeQuery();
			rs = statement.executeQuery();
			if(rs.next())
			{
				int aug_attributes = rs.getInt(1);
				int aug_skillId = rs.getInt(2);
				int aug_skillLevel = rs.getInt(3);
				if(aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1)
					_augmentation = new L2Augmentation(aug_attributes, aug_skillId, aug_skillLevel);
			}
		}
		catch(Exception e)
		{
			_log.info("Could not restore augmentation and elemental data for item " + getObjectId() + " from DB: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void updateItemAttributes()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?,?,?)");
			statement.setInt(1, getObjectId());
			if(_augmentation == null)
			{
				statement.setInt(2, -1);
				statement.setInt(3, -1);
				statement.setInt(4, -1);
			}
			else
			{
				statement.setInt(2, _augmentation.getAugmentationId());
				if(_augmentation.getSkill() == null)
				{
					statement.setInt(3, 0);
					statement.setInt(4, 0);
				}
				else
				{
					statement.setInt(3, _augmentation.getSkill().getId());
					statement.setInt(4, _augmentation.getSkill().getLevel());
				}
			}
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.info("Could not remove elemental enchant for item: " + getObjectId() + " from DB:");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Returns the type of charge with SoulShot of the item.
	 * 
	 * @return int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public byte getChargedSoulshot()
	{
		return _chargedSoulshot;
	}

	/**
	 * Returns the type of charge with SpiritShot of the item
	 * 
	 * @return int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public byte getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}

	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}

	/**
	 * Sets the type of charge with SoulShot of the item
	 * 
	 * @param type
	 *            : int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public void setChargedSoulshot(byte type)
	{
		_chargedSoulshot = type;
	}

	/**
	 * Sets the type of charge with SpiritShot of the item
	 * 
	 * @param type
	 *            : int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public void setChargedSpiritshot(byte type)
	{
		_chargedSpiritshot = type;
	}

	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}

	protected FuncTemplate[] _funcTemplates;

	public void attachFunction(FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public void detachFunction(FuncTemplate f)
	{
		if(_funcTemplates == null || f == null)
			return;
		for(int i = 0; i < _funcTemplates.length; i++)
			if(f.equals(_funcTemplates[i]))
			{
				int len = _funcTemplates.length - 1;
				_funcTemplates[i] = _funcTemplates[len];
				FuncTemplate[] tmp = new FuncTemplate[len];
				System.arraycopy(_funcTemplates, 0, tmp, 0, len);
				_funcTemplates = tmp;
				break;
			}
	}

	/**
	 * This function basically returns a set of functions from
	 * L2Item/L2Armor/L2Weapon, but may add additional
	 * functions, if this particular item instance is enhanched
	 * for a particular player.
	 * 
	 * @return Func[]
	 */
	public Func[] getStatFuncs()
	{
		ArrayList<Func> funcs = new ArrayList<Func>();
		if(_itemTemplate.getAttachedFuncs() != null)
			for(FuncTemplate t : _itemTemplate.getAttachedFuncs())
			{
				Func f = t.getFunc(this);
				if(f != null)
					funcs.add(f);
			}
		if(_funcTemplates != null)
			for(FuncTemplate t : _funcTemplates)
			{
				Func f = t.getFunc(this);
				if(f != null)
					funcs.add(f);
			}
		if(funcs.size() == 0)
			return new Func[0];
		return funcs.toArray(new Func[funcs.size()]);
	}

	/**
	 * Updates database.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * <B>IF</B> the item exists in database :
	 * <UL>
	 * <LI><B>IF</B> the item has no owner, or has no location, or has a null quantity : remove item from database</LI>
	 * <LI><B>ELSE</B> : update item in database</LI>
	 * </UL>
	 * <B> Otherwise</B> :
	 * <UL>
	 * <LI><B>IF</B> the item hasn't a null quantity, and has a correct location, and has a correct owner : insert item in database</LI>
	 * </UL>
	 */
	public void updateDatabase()
	{
		updateDatabase(false);
	}

	public synchronized void updateDatabase(boolean commit)
	{
		if(isWear())
			return;

		if(_existsInDb)
		{
			if(_owner_id == 0 || _loc == ItemLocation.VOID || _count == 0)
				removeFromDb();
			else if(Config.LAZY_ITEM_UPDATE && (isStackable() || Config.LAZY_ITEM_UPDATE_ALL))
			{
				if(commit)
				{
					// cancel lazy update task if need
					if(stopLazyUpdateTask(true))
					{
						insertIntoDb(); // на всякий случай...
						return;
					}
					updateInDb();
					L2World.increaseUpdateItemCount();
					return;
				}
				Future<?> lazyUpdateInDb = _lazyUpdateInDb;
				if(lazyUpdateInDb == null || lazyUpdateInDb.isDone())
				{
					_lazyUpdateInDb = ThreadPoolManager.getInstance().scheduleGeneral(new LazyUpdateInDb(this), isStackable() ? Config.LAZY_ITEM_UPDATE_TIME : Config.LAZY_ITEM_UPDATE_ALL_TIME);
					L2World.increaseLazyUpdateItem();
				}
			}
			else
			{
				updateInDb();
				L2World.increaseUpdateItemCount();
			}
		}
		else
		{
			if(_count == 0)
				return;
			if(_loc == ItemLocation.VOID || _owner_id == 0)
				return;
			insertIntoDb();
		}
	}

	public boolean stopLazyUpdateTask(boolean interrupt)
	{
		boolean ret = false;
		if(_lazyUpdateInDb != null)
		{
			ret = _lazyUpdateInDb.cancel(interrupt);
			_lazyUpdateInDb = null;
		}
		return ret;
	}

	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 * 
	 * @param objectId
	 *            : int designating the objectID of the item
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(int objectId)
	{
		L2ItemInstance inst = null;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet item_rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM items WHERE object_id=? LIMIT 1");
			statement.setInt(1, objectId);
			item_rset = statement.executeQuery();
			if(item_rset.next())
				inst = restoreFromDb(item_rset, con);
			else
				_log.severe("Item object_id=" + objectId + " not found");
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore item " + objectId + " from DB: " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, item_rset);
		}
		return inst;
	}

	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 * 
	 * @param item_rset
	 *            : ResultSet
	 * @param augment_rset
	 *            : ResultSet
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(ResultSet item_rset, ThreadConnection con)
	{
		if(item_rset == null)
			return null;
		int objectId = 0;
		try
		{
			objectId = item_rset.getInt("object_id");

			L2Item item = ItemTable.getInstance().getTemplate(item_rset.getInt("item_id"));
			if(item == null)
			{
				_log.severe("Item item_id=" + item_rset.getInt("item_id") + " not known, object_id=" + objectId);
				return null;
			}

			L2ItemInstance inst = new L2ItemInstance(objectId, item);
			inst._existsInDb = true;
			inst._storedInDb = true;
			inst._owner_id = item_rset.getInt("owner_id");
			inst._count = item_rset.getInt("count");
			inst._enchantLevel = item_rset.getInt("enchant_level");
			inst._type1 = item_rset.getInt("custom_type1");
			inst._type2 = item_rset.getInt("custom_type2");
			inst._loc = ItemLocation.valueOf(item_rset.getString("loc"));
			inst._loc_data = item_rset.getInt("loc_data");
			inst._lifeTimeRemaining = item_rset.getInt("shadow_life_time");
			inst._customFlags = item_rset.getInt("flags");

			// load augmentation and elemental enchant
			if(inst.isEquipable())
				inst.restoreAttributes();

			if(inst.isTemporalItem() && inst.getLifeTimeRemaining() <= 0)
			{
				inst.removeFromDb();
				inst = null;
			}
			else
				L2World.addObject(inst);

			return inst;
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore(2) item " + objectId + " from DB: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Update the database with values of the item
	 * Не вызывать нестандартными способами
	 */
	public synchronized void updateInDb()
	{
		if(isWear())
			return;

		if(_storedInDb)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,shadow_life_time=?,item_id=?,flags=? WHERE object_id = ? LIMIT 1");
			statement.setInt(1, _owner_id);
			statement.setLong(2, _count);
			statement.setString(3, _loc.name());
			statement.setInt(4, _loc_data);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, _lifeTimeRemaining);
			statement.setInt(7, getItemId());
			statement.setInt(8, _customFlags);
			statement.setInt(9, getObjectId());
			statement.executeUpdate();

			_existsInDb = true;
			_storedInDb = true;
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not update item " + getObjectId() + " itemID " + _itemId + " count " + getCount() + " owner " + _owner_id + " in DB:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Insert the item in database
	 */
	private synchronized void insertIntoDb()
	{
		if(isWear())
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,shadow_life_time,name,class,flags) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _owner_id);
			statement.setInt(2, _itemId);
			statement.setLong(3, _count);
			statement.setString(4, _loc.name());
			statement.setInt(5, _loc_data);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getObjectId());
			statement.setInt(8, _type1);
			statement.setInt(9, _type2);
			statement.setInt(10, _lifeTimeRemaining);
			statement.setString(11, getItem().getName());
			statement.setString(12, getItemClass().name());
			statement.setInt(13, _customFlags);
			statement.executeUpdate();

			_existsInDb = true;
			_storedInDb = true;

			L2World.increaseInsertItemCount();
			L2World.addObject(this);
		}
		catch(Exception e)
		{
			_log.warning("Could not insert item " + getObjectId() + "; itemID=" + _itemId + "; count=" + getIntegerLimitedCount() + "; owner=" + _owner_id + "; exception: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Delete item from database
	 */
	private synchronized void removeFromDb()
	{
		if(isWear())
			return;

		// cancel lazy update task if need
		stopLazyUpdateTask(true);

		if(!_whflag)
			removeAugmentation();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM items WHERE object_id = ? LIMIT 1");
			statement.setInt(1, _objectId);
			statement.executeUpdate();

			_existsInDb = false;
			_storedInDb = false;

			L2World.increaseDeleteItemCount();
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not delete item " + _objectId + " in DB:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean isBlessedEnchantScroll()
	{
		switch(_itemId)
		{
			case 6569: // Wpn A
			case 6570: // Arm A
			case 6571: // Wpn B
			case 6572: // Arm B
			case 6573: // Wpn C
			case 6574: // Arm C
			case 6575: // Wpn D
			case 6576: // Arm D
			case 6577: // Wpn S
			case 6578: // Arm S
				return true;
		}
		return false;
	}

	public boolean isCrystallEnchantScroll()
	{
		switch(_itemId)
		{
			case 731:
			case 732:
			case 949:
			case 950:
			case 953:
			case 954:
			case 957:
			case 958:
			case 961:
			case 962:
				return true;
		}
		return false;
	}

	public int getEnchantCrystalId(L2ItemInstance scroll)
	{
		int scrollItemId = scroll.getItemId();
		int crystalId = 0;
		switch(_itemTemplate.getCrystalType().cry)
		{
			case L2Item.CRYSTAL_A:
				crystalId = 1461;
				break;
			case L2Item.CRYSTAL_B:
				crystalId = 1460;
				break;
			case L2Item.CRYSTAL_C:
				crystalId = 1459;
				break;
			case L2Item.CRYSTAL_D:
				crystalId = 1458;
				break;
			case L2Item.CRYSTAL_S:
				crystalId = 1462;
		}

		for(int scrollId : getEnchantScrollId())
			if(scrollItemId == scrollId)
				return crystalId;

		return 0;
	}

	public int[] getEnchantScrollId()
	{
		if(_itemTemplate.getType2() == L2Item.TYPE2_WEAPON)
			switch(_itemTemplate.getCrystalType().cry)
			{
				case L2Item.CRYSTAL_A:
					return new int[] { 729, 6569, 731 };
				case L2Item.CRYSTAL_B:
					return new int[] { 947, 6571, 949 };
				case L2Item.CRYSTAL_C:
					return new int[] { 951, 6573, 953 };
				case L2Item.CRYSTAL_D:
					return new int[] { 955, 6575, 957 };
				case L2Item.CRYSTAL_S:
					return new int[] { 959, 6577, 961 };
			}
		else if(_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR || _itemTemplate.getType2() == L2Item.TYPE2_ACCESSORY)
			switch(_itemTemplate.getCrystalType().cry)
			{
				case L2Item.CRYSTAL_A:
					return new int[] { 730, 6570, 732 };
				case L2Item.CRYSTAL_B:
					return new int[] { 948, 6572, 950 };
				case L2Item.CRYSTAL_C:
					return new int[] { 952, 6574, 954 };
				case L2Item.CRYSTAL_D:
					return new int[] { 956, 6576, 958 };
				case L2Item.CRYSTAL_S:
					return new int[] { 960, 6578, 962 };
			}
		return new int[] { 0, 0, 0 };
	}

	/**
	 * Return true if item is hero-item
	 * 
	 * @return boolean
	 */
	public boolean isHeroItem()
	{
		int myid = _itemId;
		return myid >= 6611 && myid <= 6621 || myid >= 9388 && myid <= 9390 || myid == 6842;
	}

	/**
	 * Return true if item is ClanApella-item
	 * 
	 * @return boolean
	 */
	public boolean isClanApellaItem()
	{
		int myid = _itemId;
		return myid >= 7860 && myid <= 7879 || myid >= 9830 && myid <= 9839;
	}

	/**
	 * Return true if item can be destroyed
	 * 
	 * @return boolean
	 */
	public boolean canBeDestroyed(L2Player player)
	{
		if((_customFlags & FLAG_NO_DESTROY) == FLAG_NO_DESTROY)
			return false;

		if(isHeroItem())
			return false;

		if(PetDataTable.isPetControlItem(this) && player.isMounted())
			return false;

		if(player.getPet() != null && getObjectId() == player.getPet().getControlItemObjId())
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isEquipped())
			return false;

		if(isWear())
			return false;

		return _itemTemplate.isDestroyable();
	}

	/**
	 * Return true if item can be dropped
	 * 
	 * @return boolean
	 */
	public boolean canBeDropped(L2Player player)
	{
		if((_customFlags & FLAG_NO_DROP) == FLAG_NO_DROP)
			return false;

		if(isHeroItem())
			return false;

		if(isShadowItem())
			return false;

		if(isTemporalItem())
			return false;

		if(isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		if(_itemTemplate.getType2() == L2Item.TYPE2_QUEST)
			return false;

		if(PetDataTable.isPetControlItem(this) && player.isMounted())
			return false;

		if(player.getPet() != null && getObjectId() == player.getPet().getControlItemObjId())
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isEquipped())
			return false;

		if(isWear())
			return false;

		if(getItem().getType2() == L2Item.TYPE2_QUEST)
			return false;

		if(player.getEnchantScroll() == this)
			return false;

		if(getItemId() == 57 && player.getLevel() < 60)
		{
			player.sendMessage("Adenas can be droped only from 60 lvl!");
			return false;
		}		
		return _itemTemplate.isDropable();
	}

	public boolean canBeTraded(L2Player player)
	{
		if((_customFlags & FLAG_NO_TRADE) == FLAG_NO_TRADE)
			return false;

		if(isHeroItem())
			return false;

		if(isShadowItem())
			return false;

		if(isTemporalItem())
			return false;

		if(PetDataTable.isPetControlItem(this) && player.isMounted())
			return false;

		if(player.getPet() != null && getObjectId() == player.getPet().getControlItemObjId())
			return false;

		if(isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isEquipped())
			return false;

		if(isWear())
			return false;

		if(getItem().getType2() == L2Item.TYPE2_QUEST)
			return false;

		if(player.getEnchantScroll() == this)
			return false;
		if(getItemId() == 57 && player.getLevel() < 60)
		{
			player.sendMessage("Adenas can be traded only from 60 lvl!");
			return false;
		}
		return _itemTemplate.isTradeable();
	}

	/**
	 * Можно ли положить на клановый склад или передать фрейтом
	 * 
	 * @param player
	 * @return
	 */
	public boolean canBeStored(L2Player player, boolean privatewh)
	{
		if((_customFlags & FLAG_NO_TRANSFER) == FLAG_NO_TRANSFER)
			return false;

		if(isHeroItem())
			return false;

		if(!privatewh && (isShadowItem() || isTemporalItem()))
			return false;

		if(PetDataTable.isPetControlItem(this) && player.isMounted())
			return false;

		if(player.getPet() != null && getObjectId() == player.getPet().getControlItemObjId())
			return false;

		if(!privatewh && isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isEquipped())
			return false;

		if(isWear())
			return false;

		if(getItem().getType2() == L2Item.TYPE2_QUEST)
			return false;

		if(player.getEnchantScroll() == this)
			return false;

		if(getItemId() == 57 && player.getLevel() < 60)
		{
			player.sendMessage("Adenas can be stored only from 60 lvl!");
			return false;
		}
		return privatewh || _itemTemplate.isTradeable();
	}

	public boolean canBeCrystallized(L2Player player, boolean msg)
	{
		if((_customFlags & FLAG_NO_CRYSTALLIZE) == FLAG_NO_CRYSTALLIZE)
			return false;

		if(isHeroItem())
			return false;

		if(isShadowItem())
			return false;

		if(isTemporalItem())
			return false;

		// can player crystallize?
		int level = player.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
		if(level < 1 || _itemTemplate.getCrystalType().cry - L2Item.CRYSTAL_D + 1 > level)
		{
			if(msg)
			{
				player.sendPacket(new SystemMessage(SystemMessage.CANNOT_CRYSTALLIZE_CRYSTALLIZATION_SKILL_LEVEL_TOO_LOW));
				player.sendActionFailed();
			}
			return false;
		}

		if(PetDataTable.isPetControlItem(this) && player.isMounted())
			return false;

		if(player.getPet() != null && getObjectId() == player.getPet().getControlItemObjId())
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isEquipped())
			return false;

		if(isWear())
			return false;

		if(getItem().getType2() == L2Item.TYPE2_QUEST)
			return false;

		return _itemTemplate.isCrystallizable();
	}

	public boolean canBeEnchanted()
	{
		if((_customFlags & FLAG_NO_ENCHANT) == FLAG_NO_ENCHANT)
			return false;

		if(isHeroItem())
			return false;

		if(isShadowItem())
			return false;

		if(isTemporalItem())
			return false;

		if(getItemType() == WeaponType.ROD)
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(_itemId))
			return false;

		if(isWear())
			return false;

		if(getItem().getType2() == L2Item.TYPE2_QUEST)
			return false;

		return true;
	}

	/**
	 * Returns the item in String format
	 * 
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _itemTemplate.toString();
	}

	public boolean isNightLure()
	{
		return _itemId >= 8505 && _itemId <= 8513 || _itemId == 8485;
	}

	public void notifyEquipped(boolean equipped)
	{
		if(!equipped && !isTemporalItem())
		{
			_itemLifeTimeTask = null;
			return;
		}

		L2Player owner = getOwner();
		if(owner == null)
			return;

		if(_itemLifeTimeTask == null && isShadowItem()) // Если вещь уже надета метод может вызываться из refreshListeners
			setLifeTimeRemaining(owner, getLifeTimeRemaining() - 1);

		if(!needsDestruction(owner))
		{
			_itemLifeTimeTask = new LifeTimeTask();
			ThreadPoolManager.getInstance().scheduleEffect(_itemLifeTimeTask, 60000);
		}
	}

	public boolean isShadowItem()
	{
		return _itemTemplate.isShadowItem();
	}

	public boolean isTemporalItem()
	{
		return _itemTemplate.isTemporal();
	}

	public boolean isAltSeed()
	{
		return _itemTemplate.isAltSeed();
	}

	private L2Player getOwner()
	{
		return L2World.getPlayer(_owner_id);
	}

	/**
	 * true означает завершить таск, false продолжить
	 */
	private boolean needsDestruction(L2Player owner)
	{
		if(!isShadowItem() && !isTemporalItem())
			return true;

		int left = getLifeTimeRemaining();
		if(isTemporalItem())
			left /= 60;
		if(left == 10 || left == 5 || left == 1 || left <= 0)
		{
			if(isShadowItem())
			{
				SystemMessage sm;
				if(left == 10)
					sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_10);
				else if(left == 5)
					sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_5);
				else if(left == 1)
					sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_1_IT_WILL_DISAPPEAR_SOON);
				else
					sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_0_AND_THE_ITEM_HAS_DISAPPEARED);
				sm.addItemName(getItemId());
				owner.sendPacket(sm);
			}

			if(left <= 0)
			{
				owner.getInventory().unEquipItem(this);
				owner.getInventory().destroyItem(this, getCount(), true);
				if(isTemporalItem())
					owner.sendPacket(new SystemMessage(SystemMessage.THE_LIMITED_TIME_ITEM_HAS_BEEN_DELETED).addItemName(_itemTemplate.getItemId()));
				owner.sendPacket(new ItemList(owner, false)); // перестраховка
				owner.broadcastUserInfo(true);
				return true;
			}
		}

		return false;
	}

	public int getLifeTimeRemaining()
	{
		if(isTemporalItem())
			return _lifeTimeRemaining - (int) (System.currentTimeMillis() / 1000);
		return _lifeTimeRemaining;
	}

	private void setLifeTimeRemaining(L2Player owner, int lt)
	{
		assert !isTemporalItem();

		_lifeTimeRemaining = lt;
		_storedInDb = false;

		owner.sendPacket(new InventoryUpdate().addModifiedItem(this));
	}

	public class LifeTimeTask implements Runnable
	{
		@Override
		public void run()
		{
			if(_itemLifeTimeTask != this)
				return;

			if(!isEquipped())
				return;

			L2Player owner = getOwner();
			if(owner == null || !owner.isOnline())
				return;

			if(isShadowItem())
				setLifeTimeRemaining(owner, getLifeTimeRemaining() - 1);

			if(needsDestruction(owner))
				return;

			ThreadPoolManager.getInstance().scheduleEffect(this, 60000); // У шэдовов 1 цикл = 60 сек.
		}
	}

	public void dropToTheGround(L2Player lastAttacker, L2NpcInstance dropper)
	{
		if(dropper == null)
		{
			Location dropPos = Rnd.coordsRandomize(lastAttacker, 70);
			for(int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(lastAttacker.getX(), lastAttacker.getY(), lastAttacker.getZ(), dropPos.x, dropPos.y, dropPos.z); i++)
				dropPos = Rnd.coordsRandomize(lastAttacker, 70);
			dropMe(lastAttacker, dropPos);
			ItemsAutoDestroy.getInstance().addItem(this);
			return;
		}

		// 20 попыток уронить дроп в точке смерти моба
		Location dropPos = Rnd.coordsRandomize(dropper, 70);

		if(lastAttacker != null)
		{
			for(int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(lastAttacker.getX(), lastAttacker.getY(), lastAttacker.getZ(), dropPos.x, dropPos.y, dropPos.z); i++)
				dropPos = Rnd.coordsRandomize(dropper, 70);

			// Если в точке смерти моба дропу негде упасть, то падает под ноги чару
			if(!GeoEngine.canMoveWithCollision(lastAttacker.getX(), lastAttacker.getY(), lastAttacker.getZ(), dropPos.x, dropPos.y, dropPos.z))
			{
				dropPos.x = lastAttacker.getX();
				dropPos.y = lastAttacker.getY();
				dropPos.z = lastAttacker.getZ();
			}
		}

		// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
		dropMe(dropper, dropPos);

		// Add drop to auto destroy item task
		if(isHerb())
			ItemsAutoDestroy.getInstance().addHerb(this);
		else if(Config.AUTODESTROY_ITEM_AFTER > 0)
			ItemsAutoDestroy.getInstance().addItem(this);

		// activate nonowner penalty
		L2Player owner = lastAttacker;
		L2Character MostHated = dropper.getMostHated();
		if(MostHated != null && MostHated instanceof L2Playable)
			owner = MostHated.getPlayer();
		setItemDropOwner(owner, Config.NONOWNER_ITEM_PICKUP_DELAY + (dropper instanceof L2RaidBossInstance ? 285000 : 0));
	}

	/**
	 * Бросает вещь на землю туда, где ее можно поднять
	 * 
	 * @param dropper
	 */
	public void dropToTheGround(L2Character dropper, Location dropPos)
	{
		if(GeoEngine.canMoveToCoord(dropper.getX(), dropper.getY(), dropper.getZ(), dropPos.x, dropPos.y, dropPos.z))
			dropMe(dropper, dropPos);
		else
			dropMe(dropper, dropper.getLoc());

		// Add drop to auto destroy item task
		if(Config.AUTODESTROY_PLAYER_ITEM_AFTER > 0)
			ItemsAutoDestroy.getInstance().addItem(this);
	}

	public boolean isDestroyable()
	{
		return true;
	}

	public void setWhFlag(boolean whflag)
	{
		_whflag = whflag;
	}

	public void deleteMe()
	{
		removeFromDb();
		decayMe();
		L2World.removeObject(this);
	}

	public ItemClass getItemClass()
	{
		return _itemTemplate.getItemClass();
	}

	public void setItemId(int id)
	{
		_itemId = id;
		_itemTemplate = ItemTable.getInstance().getTemplate(id);
		_storedInDb = false;
	}

	private FuncTemplate _enchantAttributeFuncTemplate;

	public FuncTemplate getAttributeFuncTemplate()
	{
		return _enchantAttributeFuncTemplate;
	}

	/**
	 * Проверяет, является ли данный инстанс предмета хербом
	 * 
	 * @return true если предмет является хербом
	 */
	public boolean isHerb()
	{
		return getItem().isHerb();
	}

	public Grade getCrystalType()
	{
		return _itemTemplate.getCrystalType();
	}

	public void setCustomFlags(int i)
	{
		if(_customFlags != i)
		{
			_customFlags = i;
			updateDatabase();
		}
	}

	public int getCustomFlags()
	{
		return _customFlags;
	}
}