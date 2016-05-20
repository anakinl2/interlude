package l2d.game.model.instances;

import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.entity.siege.Siege;
import l2d.game.model.entity.siege.SiegeClan;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.ValidateLocation;
import l2d.game.templates.L2NpcTemplate;

public class L2SiegeHeadquarterquarterInstance extends L2NpcInstance
{
	private L2Player _player;
	private Siege _siege;
	private L2Clan _owner;
	private long _lastAnnouncedAttackedTime = 0;

	public L2SiegeHeadquarterquarterInstance(L2Player player, int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		_player = player;
		_siege = SiegeManager.getSiege(_player.getX(), _player.getY(), true);
		if(_player.getClan() == null || _siege == null)
			deleteMe();
		else
		{
			SiegeClan sc = _siege.getAttackerClan(_player.getClan());
			if(sc == null)
				deleteMe();
			else
				sc.setHeadquarter(this);

			_owner = _player.getClan();
		}
	}

	@Override
	public String getName()
	{
		return _owner.getName();
	}

	@Override
	public String getTitle()
	{
		return "";
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		L2Player player = attacker.getPlayer();
		if(player == null)
			return false;
		L2Clan clan = player.getClan();
		return clan == null || _owner.getLeaderId() != clan.getLeaderId();
	}

	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}

	@Override
	public void onAction(L2Player player)
	{
		if(player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(isAutoAttackable(player))
				player.getAI().Attack(this, false);
			else
				player.sendActionFailed();
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		SiegeClan sc = _siege.getAttackerClan(_player.getClan());
		if(sc != null)
			sc.removeHeadquarter();

		super.doDie(killer);
	}

	@Override
	public void reduceCurrentHp(final double damage, final L2Character attacker, L2Skill skill, final boolean awake, final boolean standUp, boolean directHp, boolean canReflect)
	{
		if(System.currentTimeMillis() - _lastAnnouncedAttackedTime > 120000)
		{
			_lastAnnouncedAttackedTime = System.currentTimeMillis();
			_owner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_BASE_IS_BEING_ATTACKED));
		}
		super.reduceCurrentHp(damage, attacker, skill, awake, standUp, directHp, canReflect);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public boolean isInvul()
	{
		return false;
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
}