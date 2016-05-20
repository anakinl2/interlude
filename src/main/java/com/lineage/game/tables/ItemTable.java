package com.lineage.game.tables;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.scripts.Scripts;
import com.lineage.ext.scripts.Scripts.ScriptClassAndMethod;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.templates.Item;
import com.lineage.game.templates.L2Armor;
import com.lineage.game.templates.L2EtcItem;
import com.lineage.game.templates.L2Weapon;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.skills.DocumentItem;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Item.Grade;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Log;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class ItemTable
{
	private final Logger _log = Logger.getLogger(ItemTable.class.getName());

	private static final HashMap<String, Integer> _crystalTypes = new HashMap<String, Integer>();
	private static final HashMap<String, L2Weapon.WeaponType> _weaponTypes = new HashMap<String, L2Weapon.WeaponType>();
	private static final HashMap<String, L2Armor.ArmorType> _armorTypes = new HashMap<String, L2Armor.ArmorType>();
	private static final HashMap<String, Integer> _slots = new HashMap<String, Integer>();

	static
	{
		_crystalTypes.put("s", Grade.S.ordinal());
		_crystalTypes.put("a", Grade.A.ordinal());
		_crystalTypes.put("b", Grade.B.ordinal());
		_crystalTypes.put("c", Grade.C.ordinal());
		_crystalTypes.put("d", Grade.D.ordinal());
		_crystalTypes.put("none", Grade.NONE.ordinal());

		_weaponTypes.put("blunt", L2Weapon.WeaponType.BLUNT);
		_weaponTypes.put("bigblunt", L2Weapon.WeaponType.BIGBLUNT);
		_weaponTypes.put("bow", L2Weapon.WeaponType.BOW);
		_weaponTypes.put("dagger", L2Weapon.WeaponType.DAGGER);
		_weaponTypes.put("dual", L2Weapon.WeaponType.DUAL);
		_weaponTypes.put("dualfist", L2Weapon.WeaponType.DUALFIST);
		_weaponTypes.put("etc", L2Weapon.WeaponType.ETC);
		_weaponTypes.put("fist", L2Weapon.WeaponType.FIST);
		_weaponTypes.put("none", L2Weapon.WeaponType.NONE); // these are shields !
		_weaponTypes.put("pole", L2Weapon.WeaponType.POLE);
		_weaponTypes.put("sword", L2Weapon.WeaponType.SWORD);
		_weaponTypes.put("bigsword", L2Weapon.WeaponType.BIGSWORD); // Two-Handed Swords
		_weaponTypes.put("pet", L2Weapon.WeaponType.PET); // Pet Weapon
		_weaponTypes.put("rod", L2Weapon.WeaponType.ROD); // Fishing Rods

		_armorTypes.put("none", L2Armor.ArmorType.NONE);
		_armorTypes.put("light", L2Armor.ArmorType.LIGHT);
		_armorTypes.put("heavy", L2Armor.ArmorType.HEAVY);
		_armorTypes.put("magic", L2Armor.ArmorType.MAGIC);
		_armorTypes.put("sigil", L2Armor.ArmorType.SIGIL);
		_armorTypes.put("pet", L2Armor.ArmorType.PET);

		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("face", L2Item.SLOT_DHAIR);
		_slots.put("dhair", L2Item.SLOT_HAIRALL);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("cloak", L2Item.SLOT_CLOAK);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear,lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger,lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("none", L2Item.SLOT_NONE);
		_slots.put("wolf", L2Item.SLOT_WOLF); // for wolf
		_slots.put("hatchling", L2Item.SLOT_HATCHLING); // for hatchling
		_slots.put("strider", L2Item.SLOT_STRIDER); // for strider
		_slots.put("baby", L2Item.SLOT_BABYPET); // for baby pet
		_slots.put("formalwear", L2Item.SLOT_FORMAL_WEAR);
	}

	private L2Item[] _allTemplates;

	private final HashMap<Integer, L2EtcItem> _etcItems = new HashMap<Integer, L2EtcItem>();
	private final HashMap<Integer, L2Armor> _armors = new HashMap<Integer, L2Armor>();
	private final HashMap<Integer, L2Weapon> _weapons = new HashMap<Integer, L2Weapon>();

	private final HashMap<Integer, Item> itemData = new HashMap<Integer, Item>();
	private final HashMap<Integer, Item> weaponData = new HashMap<Integer, Item>();
	private final HashMap<Integer, Item> armorData = new HashMap<Integer, Item>();

	private boolean _initialized = true;

	private static ItemTable _instance;

	/** Table of SQL request in order to obtain items from tables [etcitem], [armor], [weapon] */
	private static final String[] SQL_ITEM_SELECTS = {
			"SELECT item_id, name, class, icon, crystallizable, item_type, weight, consume_type, crystal_type, durability, price, crystal_count, sellable, skill_id, skill_level, tradeable, dropable, destroyable, temporal FROM etcitem",
			"SELECT item_id, name, `additional_name`, icon, bodypart, crystallizable, armor_type, weight, crystal_type, avoid_modify, durability, p_def, m_def, mp_bonus, price, crystal_count, sellable, tradeable, dropable, destroyable, skill_id, skill_level, enchant4_skill_id, enchant4_skill_lvl, temporal FROM armor",
			"SELECT item_id, name, `additional_name`, icon, bodypart, crystallizable, weight, soulshots, spiritshots, crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify, shield_def, shield_def_rate, atk_speed, mp_consume, m_dam, durability, price, crystal_count, sellable, tradeable, dropable, destroyable, skill_id, skill_level, enchant4_skill_id, enchant4_skill_lvl, temporal FROM weapon" };

	/**
	 * Returns instance of ItemTable
	 * 
	 * @return ItemTable
	 */
	public static ItemTable getInstance()
	{
		if(_instance == null)
			_instance = new ItemTable();
		return _instance;
	}

	public static void reload()
	{
		unload();
		_instance = null;
		getInstance();
	}

	/**
	 * Returns a new object Item
	 * 
	 * @return
	 */
	public Item newItem()
	{
		return new Item();
	}

	/**
	 * Constructor.
	 */
	private ItemTable()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			for(final String selectQuery : SQL_ITEM_SELECTS)
			{
				statement = con.prepareStatement(selectQuery);
				rset = statement.executeQuery();

				// Add item in correct HashMap
				while(rset.next())
					if(selectQuery.endsWith("etcitem"))
					{
						final Item newItem = readItem(rset);
						itemData.put(newItem.id, newItem);
					}
					else if(selectQuery.endsWith("armor"))
					{
						final Item newItem = readArmor(rset);
						armorData.put(newItem.id, newItem);
					}
					else if(selectQuery.endsWith("weapon"))
					{
						final Item newItem = readWeapon(rset);
						weaponData.put(newItem.id, newItem);
					}
				DatabaseUtils.closeDatabaseSR(statement, rset);
			}
		}
		catch(final Exception e)
		{
			_log.log(Level.WARNING, "data error on item: ", e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		for(Entry<Integer, Item> e : armorData.entrySet())
			_armors.put(e.getKey(), new L2Armor((L2Armor.ArmorType) e.getValue().type, e.getValue().set));

		_log.config("[ Item Table ]");
		_log.config(" ~ Loaded: " + _armors.size() + " Armors.");

		for(Entry<Integer, Item> e : itemData.entrySet())
			_etcItems.put(e.getKey(), new L2EtcItem((L2EtcItem.EtcItemType) e.getValue().type, e.getValue().set));

		_log.config(" ~ Loaded: " + _etcItems.size() + " Items.");

		new File("log/game/unimplemented_sa.txt").delete();

		for(Entry<Integer, Item> e : weaponData.entrySet())
			_weapons.put(e.getKey(), new L2Weapon((L2Weapon.WeaponType) e.getValue().type, e.getValue().set));

		_log.config(" ~ Loaded: " + _weapons.size() + " Weapons.");
		_log.config("[ Item Table ]\n");

		buildFastLookupTable();

		new Thread(new Runnable(){
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(1000);
				}
				catch(final InterruptedException e)
				{}
				for(final File f : new File("./data/stats/items/").listFiles())
					if(!f.isDirectory())
						new DocumentItem(f);
			}
		}).start();
	}

	/**
	 * Returns object Item from the record of the database
	 * 
	 * @param rset
	 *            : ResultSet designating a record of the [weapon] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readWeapon(final ResultSet rset) throws SQLException
	{
		final Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");
		item.type = _weaponTypes.get(rset.getString("weaponType"));
		if(item.type == null)
			System.out.println("Error in weapons table: unknown weapon type " + rset.getString("weaponType") + " for item " + item.id);
		item.name = rset.getString("name");
		item.set.set("class", "EQUIPMENT");

		item.set.set("item_id", item.id);
		item.set.set("name", item.name);
		item.set.set("additional_name", rset.getString("additional_name"));

		// lets see if this is a shield
		if(item.type == L2Weapon.WeaponType.NONE)
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_WEAPON);
		}

		item.set.set("bodypart", _slots.get(rset.getString("bodypart")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")).booleanValue());
		item.set.set("weight", rset.getInt("weight"));
		item.set.set("soulshots", rset.getInt("soulshots"));
		item.set.set("spiritshots", rset.getInt("spiritshots"));
		item.set.set("p_dam", rset.getInt("p_dam"));
		item.set.set("rnd_dam", rset.getInt("rnd_dam"));
		item.set.set("critical", rset.getInt("critical"));
		item.set.set("hit_modify", rset.getDouble("hit_modify"));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("shield_def", rset.getInt("shield_def"));
		item.set.set("shield_def_rate", rset.getInt("shield_def_rate"));
		item.set.set("atk_speed", rset.getInt("atk_speed"));
		item.set.set("mp_consume", rset.getInt("mp_consume"));
		item.set.set("m_dam", rset.getInt("m_dam"));
		item.set.set("durability", rset.getInt("durability"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("tradeable", rset.getInt("tradeable") > 0);
		item.set.set("dropable", rset.getInt("dropable") > 0);
		item.set.set("destroyable", rset.getInt("destroyable") > 0);
		item.set.set("temporal", rset.getInt("temporal") > 0);
		item.set.set("skill_id", rset.getString("skill_id"));
		item.set.set("skill_level", rset.getString("skill_level"));
		item.set.set("enchant4_skill_id", rset.getInt("enchant4_skill_id"));
		item.set.set("enchant4_skill_lvl", rset.getInt("enchant4_skill_lvl"));
		item.set.set("icon", rset.getString("icon"));

		if(item.type == L2Weapon.WeaponType.PET)
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);

			if(item.set.getInteger("bodypart") == L2Item.SLOT_WOLF)
				item.set.set("type2", L2Item.TYPE2_PET_WOLF);
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_HATCHLING)
				item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
			else
				item.set.set("type2", L2Item.TYPE2_PET_STRIDER);

			item.set.set("bodypart", L2Item.SLOT_R_HAND);
		}

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 * 
	 * @param rset
	 *            : ResultSet designating a record of the [armor] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readArmor(final ResultSet rset) throws SQLException
	{
		final Item item = new Item();
		item.set = new StatsSet();
		item.type = _armorTypes.get(rset.getString("armor_type"));
		item.id = rset.getInt("item_id");
		item.name = rset.getString("name");
		item.set.set("class", "EQUIPMENT");

		item.set.set("item_id", item.id);
		item.set.set("name", item.name);
		final int bodypart = _slots.get(rset.getString("bodypart"));
		item.set.set("bodypart", bodypart);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));

		if(bodypart == L2Item.SLOT_NECK || bodypart == L2Item.SLOT_HAIR || bodypart == L2Item.SLOT_DHAIR || bodypart == L2Item.SLOT_DHAIR || (bodypart & L2Item.SLOT_L_EAR) != 0 || (bodypart & L2Item.SLOT_L_FINGER) != 0)
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_ACCESSORY);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}

		item.set.set("weight", rset.getInt("weight"));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("durability", rset.getInt("durability"));
		item.set.set("p_def", rset.getInt("p_def"));
		item.set.set("m_def", rset.getInt("m_def"));
		item.set.set("mp_bonus", rset.getInt("mp_bonus"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("tradeable", rset.getInt("tradeable") > 0);
		item.set.set("dropable", rset.getInt("dropable") > 0);
		item.set.set("destroyable", rset.getInt("destroyable") > 0);
		item.set.set("temporal", rset.getInt("temporal") > 0);
		item.set.set("skill_id", rset.getString("skill_id"));
		item.set.set("skill_level", rset.getString("skill_level"));
		item.set.set("enchant4_skill_id", rset.getInt("enchant4_skill_id"));
		item.set.set("enchant4_skill_lvl", rset.getInt("enchant4_skill_lvl"));
		item.set.set("icon", rset.getString("icon"));

		if(item.type == L2Armor.ArmorType.PET)
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);

			if(item.set.getInteger("bodypart") == L2Item.SLOT_WOLF)
			{
				item.set.set("type2", L2Item.TYPE2_PET_WOLF);
				item.set.set("bodypart", L2Item.SLOT_CHEST);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_HATCHLING)
			{
				item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
				item.set.set("bodypart", L2Item.SLOT_CHEST);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_BABYPET)
			{
				item.set.set("type2", L2Item.TYPE2_PET_BABY);
				item.set.set("bodypart", L2Item.SLOT_CHEST);
			}
			else
			{
				item.set.set("type2", L2Item.TYPE2_PET_STRIDER);
				item.set.set("bodypart", L2Item.SLOT_CHEST);
			}
		}

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 * 
	 * @param rset
	 *            : ResultSet designating a record of the [etcitem] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readItem(final ResultSet rset) throws SQLException
	{
		final Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");

		item.set.set("item_id", item.id);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("type1", L2Item.TYPE1_ITEM_QUESTITEM_ADENA);
		item.set.set("type2", L2Item.TYPE2_OTHER);
		item.set.set("bodypart", 0);
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("temporal", rset.getInt("temporal") > 0);
		item.set.set("icon", rset.getString("icon"));
		item.set.set("class", rset.getString("class"));
		final String itemType = rset.getString("item_type");
		if(itemType.equals("none"))
			item.type = L2EtcItem.EtcItemType.OTHER; // only for default
		else if(itemType.equals("mticket"))
			item.type = L2EtcItem.EtcItemType.SCROLL; // dummy
		else if(itemType.equals("material"))
			item.type = L2EtcItem.EtcItemType.MATERIAL;
		else if(itemType.equals("pet_collar"))
			item.type = L2EtcItem.EtcItemType.PET_COLLAR;
		else if(itemType.equals("potion"))
			item.type = L2EtcItem.EtcItemType.POTION;
		else if(itemType.equals("recipe"))
			item.type = L2EtcItem.EtcItemType.RECIPE;
		else if(itemType.equals("scroll"))
			item.type = L2EtcItem.EtcItemType.SCROLL;
		else if(itemType.equals("seed"))
			item.type = L2EtcItem.EtcItemType.SEED;
		else if(itemType.equals("spellbook"))
			item.type = L2EtcItem.EtcItemType.SPELLBOOK; // Spellbook, Amulet, Blueprint
		else if(itemType.equals("shot"))
			item.type = L2EtcItem.EtcItemType.SHOT;
		else if(itemType.equals("arrow"))
		{
			item.type = L2EtcItem.EtcItemType.ARROW;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("bait"))
		{
			item.type = L2EtcItem.EtcItemType.BAIT;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("quest"))
		{
			item.type = L2EtcItem.EtcItemType.QUEST;
			item.set.set("type2", L2Item.TYPE2_QUEST);
		}
		else
		{
			_log.fine("unknown etcitem type:" + itemType);
			item.type = L2EtcItem.EtcItemType.OTHER;
		}

		final String consume = rset.getString("consume_type");
		if(consume.equals("asset"))
		{
			item.type = L2EtcItem.EtcItemType.MONEY;
			item.set.set("stackable", true);
			item.set.set("type2", L2Item.TYPE2_MONEY);
		}
		else if(consume.equals("stackable"))
			item.set.set("stackable", true);
		else
			item.set.set("stackable", false);

		final int crystal = _crystalTypes.get(rset.getString("crystal_type"));
		item.set.set("crystal_type", crystal);

		final int weight = rset.getInt("weight");
		item.set.set("weight", weight);
		item.name = rset.getString("name");
		item.set.set("name", item.name);

		item.set.set("durability", rset.getInt("durability"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("skill_id", rset.getString("skill_id"));
		item.set.set("skill_level", rset.getString("skill_level"));
		item.set.set("tradeable", rset.getInt("tradeable") > 0);
		item.set.set("dropable", rset.getInt("dropable") > 0);
		item.set.set("destroyable", rset.getInt("destroyable") > 0);

		return item;
	}

	/**
	 * Builds a variable in which all items are putting in in function of their ID.
	 */
	private void buildFastLookupTable()
	{
		int highestId = 0;

		for(final Integer id : _armors.keySet())
			if(id > highestId)
				highestId = id;
		for(final Integer id : _weapons.keySet())
			if(id > highestId)
				highestId = id;
		for(final Integer id : _etcItems.keySet())
			if(id > highestId)
				highestId = id;

		// Create a FastLookUp Table called _allTemplates of size : value of the highest item ID
		if(Config.DEBUG)
			_log.fine("highest item id used:" + highestId);
		_allTemplates = new L2Item[highestId + 1];

		for(final Integer id : _armors.keySet())
		{
			final L2Armor item = _armors.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
		}

		for(final Integer id : _weapons.keySet())
		{
			final L2Weapon item = _weapons.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
		}

		for(final Integer id : _etcItems.keySet())
		{
			final L2EtcItem item = _etcItems.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
		}
	}

	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and add it to _allObjects of L2world.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier </li>
	 * <li>Add the L2ItemInstance object to _allObjects of L2world </li><BR><BR>
	 * 
	 * @param itemId
	 *            The Item Identifier of the L2ItemInstance that must be created
	 */
	public L2ItemInstance createItem(final int itemId)
	{
		if(Config.DISABLE_CREATION_ID_LIST.contains(itemId))
		{
			Log.displayStackTrace(new Throwable(), "Try creating DISABLE_CREATION item " + itemId);
			return null;
		}

		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		final L2ItemInstance temp = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		if(Config.DEBUG)
			_log.fine("ItemTable: Item created	oid:" + temp.getObjectId() + " itemid:" + itemId);

		// Add the L2ItemInstance object to _allObjects of L2world
		L2World.addObject(temp);

		return temp;
	}

	/**
	 * Returns a dummy (fr = factice) item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * Dummy item is created by setting the ID of the object in the world at null value
	 * 
	 * @param itemId
	 *            : int designating the item
	 * @return L2ItemInstance designating the dummy item created
	 */
	public L2ItemInstance createDummyItem(final int itemId)
	{
		final L2Item item = getTemplate(itemId);
		if(item == null)
			return null;
		L2ItemInstance temp = new L2ItemInstance(0, item);
		try
		{
			temp = new L2ItemInstance(0, itemId);
		}
		catch(final ArrayIndexOutOfBoundsException e)
		{
			e.printStackTrace(); // this can happen if the item templates were not initialized
		}

		if(temp.getItem() == null)
			_log.warning("ItemTable: Item Template missing for Id: " + itemId);

		return temp;
	}

	public static boolean useHandler(final L2Playable self, final L2ItemInstance item)
	{
		L2Player player;
		if(self.isPlayer())
			player = (L2Player) self;
		else if(self.isPet())
			player = self.getPlayer();
		else
			return false;

		// Вызов всех определенных скриптовых итемхэндлеров
		final ArrayList<ScriptClassAndMethod> handlers = Scripts.itemHandlers.get(item.getItemId());
		if(handlers != null && handlers.size() > 0)
		{
			for(final ScriptClassAndMethod handler : handlers)
				Scripts.callScripts(handler.scriptClass, handler.method,player, new Object[] {});
			return true;
		}

		final L2Skill[] skills = item.getItem().getAttachedSkills();
		if(skills != null && skills.length > 0)
		{
			for(final L2Skill skill : skills)
			{
				final L2Character aimingTarget = skill.getAimingTarget(player, player.getTarget());
				if(skill.checkCondition(player, aimingTarget, false, false, true))
					player.getAI().Cast(skill, aimingTarget, false, false);
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the item corresponding to the item ID
	 * 
	 * @param id
	 *            : int designating the item
	 */
	public L2Item getTemplate(final int id)
	{
		if(id >= _allTemplates.length || id < 0)
		{
			_log.warning("ItemTable[604]: Not defined item_id=" + id + "; out of range");
			Thread.dumpStack();
			return null;
		}
		return _allTemplates[id];
	}

	public L2Item[] getAllTemplates()
	{
		return _allTemplates;
	}

	public Collection<L2Weapon> getAllWeapons()
	{
		return _weapons.values();
	}

	/**
	 * Returns if ItemTable initialized
	 */
	public boolean isInitialized()
	{
		return _initialized;
	}

	public static void unload()
	{
		if(_instance != null)
		{
			_instance._etcItems.clear();
			_instance._armors.clear();
			_instance._weapons.clear();
			_instance.itemData.clear();
			_instance.weaponData.clear();
			_instance.armorData.clear();
			_instance = null;
		}
		_crystalTypes.clear();
		_weaponTypes.clear();
		_armorTypes.clear();
		_slots.clear();
	}
}