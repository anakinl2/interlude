package commands.admin;

import java.util.StringTokenizer;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.instancemanager.CastleSiegeManager;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.PledgeShowInfoUpdate;
import l2d.game.serverpackets.PledgeStatusChanged;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.UserInfo;
import l2d.game.tables.ClanTable;

/**
 * Pledge Manipulation //pledge <create|dismiss|setlevel|resetcreate|resetwait|addrep>
 */
@SuppressWarnings("unused")
public class AdminPledge implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_pledge
	}

	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if(activeChar.getPlayerAccess() == null || !activeChar.getPlayerAccess().CanEditCharAll || activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
			return false;

		final L2Player target = (L2Player) activeChar.getTarget();

		if(fullString.startsWith("admin_pledge"))
		{
			final StringTokenizer st = new StringTokenizer(fullString);
			st.nextToken();

			final String action = st.nextToken(); // create|dismiss|setlevel|resetcreate|resetwait|addrep

			if(action.equals("create"))
			{
				try
				{
					final String pledgeName = st.nextToken();
					final L2Clan clan = ClanTable.getInstance().createClan(target, pledgeName);
					if(clan != null)
					{
						target.sendPacket(new PledgeShowInfoUpdate(clan));
						// target.sendUserInfo(true);
						target.sendPacket(new UserInfo(target));
						target.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_BEEN_CREATED));
						return true;
					}
				}
				catch(final Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(action.equals("dismiss"))
			{
				if(target.getClan() == null || target.getObjectId() != target.getClan().getLeaderId())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
					return false;
				}

				SiegeManager.removeSiegeSkills(target);
				final SystemMessage sm = new SystemMessage(SystemMessage.CLAN_HAS_DISPERSED);
				for(final L2Player clanMember : target.getClan().getOnlineMembers(0))
				{
					clanMember.setClan(null);
					clanMember.setTitle(null);
					clanMember.sendPacket(sm);
					clanMember.broadcastUserInfo(true);
				}

				ThreadConnection con = null;
				FiltredPreparedStatement statement = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("UPDATE characters SET clanid = 0 WHERE clanid=?");
					statement.setInt(1, target.getClanId());
					statement.execute();
					DatabaseUtils.closeStatement(statement);

					statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
					statement.setInt(1, target.getClanId());
					statement.execute();
					DatabaseUtils.closeStatement(statement);
					statement = null;
					target.sendPacket(sm);
					target.broadcastUserInfo(true);
				}
				catch(final Exception e)
				{}
				finally
				{
					DatabaseUtils.closeDatabaseCS(con, statement);
				}
				return true;
			}
			else if(action.equals("setlevel"))
			{
				if(target.getClan() == null || target.getObjectId() != target.getClan().getLeaderId())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
					return false;
				}

				try
				{
					final byte level = Byte.parseByte(st.nextToken());
					final L2Clan clan = target.getClan();

					activeChar.sendMessage("You set level " + level + " for clan " + clan.getName());
					clan.setLevel(level);
					clan.updateClanInDB();

					if(level < CastleSiegeManager.getSiegeClanMinLevel())
						SiegeManager.removeSiegeSkills(target);
					else
						SiegeManager.addSiegeSkills(target);

					if(level == 5)
						target.sendPacket(new SystemMessage(SystemMessage.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));

					final SystemMessage sm = new SystemMessage(SystemMessage.CLANS_SKILL_LEVEL_HAS_INCREASED);
					final PledgeShowInfoUpdate pu = new PledgeShowInfoUpdate(clan);
					final PledgeStatusChanged ps = new PledgeStatusChanged(clan);

					for(final L2Player member : clan.getOnlineMembers(0))
					{
						member.updatePledgeClass();
						member.sendPacket(sm);
						member.sendPacket(pu);
						member.sendPacket(ps);
						member.broadcastUserInfo(true);
					}

					return true;
				}
				catch(final Exception e)
				{}
			}
			else if(action.equals("resetcreate"))
			{
				if(target.getClan() == null)
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				target.getClan().setExpelledMemberTime(0);
				activeChar.sendMessage("The penalty for creating a clan has been lifted for" + target.getName());
			}
			else if(action.equals("resetwait"))
			{
				target.setLeaveClanTime(0);
				activeChar.sendMessage("The penalty for leaving a clan has been lifted for " + target.getName());
			}
			else if(action.equals("addrep"))
				try
				{
					final L2Clan clan = target.getClan();
					final int rep = Integer.parseInt(st.nextToken());

					if(clan == null || clan.getLevel() < 5)
					{
						activeChar.sendPacket(Msg.INVALID_TARGET);
						return false;
					}
					clan.incReputation(rep, false, "admin_manual");
					activeChar.sendMessage("Added " + rep + " clan points to clan " + clan.getName() + ".");

					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
					clan.broadcastToOnlineMembers(new PledgeStatusChanged(clan));
				}
				catch(final NumberFormatException nfe)
				{
					activeChar.sendMessage("Please specify a number of clan points to add.");
				}
		}

		return false;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}