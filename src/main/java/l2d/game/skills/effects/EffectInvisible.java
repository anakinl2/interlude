package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.model.L2Player;
import l2d.game.model.L2WorldRegion;
import l2d.game.serverpackets.UserInfo;
import l2d.game.skills.Env;

public class EffectInvisible extends L2Effect
{
	public EffectInvisible(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		L2Player player = _effected.getPlayer();
		player.setInvisible(true);
		player.sendPacket(new UserInfo(player));

		if(player.getCurrentRegion() != null)
			for(L2WorldRegion neighbor : player.getCurrentRegion().getNeighbors())
				if(neighbor != null)
					neighbor.removePlayerFromOtherPlayers(player);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		L2Player player = _effected.getPlayer();
		player.setInvisible(false);
		player.broadcastUserInfo(true);
		if(player.getPet() != null)
			player.getPet().broadcastPetInfo();
	}
}