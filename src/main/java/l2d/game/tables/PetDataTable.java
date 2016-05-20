package l2d.game.tables;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2Character;
import l2d.game.model.L2PetData;
import l2d.game.model.L2Player;
import l2d.game.model.L2Summon;
import l2d.game.model.instances.L2ItemInstance;

public class PetDataTable
{
	private static final Logger _log = Logger.getLogger(PetDataTable.class.getName());

	private static PetDataTable _instance = new PetDataTable();
	private HashMap<Integer, L2PetData> _pets;

	public final static int PET_WOLF_ID = 12077;

	public final static int HATCHLING_WIND_ID = 12311;
	public final static int HATCHLING_STAR_ID = 12312;
	public final static int HATCHLING_TWILIGHT_ID = 12313;

	public final static int STRIDER_WIND_ID = 12526;
	public final static int STRIDER_STAR_ID = 12527;
	public final static int STRIDER_TWILIGHT_ID = 12528;

	public final static int WYVERN_ID = 12621;

	public final static int BABY_BUFFALO_ID = 12780;
	public final static int BABY_KOOKABURRA_ID = 12781;
	public final static int BABY_COUGAR_ID = 12782;

	public final static int SIN_EATER_ID = 12564;

	public static PetDataTable getInstance()
	{
		return _instance;
	}

	public void reload()
	{
		_instance = new PetDataTable();
	}

	private PetDataTable()
	{
		_pets = new HashMap<Integer, L2PetData>(1200, 0.95f);
		FillPetDataTable();
	}

	public L2PetData getInfo(int petNpcId, int level)
	{
		L2PetData result = null;
		while(result == null && level < 100)
		{
			result = _pets.get(petNpcId * 100 + level);
			level++;
		}

		return result;
	}

