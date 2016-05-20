package com.lineage.game.ai;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill.SkillTargetType;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2ArtefactInstance;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2SiegeGuardInstance;
import com.lineage.util.Rnd;

public class L2StaticObjectAI extends L2CharacterAI
{
	L2Character _actor;
	L2Player _attacker;

	public L2StaticObjectAI(L2Character actor)
	{
		super(actor);
		_actor = actor;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		if(!(_actor instanceof L2DoorInstance) || attacker == null)
			return;

		L2Player player = attacker.getPlayer();

		if(player != null)
		{
			L2Clan clan = player.getClan();

			Siege siege = SiegeManager.getSiege(_actor, true);

			if(siege == null)
				return;

			if(clan != null && siege == clan.getSiege() && clan.isDefender())
				return;

			for(L2NpcInstance npc : _actor.getAroundNpc(900, 200))
			{
				if(!(npc instanceof L2SiegeGuardInstance))
					continue;

				if(Rnd.chance(20))
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 10000);
				else
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 2000);
			}
		}
	}

	@Override
	protected void onEvtAggression(L2Character attacker, int aggro)
	{
		if(!(_actor instanceof L2ArtefactInstance))
			return;

		if(attacker != null && attacker.getPlayer() != null)
		{
			L2Clan clan = attacker.getPlayer().getClan();
			//TODO присвоить осаду обьекту при спавне, чтобы избавиться от перебора
			if(clan != null && SiegeManager.getSiege(_actor, true) == clan.getSiege() && clan.isDefender())
				return;
			ThreadPoolManager.getInstance().scheduleAi(new notifyGuard(attacker.getPlayer()), 1000, false);
		}
	}

	public class notifyGuard implements Runnable
	{
		notifyGuard(L2Player attacker)
		{
			_attacker = attacker;
		}

		@Override
		public void run()
		{
			if(_attacker == null)
				return;

			for(L2NpcInstance npc : _actor.getAroundNpc(900, 200))
				if(npc instanceof L2SiegeGuardInstance && Rnd.chance(20))
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 5000);

			if(_attacker.getCastingSkill() != null && _attacker.getCastingSkill().getTargetType() == SkillTargetType.TARGET_HOLY)
				ThreadPoolManager.getInstance().scheduleAi(new notifyGuard(_attacker), 10000, false);
		}
	}
}