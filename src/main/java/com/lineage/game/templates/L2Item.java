package com.lineage.game.templates;

import java.util.regex.Pattern;

import com.lineage.Config;
import com.lineage.game.skills.funcs.FuncTemplate;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Skill.SkillType;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 */
public abstract class L2Item
{
	/**
	 * Pc Cafe Bang Points item id. Используется на корейских серверах, но английский клиент в состоянии
	 * поддерживать даный функционал.
	 */
	public static final short ITEM_ID_PC_BANG_POINTS = -100;
	/** Item ID для клановой репутации */
	public static final short ITEM_ID_CLAN_REPUTATION_SCORE = -200;

	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;

	public static final byte TYPE2_WEAPON = 0;
	public static final byte TYPE2_SHIELD_ARMOR = 1;
	public static final byte TYPE2_ACCESSORY = 2;
	public static final byte TYPE2_QUEST = 3;
	public static final byte TYPE2_MONEY = 4;
	public static final byte TYPE2_OTHER = 5;
	public static final byte TYPE2_PET_WOLF = 6;
	public static final byte TYPE2_PET_HATCHLING = 7;
	public static final byte TYPE2_PET_STRIDER = 8;
	public static final byte TYPE2_PET_BABY = 9;

	public static final int SLOT_NONE = 0x00000;
	public static final int SLOT_UNDERWEAR = 0x00001;
	public static final int SLOT_CLOAK = 0x0003; // TODO:????
	public static final int SLOT_R_EAR = 0x00002;
	public static final int SLOT_L_EAR = 0x00004;
	public static final int SLOT_NECK = 0x00008;
	public static final int SLOT_R_FINGER = 0x00010;
	public static final int SLOT_L_FINGER = 0x00020;
	public static final int SLOT_HEAD = 0x00040;
	public static final int SLOT_R_HAND = 0x00080;
	public static final int SLOT_L_HAND = 0x00100;
	public static final int SLOT_GLOVES = 0x00200;
	public static final int SLOT_CHEST = 0x00400;
	public static final int SLOT_LEGS = 0x00800;
	public static final int SLOT_FEET = 0x01000;
	public static final int SLOT_BACK = 0x02000;
	public static final int SLOT_LR_HAND = 0x04000;
	public static final int SLOT_FULL_ARMOR = 0x08000;
	public static final int SLOT_HAIR = 0x10000;
	public static final int SLOT_FORMAL_WEAR = 0x20000;
	public static final int SLOT_DHAIR = 0x40000;
	public static final int SLOT_HAIRALL = 0x80000;

	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;

	public static final byte MATERIAL_STEEL = 0x00;
	public static final byte MATERIAL_FINE_STEEL = 0x01;
	public static final byte MATERIAL_BLOOD_STEEL = 0x02;
	public static final byte MATERIAL_BRONZE = 0x03;
	public static final byte MATERIAL_SILVER = 0x04;
	public static final byte MATERIAL_GOLD = 0x05;
	public static final byte MATERIAL_MITHRIL = 0x06;
	public static final byte MATERIAL_ORIHARUKON = 0x07;
	public static final byte MATERIAL_PAPER = 0x08;
	public static final byte MATERIAL_WOOD = 0x09;
	public static final byte MATERIAL_CLOTH = 0x0a;
	public static final byte MATERIAL_LEATHER = 0x0b;
	public static final byte MATERIAL_BONE = 0x0c;
	public static final byte MATERIAL_HORN = 0x0d;
	public static final byte MATERIAL_DAMASCUS = 0x0e;
	public static final byte MATERIAL_ADAMANTAITE = 0x0f;
	public static final byte MATERIAL_CHRYSOLITE = 0x10;
	public static final byte MATERIAL_CRYSTAL = 0x11;
	public static final byte MATERIAL_LIQUID = 0x12;
	public static final byte MATERIAL_SCALE_OF_DRAGON = 0x13;
	public static final byte MATERIAL_DYESTUFF = 0x14;
	public static final byte MATERIAL_COBWEB = 0x15;
	public static final byte MATERIAL_SEED = 0x16;
	public static final byte MATERIAL_FISH = 0x17;

