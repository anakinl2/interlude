package l2d.game.skills.effects;

import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Effect;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import com.lineage.util.Location;

public class EffectTurner extends L2Effect
{
	public EffectTurner(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		int posX = _effected.getX();
		int posY = _effected.getY();
		int signx = -1;
		int signy = -1;
		if(_effected.getX() > _effected.getX())
			signx = 1;
		if(_effected.getY() > _effected.getY())
			signy = 1;

		_effected.stopMove();
		_effected.setHeading(_effected, false);

		if(!_effected.isMonster())
			_effected.setTarget(null);
		_effected.setRunning();

		Location loc = GeoEngine.moveCheck(_effected.getX(), _effected.getY(), _effected.getZ(), posX + signx * 40, posY + signy * 40);
		_effected.moveToLocation(loc, 0, false);

		_effected.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(_displayId, _displayLevel));
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopStunning();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}