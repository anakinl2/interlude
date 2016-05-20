/**
 * 
 */
package l2d.game.model.quest;

import javolution.util.FastList;

public class Drop
{
	public int condition;
	public int maxcount;
	public int chance;

	public FastList<Short> itemList = new FastList<Short>();

	public Drop(Integer _condition, Integer _maxcount, Integer _chance)
	{
		condition = _condition;
		maxcount = _maxcount;
		chance = _chance;
	}

	public Drop addItem(Short item)
	{
		itemList.add(item);
		return this;
	}
}