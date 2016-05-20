package com.lineage.game.model.instances;

import com.lineage.game.ai.L2CharacterAI;
import com.lineage.game.ai.L2StaticObjectAI;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.templates.L2NpcTemplate;

public final class L2ArtefactInstance extends L2NpcInstance
{
	public L2ArtefactInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2StaticObjectAI(this);
		return _ai;
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

	/**
	 * Артефакт нельзя убить
	 */
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
	public boolean isParalyzeImmune()
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
}