package l2d.game.clientpackets;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.CastleManorManager.SeedProduction;

/**
 * Format: (ch) dd [ddd]
 * d - manor id
 * d - size
 * [
 * d - seed id
 * d - sales
 * d - price
 * ]
 */
public class RequestSetSeed extends L2GameClientPacket
{
	private int _size, _manorId;

	private int[] _items; // _size*3

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if(_size * 12 > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
		{
			_size = 0;
			return;
		}
		_items = new int[_size * 3];
		for(int i = 0; i < _size; i++)
		{
			int itemId = readD();
			_items[i * 3 + 0] = itemId;
			int sales = readD();
			_items[i * 3 + 1] = sales;
			int price = readD();
			_items[i * 3 + 2] = price;
		}
	}

	@Override
	protected void runImpl()
	{
		if(_size < 1)
			return;

		FastList<SeedProduction> seeds = new FastList<SeedProduction>();
		for(int i = 0; i < _size; i++)
		{
			int id = _items[i * 3 + 0];
			int sales = _items[i * 3 + 1];
			int price = _items[i * 3 + 2];
			if(id > 0)
			{
				SeedProduction s = CastleManorManager.getInstance().getNewSeedProduction(id, sales, price, sales);
				seeds.add(s);
			}
		}

		CastleManager.getInstance().getCastleByIndex(_manorId).setSeedProduction(seeds, CastleManorManager.PERIOD_NEXT);
		if(Config.MANOR_SAVE_ALL_ACTIONS)
			CastleManager.getInstance().getCastleByIndex(_manorId).saveSeedData(CastleManorManager.PERIOD_NEXT);
	}
}