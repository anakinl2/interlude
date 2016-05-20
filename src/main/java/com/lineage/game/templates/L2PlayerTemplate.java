package com.lineage.game.templates;

import java.util.ArrayList;

import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.base.Race;
import com.lineage.game.tables.ItemTable;

public class L2PlayerTemplate extends L2CharTemplate
{
	/** The Class object of the L2Player */
	public final ClassId classId;

	public final Race race;
	public final String className;

	public final int spawnX;
	public final int spawnY;
	public final int spawnZ;

	public final boolean isMale;

	public final int classBaseLevel;
	public final float lvlHpAdd;
	public final float lvlHpMod;
	public final float lvlCpAdd;
	public final float lvlCpMod;
	public final float lvlMpAdd;
	public final float lvlMpMod;

	private ArrayList<L2Item> _items = new ArrayList<L2Item>();

	public L2PlayerTemplate(StatsSet set)
	{
		super(set);
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");

		spawnX = set.getInteger("spawnX");
		spawnY = set.getInteger("spawnY");
		spawnZ = set.getInteger("spawnZ");

		isMale = set.getBool("isMale", true);

		classBaseLevel = set.getInteger("classBaseLevel");
		lvlHpAdd = set.getFloat("lvlHpAdd");
		lvlHpMod = set.getFloat("lvlHpMod");
		lvlCpAdd = set.getFloat("lvlCpAdd");
		lvlCpMod = set.getFloat("lvlCpMod");
		lvlMpAdd = set.getFloat("lvlMpAdd");
		lvlMpMod = set.getFloat("lvlMpMod");
	}

	/**
	 * add starter equipment
	 * @param i
	 */
	public void addItem(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if(item != null)
			_items.add(item);
	}

	/**
	 *
	 * @return itemIds of all the starter equipment
	 */
	public L2Item[] getItems()
	{
		return _items.toArray(new L2Item[_items.size()]);
	}
}