	public static final int CRYSTAL_NONE = 0;
	public static final int CRYSTAL_D = 1458;
	public static final int CRYSTAL_C = 1459;
	public static final int CRYSTAL_B = 1460;
	public static final int CRYSTAL_A = 1461;
	public static final int CRYSTAL_S = 1462;

	public static enum Grade
	{
		NONE(CRYSTAL_NONE, 0),
		D(CRYSTAL_D, 1),
		C(CRYSTAL_C, 2),
		B(CRYSTAL_B, 3),
		A(CRYSTAL_A, 4),
		S(CRYSTAL_S, 5),
		S80(CRYSTAL_S, 5);

		/** ID соответствующего грейду кристалла */
		public final int cry;
		/** ID грейда, без учета уровня S */
		public final int externalOrdinal;

		private Grade(int crystal, int ext)
		{
			cry = crystal;
			externalOrdinal = ext;
		}
	}

	protected final short _itemId;
	private final ItemClass _class;
	protected final String _name;
	protected final String _addname;
	protected final String _icon;
	private final int _type1; // needed for item list (inventory)
	private final int _type2; // different lists for armor, weapon, etc
	private final int _weight;
	private final boolean _crystallizable;
	private final boolean _stackable;
	protected final Grade _crystalType; // default to none-grade
	private final int _durability;
	private final boolean _temporal;
	private final int _bodyPart;
	private final int _referencePrice;
	private final short _crystalCount;
	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _tradeable;
	private final boolean _destroyable;

	protected L2Skill[] _skills;
	protected L2Skill _enchant4Skill = null; // skill that activates when item is enchanted +4 (for duals)
	public final Enum type;

	protected FuncTemplate[] _funcTemplates;
	private static final Pattern _noskill;
	static
	{
		_noskill = Pattern.compile("(^0$)|(^-1$)");
	}

	/**
	 * Constructor of the L2Item that fill class variables.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>type</LI>
	 * <LI>_itemId</LI>
	 * <LI>_name</LI>
	 * <LI>_type1 & _type2</LI>
	 * <LI>_weight</LI>
	 * <LI>_crystallizable</LI>
	 * <LI>_stackable</LI>
	 * <LI>_materialType & _crystalType & _crystlaCount</LI>
	 * <LI>_durability</LI>
	 * <LI>_bodypart</LI>
	 * <LI>_referencePrice</LI>
	 * <LI>_sellable</LI>
	 * 
	 * @param type
	 *            : Enum designating the type of the item
	 * @param set
	 *            : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(final Enum type, final StatsSet set)
	{
		this.type = type;
		_itemId = set.getShort("item_id");
		_class = ItemClass.valueOf(set.getString("class"));
		_name = set.getString("name");
		_addname = set.getString("additional_name", "");
		_icon = set.getString("icon");
		_type1 = set.getInteger("type1"); // needed for item list (inventory)
		_type2 = set.getInteger("type2"); // different lists for armor, weapon, etc
		_weight = set.getInteger("weight");
		_crystallizable = set.getBool("crystallizable");
		_stackable = set.getBool("stackable", false);
		_crystalType = Grade.values()[set.getInteger("crystal_type", Grade.NONE.ordinal())]; // default to none-grade
		_durability = set.getInteger("durability", -1);
		_temporal = set.getBool("temporal", false);
		_bodyPart = set.getInteger("bodypart", 0);
		_referencePrice = set.getInteger("price");
		_crystalCount = set.getShort("crystal_count", (short) 0);
		_sellable = set.getBool("sellable", true);
		_dropable = set.getBool("dropable", true);
		_destroyable = set.getBool("destroyable", true);
		_tradeable = set.getBool("tradeable", true);
		String[] skills = set.getString("skill_id", "0").split(";");
		String[] skilllevels = set.getString("skill_level", "1").split(";");
		try
		{
			for(int i = 0; i < skills.length; i++)
				if(!(_noskill.matcher(skills[i]).matches() || _noskill.matcher(skilllevels[i]).matches()))
				{
					L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(skills[i]), Integer.parseInt(skilllevels[i]));
					if(skill != null)
					{
						if(skill.getSkillType() == SkillType.NOTDONE && Config.DEBUG)
							System.out.println("WARNING: item " + _itemId + " action attached skill not done: " + skill);
						attachSkill(skill);
					}
					else if(Config.DEBUG)
						System.out.println("WARNING: item " + _itemId + " attached skill not exist: " + skills[i] + " " + skilllevels[i]);
				}

			if(getItemType() == L2Weapon.WeaponType.POLE)
				attachSkill(SkillTable.getInstance().getInfo(3599, 1));
		}
		catch(Exception e)
		{
			System.out.println("Skill: " + set.getString("skill_id", "0"));
			System.out.println("Level: " + set.getString("skill_level", "1"));
			e.printStackTrace();
		}
	}

	/**
	 * Returns the itemType.
	 * 
	 * @return Enum
	 */
	public Enum getItemType()
	{
		return type;
	}

