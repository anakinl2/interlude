package l2d.game.model.quest;

import java.util.concurrent.ScheduledFuture;

import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;

public class QuestTimer
{
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if(!isActive())
				return;

			if(getPlayer() != null && getQuest() != null && getQuest().getName() != null && getName() != null)
				getPlayer().processQuestEvent(getQuest().getName(), getName());
			cancel();
		}
	}

	private boolean _isActive = true;
	private String _name;
	private L2NpcInstance _npc;
	private L2Player _player;
	private Quest _quest;
	private ScheduledFuture _schedular;

	public QuestTimer(Quest quest, String name, long time, L2NpcInstance npc, L2Player player)
	{
		_name = name;
		_quest = quest;
		_player = player;
		_npc = npc;
		_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task
	}

	public QuestTimer(QuestState qs, String name, long time)
	{
		_name = name;
		_quest = qs.getQuest();
		_player = qs.getPlayer();
		_npc = null;
		_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task
	}

	public void cancel()
	{
		_isActive = false;

		if(_schedular != null)
			_schedular.cancel(false);

		getQuest().removeQuestTimer(this);
	}

	public final boolean isActive()
	{
		return _isActive;
	}

	public final String getName()
	{
		return _name;
	}

	public final L2NpcInstance getNpc()
	{
		return _npc;
	}

	public final L2Player getPlayer()
	{
		return _player;
	}

	public final Quest getQuest()
	{
		return _quest;
	}

	// public method to compare if this timer matches with the key attributes passed.
	// a quest and a name are required.
	// null npc or player act as wildcards for the match
	public boolean isMatch(Quest quest, String name, L2NpcInstance npc, L2Player player)
	{
		if(quest == null || name == null)
			return false;
		if(quest != getQuest() || name.compareToIgnoreCase(getName()) != 0)
			return false;
		return (npc == null || getNpc() == null || npc == getNpc()) && (player == null || getPlayer() == null || player == getPlayer());
	}

	@Override
	public final String toString()
	{
		return _name;
	}
}