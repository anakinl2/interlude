package ai;

import javolution.util.FastList;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.MagicSkillUse;
import com.lineage.util.Rnd;

/**
 * @author PaInKiLlEr
 *         Индивидуальное AI моба Imperial Gravekeeper.
 *         случайным образом портирует членов клана, пришедшего с квестом 503
 *         Pursuit of Clan Ambition
 */
public class ImperialGravekeeper extends Fighter
{
	public ImperialGravekeeper(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		if(attacker == null || attacker.getPlayer() == null || attacker.getPlayer().isGM())
			return;
		if(Rnd.chance((1 - (_actor.getCurrentHp() / (_actor.getMaxHp() + 1))) * 50))
		{
			int count = 0;
			for(L2NpcInstance npc : _actor.getKnownNpc(500))
			{
				if(npc.getNpcId() == 27180)
					count++;
			}
			if(count < 4 || _actor.getMaxHp() / 3 > _actor.getCurrentHp())
			{
				FastList<L2Player> clanMembers = new FastList<L2Player>();
				for(L2Player player : _actor.getAroundPlayers(900))
				{
					if(player.isClanLeader())
					{
						QuestState qs_530 = player.getQuestState("_503_PursuitClanAmbition");
						if(qs_530 != null && qs_530.isStarted())
						{
							for(L2Player member : _actor.getAroundPlayers(900))
							{
								if(member.getClan() == player.getClan())
									clanMembers.add(member);
							}
							L2Player tpMember = clanMembers.get(Rnd.get(clanMembers.size()));
							if(tpMember != null)
							{
								int rndX = 30 + Rnd.get(100) * (Rnd.chance(50) ? 1 : -1);
								int rndY = 30 + Rnd.get(100) * (Rnd.chance(50) ? 1 : -1);
								tpMember.broadcastPacketToOthers(new MagicSkillUse(player, player, 4671, 1, 500, 0));
								tpMember.teleToLocation(171100 + rndX, 6510 + rndY, -2700);
							}
						}
					}
				}
			}
		}
		// actor.startAttackStanceTask();
		super.onEvtAttacked(attacker, damage);
	}
}