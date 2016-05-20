package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2StaticObjectInstance;
import com.lineage.game.serverpackets.ChairSit;

public final class ChangeWaitType2 extends L2GameClientPacket
{

	private boolean _typeStand;

	@Override
	protected void readImpl()
	{
		_typeStand = readD() == 1;
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		L2Object target = activeChar.getTarget();

		if(getClient() != null)
		{

			if(activeChar.isMounted())
				activeChar.sendActionFailed();

			int distance = (int) activeChar.getDistance(activeChar.getTarget());
			if(target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && distance <= L2Character.INTERACTION_DISTANCE)
			{
				ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
				activeChar.sendPacket(cs);
				activeChar.sitDown();
				activeChar.broadcastPacket(cs);
			}

			if(activeChar.isFakeDeath())
			{
				activeChar.getEffectList().stopEffects(EffectType.FakeDeath);
				activeChar.updateEffectIcons();
			}

			if(_typeStand)
				activeChar.standUp();
			else
				activeChar.sitDown();
		}
	}
}