	public String getIcon()
	{
		return _icon;
	}

	/**
	 * Returns the durability of th item
	 * 
	 * @return int
	 */
	public final int getDurability()
	{
		return _durability;
	}

	public final boolean isTemporal()
	{
		return _temporal;
	}

	/**
	 * Returns the ID of the item
	 * 
	 * @return int
	 */
	public final short getItemId()
	{
		return _itemId;
	}

	public abstract int getItemMask();

	/**
	 * Returns the type 2 of the item
	 * 
	 * @return int
	 */
	public final int getType2()
	{
		return _type2;
	}

	public final int getType2ForPackets()
	{
		int type2 = _type2;
		switch(_type2)
		{
			case TYPE2_PET_WOLF:
			case TYPE2_PET_HATCHLING:
			case TYPE2_PET_STRIDER:
			case TYPE2_PET_BABY:
				if(_bodyPart == L2Item.SLOT_CHEST)
					type2 = TYPE2_SHIELD_ARMOR;
				else
					type2 = TYPE2_WEAPON;
				break;
		}
		return type2;
	}

	/**
	 * Returns the weight of the item
	 * 
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}

	/**
	 * Returns if the item is crystallizable
	 * 
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return _crystallizable && !isStackable() && getCrystalType() != Grade.NONE && getCrystalCount() > 0;
	}

	/**
	 * Return the type of crystal if item is crystallizable
	 * 
	 * @return int
	 */
	public final Grade getCrystalType()
	{
		return _crystalType;
	}

	/**
	 * Returns the grade of the item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this fucntion returns the type of crystal of the item.
	 * 
	 * @return int
	 */
	public final Grade getItemGrade()
	{
		return getCrystalType();
	}

	/**
	 * Returns the quantity of crystals for crystallization
	 * 
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}

	/**
	 * Returns the name of the item
	 * 
	 * @return String
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * Returns the additional name of the item
	 * 
	 * @return String
	 */
	public final String getAdditionalName()
	{
		return _addname;
	}

	/**
	 * Return the part of the body used with the item.
	 * 
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}

	/**
	 * Returns the type 1 of the item
	 * 
	 * @return int
	 */
	public final int getType1()
	{
		return _type1;
	}

	/**
	 * Returns if the item is stackable
	 * 
	 * @return boolean
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}

	/**
	 * Returns the price of reference of the item
	 * 
	 * @return int
	 */
	public final int getReferencePrice()
	{
		return _referencePrice;
	}

	/**
	 * Returns if the item can be sold
	 * 
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}

	/**
	 * Returns if item is for hatchling
	 * 
	 * @return boolean
	 */
	public boolean isForHatchling()
	{
		return _type2 == TYPE2_PET_HATCHLING;
	}

