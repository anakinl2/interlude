package com.lineage.game.model;

import java.util.ArrayList;

import com.lineage.Config;
import com.lineage.game.model.base.ItemToDrop;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.util.Rnd;

public class L2DropData implements Cloneable, Comparable<Object>
{
	private L2Item _item;
	private int _mindrop;
	private int _maxdrop;
	private boolean _sweep;
	private double _chance;
	private double _chanceInGroup;
	private int _groupId;
	private int _minLevel;
	private int _maxLevel;
	private double _baseWorth;

	public L2DropData()
	{}

	public L2DropData(final int id, final int min, final int max, final double chance, final int minLevel)
	{
		_item = ItemTable.getInstance().getTemplate(id);
		_mindrop = min;
		_maxdrop = max;
		_chance = chance;
		_minLevel = (byte) minLevel;
		_baseWorth = 1 / ((min + max) * chance / 2000000.);
	}

	public L2DropData(final int id, final int min, final int max, final double chance, final int minLevel, final int maxLevel)
	{
		_item = ItemTable.getInstance().getTemplate(id);
		_mindrop = min;
		_maxdrop = max;
		_chance = chance;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_baseWorth = 1 / ((min + max) * chance / 2000000.);
	}

	@Override
	public L2DropData clone()
	{
		return new L2DropData(getItemId(), getMinDrop(), getMaxDrop(), getChance(), getMinLevel(), getMaxLevel());
	}

	/**
	 * Возвращает абстрактный модификатор, актуальный только для своей группы
	 */
	public double getBaseWorth()
	{
		return _baseWorth;
	}

	public short getItemId()
	{
		return _item.getItemId();
	}

	public L2Item getItem()
	{
		return _item;
	}

	public void setItemId(final short itemId)
	{
		_item = ItemTable.getInstance().getTemplate(itemId);
	}

	public void setGroupId(final int gId)
	{
		_groupId = gId;
	}

	public int getGroupId()
	{
		return _groupId;
	}

	public int getMinDrop()
	{
		return _mindrop;
	}

	public int getMaxDrop()
	{
		return _maxdrop;
	}

	public boolean isSweep()
	{
		return _sweep;
	}

	public double getChance()
	{
		return _chance;
	}

	public void setMinDrop(final int mindrop)
	{
		_mindrop = mindrop;
	}

	public void setMaxDrop(final int maxdrop)
	{
		_maxdrop = maxdrop;
	}

	public void setSweep(final boolean sweep)
	{
		_sweep = sweep;
	}

	public void setChance(final double chance)
	{
		_chance = chance;
	}

	public void setChanceInGroup(final double chance)
	{
		_chanceInGroup = chance;
	}

	public double getChanceInGroup()
	{
		return _chanceInGroup;
	}

	public void recalcWorth()
	{
		if(_chance != 0)
			_baseWorth = 1 / ((_mindrop + _maxdrop) * _chance / 2000000.);
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	@Override
	public String toString()
	{
		return "ItemID: " + getItem() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
	}

	@Override
	public boolean equals(final Object o)
	{
		if(o instanceof L2DropData)
		{
			final L2DropData drop = (L2DropData) o;
			return drop.getItemId() == getItemId();
		}
		return false;
	}

	/**
	 * Подсчет шанса выпадения этой конкретной вещи
	 * Используется в эвентах и некоторых специальных механизмах
	 * 
	 * @param player
	 *            игрок (его бонус влияет на шанс)
	 * @param mod
	 *            (просто множитель шанса)
	 * @return информация о выпавшей вещи
	 */
	public ArrayList<ItemToDrop> roll(final L2Player player, final double mod, final boolean isRaid)
	{
		final float rate = (isRaid ? Config.RATE_DROP_RAIDBOSS : Config.RATE_DROP_ITEMS) * (player != null ? player.getRateItems() : 1);
		final float adenarate = (isRaid ? Config.RATE_DROP_RAIDBOSS : Config.RATE_DROP_ADENA) * (player != null ? player.getRateAdena() : 1);

		// calc group chance
		double calcChance = mod * _chance * (_item.isAdena() ? 1f : rate);

		int dropmult = 1;
		// Если шанс оказался больше 100%
		if(calcChance > L2Drop.MAX_CHANCE)
			if(calcChance % L2Drop.MAX_CHANCE == 0) // если кратен 100% то тупо умножаем количество
				dropmult = (int) (calcChance / L2Drop.MAX_CHANCE);
			else
			{ // иначе балансируем
				dropmult = (int) Math.ceil(calcChance / L2Drop.MAX_CHANCE); // множитель равен шанс / 100% округление вверх
				calcChance = calcChance / dropmult; // шанс равен шанс / множитель
				// в результате получаем увеличение количества и уменьшение шанса, при этом шанс не падает ниже 50%
			}

		final ArrayList<ItemToDrop> ret = new ArrayList<ItemToDrop>();

		for(int i = 1; i <= dropmult; i++)
		{
			if(Rnd.get(L2Drop.MAX_CHANCE) > calcChance)
				continue;

			final ItemToDrop t = new ItemToDrop(_item.getItemId());

			// если это адена то умножаем на рейт адены, иначе на множитель перебора шанса
			final float mult = _item.isAdena() ? adenarate : 1;

			if(getMinDrop() >= getMaxDrop())
				t.count = (int) (getMinDrop() * mult);
			else
				t.count = Rnd.get((int) (getMinDrop() * mult), (int) (getMaxDrop() * mult));

			ret.add(t);
		}
		return ret;
	}

	@Override
	public int hashCode()
	{
		return _item.getItemId();
	}

	/**
	 * Адекватно работает только в пределах одной группы
	 */
	@Override
	public int compareTo(final Object o)
	{
		final L2DropData c = (L2DropData) o;
		final double thsw = _baseWorth * (_maxdrop + _mindrop) * 1000;
		final double otw = c.getBaseWorth() * (c.getMaxDrop() + c.getMinDrop()) * 1000;
		if((int) Math.round(thsw - otw) == 0)
			return getItemId() - c.getItemId();
		return (int) Math.round(thsw - otw);
	}
}
