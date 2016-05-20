package com.lineage.game.model.instances;

import java.util.concurrent.ScheduledFuture;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.instancemanager.FourSepulchersManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.QuestState;
import com.lineage.game.templates.L2NpcTemplate;

/**
 * L2SepulcherBossInstance
 * 
 * @author: Ameron
 */
public class L2SepulcherBossInstance extends L2RaidBossInstance
{

	public int mysteriousBoxId = 0;
	private ScheduledFuture _onDeadEventTask;

	public L2SepulcherBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void doDie(L2Character killer)
	{
		giveCup((L2Player) killer);
		if(_onDeadEventTask != null)
			_onDeadEventTask.cancel(true);
		_onDeadEventTask = ThreadPoolManager.getInstance().scheduleEffect(new OnDeadEvent(this), 8500);
	}

	private void giveCup(L2Player player)
	{
		String questId = "_620_FourGoblets";
		int cupId = 0;
		int oldBrooch = 7262;

		switch(getNpcId())
		{
			case 25339:
				cupId = 7256;
				break;
			case 25342:
				cupId = 7257;
				break;
			case 25346:
				cupId = 7258;
				break;
			case 25349:
				cupId = 7259;
				break;
		}

		if(player.getParty() != null)
			for(L2Player mem : player.getParty().getPartyMembers())
			{
				QuestState qs = mem.getQuestState(questId);
				if(qs != null && (qs.isStarted() || qs.isCompleted()) && mem.getInventory().getItemByItemId(oldBrooch) == null)
					mem.getInventory().addItem(cupId, 1, getObjectId(), "Quest");
			}
		else
		{
			QuestState qs = player.getQuestState(questId);
			if(qs != null && (qs.isStarted() || qs.isCompleted()) && player.getInventory().getItemByItemId(oldBrooch) == null)
				player.getInventory().addItem(cupId, 1, getObjectId(), "Quest");
		}
	}

	private class OnDeadEvent implements Runnable
	{
		L2SepulcherBossInstance _activeChar;

		public OnDeadEvent(L2SepulcherBossInstance activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnEmperorsGraveNpc(_activeChar.mysteriousBoxId);
		}
	}

	@Override
	public void deleteMe()
	{
		if(_onDeadEventTask != null)
		{
			_onDeadEventTask.cancel(true);
			_onDeadEventTask = null;
		}

		super.deleteMe();
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return true;
	}

}
