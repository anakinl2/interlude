package ai;

import l2d.game.ai.Fighter;
import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.quest.Quest;

public class Quest024Fighter extends Fighter
{
	private final String myEvent;

	public Quest024Fighter(L2Character actor)
	{
		super(actor);
		myEvent = "playerInMobRange_" + actor.getNpcId();
	}

	@Override
	protected boolean thinkActive()
	{
		Quest q = QuestManager.getQuest(24);
		if(q != null)
			for(L2Player player : L2World.getAroundPlayers(getActor(), 900, 200))
				player.processQuestEvent(q.getName(), myEvent);

		return super.thinkActive();
	}
}