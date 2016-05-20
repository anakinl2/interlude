/**
 * 
 */
package com.lineage.game.model.quest;

public enum QuestEventType
{
	MOB_TARGETED_BY_SKILL(true), // onSkillUse action triggered when a character uses a skill on a mob
	MOBGOTATTACKED(true), // onAttack action triggered when a mob attacked by someone
	MOBKILLED(true), // onKill action triggered when a mob killed.
	QUEST_START(true), // onTalk action from start npcs
	QUEST_TALK(true), // onTalk action from npcs participating in a quest
	NPC_FIRST_TALK(false);

	// control whether this event type is allowed for the same npc template
	// in multiple quests
	// or if the npc must be registered in at most one quest for the
	// specified event
	private boolean _allowMultipleRegistration;

	QuestEventType(boolean allowMultipleRegistration)
	{
		_allowMultipleRegistration = allowMultipleRegistration;
	}

	public boolean isMultipleRegistrationAllowed()
	{
		return _allowMultipleRegistration;
	}
}