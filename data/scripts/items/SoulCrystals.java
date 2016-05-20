package items;

import l2d.ext.scripts.ScriptFile;
import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SetupGauge;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillTable;

public class SoulCrystals implements IItemHandler, ScriptFile
{
	// First line is for Red Soul Crystals, second is Green and third is Blue Soul
	// Crystals,
	// ordered by ascending level, from 0 to 14
	public static final int[] _itemIds = {
			4629,
			4640,
			4651,
			4630,
			4641,
			4652,
			4631,
			4642,
			4653,
			4632,
			4643,
			4654,
			4633,
			4644,
			4655,
			4634,
			4645,
			4656,
			4635,
			4646,
			4657,
			4636,
			4647,
			4658,
			4637,
			4648,
			4659,
			4638,
			4649,
			4660,
			4639,
			4650,
			4661,
			5577,
			5578,
			5579,
			5580,
			5581,
			5582,
			5908,
			5911,
			5914,
			9570,
			9571,
			9572 };

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(player.getTarget() == null || !player.getTarget().isMonster())
		{
			player.sendPacket(Msg.INVALID_TARGET);
			player.sendActionFailed();
			return;
		}

		if(player.isImobilised() || player.isCastingNow())
		{
			player.sendActionFailed();
			return;
		}

		L2MonsterInstance target = (L2MonsterInstance) player.getTarget();

		// u can use soul crystal only when target hp goes to <50%
		if(target.getCurrentHpPercents() >= 50)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_SOUL_CRYSTAL_WAS_NOT_ABLE_TO_ABSORB_A_SOUL));
			player.sendActionFailed();
			return;
		}

		// Soul Crystal Casting section
		L2Skill skill = SkillTable.getInstance().getInfo(2096, 1);
		player.broadcastPacket(new MagicSkillUse(player, 2096, 1, skill.getHitTime(), 0));
		player.sendPacket(new SetupGauge(0, skill.getHitTime()));
		// End Soul Crystal Casting section

		// Continue execution later
		player._skillTask = ThreadPoolManager.getInstance().scheduleAi(new CrystalFinalizer(player, target, item.getItemId()), skill.getHitTime(), true);
	}

	static class CrystalFinalizer implements Runnable
	{
		private L2Player _activeChar;
		private L2MonsterInstance _target;
		private int _crystalId;

		CrystalFinalizer(L2Player activeChar, L2MonsterInstance target, int crystalId)
		{
			_activeChar = activeChar;
			_target = target;
			_crystalId = crystalId;
		}

		public void run()
		{
			_activeChar.sendActionFailed();
			_activeChar._skillTask = null;
			if(_activeChar.isDead() || _target.isDead())
				return;
			_target.addAbsorber(_activeChar, _crystalId);
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