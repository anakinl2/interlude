package l2d.game.clientpackets;

import l2d.Config;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;

/**
 * [C] 30 Appearing <p>
 * <b>Format:</b> cddddc
 * @author Felixx
 *<p>
 * 0000: 30 <p>
 */
@SuppressWarnings({ "nls", "unqualified-field-access", "boxing", "unused" })
public class AttackRequest extends L2GameClientPacket
{
	private int _objectId;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _attackId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC(); // 0 Для обычного клика 1 для Шифт + клик
	}

	@Override
	public void runImpl()
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

		if(!activeChar.getPlayerAccess().CanAttack)
		{
			activeChar.sendActionFailed();
			return;
		}

		L2Object target = activeChar.getVisibleObject(_objectId);

		if(target == null)
		{
			// Для провалившихся предметов, чтобы можно было все равно поднять
			target = L2World.findObject(_objectId);
			if(target == null || !(target instanceof L2ItemInstance))
			{
				activeChar.sendActionFailed();
				return;
			}
		}

		if(activeChar.getAggressionTarget() != null && activeChar.getAggressionTarget() != target)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getTarget() != target)
		{
			target.onAction(activeChar);
			return;
		}

		//noinspection ConstantConditions
		if(target.isPlayer() && (activeChar.isInBoat() || ((L2Player) target).isInBoat()))
		{
			activeChar.sendActionFailed();
			return;
		}

		if(target.getObjectId() != activeChar.getObjectId() && activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE && activeChar.getTransactionRequester() == null)
			target.onForcedAttack(activeChar);
	}
}