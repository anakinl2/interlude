package l2d.game.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastList.Node;
import l2d.Config;
import l2d.game.model.base.Experience;
import l2d.game.model.base.ItemToDrop;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.util.Rnd;

public class L2DropGroup implements Cloneable
{
	private int _id;
	private double _chance;
	private boolean _isAdena = false; // Шанс фиксирован, растет только количество
	private boolean _fixedQty = false; // Вместо увеличения количества используется увеличение количества роллов группы
	private boolean _notRate = false; // Рейты вообще не применяются
	private FastList<L2DropData> _items = new FastList<L2DropData>();

	public L2DropGroup(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}

	public boolean fixedQty()
	{
		return _fixedQty;
	}

	public boolean notRate()
	{
		return _notRate;
	}

	public void addDropItem(L2DropData item)
	{
		if(item.getItem().isAdena())
			_isAdena = true;
		if(item.getItem().isRaidAccessory() || item.getItem().isArrow() || item.getItem().isHerb())
			_notRate = true;
		if(item.getItem().isEquipment() || item.getItem().isKeyMatherial())
			_fixedQty = true;
		item.setChanceInGroup(_chance);
		_chance += item.getChance();
		_items.add(item);
	}

	/**
	 * Возвращает список вещей или копию списка
	 */
	public FastList<L2DropData> getDropItems(boolean copy)
	{
		if(!copy)
			return _items;
		return new FastList<L2DropData>(_items);
	}

	/**
	 * Возвращает полностью независимую копию группы
	 */
	@Override
	public L2DropGroup clone()
	{
		L2DropGroup ret = new L2DropGroup(_id);
		for(L2DropData i : _items)
			ret.addDropItem(i.clone());
		return ret;
	}

	/**
	 * Возвращает оригинальный список вещей если рейты не нужны или клон с примененными рейтами
	 */
	public FastList<L2DropData> getRatedItems(double mod)
	{
		if(mod == 1 || _notRate)
			return _items;
		FastList<L2DropData> ret = new FastList<L2DropData>();
		double groupWorth = 0;
		for(L2DropData i : _items)
		{
			ret.add(i.clone());
			groupWorth += i.getChance() * i.getBaseWorth() * (i.getMaxDrop() + i.getMinDrop()) / 2000000.;
		}
		groupWorth *= mod;
		double perItem = groupWorth / ret.size();
		int c = 0;
		while(calcGChance(ret, perItem) > 1000000)
		{
			if(c++ >= 500) // на всякий случай, защита от зацикливания
				break;
			L2DropData i = new TreeSet<L2DropData>(ret).first(); // надо бы переделать на более быстрый вариант
			if(i.getMinDrop() >= i.getMaxDrop() / 2)
				i.setMaxDrop(i.getMaxDrop() + 1);
			else
				i.setMinDrop(i.getMinDrop() + 1);
		}
		return ret;
	}

	private double calcGChance(Collection<L2DropData> items, double perItem)
	{
		double gChance = 0;
		for(L2DropData i : items)
		{
			if(perItem > 0)
			{
				double base = i.getBaseWorth();
				double currWorth = base * (i.getMaxDrop() + i.getMinDrop()) / 2.;
				double calc = perItem * 1000000 / currWorth;
				i.setChance(calc);
			}
			i.setChanceInGroup(gChance);
			gChance += i.getChance();
		}
		return gChance;
	}

	/**
	 * Эта функция выбирает одну вещь из группы
	 * Используется в основном механизме расчета дропа
	 */
	public Collection<ItemToDrop> roll(int diff, boolean isSpoil, L2MonsterInstance monster, L2Player player, double mod)
	{
		if(_isAdena)
			return rollAdena(diff, player, mod);
		if(isSpoil)
			return rollSpoil(diff, player, mod);
		if(monster.isRaid() || _notRate || _fixedQty || monster.getChampion() > 0 && Config.RATE_DROP_ITEMS * player.getRateItems() <= 10)
			return rollFixedQty(diff, monster, player, mod);

		// Поправка на глубоко синих мобов
		if(Config.DEEPBLUE_DROP_RULES && diff > 0)
			mod *= Experience.penaltyModifier(diff, 9);
		if(mod <= 0)
			return null;
		float rate;
		if(!monster.isRaid())
			rate = Config.RATE_DROP_ITEMS * player.getRateItems();
		else
			rate = Config.RATE_DROP_RAIDBOSS * player.getRateItems();

		FastList<L2DropData> items = getRatedItems(rate * mod);

		// Считаем шанс группы
		double calcChance = 0;
		for(L2DropData i : items)
			calcChance += i.getChance();

		if(Rnd.get(1, L2Drop.MAX_CHANCE) > calcChance)
			return null;

		ArrayList<ItemToDrop> ret = new ArrayList<ItemToDrop>();
		rollFinal(items, ret, 1, calcChance);
		return ret;
	}

