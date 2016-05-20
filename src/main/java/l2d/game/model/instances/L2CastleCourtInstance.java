package l2d.game.model.instances;

import l2d.game.model.L2Clan;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.templates.L2NpcTemplate;

public class L2CastleCourtInstance extends L2NpcInstance
{
	// private static Logger _log = Logger.getLogger(L2CastleCourtInstance.class.getName());

	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

	/**
	 * @param template
	 */
	public L2CastleCourtInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();
		if(!isInRange(player, INTERACTION_DISTANCE))
			return;

		int condition = validateCondition(player);
		if(condition <= COND_ALL_FALSE || condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;

		if((player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) != L2Clan.CP_CS_USE_FUNCTIONS)
		{
			player.sendMessage("You don't have rights to do that.");
			return;
		}

		if(condition == COND_OWNER)
		{
			if(command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch(IndexOutOfBoundsException ioobe)
				{}
				catch(NumberFormatException nfe)
				{}
				showChatWindow(player, val);
			}
			else if(command.equals("gotoleader"))
			{
				String filename = "data/html/castle/courtmagician/courtmagician-nogate.htm";
				if(player.getClan() != null)
				{
					L2Player clanLeader = player.getClan().getLeader().getPlayer();
					if(clanLeader == null)
						return;

					if(clanLeader.getEffectList().getEffectByType(L2Effect.EffectType.ClanGate) != null)
						player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), false);
					else
						showChatWindow(player, filename);
				}
			}
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		player.sendActionFailed();
		String filename = "data/html/castle/courtmagician/courtmagician-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castle/courtmagician/courtmagician-busy.htm"; // Busy because of siege
			else if(condition == COND_OWNER)
				if(val == 0)
					filename = "data/html/castle/courtmagician/courtmagician.htm";
				else
					filename = "data/html/castle/courtmagician/courtmagician-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	protected int validateCondition(L2Player player)
	{
		if(player.isGM())
			return COND_OWNER;
		if(getCastle() != null && getCastle().getId() > 0)
			if(player.getClan() != null)
				if(getCastle().getSiege().isInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if(getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
		return COND_ALL_FALSE;
	}
}