package com.lineage.game.instancemanager;

import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.entity.Auction;

public class AuctionManager
{
	protected static Logger _log = Logger.getLogger(AuctionManager.class.getName());

	private static AuctionManager _instance;

	public static AuctionManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing AuctionManager");
			_instance = new AuctionManager();
			_instance.load();
		}
		return _instance;
	}

	private List<Auction> _auctions;

	public void reload()
	{
		getAuctions().clear();
		load();
	}

	private void load()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id FROM auction ORDER BY id");
			rs = statement.executeQuery();
			while(rs.next())
				getAuctions().add(new Auction(rs.getInt("id")));
			_log.info("Loaded: " + getAuctions().size() + " auction(s)");
		}
		catch(Exception e)
		{
			System.out.println("Exception: AuctionManager.load(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
	}

	public final Auction getAuction(int auctionId)
	{
		int index = getAuctionIndex(auctionId);
		if(index >= 0)
			return getAuctions().get(index);
		return null;
	}

	public final Auction getAuctionByCH(int chId)
	{
		for(Auction auction : _auctions)
			if(auction != null && auction.getItemId() == chId)
				return auction;
		return null;
	}

	public final int getAuctionIndex(int auctionId)
	{
		Auction auction;
		for(int i = 0; i < getAuctions().size(); i++)
		{
			auction = getAuctions().get(i);
			if(auction != null && auction.getId() == auctionId)
				return i;
		}
		return -1;
	}

	public final List<Auction> getAuctions()
	{
		if(_auctions == null)
			_auctions = new FastList<Auction>();
		return _auctions;
	}

	public void deleteAuctionFromDB(int itemId)
	{
		if(getAuctionByCH(itemId) == null)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
			statement.setInt(1, itemId);
			statement.execute();
			DatabaseUtils.closeStatement(statement);
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
			statement.setInt(1, getAuctionByCH(itemId).getId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		_auctions.remove(getAuctionByCH(itemId));
	}
}