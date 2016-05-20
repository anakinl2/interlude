package l2d.game.instancemanager;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.ext.multilang.CustomMessage;
import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.entity.Couple;

public class CoupleManager
{
	protected static Logger _log = Logger.getLogger(CoupleManager.class.getName());

	private static CoupleManager _instance;

	private List<Couple> _couples;
	private volatile List<Couple> _deletedCouples;

	public static CoupleManager getInstance()
	{
		if(_instance == null)
			new CoupleManager();
		return _instance;
	}

	public CoupleManager()
	{
		_instance = this;
		_log.info("Initializing CoupleManager");
		_instance.load();
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new StoreTask(), 10 * 60 * 1000, 10 * 60 * 1000);
	}

	private void load()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM couples ORDER BY id");
			rs = statement.executeQuery();
			while(rs.next())
			{
				Couple c = new Couple(rs.getInt("id"));
				c.setPlayer1Id(rs.getInt("player1Id"));
				c.setPlayer2Id(rs.getInt("player2Id"));
				c.setMaried(rs.getBoolean("maried"));
				c.setAffiancedDate(rs.getLong("affiancedDate"));
				c.setWeddingDate(rs.getLong("weddingDate"));
				getCouples().add(c);
			}
			_log.info("Loaded: " + getCouples().size() + " couples(s)");
		}
		catch(Exception e)
		{
			_log.warning("Exception: CoupleManager.load(): " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
	}

	public final Couple getCouple(int coupleId)
	{
		for(Couple c : getCouples())
			if(c.getId() == coupleId)
				return c;
		return null;
	}

	/**
	 * Вызывается при каждом входе персонажа в мир
	 * @param cha
	 */
	public void engage(L2Player cha)
	{
		int chaId = cha.getObjectId();

		for(Couple cl : getCouples())
			if(cl.getPlayer1Id() == chaId || cl.getPlayer2Id() == chaId)
			{
				if(cl.getMaried())
					cha.setMaried(true);

				cha.setCoupleId(cl.getId());

				if(cl.getPlayer1Id() == chaId)
					cha.setPartnerId(cl.getPlayer2Id());
				else
					cha.setPartnerId(cl.getPlayer1Id());
			}
	}

	/**
	 * Уведомляет партнера персонажа о его входе в мир.
	 * @param cha
	 */
	public void notifyPartner(L2Player cha)
	{
		if(cha.getPartnerId() != 0)
		{
			L2Object partner = L2World.findObject(cha.getPartnerId());
			if(partner != null)
			{
				if(partner.isPlayer())
					((L2Player) partner).sendMessage(new CustomMessage("l2d.game.instancemanager.CoupleManager.PartnerEntered", partner));
				else
					_log.warning(cha + " partner is " + partner.getL2ClassShortName() + "?!!");
			}
			else if(Config.DEBUG)
				_log.info(cha + " partner not in world.");
		}
	}

	public void createCouple(L2Player player1, L2Player player2)
	{
		if(player1 != null && player2 != null)
			if(player1.getPartnerId() == 0 && player2.getPartnerId() == 0)
				getCouples().add(new Couple(player1, player2));
	}

	public final List<Couple> getCouples()
	{
		if(_couples == null)
			_couples = new FastList<Couple>();
		return _couples;
	}

	public List<Couple> getDeletedCouples()
	{
		if(_deletedCouples == null)
			_deletedCouples = new FastList<Couple>();
		return _deletedCouples;
	}

	/**
	 * Вызывется при шатдауне
	 * Сначала очищаем таблицу от ненужных свадеб, потом загоняем в нее все нужные.
	 * Обращение происходит только при загрузке/шатдауне сервера, ну или по запросу
	 */
	public void store()
	{
		ThreadConnection con = null;

		try
		{
			if(_deletedCouples != null && !_deletedCouples.isEmpty())
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				for(Couple c : _deletedCouples)
				{
					FiltredPreparedStatement statement = con.prepareStatement("DELETE FROM couples WHERE id = ?");
					statement.setInt(1, c.getId());
					statement.execute();
					statement.close();
				}
				_deletedCouples.clear();
			}

			if(_couples != null && !_couples.isEmpty())
				for(Couple c : _couples)
					if(c.isChanged())
					{
						if(con == null)
							con = L2DatabaseFactory.getInstance().getConnection();

						c.store(con);
						c.setChanged(false);
					}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}
	}

	private class StoreTask implements Runnable
	{
		private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

		@Override
		public void run()
		{
			store();
			_log.fine("Scheduled couple DB storing finished at: " + formatter.format(System.currentTimeMillis()));
		}
	}
}