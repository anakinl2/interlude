package com.lineage.game.model.entity.residence;

import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.entity.siege.castle.CastleSiege;
import javolution.util.FastList;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.CastleManorManager;
import com.lineage.game.instancemanager.CastleManorManager.CropProcure;
import com.lineage.game.instancemanager.CastleManorManager.SeedProduction;
import com.lineage.game.instancemanager.SiegeGuardManager;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Manor;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.Warehouse;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Log;

public class Castle extends Residence
{
	protected static Logger _log = Logger.getLogger(Castle.class.getName());

	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";

	private FastList<CropProcure> _procure;
	private FastList<SeedProduction> _production;
	private FastList<CropProcure> _procureNext;
	private FastList<SeedProduction> _productionNext;
	private boolean _isNextPeriodApproved;

	private CastleSiege _Siege;
	private int _SiegeDayOfWeek;
	private int _SiegeHourOfDay;
	private int _TaxPercent;
	private double _TaxRate;
	private int _treasury;
	private int _townId;
	private int _collectedShops;
	private int _collectedSeed;

	public Castle(int castleId)
	{
		super(castleId);
	}

	@Override
	public CastleSiege getSiege()
	{
		if(_Siege == null)
			_Siege = new CastleSiege(this);
		return _Siege;
	}

	@Override
	public int getSiegeDayOfWeek()
	{
		return _SiegeDayOfWeek;
	}

	@Override
	public int getSiegeHourOfDay()
	{
		return _SiegeHourOfDay;
	}

