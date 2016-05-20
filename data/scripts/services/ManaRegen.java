package services;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SystemMessage;

public class ManaRegen extends Functions implements ScriptFile
{
	public static L2Object self;

	public void DoManaRegen()
	{
		L2Player player = (L2Player) self;
		//5 аден за 1 МП
		int cost = 5;
		int tax = (player.getMaxMp() - (int) player.getCurrentMp()) * cost;
		L2ItemInstance pay = player.getInventory().getItemByItemId(57);
		if(pay != null && pay.getCount() >= tax)
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(tax));
			player.sendPacket(new SystemMessage(SystemMessage.S1_MPS_HAVE_BEEN_RESTORED).addNumber(tax / 5));
			player.getInventory().destroyItem(pay, tax, true);
			player.setCurrentMp(player.getMaxMp());
		}
		else
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Mana Regen");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}