package l2d.game.clientpackets;

import com.lineage.Config;
import l2d.game.model.L2ManufactureItem;
import l2d.game.model.L2ManufactureList;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.RecipeShopMsg;
import l2d.game.serverpackets.SystemMessage;

public class RequestRecipeShopListSet extends L2GameClientPacket
{
	// format: cdb, b - array of (dd)
	private int _count;
	L2ManufactureList createList = new L2ManufactureList();

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 8 > _buf.remaining() || _count > Short.MAX_VALUE || _count < 0)
		{
			_count = 0;
			return;
		}
		for(int x = 0; x < _count; x++)
			createList.add(new L2ManufactureItem(readD(), readD()));
		_count = createList.size();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(!activeChar.checksForShop(true))
		{
			cancelStore(activeChar);
			return;
		}

		if(activeChar.getNoChannel() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_CURRENTLY_BANNED_FROM_ACTIVITIES_RELATED_TO_THE_PRIVATE_STORE_AND_PRIVATE_WORKSHOP));
			cancelStore(activeChar);
			return;
		}

		if(_count == 0 || activeChar.getCreateList() == null)
		{
			cancelStore(activeChar);
			return;
		}

		if(_count > Config.MAX_PVTCRAFT_SLOTS)
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
			cancelStore(activeChar);
			return;
		}

		createList.setStoreName(activeChar.getCreateList().getStoreName());
		activeChar.setCreateList(createList);

		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_MANUFACTURE);
		activeChar.broadcastUserInfo(true);
		activeChar.broadcastPacket(new RecipeShopMsg(activeChar));
		activeChar.sitDown();
	}

	private void cancelStore(L2Player activeChar)
	{
		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.broadcastUserInfo(true);
		activeChar.getBuyList().clear();
		if(activeChar.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			activeChar.setOfflineMode(false);
			activeChar.logout(false, false, true);
			activeChar.getNetConnection().disconnectOffline();
		}
	}
}