	public Collection<ItemToDrop> rollFixedQty(int diff, L2MonsterInstance monster, L2Player player, double mod)
	{
		// Поправка на глубоко синих мобов
		if(Config.DEEPBLUE_DROP_RULES && diff > 0)
			mod *= Experience.penaltyModifier(diff, 9);
		if(mod <= 0)
			return null;
		double rate;
		if(_notRate)
			rate = Math.min(mod, 1);
		else if(monster.isRaid())
			rate = Config.RATE_DROP_RAIDBOSS * player.getRateItems() * mod;
		else
			rate = Config.RATE_DROP_ITEMS * player.getRateItems() * mod;

		// Считаем шанс группы
		double calcChance = _chance * rate;
		Entry<Double, Integer> e = balanceChanceAndMult(calcChance);
		calcChance = e.getKey();
		int dropmult = e.getValue();

		ArrayList<ItemToDrop> ret = new ArrayList<ItemToDrop>();
		for(int n = 0; n < dropmult; n++)
			if(Rnd.get(1, L2Drop.MAX_CHANCE) < calcChance)
				rollFinal(_items, ret, 1, _chance);
		return ret;
	}

	private Collection<ItemToDrop> rollSpoil(int diff, L2Player player, double mod)
	{
		float rate = Config.RATE_DROP_SPOIL * player.getRateSpoil();
		// Поправка на глубоко синих мобов
		if(Config.DEEPBLUE_DROP_RULES && diff > 0)
			mod *= Experience.penaltyModifier(diff, 9);
		if(mod <= 0)
			return null;

		// Считаем шанс группы
		double calcChance = _chance * rate * mod;
		Entry<Double, Integer> e = balanceChanceAndMult(calcChance);
		calcChance = e.getKey();
		int dropmult = e.getValue();

		if(Rnd.get(1, L2Drop.MAX_CHANCE) > calcChance)
			return null;

		ArrayList<ItemToDrop> ret = new ArrayList<ItemToDrop>(1);
		rollFinal(_items, ret, dropmult, _chance);
		return ret;
	}

	private Collection<ItemToDrop> rollAdena(int diff, L2Player player, double mod)
	{
		float rate = Config.RATE_DROP_ADENA * player.getRateAdena();
		if(Config.DEEPBLUE_DROP_RULES && diff > 0)
			mod *= Experience.penaltyModifier(diff, 9);
		double chance = _chance;
		if(mod > 10)
		{
			mod *= _chance / L2Drop.MAX_CHANCE;
			chance = L2Drop.MAX_CHANCE;
		}
		if(mod <= 0 || Rnd.get(1, L2Drop.MAX_CHANCE) > chance)
			return null;
		double mult = rate * mod;

		ArrayList<ItemToDrop> ret = new ArrayList<ItemToDrop>(1);
		rollFinal(_items, ret, mult, _chance);
		for(ItemToDrop i : ret)
			i.isAdena = true;
		return ret;
	}

	public static Entry<Double, Integer> balanceChanceAndMult(Double calcChance)
	{
		Integer dropmult = 1;
		if(calcChance > L2Drop.MAX_CHANCE)
			if(calcChance % L2Drop.MAX_CHANCE == 0) // если кратен 100% то тупо умножаем количество
				dropmult = (int) (calcChance / L2Drop.MAX_CHANCE);
			else
			{ // иначе балансируем
				dropmult = (int) Math.ceil(calcChance / L2Drop.MAX_CHANCE); // множитель равен шанс / 100% округление вверх
				calcChance = calcChance / dropmult; // шанс равен шанс / множитель
				// в результате получаем увеличение количества и уменьшение шанса, при этом шанс не падает ниже 50%
			}
		return new SimpleEntry<Double, Integer>(calcChance, dropmult);
	}

	private void rollFinal(FastList<L2DropData> items, ArrayList<ItemToDrop> ret, double mult, double chanceSum)
	{
		// перебираем все вещи в группе и проверяем шанс
		int chance = Rnd.get(0, (int) chanceSum);
		for(Node<L2DropData> n = items.head(); n != items.tail(); n = n.getNext())
		{
			if(n.getValue() == null)
				continue;
			L2DropData i = n.getValue();
			if(chance < i.getChanceInGroup())
				continue;
			boolean notlast = false;
			for(Node<L2DropData> t = items.head(); t != items.tail(); t = t.getNext())
				if(t.getValue() != null && t.getValue().getChanceInGroup() > i.getChanceInGroup() && chance > t.getValue().getChanceInGroup())
				{
					notlast = true;
					break;
				}
			if(notlast)
				continue;

			ItemToDrop t = new ItemToDrop(i.getItemId());

			if(i.getMinDrop() >= i.getMaxDrop())
				t.count = (int) Math.round(i.getMinDrop() * mult);
			else
				t.count = Rnd.get((int) Math.round(i.getMinDrop() * mult), (int) Math.round(i.getMaxDrop() * mult));

			ret.add(t);
			break;
		}
	}

	public double getChance()
	{
		return _chance;
	}

	public void setChance(double chance)
	{
		_chance = chance;
	}

	public boolean isAdena()
	{
		return _isAdena;
	}
}