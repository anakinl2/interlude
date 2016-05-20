package com.lineage.util;

import java.text.NumberFormat;
import java.util.Map.Entry;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.game.model.L2Drop;
import com.lineage.game.model.L2DropData;
import com.lineage.game.model.L2DropGroup;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.templates.L2NpcTemplate;

public abstract class DropList
{
	private static NumberFormat df = NumberFormat.getPercentInstance();
	static
	{
		df.setMaximumFractionDigits(4);
	}

	public static String generateDroplist(L2NpcTemplate template, L2MonsterInstance monster, double mod, L2Player pl)
	{
		StringBuffer tmp = new StringBuffer();
		tmp.append("<html><body><center><font color=\"LEVEL\">").append(template.name).append(", Id: ").append(template.getNpcId()).append("</font>");
		if(template.isDropHerbs)
			tmp.append("<br1><font color=\"00FF00\">herbs</font>");
		tmp.append("</center><table><tr><td></td></tr>");
		boolean emptylist = false;
		boolean overlevel = true;
		boolean icons = pl != null ? pl.getVarB("DroplistIcons") : false;
		double rateAdena = mod * Config.RATE_DROP_ADENA * (pl != null ? pl.getRateAdena() : 1);
		double rateDrop = mod * (template.isRaid ? Config.RATE_DROP_RAIDBOSS : Config.RATE_DROP_ITEMS) * (pl != null ? pl.getRateItems() : 1);
		double rateSpoil = mod * Config.RATE_DROP_SPOIL * (pl != null ? pl.getRateSpoil() : 1);
		if(template.getDropData() != null)
		{
			if(template.getDropData().getNormal() != null)
				for(L2DropGroup g : template.getDropData().getNormal())
				{
					if(g.isAdena() && rateAdena == 0)
						continue;
					else if(!g.isAdena() && rateDrop == 0)
						continue;
					overlevel = false;

					FastList<L2DropData> items; // список вещей группы
					double GCHANCE; // шанс группы, по сути сумма шансов вещей в ней, в расчетах используется только для фиксированного количества
					double dropmult; // множитель количества дропа, только для адены или фиксированного количества (как количество проходов)
					double chancemult; // множитель шанса дропа, только для фиксированного количества

					if(g.notRate()) // фактически только эпики
					{
						mod = Math.min(1, mod); // модификатор не может быть положительным, шанс может только уменьшиться
						GCHANCE = g.getChance() * mod; // на шанс влияет только модификатор уровня
						chancemult = mod;
						dropmult = 1; // количество жестко фиксировано
						items = g.getDropItems(false); // список стандартный
					}
					else if(g.isAdena())
					{
						if(mod < 10)
						{ // обычный моб
							GCHANCE = g.getChance(); // шанс жестко фиксирован
							chancemult = 1;
							dropmult = rateAdena; // количество меняется по рейту адены
						}
						else
						{ // чамп
							chancemult = L2Drop.MAX_CHANCE / g.getChance();
							dropmult = rateAdena * g.getChance() / L2Drop.MAX_CHANCE;
							GCHANCE = L2Drop.MAX_CHANCE;
						}
						items = g.getDropItems(false); // список стандартный
					}
					else if(template.isRaid || monster != null && monster.getChampion() > 0 || g.fixedQty()) // моб чамп/рейд или дроп экипировки/кеев
					{
						GCHANCE = g.getChance() * rateDrop; // в шансе группы берем рейт дропа
						Entry<Double, Integer> balanced = L2DropGroup.balanceChanceAndMult(GCHANCE); // балансируем шанс и количество так чтобы не зашкаливать за 100% 
						chancemult = balanced.getKey() / g.getChance();
						GCHANCE = balanced.getKey();
						dropmult = balanced.getValue();
						items = g.getDropItems(false); // список стандартный		
					}
					else
					{ // все остальные случаи - моб обычный, дроп всякой фигни
						items = g.getRatedItems(rateDrop); // стандартный балансирующий механизм обработки рейтов для дропа
						dropmult = 1; // уже учтено в механизме
						chancemult = 1; // уже учтено в механизме
						GCHANCE = 0;
						for(L2DropData i : items)
							GCHANCE += i.getChance(); // шенс группы пересчитываем
					}

					tmp.append("</table><br><center>Group chance: ").append(df.format(GCHANCE / L2Drop.MAX_CHANCE));
					if(dropmult > 1 && !g.isAdena()) // если количество фиксировано то используется увеличение числа проходов
					{
						tmp.append(" x").append((int) dropmult);
						dropmult = 1;
					}
					tmp.append("</center><table width=100%>");

					for(L2DropData d : items)
					{
						String chance = df.format(d.getChance() * chancemult / L2Drop.MAX_CHANCE);
						if(icons)
						{
							tmp.append("<tr><td width=32><img src=icon.").append(d.getItem().getIcon()).append(" width=32 height=32></td><td width=200>").append(compact(d.getItem().getName())).append("<br1>[");
							tmp.append(Math.round(d.getMinDrop() * dropmult)).append("-").append(Math.round(d.getMaxDrop() * dropmult)).append("]    ");
							tmp.append(chance).append("</td></tr>");
						}
						else
						{
							tmp.append("<tr><td width=80%>").append(compact(d.getItem().getName())).append("</td><td width=10%>");
							tmp.append(Math.min(Math.round((d.getMinDrop() + d.getMaxDrop()) * dropmult / 2f), 9999999)).append("</td><td width=10%>");
							tmp.append(chance).append("</td></tr>");
						}
					}
				}

			if(template.getDropData().getSpoil() != null)
				if(template.getDropData().getSpoil().size() > 0)
					if(rateSpoil > 0)
					{
						overlevel = false;
						tmp.append("</table><center>Spoil:</center><table width=100%>");
						for(L2DropGroup g : template.getDropData().getSpoil())
							for(L2DropData d : g.getDropItems(false))
							{
								Entry<Double, Integer> e = L2DropGroup.balanceChanceAndMult(d.getChance() * rateSpoil);
								double GCHANCE = e.getKey() / 1000000;
								int dropmult = e.getValue();
								if(icons)
								{
									tmp.append("<tr><td width=32><img src=icon.").append(d.getItem().getIcon()).append(" width=32 height=32></td><td width=200>").append(compact(d.getItem().getName())).append("<br1>[");
									tmp.append(d.getMinDrop() * dropmult).append("-").append(d.getMaxDrop() * dropmult).append("]    ");
									tmp.append(df.format(GCHANCE)).append("</td></tr>");
								}
								else
								{
									float qty = (d.getMinDrop() + d.getMaxDrop()) * dropmult / 2f;
									tmp.append("<tr><td width=80%>").append(compact(d.getItem().getName())).append("</td><td width=10%>");
									tmp.append(Math.round(qty)).append("</td><td width=10%>");
									tmp.append(df.format(GCHANCE)).append("</td></tr>");
								}
							}
					}
		}
		else
			emptylist = true;

		tmp.append("</table>");
		if(emptylist)
			tmp.append("<center>Droplist is empty</center>");
		else if(overlevel)
			tmp.append("<center>This monster is too weak for you!</center>");
		tmp.append("</body></html>");
		return tmp.toString();
	}

	public static String compact(String s)
	{
		return s.replaceFirst("Recipe:", "R:").replaceFirst("Common Item - ", "Common ").replaceFirst("Scroll: Enchant", "Enchant").replaceFirst("Compressed Package", "CP");
	}
}