	/**
	 * Returns if item is for strider
	 * 
	 * @return boolean
	 */
	public boolean isForStrider()
	{
		return _type2 == TYPE2_PET_STRIDER;
	}

	/**
	 * Returns if item is for wolf
	 * 
	 * @return boolean
	 */
	public boolean isForWolf()
	{
		return _type2 == TYPE2_PET_WOLF;
	}

	public boolean isForPetBaby()
	{
		return _type2 == TYPE2_PET_BABY;
	}

	public boolean isTradeable()
	{
		return _tradeable;
	}

	public boolean isDestroyable()
	{
		return _destroyable;
	}

	public boolean isDropable()
	{
		return _dropable;
	}

	public boolean isForPet()
	{
		return _type2 == TYPE2_PET_HATCHLING || _type2 == TYPE2_PET_WOLF || _type2 == TYPE2_PET_STRIDER || _type2 == TYPE2_PET_BABY;
	}

	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * 
	 * @param f
	 *            : FuncTemplate to add
	 */
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

	public FuncTemplate[] getAttachedFuncs()
	{
		return _funcTemplates;
	}

	/**
	 * Add the L2Skill skill to the list of skills generated by the item
	 * 
	 * @param skill
	 *            : L2Skill
	 */
	public void attachSkill(L2Skill skill)
	{
		if(_skills == null)
			_skills = new L2Skill[] { skill };
		else
		{
			int len = _skills.length;
			L2Skill[] tmp = new L2Skill[len + 1];
			System.arraycopy(_skills, 0, tmp, 0, len);
			tmp[len] = skill;
			_skills = tmp;
		}
	}

	public L2Skill[] getAttachedSkills()
	{
		return _skills;
	}

	public L2Skill getFirstSkill()
	{
		if(_skills != null && _skills.length > 0)
			return _skills[0];
		return null;
	}

	/**
	 * @return skill that player get when has equipped weapon +4 or more (for duals SA)
	 */
	public L2Skill getEnchant4Skill()
	{
		return _enchant4Skill;
	}

	/**
	 * Returns the name of the item
	 * 
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}

	/**
	 * Определяет призрачный предмет или нет
	 * 
	 * @return true, если предмет призрачный
	 */
	public boolean isShadowItem()
	{
		return _durability > 0 && !isTemporal();
	}

	public boolean isAltSeed()
	{
		return _name.contains("Alternative");
	}

	public ItemClass getItemClass()
	{
		return _class;
	}

	/**
	 * Является ли вещь аденой или камнем печати
	 */
	public boolean isAdena()
	{
		return _itemId == 57 || _itemId == 6360 || _itemId == 6361 || _itemId == 6362;
	}

	public boolean isEquipment()
	{
		return _type1 != TYPE1_ITEM_QUESTITEM_ADENA;
	}

	public boolean isKeyMatherial()
	{
		return _class == ItemClass.PIECES;
	}

	public boolean isSpellBook()
	{
		return _class == ItemClass.SPELLBOOKS;
	}

	public boolean isRaidAccessory()
	{
		return _itemId == 6661 || _itemId == 6659 || _itemId == 6656 || _itemId == 6660 || _itemId == 6662 || _itemId == 6658 || _itemId == 8191 || _itemId == 6657;
	}

	public boolean isSpecialKey()
	{
		if(_itemId == 1661) // thief key
			return false;
		if(_itemId >= 6665 && _itemId <= 6672) // deluxe chest key
			return false;
		return getName().contains("Key");
	}

	public boolean isArrow()
	{
		return type == L2EtcItem.EtcItemType.ARROW;
	}

	public boolean isHerb()
	{
		return _itemId >= 8154 && _itemId <= 8157 || _itemId >= 8600 && _itemId <= 8614 || _itemId >= 8952 && _itemId <= 8953;
	}
}