	private void FillPetDataTable()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		L2PetData petData;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id, level, exp, hp, mp, patk, pdef, matk, mdef, acc, evasion, crit, speed, atk_speed, cast_speed, max_meal, battle_meal, normal_meal, loadMax, hpregen, mpregen FROM pet_data");
			rset = statement.executeQuery();
			while(rset.next())
			{
				petData = new L2PetData();
				petData.setID(rset.getInt("id"));
				petData.setLevel(rset.getInt("level"));
				petData.setExp(rset.getLong("exp"));
				petData.setHP(rset.getInt("hp"));
				petData.setMP(rset.getInt("mp"));
				petData.setPAtk(rset.getInt("patk"));
				petData.setPDef(rset.getInt("pdef"));
				petData.setMAtk(rset.getInt("matk"));
				petData.setMDef(rset.getInt("mdef"));
				petData.setAccuracy(rset.getInt("acc"));
				petData.setEvasion(rset.getInt("evasion"));
				petData.setCritical(rset.getInt("crit"));
				petData.setSpeed(rset.getInt("speed"));
				petData.setAtkSpeed(rset.getInt("atk_speed"));
				petData.setCastSpeed(rset.getInt("cast_speed"));
				petData.setFeedMax(rset.getInt("max_meal"));
				petData.setFeedBattle(rset.getInt("battle_meal"));
				petData.setFeedNormal(rset.getInt("normal_meal"));
				petData.setMaxLoad(rset.getInt("loadMax"));
				petData.setHpRegen(rset.getInt("hpregen"));
				petData.setMpRegen(rset.getInt("mpregen"));

				petData.setControlItemId(getControlItemId(petData.getID()));
				petData.setFoodId(getFoodId(petData.getID()));
				petData.setMountable(isMountable(petData.getID()));
				petData.setMinLevel(getMinLevel(petData.getID()));
				petData.setAddFed(getAddFed(petData.getID()));

				_pets.put(petData.getID() * 100 + petData.getLevel(), petData);
			}
		}
		catch(Exception e)
		{
			_log.warning("Cannot fill up PetDataTable: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		_log.config("PetDataTable: Loaded " + _pets.size() + " pets.");
	}

	public static void deletePet(L2ItemInstance item, L2Character owner)
	{
		int petObjectId = 0;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId FROM pets WHERE item_obj_id=?");
			statement.setInt(1, item.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
				petObjectId = rset.getInt("objId");
			DatabaseUtils.closeDatabaseSR(statement, rset);

			L2Summon summon = owner.getPet();
			if(summon != null && summon.getObjectId() == petObjectId)
				summon.unSummon();

			L2Player player = owner.getPlayer();
			if(player != null && player.isMounted() && player.getMountObjId() == petObjectId)
				player.setMount(0, 0, 0);

			// if it's a pet control item, delete the pet
			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
			statement.setInt(1, item.getObjectId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not restore pet objectid:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public static void unSummonPet(L2ItemInstance oldItem, L2Character owner)
	{
		int petObjectId = 0;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId FROM pets WHERE item_obj_id=?");
			statement.setInt(1, oldItem.getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
				petObjectId = rset.getInt("objId");

			if(owner == null)
				return;

			L2Summon summon = owner.getPet();
			if(summon != null && summon.getObjectId() == petObjectId)
				summon.unSummon();

			L2Player player = owner.getPlayer();
			if(player != null && player.isMounted() && player.getMountObjId() == petObjectId)
				player.setMount(0, 0, 0);
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not restore pet objectid:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public static enum L2Pet
	{
		WOLF(PET_WOLF_ID, 2375, 2515, false, 1, 12),

		HATCHLING_WIND(HATCHLING_WIND_ID, 3500, 4038, false, 1, 12), //
		HATCHLING_STAR(HATCHLING_STAR_ID, 3501, 4038, false, 1, 12), //
		HATCHLING_TWILIGHT(HATCHLING_TWILIGHT_ID, 3502, 4038, false, 1, 100), //

		STRIDER_WIND(STRIDER_WIND_ID, 4422, 5168, true, 1, 12), //
		STRIDER_STAR(STRIDER_STAR_ID, 4423, 5168, true, 1, 12), //
		STRIDER_TWILIGHT(STRIDER_TWILIGHT_ID, 4424, 5168, true, 1, 100), //

		WYVERN(WYVERN_ID, 5249, 6316, true, 1, 12), //

		BABY_BUFFALO(BABY_BUFFALO_ID, 6648, 7582, false, 24, 12), //
		BABY_KOOKABURRA(BABY_KOOKABURRA_ID, 6650, 7582, false, 25, 12), //
		BABY_COUGAR(BABY_COUGAR_ID, 6649, 7582, false, 26, 12),

		SIN_EATER(SIN_EATER_ID, 4425, 2515, false, 1, 12); //

		private final int _npcId; // Что призвать
		private final int _controlItemId; // Чем призывать
		private final int _foodId; // Чем кормить
		private final boolean _isMountable; // Ездовой ли...
		private final int _minLevel; // Уровень, ниже которого не может опускаться пет
		private final int _addFed; // На сколько процентов увеличивается полоска еды, при кормлении

		private L2Pet(int npcId, int controlItemId, int foodId, boolean isMountabe, int minLevel, int addFed)
		{
			_npcId = npcId;
			_controlItemId = controlItemId;
			_foodId = foodId;
			_isMountable = isMountabe;
			_minLevel = minLevel;
			_addFed = addFed;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getControlItemId()
		{
			return _controlItemId;
		}

		public int getFoodId()
		{
			return _foodId;
		}

		public boolean isMountable()
		{
			return _isMountable;
		}

		public int getMinLevel()
		{
			return _minLevel;
		}

		public int getAddFed()
		{
			return _addFed;
		}
	}

	public static int getControlItemId(int npcId)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getNpcId() == npcId)
				return pet.getControlItemId();
		return 1;
	}

	public static int getFoodId(int npcId)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getNpcId() == npcId)
				return pet.getFoodId();
		return 1;
	}

	public static boolean isMountable(int npcId)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getNpcId() == npcId)
				return pet.isMountable();
		return false;
	}

	public static int getMinLevel(int npcId)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getNpcId() == npcId)
				return pet.getMinLevel();
		return 1;
	}

	public static int getAddFed(int npcId)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getNpcId() == npcId)
				return pet.getAddFed();
		return 1;
	}

	public static int getSummonId(L2ItemInstance item)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getControlItemId() == item.getItemId())
				return pet.getNpcId();
		return 0;
	}

	public static int[] getPetControlItems()
	{
		int[] items = new int[L2Pet.values().length];
		int i = 0;
		for(L2Pet pet : L2Pet.values())
			items[i++] = pet.getControlItemId();
		return items;
	}

	public static boolean isPetControlItem(L2ItemInstance item)
	{
		for(L2Pet pet : L2Pet.values())
			if(pet.getControlItemId() == item.getItemId())
				return true;
		return false;
	}

	public static boolean isBabyPet(int id)
	{
		switch(id)
		{
			case BABY_BUFFALO_ID:
			case BABY_KOOKABURRA_ID:
			case BABY_COUGAR_ID:
				return true;
			default:
				return false;
		}
	}

	public static boolean isWolf(int id)
	{
		return id == PET_WOLF_ID;
	}

	public static boolean isHatchling(int id)
	{
		switch(id)
		{
			case HATCHLING_WIND_ID:
			case HATCHLING_STAR_ID:
			case HATCHLING_TWILIGHT_ID:
				return true;
			default:
				return false;
		}
	}

	public static boolean isStrider(int id)
	{
		switch(id)
		{
			case STRIDER_WIND_ID:
			case STRIDER_STAR_ID:
			case STRIDER_TWILIGHT_ID:
				return true;
			default:
				return false;
		}
	}

	public static void unload()
	{
		if(_instance != null)
			_instance = null;
	}
}