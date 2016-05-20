package l2d.game.templates;

import l2d.game.skills.Stats;
import l2d.game.skills.funcs.FuncTemplate;
import l2d.game.tables.SkillTable;
import com.lineage.util.Log;

public final class L2Weapon extends L2Item
{
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _pDam;
	private final int _rndDam;
	private final int _atkReuse;
	private final int _mpConsume;
	private final int _mDam;
	private final int _aSpd;
	private final int _critical;
	private final double _accmod;
	private final double _evsmod;
	private final int _sDef;
	private final int _rShld;

	private final boolean _sa;

	public enum WeaponType
	{
		NONE(1, "Shield"),
		SWORD(2, "Sword"),
		BLUNT(3, "Blunt"),
		DAGGER(4, "Dagger"),
		BOW(5, "Bow"),
		POLE(6, "Pole"),
		ETC(7, "Etc"),
		FIST(8, "Fist"),
		DUAL(9, "Dual Sword"),
		DUALFIST(10, "Dual Fist"),
		BIGSWORD(11, "Big Sword"), // Two Handed Swords
		PET(12, "Pet"),
		ROD(13, "Rod"),
		BIGBLUNT(14, "Big Blunt");
		private final int _id;
		private final String _name;

		private WeaponType(int id, String name)
		{
			_id = id;
			_name = name;
		}

		public int mask()
		{
			return 1 << _id;
		}

		@Override
		public String toString()
		{
			return _name;
		}
	}

	/**
	 * Constructor for Weapon.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_soulShotCount & _spiritShotCount</LI>
	 * <LI>_pDam & _mDam & _rndDam</LI>
	 * <LI>_critical</LI>
	 * <LI>_hitModifier</LI>
	 * <LI>_avoidModifier</LI>
	 * <LI>_shieldDes & _shieldDefRate</LI>
	 * <LI>_atkSpeed & _AtkReuse</LI>
	 * <LI>_mpConsume</LI>
	 * @param type : L2ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Weapon(WeaponType type, StatsSet set)
	{
		super(type, set);
		_soulShotCount = set.getInteger("soulshots");
		_spiritShotCount = set.getInteger("spiritshots");
		_pDam = set.getInteger("p_dam");
		_rndDam = set.getInteger("rnd_dam");
		_atkReuse = set.getInteger("atk_reuse", 1500);
		_mpConsume = set.getInteger("mp_consume");
		_mDam = set.getInteger("m_dam");
		_aSpd = set.getInteger("atk_speed");
		_critical = set.getInteger("critical");
		_sa = !_icon.endsWith("i00");
		_accmod = set.getDouble("hit_modify");
		_evsmod = set.getDouble("avoid_modify");
		_sDef = set.getInteger("shield_def");
		_rShld = set.getInteger("shield_def_rate");

		if(_addname.length() > 0 && _skills == null)
			Log.add("id=" + _itemId + " name=" + _name + " [" + _addname + "]", "unimplemented_sa");

		if(type == WeaponType.POLE)
			attachSkill(SkillTable.getInstance().getInfo(3599, 1)); // ничего не делает, просто для красоты

		int sId = set.getInteger("enchant4_skill_id");
		int sLv = set.getInteger("enchant4_skill_lvl");
		if(sId > 0 && sLv > 0)
			_enchant4Skill = SkillTable.getInstance().getInfo(sId, sLv);

		if(_pDam != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.POWER_ATTACK, 0x10, _pDam));
		if(_mDam != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.MAGIC_ATTACK, 0x10, _mDam));
		if(_critical != 0)
			attachFunction(new FuncTemplate(null, "Set", Stats.CRITICAL_BASE, 0x08, _critical * 10));
		if(_aSpd != 0)
			attachFunction(new FuncTemplate(null, "Set", Stats.ATK_BASE, 0x08, _aSpd));

		if(_sDef != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.SHIELD_DEFENCE, 0x10, _sDef));
		if(_accmod != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.ACCURACY_COMBAT, 0x10, _accmod));
		if(_evsmod != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.EVASION_RATE, 0x10, _evsmod));
		if(_rShld != 0)
			attachFunction(new FuncTemplate(null, "Add", Stats.SHIELD_RATE, 0x10, _rShld));

		if(_crystalType != Grade.NONE)
		{
			if(_sDef > 0)
			{
				attachFunction(new FuncTemplate(null, "Enchant", Stats.SHIELD_DEFENCE, 0x0C, 0));
				if(set.getInteger("type2") == L2Item.TYPE2_SHIELD_ARMOR)
					attachFunction(new FuncTemplate(null, "Enchant", Stats.MAX_HP, 0x0C, 0));
			}
			if(_pDam > 0)
				attachFunction(new FuncTemplate(null, "Enchant", Stats.POWER_ATTACK, 0x0C, 0));
			if(_mDam > 0)
				attachFunction(new FuncTemplate(null, "Enchant", Stats.MAGIC_ATTACK, 0x0C, 0));
		}
	}

	/**
	 * Returns the type of Weapon
	 * @return L2WeaponType
	 */
	@Override
	public WeaponType getItemType()
	{
		return (WeaponType) type;
	}

	/**
	 * Возвращает базовую скорость атаки
	 */
	public int getBaseSpeed()
	{
		return _aSpd;
	}

	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the Weapon
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	/**
	 * Returns the quantity of SoulShot used.
	 * @return int
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}

	/**
	 * Returns the quatity of SpiritShot used.
	 * @return int
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}

	/**
	 * Returns the physical damage.
	 * @return int
	 */
	public int getPDamage()
	{
		return _pDam;
	}

	public int getCritical()
	{
		return _critical;
	}

	/**
	 * Returns the random damage inflicted by the weapon
	 * @return int
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}

	/**
	 * Return the Attack Reuse Delay of the L2Weapon.<BR><BR>
	 * @return int
	 */
	public int getAttackReuseDelay()
	{
		return _atkReuse;
	}

	/**
	 * Returns the magical damage inflicted by the weapon
	 * @return int
	 */
	public int getMDamage()
	{
		return _mDam;
	}

	/**
	 * Returns the MP consumption with the weapon
	 * @return int
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}

	public boolean isSa()
	{
		return _sa;
	}

	/**
	 * Возвращает разницу между длиной этого оружия и стандартной, то есть x-40 
	 */
	public int getAttackRange()
	{
		switch(getItemType())
		{
			case BOW:
				return 460;
			case POLE:
				return 40;
			default:
				return 0;
		}
	}
}