	// This method sets the castle owner; null here means give it back to NPC
	@Override
	public void changeOwner(L2Clan clan)
	{
		// Если клан уже владел каким-либо замком/крепостью, отбираем его.
		if(clan != null)
			if(clan.getHasCastle() != 0)
			{
				Castle oldCastle = CastleManager.getInstance().getCastleByIndex(clan.getHasCastle());
				if(oldCastle != null)
					oldCastle.changeOwner(null);
			}

		// Если этим замком уже кто-то владел, отбираем у него замок
		if(getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
		{
			// Удаляем замковые скилы у старого владельца
			removeSkills();

			// Убираем налог
			setTaxPercent(null, 0);

			L2Clan oldOwner = getOwner();
			if(oldOwner != null)
			{
				// Переносим сокровищницу в вархауз старого владельца
				int amount = getTreasury();
				if(amount > 0)
				{
					Warehouse warehouse = oldOwner.getWarehouse();
					if(warehouse != null)
					{
						L2ItemInstance item = ItemTable.getInstance().createItem(57);
						item.setCount(amount);
						warehouse.addItem(item, null);
						addToTreasuryNoTax(-amount, false, false);
						Log.add(getName() + "|" + -amount + "|Castle:changeOwner", "treasury");
					}
				}

				// Отнимаем замок у старого владельца
				oldOwner.setHasCastle(0);
			}
		}

		// Выдаем замок новому владельцу
		if(clan != null)
			clan.setHasCastle(getId());

		// Сохраняем в базу
		updateOwnerInDB(clan);

		// Выдаем замковые скилы новому владельцу
		rewardSkills();

		// Удаляем наемных гвардов
		SiegeGuardManager.removeMercsFromDb(_id);

		// Удаляем все апгрейды замка
		removeUpgrade();

		// Полупобеда, если идет осада
		if(clan != null && getSiege().isInProgress())
			getSiege().midVictory();
	}

	// This method loads castle
	@Override
	protected void loadData()
	{
		_SiegeDayOfWeek = 7; // Default to saturday
		_SiegeHourOfDay = 20; // Default to 8 pm server time
		_TaxPercent = 0;
		_TaxRate = 0;
		_treasury = 0;
		_townId = 0;
		_Siege = null;
		_procure = new FastList<CropProcure>();
		_production = new FastList<SeedProduction>();
		_procureNext = new FastList<CropProcure>();
		_productionNext = new FastList<SeedProduction>();
		_isNextPeriodApproved = false;

		_type = ResidenceType.Castle;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ? LIMIT 1");
			statement.setInt(1, getId());
			rset = statement.executeQuery();

			if(rset.next())
				_ownerId = rset.getInt("clan_id");

			statement = con.prepareStatement("SELECT * FROM castle WHERE id=? LIMIT 1");
			statement.setInt(1, getId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				_name = rset.getString("name");

				_SiegeDayOfWeek = rset.getInt("siegeDayOfWeek");
				if(_SiegeDayOfWeek < 1 || _SiegeDayOfWeek > 7)
					_SiegeDayOfWeek = 7;

				_SiegeHourOfDay = rset.getInt("siegeHourOfDay");
				if(_SiegeHourOfDay < 0 || _SiegeHourOfDay > 23)
					_SiegeHourOfDay = 20;

				_TaxPercent = rset.getInt("taxPercent");
				_treasury = rset.getInt("treasury");
				_townId = rset.getInt("townId");
				getSiege().setSiegeDateTime(rset.getLong("siegeDate") * 1000);

				StringTokenizer st = new StringTokenizer(rset.getString("skills"), ";");
				while(st.hasMoreTokens())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));
					if(skill != null)
						_skills.put(skill.getId(), skill);
				}
			}
			DatabaseUtils.closeDatabaseSR(statement, rset);

			_TaxRate = _TaxPercent / 100.0;

			_zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.Castle, getId(), true);
		}
		catch(Exception e)
		{
			_log.warning("Exception: loadData(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	private void updateOwnerInDB(L2Clan clan)
	{
		_ownerId = clan == null ? 0 : clan.getClanId(); // Update owner id property

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=? LIMIT 1");
			statement.setInt(1, getId());
			statement.execute();
			DatabaseUtils.closeStatement(statement);
			statement = null;

			if(clan != null)
			{
				statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=? LIMIT 1");
				statement.setInt(1, getId());
				statement.setInt(2, getOwnerId());
				statement.execute();

				clan.broadcastClanStatus(false, true);
				clan.broadcastToOnlineMembers(new PlaySound("Siege_Victory"));
			}
		}
		catch(Exception e)
		{
			_log.warning("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public int getTaxPercent()
	{
		// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
		if(_TaxPercent > 5 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
			_TaxPercent = 5;
		return _TaxPercent;
	}

	public int getCollectedShops()
	{
		return _collectedShops;
	}

	public int getCollectedSeed()
	{
		return _collectedSeed;
	}

	public void setCollectedShops(int value)
	{
		_collectedShops = value;
	}

	public void setCollectedSeed(int value)
	{
		_collectedSeed = value;
	}

	// This method add to the treasury
	/** Add amount to castle instance's treasury (warehouse). */
	public void addToTreasury(int amount, boolean shop, boolean seed)
	{
		if(getOwnerId() <= 0)
			return;

		if(amount == 0)
			return;

		if(amount > 1 && _id != 5 && _id != 8) // If current castle instance is not Aden or Rune
		{
			Castle royal = CastleManager.getInstance().getCastleByIndex(_id >= 7 ? 8 : 5);
			if(royal != null)
			{
				int royalTax = (int) (amount * royal.getTaxRate()); // Find out what royal castle gets from the current castle instance's income
				if(royal.getOwnerId() > 0)
				{
					royal.addToTreasury(royalTax, shop, seed); // Only bother to really add the tax to the treasury if not npc owned
					if(_id == 5)
						Log.add("Aden|" + royalTax + "|Castle:adenTax", "treasury");
					else if(_id == 8)
						Log.add("Rune|" + royalTax + "|Castle:runeTax", "treasury");
				}

				amount -= royalTax; // Subtract royal castle income from current castle instance's income
			}
		}

		addToTreasuryNoTax(amount, shop, seed);
	}

	/** Add amount to castle instance's treasury (warehouse), no tax paying. */
	public void addToTreasuryNoTax(int amount, boolean shop, boolean seed)
	{
		if(getOwnerId() <= 0)
			return;

		if(amount == 0)
			return;

		// Add to the current treasury total. Use "-" to substract from treasury
		if(_treasury + amount > Integer.MAX_VALUE)
			_treasury = Integer.MAX_VALUE;
		else
			_treasury += amount;

		if(shop)
			_collectedShops += amount;

		if(seed)
			_collectedSeed += amount;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Update castle set treasury = ? where id = ?");
			statement.setLong(1, getTreasury());
			statement.setInt(2, getId());
			statement.execute();
			statement.close();

			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		catch(Exception e)
		{
			_log.warning("Exception: addToTreasuryNoTax(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public int getCropRewardType(int crop)
	{
		int rw = 0;
		for(CropProcure cp : _procure)
			if(cp.getId() == crop)
				rw = cp.getReward();
		return rw;
	}

	// This method updates the castle tax rate
	public void setTaxPercent(L2Player activeChar, int taxPercent)
	{
		_TaxPercent = taxPercent;
		_TaxRate = _TaxPercent / 100.0;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ? LIMIT 1");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getId());
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}

		if(activeChar != null)
			activeChar.sendMessage(new CustomMessage("com.lineage.game.model.entity.Castle.OutOfControl.CastleTaxChangetTo", activeChar).addString(getName()).addNumber(taxPercent));
	}

	public double getTaxRate()
	{
		// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
		if(_TaxRate > 0.05 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
			_TaxRate = 0.05;
		return _TaxRate;
	}

	public int getTreasury()
	{
		return _treasury;
	}

	/**
	 * Возвращает ID города, за которым закреплен замок
	 * 
	 * @return идентификатор города
	 */
	public int getTown()
	{
		return _townId;
	}

	public FastList<SeedProduction> getSeedProduction(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext;
	}

	public FastList<CropProcure> getCropProcure(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext;
	}

	public void setSeedProduction(FastList<SeedProduction> seed, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
			_production = seed;
		else
			_productionNext = seed;
	}

	public void setCropProcure(FastList<CropProcure> crop, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
			_procure = crop;
		else
			_procureNext = crop;
	}

	public synchronized SeedProduction getSeed(int seedId, int period)
	{
		for(SeedProduction seed : getSeedProduction(period))
			if(seed.getId() == seedId)
				return seed;
		return null;
	}

	public synchronized CropProcure getCrop(int cropId, int period)
	{
		for(CropProcure crop : getCropProcure(period))
			if(crop.getId() == cropId)
				return crop;
		return null;
	}

	public int getManorCost(int period)
	{
		FastList<CropProcure> procure;
		FastList<SeedProduction> production;

		if(period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}

		int total = 0;
		if(production != null)
			for(SeedProduction seed : production)
				total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
		if(procure != null)
			for(CropProcure crop : procure)
				total += crop.getPrice() * crop.getStartAmount();
		return total;
	}

	// Save manor production data
	public void saveSeedData()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
			statement.setInt(1, getId());

			statement.execute();
			statement.close();

			if(_production != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_production.size()];
				for(SeedProduction s : _production)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			if(_productionNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];
				for(SeedProduction s : _productionNext)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	// Save manor production data for specified period
	public void saveSeedData(int period)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			FastList<SeedProduction> prod = null;
			prod = getSeedProduction(period);

			if(prod != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[prod.size()];
				for(SeedProduction s : prod)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	// Save crop procure data
	public void saveCropData()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
			statement.setInt(1, getId());
			statement.execute();
			statement.close();
			if(_procure != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procure.size()];
				for(CropProcure cp : _procure)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
			if(_procureNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];
				for(CropProcure cp : _procureNext)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	// Save crop procure data for specified period
	public void saveCropData(int period)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			FastList<CropProcure> proc = null;
			proc = getCropProcure(period);

			if(proc != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[proc.size()];

				for(CropProcure cp : proc)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void updateCrop(int cropId, int amount, int period)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_CROP);
			statement.setInt(1, amount);
			statement.setInt(2, cropId);
			statement.setInt(3, getId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void updateSeed(int seedId, int amount, int period)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_SEED);
			statement.setInt(1, amount);
			statement.setInt(2, seedId);
			statement.setInt(3, getId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}

	public void setNextPeriodApproved(boolean val)
	{
		_isNextPeriodApproved = val;
	}
}