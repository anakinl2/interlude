package com.lineage.game.model;

import java.util.Collection;

import javolution.util.FastList;
import com.lineage.game.model.base.ItemToDrop;
import com.lineage.game.model.instances.L2MonsterInstance;

public class L2Drop
{
	public static final int MAX_CHANCE = 1000000;
	private FastList<L2DropGroup> _drop;
	private FastList<L2DropGroup> _spoil;

	public void addData(L2DropData d)
	{
		if(d.isSweep())
			addSpoil(d);
		else
			addDrop(d);
	}

	public void addDrop(L2DropData d)
	{
		if(_drop == null)
			_drop = new FastList<L2DropGroup>();
		if(_drop.size() != 0)
			for(L2DropGroup g : _drop)
				if(g.getId() == d.getGroupId())
				{
					g.addDropItem(d);
					return;
				}
		L2DropGroup temp = new L2DropGroup(d.getGroupId());
		temp.addDropItem(d);
		_drop.add(temp);
	}

	public void addSpoil(L2DropData s)
	{
		if(_spoil == null)
			_spoil = new FastList<L2DropGroup>();
		L2DropGroup temp = new L2DropGroup(0);
		temp.addDropItem(s);
		_spoil.add(temp);
	}

	public FastList<ItemToDrop> rollDrop(int diff, L2MonsterInstance monster, L2Player player, double mod)
	{
		FastList<ItemToDrop> temp = new FastList<ItemToDrop>();
		if(_drop != null)
			for(L2DropGroup g : _drop)
			{
				Collection<ItemToDrop> tdl = g.roll(diff, false, monster, player, mod);
				if(tdl != null)
					for(ItemToDrop itd : tdl)
						temp.add(itd);
			}
		return temp;
	}

	public FastList<ItemToDrop> rollSpoil(int diff, L2MonsterInstance monster, L2Player player, double mod)
	{
		FastList<ItemToDrop> temp = new FastList<ItemToDrop>();
		if(_spoil != null)
			for(L2DropGroup g : _spoil)
			{
				Collection<ItemToDrop> tdl = g.roll(diff, true, monster, player, mod);
				if(tdl != null)
					for(ItemToDrop itd : tdl)
						temp.add(itd);
			}
		return temp;
	}

	public FastList<L2DropGroup> getSpoil()
	{
		return _spoil;
	}

	public FastList<L2DropGroup> getNormal()
	{
		return _drop;
	}

	public boolean validate()
	{
		if(_drop == null)
			return false;
		for(L2DropGroup g : _drop)
		{
			int sum_chance = 0; // сумма шансов группы
			for(L2DropData d : g.getDropItems(false))
				sum_chance += d.getChance();
			if(sum_chance <= MAX_CHANCE) // всё в порядке?
				return true;
			double mod = MAX_CHANCE / sum_chance;
			for(L2DropData d : g.getDropItems(false))
			{
				double group_chance = d.getChance() * mod; // коррекция шанса группы
				d.setChance(group_chance);
				d.recalcWorth();
				g.setChance(MAX_CHANCE);
			}
		}
		return false;
	}
}