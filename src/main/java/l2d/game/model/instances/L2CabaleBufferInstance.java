package l2d.game.model.instances;

import java.util.concurrent.ScheduledFuture;

import l2d.game.ThreadPoolManager;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Summon;
import l2d.game.model.L2World;
import l2d.game.model.entity.SevenSigns;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2NpcTemplate;

public class L2CabaleBufferInstance extends L2NpcInstance
{
	ScheduledFuture aiTask;

	class CabalaAI implements Runnable
	{
		L2CabaleBufferInstance _caster;

		CabalaAI(L2CabaleBufferInstance caster)
		{
			_caster = caster;
		}

		@Override
		public void run()
		{
			int winningCabal = SevenSigns.getInstance().getCabalHighestScore();

			if(winningCabal == SevenSigns.CABAL_NULL)
				return;

			int losingCabal = SevenSigns.CABAL_NULL;

			if(winningCabal == SevenSigns.CABAL_DAWN)
				losingCabal = SevenSigns.CABAL_DUSK;
			else if(winningCabal == SevenSigns.CABAL_DUSK)
				losingCabal = SevenSigns.CABAL_DAWN;

			/**
			 * For each known player in range, cast either the positive or negative buff.
			 * <BR>
			 * The stats affected depend on the player type, either a fighter or a mystic.
			 * <BR>
			 * Curse of Destruction (Loser)
			 *  - Fighters: -25% Accuracy, -25% Effect Resistance
			 *  - Mystics: -25% Casting Speed, -25% Effect Resistance
			 *
			 * Blessing of Prophecy (Winner)
			 *  - Fighters: +25% Max Load, +25% Effect Resistance
			 *  - Mystics: +25% Magic Cancel Resist, +25% Effect Resistance
			 */
			for(L2Player player : L2World.getAroundPlayers(L2CabaleBufferInstance.this, 900, 200))
			{
				if(player == null)
					continue;
				int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
				if(playerCabal == winningCabal && _caster.getNpcId() == SevenSigns.ORATOR_NPC_ID)
					if(player.isMageClass())
						handleCast(player, 4365);
					else
						handleCast(player, 4364);
				else if(playerCabal == losingCabal && _caster.getNpcId() == SevenSigns.PREACHER_NPC_ID)
					if(player.isMageClass())
						handleCast(player, 4362);
					else
						handleCast(player, 4361);
			}
		}

		private void handleCast(L2Player player, int skillId)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, 2);
			if(player.getEffectList().getEffectsBySkill(skill) == null && GeoEngine.canSeeTarget(_caster, player, false))
			{
				skill.getEffects(_caster, player, false, false);
				broadcastPacket(new MagicSkillUse(_caster, player, skill.getId(), 2, skill.getHitTime(), 0));
			}

			L2Summon summon = player.getPet();
			if(summon != null && summon.getEffectList().getEffectsBySkill(skill) == null && summon.isInRangeZ(_caster, 900) && GeoEngine.canSeeTarget(_caster, summon, false))
			{
				skill.getEffects(_caster, summon, false, false);
				broadcastPacket(new MagicSkillUse(_caster, summon, skill.getId(), 2, skill.getHitTime(), 0));
			}
		}
	}

	public L2CabaleBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new CabalaAI(this), 3000, 3000);
	}

	@Override
	public void deleteMe()
	{
		if(aiTask != null)
			aiTask.cancel(true);

		super.deleteMe();
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}