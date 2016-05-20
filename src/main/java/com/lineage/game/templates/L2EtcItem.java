package com.lineage.game.templates;

public final class L2EtcItem extends L2Item
{
	public enum EtcItemType
	{
		ARROW(0, "Arrow"),
		MATERIAL(1, "Material"),
		PET_COLLAR(2, "PetCollar"),
		POTION(3, "Potion"),
		RECIPE(4, "Recipe"),
		SCROLL(5, "Scroll"),
		QUEST(6, "Quest"),
		MONEY(7, "Money"),
		OTHER(8, "Other"),
		SPELLBOOK(9, "Spellbook"),
		SEED(10, "Seed"),
		BAIT(11, "Bait"),
		SHOT(12, "Shot");
		final int _id;
		final String _name;

		EtcItemType(int id, String name)
		{
			_id = id;
			_name = name;
		}

		public int mask()
		{
			return 1 << _id + 30;
		}

		@Override
		public String toString()
		{
			return _name;
		}
	}

	/**
	 * Constructor for EtcItem.
	 * @see L2Item constructor
	 * @param type : L2EtcItemType designating the type of object Etc
	 * @param set : StatsSet designating the set of couples (key,value) for description of the Etc
	 */
	public L2EtcItem(EtcItemType type, StatsSet set)
	{
		super(type, set);
	}

	/**
	 * Returns the type of Etc Item
	 * @return L2EtcItemType
	 */
	@Override
	public EtcItemType getItemType()
	{
		return (EtcItemType) super.type;
	}

	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the EtcItem
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	@Override
	public final boolean isShadowItem()
	{
		return false;
	}
}