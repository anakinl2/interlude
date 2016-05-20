package l2d.game.model.base;

import l2d.game.tables.ItemTable;

public class MultiSellIngredient implements Cloneable
{
	private int _itemId;
	private long _itemCount;
	private int _itemEnchant;

	public MultiSellIngredient(int itemId, long itemCount, int itemEnchant)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setItemEnchant(itemEnchant);
	}

	public MultiSellIngredient(int itemId, long itemCount)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setItemEnchant(0);
	}

	@Override
	public MultiSellIngredient clone()
	{
		return new MultiSellIngredient(_itemId, _itemCount, _itemEnchant);
	}

	/**
	 * @param itemId The itemId to set.
	 */
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}

	/**
	 * @return Returns the itemId.
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * @param itemCount The itemCount to set.
	 */
	public void setItemCount(long itemCount)
	{
		_itemCount = itemCount;
	}

	/**
	 * @return Returns the itemCount.
	 */
	public long getItemCount()
	{
		return _itemCount;
	}

	/**
	 * Returns if item is stackable
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return ItemTable.getInstance().getTemplate(_itemId).isStackable();
	}

	/**
	 * @param itemEnchant The itemEnchant to set.
	 */
	public void setItemEnchant(int itemEnchant)
	{
		_itemEnchant = itemEnchant;
	}

	/**
	 * @return Returns the itemEnchant.
	 */
	public int getItemEnchant()
	{
		return _itemEnchant;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_itemCount ^ _itemCount >>> 32);
		result = prime * result + _itemEnchant;
		result = prime * result + _itemId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MultiSellIngredient other = (MultiSellIngredient) obj;
		if(_itemCount != other._itemCount)
			return false;
		if(_itemEnchant != other._itemEnchant)
			return false;
		if(_itemId != other._itemId)
			return false;
		return true;
	}
}