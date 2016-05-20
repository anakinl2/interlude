package l2d.game.skills.effects;

import l2d.game.ai.CtrlIntention;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Summon;
import l2d.game.skills.Env;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public final class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;

	public EffectFear(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isFearImmune())
		{
			getEffector().sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			exit();
			return;
		}

		// Fear нельзя наложить на осадных саммонов
		if(_effected instanceof L2Summon && ((L2Summon) _effected).isSiegeWeapon())
		{
			getEffector().sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			exit();
			return;
		}

		if(_effected.isInZonePeace())
		{
			getEffector().sendPacket(Msg.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE);
			exit();
			return;
		}

		_effected.startFear();

		onActionTime();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopFear();
		_effected.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}

	@Override
	public boolean onActionTime()
	{
		Location pos = Rnd.coordsRandomize(_effected.getLoc(), FEAR_RANGE, FEAR_RANGE);
		Location loc = GeoEngine.moveCheck(_effected.getX(), _effected.getY(), _effected.getZ(), pos.x, pos.y);

		_effected.setRunning();
		_effected.moveToLocation(loc, 0, false);
		_effected.sendMessage("You can feel Fears's effect");

		return true;
	}
}