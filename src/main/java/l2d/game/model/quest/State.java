package l2d.game.model.quest;

public class State
{
	/** Name of the quest */
	private String _name;

	/** Quest object associated to the state */
	private final Quest _quest;

	/**
	 * Constructor for the state of the quest.
	 * @param name : String pointing out the name of the quest
	 * @param quest : Quest
	 */
	public State(String name, Quest quest)
	{
		_name = name;
		_quest = quest;
		quest.addState(this);
	}

	/**
	 * Add drop for the quest at this state of the quest
	 * @param npcId : int designating the ID of the NPC
	 * @param itemId : int designating the ID of the item dropped
	 * @param chance : int designating the chance the item dropped
	 */
	@Deprecated
	public void addQuestDrop(int npcId, int itemId, int chance)
	{
		_quest.addQuestItem(itemId);
	}

	/**
	 * Return name of the quest
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}

	public Quest getQuest()
	{
		return _quest;
	}

	/**
	 * Return name of the quest
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(!(o instanceof State))
			return false;
		if(((State) o)._name.equals(_name))
			return true;
		return false;
	}
}