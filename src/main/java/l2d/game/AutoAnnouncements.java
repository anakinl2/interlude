package l2d.game;

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;

public final class AutoAnnouncements
{
	private static final Logger _log = Logger.getLogger(AutoAnnouncements.class.getName());

	private static AutoAnnouncements _instance;

	public static AutoAnnouncements getInstance()
	{
		if(_instance == null)
			_instance = new AutoAnnouncements();

		return _instance;
	}

	private final List<AutoAnnouncer> _announcers = new FastList<AutoAnnouncer>();

	private AutoAnnouncements()
	{
		restore();
	}

	public void reload()
	{
		for(final AutoAnnouncer exec : _announcers)
			exec.cancel();

		_announcers.clear();

		restore();
	}

	private void announce(final String text)
	{
		Announcements.getInstance().announceToAll(text);

		_log.info("AutoAnnounce: " + text);
	}

	private void restore()
	{
		ThreadConnection conn = null;
		try
		{
			conn = L2DatabaseFactory.getInstance().getConnection();

			final FiltredPreparedStatement statement = conn.prepareStatement("SELECT initial, delay, cycle, memo FROM auto_announcements");
			final ResultSet data = statement.executeQuery();

			while(data.next())
			{
				final long initial = data.getLong("initial");
				final long delay = data.getLong("delay");
				final int repeat = data.getInt("cycle");
				final String[] memo = data.getString("memo").split("\n");

				_announcers.add(new AutoAnnouncer(memo, repeat, initial, delay));
			}

			data.close();
			statement.close();
		}
		catch(final Exception e)
		{
			_log.warning("AutoAnnoucements: Fail to load announcements data.");
		}
		finally
		{
			DatabaseUtils.closeConnection(conn);
		}

		_log.info("AutoAnnoucements: Load " + _announcers.size() + " Auto Annoucement Data.");
	}

	private final class AutoAnnouncer implements Runnable
	{
		private final String[] memo;
		@SuppressWarnings("unchecked")
		private Future task;

		private int repeat;

		private AutoAnnouncer(final String[] memo, final int repeat, final long initial, final long delay)
		{
			this.memo = memo;

			if(repeat > 0)
				this.repeat = repeat;
			else
				this.repeat = -1;

			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, initial * 1000, delay * 1000);
		}

		private void cancel()
		{
			task.cancel(false);
		}

		@Override
		public void run()
		{
			for(final String text : memo)
				announce(text);

			if(repeat > 0)
				repeat--;

			if(repeat == 0)
				cancel();
		}
	}
}