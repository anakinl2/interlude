package l2d.game.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.base.L2EnchantSkillLearn;
import l2d.game.skills.conditions.Condition;
import l2d.game.tables.EnchantTable;
import l2d.game.tables.SkillTreeTable;
import l2d.game.templates.StatsSet;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

final class DocumentSkill extends DocumentBase
{
	public class Skill
	{
		public int id;
		public String name;
		public StatsSet[] sets;
		public int currentLevel;
		public ArrayList<L2Skill> skills = new ArrayList<L2Skill>();
		public ArrayList<L2Skill> currentSkills = new ArrayList<L2Skill>();
	}

	private Skill currentSkill;
	private List<L2Skill> skillsInFile = new LinkedList<L2Skill>();

	DocumentSkill(File file)
	{
		super(file);
	}

	private void setCurrentSkill(Skill skill)
	{
		currentSkill = skill;
	}

	protected List<L2Skill> getSkills()
	{
		return skillsInFile;
	}

	@Override
	protected Number getTableValue(String name)
	{
		try
		{
			Number[] a = tables.get(name);
			if(a.length - 1 >= currentSkill.currentLevel)
				return a[currentSkill.currentLevel];
			return a[a.length - 1];
		}
		catch(RuntimeException e)
		{
			_log.log(Level.SEVERE, "error in table of skill Id " + currentSkill.id, e);
			return 0;
		}
	}

	@Override
	protected Number getTableValue(String name, int idx)
	{
		idx--;
		try
		{
			Number[] a = tables.get(name);
			if(a.length - 1 >= idx)
				return a[idx];
			return a[a.length - 1];
		}
		catch(RuntimeException e)
		{
			_log.log(Level.SEVERE, "wrong level count in skill Id " + currentSkill.id + " table " + name + " level " + idx, e);
			return 0;
		}
	}

