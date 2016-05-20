package com.lineage.game.model.base;

import javolution.util.FastList;

public class MultiSellEntry
{
	private int _entryId;
	private FastList<MultiSellIngredient> _ingredients = new FastList<MultiSellIngredient>();
	private FastList<MultiSellIngredient> _production = new FastList<MultiSellIngredient>();

	public MultiSellEntry()
	{}

	public MultiSellEntry(int id)
	{
		_entryId = id;
	}

	public MultiSellEntry(int id, int product, int prod_count, int enchant)
	{
		_entryId = id;
		addProduct(new MultiSellIngredient(product, prod_count, enchant));
	}

	/**
	 * @param entryId The entryId to set.
	 */
	public void setEntryId(int entryId)
	{
		_entryId = entryId;
	}

	/**
	 * @return Returns the entryId.
	 */
	public int getEntryId()
	{
		return _entryId;
	}

	/**
	 * @param ingredients The ingredients to set.
	 */
	public void addIngredient(MultiSellIngredient ingredient)
	{
		_ingredients.add(ingredient);
	}

	/**
	 * @return Returns the ingredients.
	 */
	public FastList<MultiSellIngredient> getIngredients()
	{
		return _ingredients;
	}

	/**
	 * @param ingredients The ingredients to set.
	 */
	public void addProduct(MultiSellIngredient ingredient)
	{
		_production.add(ingredient);
	}

	/**
	 * @return Returns the ingredients.
	 */
	public FastList<MultiSellIngredient> getProduction()
	{
		return _production;
	}

	@Override
	public int hashCode()
	{
		return _entryId;
	}
}