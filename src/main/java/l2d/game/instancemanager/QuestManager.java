package l2d.game.instancemanager;

import java.util.Collection;

import javolution.util.FastMap;
import l2d.game.model.quest.Quest;

public class QuestManager
{
	private static FastMap<String, Quest> _questsByName = new FastMap<String, Quest>();
	private static FastMap<Integer, Quest> _questsById = new FastMap<Integer, Quest>();

	public static Quest getQuest(String name)
	{
		return _questsByName.get(name);
	}

	public static Quest getQuest(int questId)
	{
		return _questsById.get(questId);
	}

	public static Quest getQuest2(String name)
	{
		if(_questsByName.containsKey(name))
			return _questsByName.get(name);
		try
		{
			int questId = Integer.valueOf(name);
			return _questsById.get(questId);
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public static void addQuest(Quest newQuest)
	{
		_questsByName.put(newQuest.getName(), newQuest);
		_questsById.put(newQuest.getQuestIntId(), newQuest);
	}

	public static Collection<Quest> getQuests()
	{
		return _questsByName.values();
	}
}