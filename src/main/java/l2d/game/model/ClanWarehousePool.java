package l2d.game.model;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.game.model.instances.L2ItemInstance;

public class ClanWarehousePool
{
	private class ClanWarehouseWork
	{
		private L2Player activeChar;
		private L2ItemInstance[] items;
		private int[] counts;
		public boolean complete;

		public ClanWarehouseWork(L2Player _activeChar, L2ItemInstance[] _items, int[] _counts)
		{
			activeChar = _activeChar;
			items = _items;
			counts = _counts;
			complete = false;
		}

		public synchronized void RunWork()
		{
			Warehouse warehouse2 = null;
			warehouse2 = activeChar.getClan().getWarehouse();

			for(int i = 0; i < items.length; i++)
			{
				if(counts[i] < 0)
				{
					_log.warning("Warning char:" + activeChar.getName() + " get Item from ClanWarhouse count < 0: objid=" + items[i].getObjectId());
					return;
				}
				L2ItemInstance TransferItem = warehouse2.takeItemByObj(items[i].getObjectId(), counts[i]);
				if(TransferItem == null)
				{
					_log.warning("Warning char:" + activeChar.getName() + " get null Item from ClanWarhouse: objid=" + items[i].getObjectId());
					continue;
				}
				activeChar.getInventory().addItem(TransferItem);
			}

			activeChar.sendChanges();

			complete = true;
		}
	}

	static final Logger _log = Logger.getLogger(ClanWarehousePool.class.getName());

	private static ClanWarehousePool _instance;
	private List<ClanWarehouseWork> _works;
	private boolean inWork;

	public static ClanWarehousePool getInstance()
	{
		if(_instance == null)
			_instance = new ClanWarehousePool();
		return _instance;
	}

	public ClanWarehousePool()
	{
		_works = new FastList<ClanWarehouseWork>();
		inWork = false;
	}

	public void AddWork(L2Player _activeChar, L2ItemInstance[] _items, int[] _counts)
	{
		ClanWarehouseWork cww = new ClanWarehouseWork(_activeChar, _items, _counts);
		_works.add(cww);
		if(Config.DEBUG)
			_log.warning("ClanWarehousePool: add work, work count " + _works.size());
		RunWorks();
	}

	private void RunWorks()
	{
		if(inWork)
		{
			if(Config.DEBUG)
				_log.warning("ClanWarehousePool: work in progress, work count " + _works.size());
			return;
		}

		inWork = true;
		try
		{
			if(_works.size() > 0)
			{
				ClanWarehouseWork cww = _works.get(0);
				if(!cww.complete)
				{
					if(Config.DEBUG)
						_log.warning("ClanWarehousePool: run work, work count " + _works.size());
					cww.RunWork();
				}
				_works.remove(0);
			}
		}
		catch(Exception e)
		{
			_log.warning("Error ClanWarehousePool: " + e);
		}
		inWork = false;

		if(!_works.isEmpty())
			RunWorks();
	}
}
