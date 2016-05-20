package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2WorldRegion;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.util.GArray;

/**
 * [C] 04 Action <p>
 * <b>Format:</b> cddddc
 * @author Felixx
 */
public class Action extends L2GameClientPacket
{
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX;
	@SuppressWarnings("unused")
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	private int _actionId;

	@Override
	public void readImpl()
	{
		_objectId = readD(); // objectId Цели
		_originX = readD(); // Координата x 
		_originY = readD(); // Координата y
		_originZ = readD(); // Координата z
		_actionId = readC(); // 0 Для обычного клика  1 Для Шифт + Клик
	}

	@Override
	public void runImpl()
	{
		try
		{
			L2Player activeChar = getClient().getActiveChar();
			if(activeChar == null)
				return;

			if(System.currentTimeMillis() - activeChar.getLastAttackPacket() < Config.ATTACK_PACKET_DELAY)
			{
				activeChar.sendActionFailed();
				return;
			}

			activeChar.setLastAttackPacket();

			if(activeChar.isOutOfControl())
			{
				activeChar.sendActionFailed();
				return;
			}

			if(activeChar.inObserverMode() && activeChar.getObservNeighbor() != null)
				for(L2WorldRegion region : activeChar.getObservNeighbor().getNeighbors())
					for(L2Object obj : region.getObjectsList(new GArray<L2Object>(), activeChar.getObjectId(), activeChar.getReflection()))
						if(obj != null && obj.getObjectId() == _objectId && activeChar.getTarget() != obj)
						{
							obj.onAction(activeChar);
							return;
						}

			if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			{
				activeChar.sendActionFailed();
				return;
			}

			L2Object obj = activeChar.getVisibleObject(_objectId);

			if(obj == null)
			{
				// Для провалившихся предметов, чтобы можно было все равно поднять
				obj = L2World.findObject(_objectId);
				if(obj == null || !(obj instanceof L2ItemInstance))
				{
					activeChar.sendActionFailed();
					return;
				}
			}

			if(activeChar.getAggressionTarget() != null && activeChar.getAggressionTarget() != obj)
			{
				activeChar.sendActionFailed();
				return;
			}

			switch(_actionId)
			{
				case 0:
					obj.onAction(activeChar);
					break;
				case 1:
					if(obj.isCharacter() && ((L2Character) obj).isAlikeDead())
						obj.onAction(activeChar);
					else
						obj.onActionShift(activeChar);
					break;
				default:
					// Обнаружение неправельных actionId.
					_log.warning("Character: " + activeChar.getName() + " requested invalid action: " + _actionId);
					activeChar.sendActionFailed();
					break;
			}
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
		}
	}
}