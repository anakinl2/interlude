package l2d.game.clientpackets;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.CastleManorManager.CropProcure;

/**
 * Format: (ch) dd [dddc]
 * d - manor id
 * d - size
 * [
 * d - crop id
 * d - sales
 * d - price
 * c - reward type
 * ]
 */
public class RequestSetCrop extends L2GameClientPacket
{
	private int _size, _manorId;

	private int[] _items; // _size*4

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if(_size * 13 > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
		{
			_size = 0;
			return;
		}
		_items = new int[_size * 4];
		for(int i = 0; i < _size; i++)
		{
			int itemId = readD();
			_items[i * 4 + 0] = itemId;
			int sales = readD();
			_items[i * 4 + 1] = sales;
			int price = readD();
			_items[i * 4 + 2] = price;
			int type = readC();
			_items[i * 4 + 3] = type;
		}
	}

	@Override
	protected void runImpl()
	{
		if(_size < 1)
			return;

		FastList<CropProcure> crops = new FastList<CropProcure>();
		for(int i = 0; i < _size; i++)
		{
			int id = _items[i * 4 + 0];
			int sales = _items[i * 4 + 1];
			int price = _items[i * 4 + 2];
			int type = _items[i * 4 + 3];
			if(id > 0)
			{
				CropProcure s = CastleManorManager.getInstance().getNewCropProcure(id, sales, type, price, sales);
				crops.add(s);
			}
		}

		CastleManager.getInstance().getCastleByIndex(_manorId).setCropProcure(crops, CastleManorManager.PERIOD_NEXT);
		if(Config.MANOR_SAVE_ALL_ACTIONS)
			CastleManager.getInstance().getCastleByIndex(_manorId).saveCropData(CastleManorManager.PERIOD_NEXT);
	}
}