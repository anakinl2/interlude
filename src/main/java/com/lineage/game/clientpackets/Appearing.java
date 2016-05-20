package com.lineage.game.clientpackets;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;

/**
 * [C] 30 Appearing <p>
 * <b>Format:</b> 
 * @author Felixx
 * <p>
 * 0000: 30 <p>
 */
public class Appearing extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.inObserverMode())
		{
			activeChar.appearObserverMode();
			return;
		}

		if(!activeChar.isTeleporting() && L2World.containsPlayer(activeChar))
		{
			activeChar.sendActionFailed();
			return;
		}

		// 15 секунд после телепорта на персонажа не агрятся мобы
		activeChar.setNonAggroTime(System.currentTimeMillis() + 15000);

		// Персонаж появляется только после полной прогрузки
		ThreadPoolManager.getInstance().scheduleAi(new Runnable(){
			@Override
			public void run()
			{
				if(activeChar.isTeleporting())
					activeChar.onTeleported();
				else
					activeChar.spawnMe(activeChar.getLoc());

				activeChar.sendUserInfo(true);
				if(activeChar.getPet() != null)
					activeChar.getPet().teleportToOwner();
			}
		}, 2000, true);
	}
}