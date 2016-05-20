package l2d.game.model.instances;

import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.templates.L2NpcTemplate;

public class L2EffectPointInstance extends L2NpcInstance
{
	private L2Player _owner;

	public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2Player owner)
	{
		super(objectId, template);
		_owner = owner;
	}

	@Override
	public L2Player getPlayer()
	{
		return _owner;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	@Override
	public boolean isAttackable()
	{
		return false;
	}

	@Override
	public void doDie(L2Character killer)
	{}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{}

	@Override
	public void showChatWindow(L2Player player, String filename)
	{}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{}

	@Override
	public void onAction(L2Player player)
	{
		player.sendActionFailed();
	}
}