package com.lineage.game.model.entity.residence;

import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

public class TeleportLocation
{
	public int _price;
	public L2Item _item;
	public String _name;
	public String _target;

	public TeleportLocation(String target, int item, int price, String name)
	{
		_target = target;
		_price = price;
		_name = name;
		_item = ItemTable.getInstance().getTemplate(item);
	}
}