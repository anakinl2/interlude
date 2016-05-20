package l2d.game.serverpackets;

import l2d.game.GameTimeController;
import l2d.game.model.L2Player;
import l2d.util.Location;

public class CharSelected extends L2GameServerPacket
{
	//   SdSddddddddddffddddddddddddddddddddddddddddddddddddddddd d
	private int _sessionId, char_id, clan_id, sex, race, class_id;
	private String _name, _title;
	private Location _loc;
	private double curHp, curMp;
	private int _sp, level, karma, _int, _str, _con, _men, _dex, _wit;
	private long _exp;

	public CharSelected(final L2Player cha, final int sessionId)
	{
		_sessionId = sessionId;

		_name = cha.getName();
		char_id = cha.getObjectId(); //FIXME 0x00030b7a ??
		_title = cha.getTitle();
		clan_id = cha.getClanId();
		sex = cha.getSex();
		race = cha.getRace().ordinal();
		class_id = cha.getClassId().getId();
		_loc = cha.getLoc();
		curHp = cha.getCurrentHp();
		curMp = cha.getCurrentMp();
		_sp = cha.getSp();
		_exp = cha.getExp();
		level = cha.getLevel();
		karma = cha.getKarma();
		_int = cha.getINT();
		_str = cha.getSTR();
		_con = cha.getCON();
		_men = cha.getMEN();
		_dex = cha.getDEX();
		_wit = cha.getWIT();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x15);

		writeS(_name);
		writeD(char_id);
		writeS(_title);
		writeD(_sessionId);
		writeD(clan_id);
		writeD(0x00); //??
		writeD(sex);
		writeD(race);
		writeD(class_id);
		writeD(0x01); // active ??
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);

		writeF(curHp);
		writeF(curMp);
		writeD(_sp);
		writeQ(_exp);
		writeD(level);
		writeD(karma); //?
		writeD(0x0); //?
		writeD(_int);
		writeD(_str);
		writeD(_con);
		writeD(_men);
		writeD(_dex);
		writeD(_wit);
		for(int i = 0; i < 30; i++)
			writeD(0x00);
		writeD(0x00); //c3  work
		writeD(0x00); //c3  work

		// extra info
		writeD(GameTimeController.getInstance().getGameTime()); // in-game time

		writeD(0x00); //

		writeD(0x00); //c3

		writeD(0x00); //c3 InspectorBin
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3

		writeD(0x00); //c3 InspectorBin for 528 client
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3

	}
}