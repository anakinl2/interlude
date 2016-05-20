package items;

import java.util.logging.Logger;

import l2d.Config;
import l2d.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2PetInstance;
import l2d.game.serverpackets.MagicSkillLaunched;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.NpcTable;
import l2d.game.tables.PetDataTable;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2NpcTemplate;

public class PetSummon implements IItemHandler, ScriptFile
{
	protected static Logger _log = Logger.getLogger(PetSummon.class.getName());

	// all the items ids that this handler knowns
	private static final int[] _itemIds = PetDataTable.getPetControlItems();
	private static final int _skillId = 2046;

	static final SystemMessage YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME = new SystemMessage(SystemMessage.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
	static final SystemMessage A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE = new SystemMessage(SystemMessage.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
	static final SystemMessage SUMMON_A_PET = new SystemMessage(SystemMessage.SUMMON_A_PET);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		// pets resummon fast fix
		if(System.currentTimeMillis() <= player.lastDiceThrown)
			return;
		player.lastDiceThrown = System.currentTimeMillis() + 4000L;

		if(player.isTransactionInProgress())
			return;

		if(player.isSitting())
		{
			player.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			return;
		}

		if(player.isInOlympiadMode())
		{
			player.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if(player.isCastingNow() || player.isActionsDisabled())
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, 1);
		if(!skill.checkCondition(player, player, false, true, true))
			return;

		if(player.getPet() != null)
		{
			player.sendPacket(YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
			return;
		}

		if(player.isMounted() || player.isInBoat())
			return;

		if(player.isCursedWeaponEquipped())
		{
			// You can't mount while weilding a cursed weapon
			player.sendPacket(A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
			return;
		}

		int npcId = PetDataTable.getSummonId(item);
		if(npcId == 0)
			return;

		if(Config.ALT_DONT_ALLOW_PETS_ON_SIEGE && (PetDataTable.isBabyPet(npcId) && SiegeManager.getSiege(player, true) != null))
		{
			player.sendMessage("These pets may not be used in areas of sieges.");
			return;
		}

		L2NpcTemplate petTemplate = NpcTable.getTemplate(npcId);
		L2PetInstance newpet = L2PetInstance.spawnPet(petTemplate, player, item);

		if(newpet == null)
			return;

		newpet.setTitle(player.getName());

		if(!newpet.isRespawned())
			try
			{
				newpet.setCurrentHp(newpet.getMaxHp(), false);
				newpet.setCurrentMp(newpet.getMaxMp());
				newpet.setExp(newpet.getExpForThisLevel());
				newpet.setCurrentFed(newpet.getMaxFed());
			}
			catch(NullPointerException e)
			{
				_log.warning("PetSummon: failed set stats for summon " + npcId + ".");
				return;
			}

		if(!newpet.isRespawned())
			newpet.store();

		player.setPet(newpet);

		player.doCast(SkillTable.getInstance().getInfo(2046, 1), player, true);

		int _sleeping = SkillTable.getInstance().getInfo(2046, 1).getHitTime() + 500;

		try
		{
			Thread.sleep(_sleeping);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		player.sendPacket(SUMMON_A_PET);
		L2World.addObject(newpet);
		newpet.spawnMe(GeoEngine.findPointToStay(player.getX(), player.getY(), player.getZ(), 100, 150));
		newpet.setRunning();
		newpet.broadcastPetInfo();
		newpet.setShowSpawnAnimation(false);
		newpet.startFeed(false);

		// continue execution in 1 seconds
		ThreadPoolManager.getInstance().scheduleAi(new SummonFinalizer(player, newpet), 900, true);
	}

	static class SummonFinalizer implements Runnable
	{
		private L2Player _activeChar;
		private L2PetInstance _newpet;

		SummonFinalizer(L2Player activeChar, L2PetInstance newpet)
		{
			_activeChar = activeChar;
			_newpet = newpet;
		}

		public void run()
		{
			try
			{
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar.getObjectId(), 2046, 1, _newpet, true));
				_newpet.setFollowStatus(true);
			}
			catch(Throwable e)
			{
				_log.severe(e.toString());
			}
		}
	}

	public final int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}