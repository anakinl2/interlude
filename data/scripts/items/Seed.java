package items;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.TownManager;
import l2d.game.model.L2Manor;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.NextAction;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.instances.L2ChestInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2RaidBossInstance;
import l2d.game.tables.SkillTable;

public class Seed implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = {};

	private int _seedId;

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		// Цель не выбрана
		if(playable.getTarget() == null)
		{
			player.sendActionFailed();
			return;
		}

		// Цель не моб, РБ или миньон
		if(!player.getTarget().isMonster() || player.getTarget() instanceof L2RaidBossInstance || (player.getTarget() instanceof L2MinionInstance && ((L2MinionInstance) player.getTarget()).getLeader() instanceof L2RaidBossInstance) || player.getTarget() instanceof L2ChestInstance || (((L2MonsterInstance) playable.getTarget()).getChampion() > 0 && !item.isAltSeed()))
		{
			player.sendPacket(Msg.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING);
			return;
		}

		L2MonsterInstance target = (L2MonsterInstance) playable.getTarget();

		if(target == null)
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		// Моб мертв
		if(target.isDead())
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		// Уже посеяно
		if(target.isSeeded())
		{
			player.sendPacket(Msg.THE_SEED_HAS_BEEN_SOWN);
			return;
		}

		_seedId = item.getItemId();
		if(_seedId == 0 || player.getInventory().getItemByItemId(item.getItemId()) == null)
		{
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		int castleId = TownManager.getInstance().getClosestTown(player).getCastleIndex();
		if(castleId < 0)
			castleId = 1; // gludio manor dy default
		else
		{
			Residence castle = CastleManager.getInstance().getCastleByIndex(castleId);
			if(castle != null)
				castleId = castle.getId();
		}

		//System.out.println(CastleManager.getInstance().findNearestCastleIndex(activeChar) + " " +castleId + " " + L2Manor.getInstance().getSeedManorId(_seedId));
		// Несовпадение зоны
		if(L2Manor.getInstance().getCastleIdForSeed(_seedId) != castleId)
		{
			//System.out.println("seed (" + _seedId + ") zone " + L2Manor.getInstance().getSeedManorId(_seedId) + " != castle_zone " + castleId);
			player.sendPacket(Msg.THIS_SEED_MAY_NOT_BE_SOWN_HERE);
			return;
		}

		// use Sowing skill, id 2097
		L2Skill skill = SkillTable.getInstance().getInfo(2097, 1);
		if(skill == null)
		{
			player.sendActionFailed();
			return;
		}

		if(skill.checkCondition(player, target, false, false, true))
		{
			player.setUseSeed(_seedId);
			player.getAI().Cast(skill, target);
		}
		else if(skill.getNextAction() == NextAction.ATTACK && !player.equals(target) && target.isAutoAttackable(player))
			player.getAI().Attack(target, false);
		else
			player.sendActionFailed();
	}

	public final int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		_itemIds = new int[L2Manor.getInstance().getAllSeeds().size()];
		int id = 0;
		for(Integer s : L2Manor.getInstance().getAllSeeds().keySet())
			_itemIds[id++] = s.shortValue();
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}