package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;

/**
 * @author PaInKiLlEr (L2Dream)
 */

public final class EffectClanGate extends L2Effect
{
	public EffectClanGate(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		_effected.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
		if(_effected instanceof L2Player)
		{
			L2Clan clan = ((L2Player) getEffected()).getClan();
			if(clan != null)
			{
				SystemMessage msg = new SystemMessage(SystemMessage.COURT_MAGICIAN_CREATED_PORTAL);
				clan.broadcastToOtherOnlineMembers(msg, ((L2Player) getEffected()));
			}
		}

		return;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
	}
}