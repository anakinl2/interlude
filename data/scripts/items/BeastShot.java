package items;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Summon;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.SystemMessage;

public class BeastShot implements IItemHandler, ScriptFile
{
	private final static int[] _itemIds = {
			6645,	// Beast Soulshot
			6646,	// Beast Spiritshot
			6647	// Blessed Beast Spiritshot
	};

	static final SystemMessage PETS_AND_SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME = new SystemMessage(SystemMessage.PETS_AND_SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
	static final SystemMessage WHEN_PET_OR_SERVITOR_IS_DEAD_SOULSHOTS_OR_SPIRITSHOTS_FOR_PET_OR_SERVITOR_ARE_NOT_AVAILABLE = new SystemMessage(SystemMessage.WHEN_PET_OR_SERVITOR_IS_DEAD_SOULSHOTS_OR_SPIRITSHOTS_FOR_PET_OR_SERVITOR_ARE_NOT_AVAILABLE);
	static final SystemMessage YOU_DONT_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_PET_SERVITOR = new SystemMessage(SystemMessage.YOU_DONT_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_PET_SERVITOR);
	static final SystemMessage YOU_DONT_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR = new SystemMessage(SystemMessage.YOU_DONT_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		L2Summon pet = player.getPet();
		if(pet == null)
		{
			player.sendPacket(PETS_AND_SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return;
		}

		if(pet.isDead())
		{
			player.sendPacket(WHEN_PET_OR_SERVITOR_IS_DEAD_SOULSHOTS_OR_SPIRITSHOTS_FOR_PET_OR_SERVITOR_ARE_NOT_AVAILABLE);
			return;
		}

		int consumption = 0;
		int skillid = 0;

		switch(item.getItemId())
		{
			case 6645:
				if(pet.getChargedSoulShot())
					return;
				consumption = pet.getSoulshotConsumeCount();
				if(item.getIntegerLimitedCount() < consumption)
				{
					player.sendPacket(YOU_DONT_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_PET_SERVITOR);
					return;
				}
				pet.chargeSoulShot();
				skillid = 2033;
				break;
			case 6646:
				if(pet.getChargedSpiritShot() > 0)
					return;
				consumption = pet.getSpiritshotConsumeCount();
				if(item.getIntegerLimitedCount() < consumption)
				{
					player.sendPacket(YOU_DONT_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR);
					return;
				}
				pet.chargeSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
				skillid = 2008;
				break;
			case 6647:
				if(pet.getChargedSpiritShot() > 1)
					return;
				consumption = pet.getSpiritshotConsumeCount();
				if(item.getIntegerLimitedCount() < consumption)
				{
					player.sendPacket(YOU_DONT_HAVE_ENOUGH_SPIRITSHOTS_NEEDED_FOR_A_PET_SERVITOR);
					return;
				}
				pet.chargeSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
				skillid = 2009;
				break;
		}

		pet.broadcastPacket(new MagicSkillUse(pet, pet, skillid, 1, 0, 0));
		player.getInventory().destroyItem(item, consumption, false);
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