	@Override
	protected void parseDocument(Document doc)
	{
		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("skill".equalsIgnoreCase(d.getNodeName()))
					{
						setCurrentSkill(new Skill());
						parseSkill(d);
						skillsInFile.addAll(currentSkill.skills);
						resetTable();
					}
			}
			else if("skill".equalsIgnoreCase(n.getNodeName()))
			{
				setCurrentSkill(new Skill());
				parseSkill(n);
				skillsInFile.addAll(currentSkill.skills);
			}
	}

	private int[] ench_sp = {
			0,
			306000,
			315000,
			325000,
			346000,
			357000,
			368000,
			390000,
			402000,
			414000,
			507000,
			523000,
			538000,
			659000,
			680000,
			699000,
			857000,
			884000,
			909000,
			1114000,
			1149000,
			1182000,
			1448000,
			1494000,
			1537000,
			1882000,
			1942000,
			1998000,
			2447000,
			2525000,
			2597000 };

	private static byte[] elevels30 = {
			0,
			76,
			76,
			76,
			77,
			77,
			77,
			78,
			78,
			78,
			79,
			79,
			79,
			80,
			80,
			80,
			81,
			81,
			81,
			82,
			82,
			82,
			83,
			83,
			83,
			84,
			84,
			84,
			85,
			85,
			85 };

	protected void parseSkill(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		String skillName = attrs.getNamedItem("name").getNodeValue();
		String levels = attrs.getNamedItem("levels").getNodeValue();
		int lastLvl = Integer.parseInt(levels);

		HashMap<Short, Short> displayLevels = new HashMap<Short, Short>();

		// перебираем энчанты
		Node enchant = null;
		HashMap<String, Number[]> etables = new HashMap<String, Number[]>();
		int count = 0, eLevels = 0;
		Node d = n.cloneNode(true);
		for(int k = 0; k < d.getChildNodes().getLength(); k++)
		{
			enchant = d.getChildNodes().item(k);
			if(!enchant.getNodeName().startsWith("enchant"))
				continue;
			if(eLevels == 0)
				if(enchant.getAttributes().getNamedItem("levels") != null)
					eLevels = Integer.parseInt(enchant.getAttributes().getNamedItem("levels").getNodeValue());
				else
					eLevels = 30;
			String ename = enchant.getAttributes().getNamedItem("name").getNodeValue();
			for(int r = 1; r <= eLevels; r++)
			{
				short level = (short) (lastLvl + 30 * count + r);
				L2EnchantSkillLearn e = new L2EnchantSkillLearn(skillId, (count >= 1 ? 140 : 100) + r, skillName, "+" + r + " " + ename, (r == 1 ? lastLvl : (count >= 1 ? 140 : 100) + r), lastLvl, eLevels, ench_sp[r * (eLevels == 15 ? 2 : 1)]);

				ArrayList<L2EnchantSkillLearn> t = EnchantTable._enchant.get(skillId);

				if(t == null)
					t = new ArrayList<L2EnchantSkillLearn>();
				t.add(e);
				EnchantTable._enchant.put(skillId, t);
				displayLevels.put(level, count >= 1 ? (short) (100 + 40 + r) : (short) (100 + r));
			}
			count++;
			Node first = enchant.getFirstChild();
			Node curr = null;
			for(curr = first; curr != null; curr = curr.getNextSibling())
				if("table".equalsIgnoreCase(curr.getNodeName()))
				{
					NamedNodeMap a = curr.getAttributes();
					String name = a.getNamedItem("name").getNodeValue();
					Number[] table = parseTable(curr, false);
					table = fillTableToSize(table, eLevels);
					Number[] fulltable = etables.get(name);
					if(fulltable == null)
						fulltable = new Number[lastLvl + 30 * 6 + 1];
					
					System.arraycopy(table, 0, fulltable, lastLvl + (count - 1) * 30, eLevels);
					etables.put(name, fulltable);
				}
		}
		lastLvl += 30 * count;

		currentSkill.id = skillId;
		currentSkill.name = skillName;
		currentSkill.sets = new StatsSet[lastLvl];

		for(int i = 0; i < lastLvl; i++)
		{
			currentSkill.sets[i] = new StatsSet();
			currentSkill.sets[i].set("skill_id", currentSkill.id);
			currentSkill.sets[i].set("level", i + 1);
			currentSkill.sets[i].set("name", currentSkill.name);
		}

		if(currentSkill.sets.length != lastLvl)
			throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + lastLvl + " levels expected");

		Node first = n.getFirstChild();
		for(n = first; n != null; n = n.getNextSibling())
			if("table".equalsIgnoreCase(n.getNodeName()))
				parseTable(n, true);

		// обрабатываем таблицы сливая их с энчантами
		for(String tn : tables.keySet())
		{
			Number[] et = etables.get(tn);
			if(et != null)
			{
				Number[] t = tables.get(tn);
				Number max = t[t.length - 1];
				System.arraycopy(t, 0, et, 0, t.length);
				for(int j = 0; j < et.length; j++)
					if(et[j] == null)
						et[j] = max;
				tables.put(tn, et);
			}
		}

		for(int i = 1; i <= lastLvl; i++)
			for(n = first; n != null; n = n.getNextSibling())
				if("set".equalsIgnoreCase(n.getNodeName()))
					parseBeanSet(n, currentSkill.sets[i - 1], i);

		makeSkills();
		for(int i = 0; i < lastLvl; i++)
		{
			currentSkill.currentLevel = i;
			L2Skill current = currentSkill.currentSkills.get(i);
			if(displayLevels.get(current.getLevel()) != null)
			{
				current.setDisplayLevel(displayLevels.get(current.getLevel()).shortValue());
				if(current.getDisplayLevel() > 140)
					current.setMagicLevel(elevels30[current.getDisplayLevel() - 40 - (current.getDisplayLevel() - 40) / 100 * 100]);
				else
					current.setMagicLevel(elevels30[current.getDisplayLevel() - current.getDisplayLevel() / 100 * 100]);
			}
			for(n = first; n != null; n = n.getNextSibling())
			{
				if("cond".equalsIgnoreCase(n.getNodeName()))
				{
					Condition condition = parseCondition(n.getFirstChild());
					Node msg = n.getAttributes().getNamedItem("msg");
					Node msgId = n.getAttributes().getNamedItem("msgId");
					if(condition != null && msg != null)
						condition.setMessage(msg.getNodeValue());
					else if(condition != null && msgId != null)
					{
						condition.setMessageId(Integer.parseInt(msgId.getNodeValue()));
						Node addName = n.getAttributes().getNamedItem("addName");
						if(addName != null && Integer.parseInt(msgId.getNodeValue()) > 0)
							condition.addName();
					}
					current.attach(condition);
				}
				if("for".equalsIgnoreCase(n.getNodeName()))
					parseTemplate(n, current);
			}
		}
		currentSkill.skills.addAll(currentSkill.currentSkills);
	}

	private Number[] fillTableToSize(Number[] table, int size)
	{
		if(table.length < size)
		{
			Number[] ret = new Number[size];
			System.arraycopy(table, 0, ret, 0, table.length);
			table = ret;
		}
		for(int j = 1; j < size; j++)
			if(table[j] == null)
				table[j] = table[j - 1];
		return table;
	}

	private void makeSkills()
	{
		currentSkill.currentSkills = new ArrayList<L2Skill>(currentSkill.sets.length);
		// System.out.println(sets.length);
		for(int i = 0; i < currentSkill.sets.length; i++)
			currentSkill.currentSkills.add(i, currentSkill.sets[i].getEnum("skillType", SkillType.class).makeSkill(currentSkill.sets[i]));
	}
}