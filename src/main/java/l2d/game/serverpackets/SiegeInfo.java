package l2d.game.serverpackets;

import java.util.Calendar;
import java.util.logging.Logger;

import l2d.game.model.L2Alliance;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.entity.residence.Residence;
import l2d.game.tables.ClanTable;

/**
 * Shows the Siege Info<BR>
 * <BR>
 * packet type id 0xc9<BR>
 * format: cdddSSdSdd<BR>
 * <BR>
 * c = c9<BR>
 * d = UnitID<BR>
 * d = Show Owner Controls (0x00 default || >=0x02(mask?) owner)<BR>
 * d = Owner ClanID<BR>
 * S = Owner ClanName<BR>
 * S = Owner Clan LeaderName<BR>
 * d = Owner AllyID<BR>
 * S = Owner AllyName<BR>
 * d = current time (seconds)<BR>
 * d = Siege time (seconds) (0 for selectable)<BR>
 * d = (UNKNOW) Siege Time Select Related
 */
public class SiegeInfo extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(SiegeInfo.class.getName());
	private Residence _unit;
	private long _startTime;
	private L2Player _activeChar;
	private int _id;
	private int _isOwner;
	private int _owner;
	private String _ownerName;
	private String _leaderName;
	private String _allyNname;
	private int _allyId;

	public SiegeInfo(Residence unit)
	{
		_unit = unit;
	}

	@Override
	public void runImpl()
	{
		_activeChar = getClient().getActiveChar();
		if(_activeChar == null)
			return;

		_ownerName = "NPC";
		_leaderName = "";
		_allyNname = "";
		_allyId = 0;

		if(_unit != null && _unit.getSiege() != null)
		{
			_id = _unit.getId();
			_isOwner = _unit.getOwnerId() == _activeChar.getClanId() && _activeChar.isClanLeader() ? 0x01 : 0x00;
			_owner = _unit.getOwnerId();
			if(_unit.getOwnerId() > 0)
			{
				L2Clan owner = ClanTable.getInstance().getClan(_unit.getOwnerId());
				if(owner != null)
				{
					_ownerName = owner.getName();
					_leaderName = owner.getLeaderName();
					if(owner.getAllyId() != 0)
					{
						L2Alliance alliance = ClanTable.getInstance().getAlliance(owner.getAllyId());
						_allyId = alliance.getAllyId();
						_allyNname = alliance.getAllyName();
					}
				}
				else
					_log.warning("Null owner for unit: " + _unit.getName());
			}
			_startTime = (int) (_unit.getSiege().getSiegeDate().getTimeInMillis() / 1000);
		}
	}

	@Override
	protected void writeImpl()
	{
		if(_activeChar == null)
			return;

		writeC(0xC9);
		writeD(_id);
		writeD(_isOwner);
		writeD(_owner);

		writeS(_ownerName); // Clan Name
		writeS(_leaderName); // Clan Leader Name
		writeD(_allyId); // Ally ID
		writeS(_allyNname); // Ally Name
		writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
		writeD((int) _startTime);
		writeD(0x00); //number of choices?
	}
}