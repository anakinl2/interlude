package items;

import l2d.game.handler.IItemHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2BossInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;

/**
 * @author PaInKiLlEr
 *         Breaking Arrow используется на фринтезе
 *         Двойным щелчком по фринтезе ставит на паузу музыкальный спектакль.
 *         Выполнено специально для L2Dream.su
 */
public class BreakingArrow implements IItemHandler
{
	private static final int[] ITEM_IDS = {8192};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		if( !(playable instanceof L2Player))
			return;
		L2Player activeChar = (L2Player) playable;
		L2Object target = activeChar.getTarget();
		if( !(target instanceof L2BossInstance) || target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.INVALID_TARGET));
		}
		L2BossInstance Frintezza = (L2BossInstance) target;
		if( !activeChar.isInsideRadius(174240, -89805, -5022, 500, false, false))
		{
			activeChar.sendMessage("The purpose is inaccessible");
			return;
		}
		if(itemId == 8192 && Frintezza.getObjectId() == 29045)
		{
			Frintezza.broadcastPacket(new SocialAction(Frintezza.getObjectId(), 2));
			activeChar.getInventory().destroyItem(8192, 1, false);
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}