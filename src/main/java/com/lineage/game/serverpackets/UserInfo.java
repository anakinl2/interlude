package com.lineage.game.serverpackets;

import com.lineage.Config;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.PcInventory;
import com.lineage.game.model.instances.L2CubicInstance;
import com.lineage.game.tables.NpcTable;
import com.lineage.util.Location;

public class UserInfo extends L2GameServerPacket
{
	private boolean can_writeImpl = false, partyRoom;
	private final L2Player _cha;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd, _relation;
	private float move_speed, attack_speed, col_radius, col_height;
	private PcInventory _inv;
	private Location _loc, _fishLoc;
	private int obj_id, _race, sex, base_class, level, curCp, maxCp, _enchant;
	private long _exp;
	private int curHp, maxHp, curMp, maxMp, curLoad, maxLoad, rec_left, rec_have;
	private int _str, _con, _dex, _int, _wit, _men, _sp, ClanPrivs, InventoryLimit;
	private int _patk, _patkspd, _pdef, evasion, accuracy, crit, _matk, _matkspd;
	private int _mdef, pvp_flag, karma, hair_style, hair_color, face, gm_commands;
	private int clan_id, clan_crest_id, ally_id, ally_crest_id, large_clan_crest_id;
	private int private_store, can_crystalize, pk_kills, pvp_kills, class_id;
	private int team, AbnormalEffect, noble, hero, fishing, mount_id, cw_level;
	private int name_color, running, pledge_class, pledge_type, title_color;
	private byte mount_type;
	private String _name, title;
	private L2CubicInstance[] cubics;

	public UserInfo(final L2Player cha)
	{
		_cha = cha;
	}

	@Override
	final public void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(!activeChar.equals(_cha))
			return;

		if(_cha.isCursedWeaponEquipped())
			cw_level = CursedWeaponsManager.getInstance().getLevel(_cha.getCursedWeaponEquippedId());
		else
			cw_level = 0;

		_name = _cha.getName();
		clan_crest_id = _cha.getClanCrestId();
		ally_crest_id = _cha.getAllyCrestId();
		large_clan_crest_id = _cha.getClanCrestLargeId();

		if(_cha.isMounted())
		{
			_enchant = 0;
			mount_id = _cha.getMountNpcId() + 1000000;
			mount_type = (byte) _cha.getMountType();
		}
		else
		{
			_enchant = (byte) _cha.getEnchantEffect();
			mount_id = 0;
			mount_type = 0;
		}

		move_speed = _cha.getMovementSpeedMultiplier();
		_runSpd = (int) (_cha.getRunSpeed() / move_speed);
		_walkSpd = (int) (_cha.getWalkSpeed() / move_speed);
		_flRunSpd = _flyRunSpd = _runSpd;
		_flWalkSpd = _flyWalkSpd = _walkSpd;
		_swimRunSpd = _cha.getSwimSpeed();
		_swimWalkSpd = _cha.getSwimSpeed();
		_inv = _cha.getInventory();
		_relation = _cha.isClanLeader() ? 0x40 : 0;
		if(_cha.getSiegeState() == 1)
			_relation |= 0x180;
		else if(_cha.getSiegeState() == 2)
			_relation |= 0x80;

		_loc = _cha.getLoc();
		obj_id = _cha.getObjectId();
		_race = _cha.getRace().ordinal();
		sex = _cha.getSex();
		base_class = _cha.getBaseClassId();
		level = _cha.getLevel();
		_exp = _cha.getExp();
		_str = _cha.getSTR();
		_dex = _cha.getDEX();
		_con = _cha.getCON();
		_int = _cha.getINT();
		_wit = _cha.getWIT();
		_men = _cha.getMEN();
		curHp = (int) _cha.getCurrentHp();
		maxHp = _cha.getMaxHp();
		curMp = (int) _cha.getCurrentMp();
		maxMp = _cha.getMaxMp();
		curLoad = _cha.getCurrentLoad();
		maxLoad = _cha.getMaxLoad();
		_sp = _cha.getSp();
		_patk = _cha.getPAtk(null);
		_patkspd = _cha.getPAtkSpd();
		_pdef = _cha.getPDef(null);
		evasion = _cha.getEvasionRate(null);
		accuracy = _cha.getAccuracy();
		crit = _cha.getCriticalHit(null, null);
		_matk = _cha.getMAtk(null, null);
		_matkspd = _cha.getMAtkSpd();
		_mdef = _cha.getMDef(null, null);
		pvp_flag = _cha.getPvpFlag(); // 0=white, 1=purple, 2=purpleblink
		karma = _cha.getKarma();
		attack_speed = _cha.getAttackSpeedMultiplier();
		col_radius = _cha.getColRadius();
		col_height = _cha.getColHeight();
		hair_style = _cha.getHairStyle();
		hair_color = _cha.getHairColor();
		face = _cha.getFace();
		gm_commands = _cha.getPlayerAccess().IsGM || _cha.getPlayerAccess().CanUseGMCommand || Config.ALLOW_SPECIAL_COMMANDS ? 1 : 0;
		// builder level активирует в клиенте админские команды
		title = _cha.getTitle();
		if(_cha.isInvisible() && _cha.isGM())
			title = "[Invisible]";
		if(_cha.isPolymorphed())
			if(NpcTable.getTemplate(_cha.getPolyid()) != null)
				title += " - " + NpcTable.getTemplate(_cha.getPolyid()).name;
			else
				title += " - Polymorphed";
		clan_id = _cha.getClanId();
		ally_id = _cha.getAllyId();
		private_store = _cha.getPrivateStoreType();
		can_crystalize = _cha.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE) > 0 ? 1 : 0;
		pk_kills = _cha.getPkKills();
		pvp_kills = _cha.getPvpKills();
		cubics = _cha.getCubics().toArray(new L2CubicInstance[0]);
		AbnormalEffect = _cha.getAbnormalEffect();
		ClanPrivs = _cha.getClanPrivileges();
		rec_left = _cha.getRecomLeft(); // c2 recommendations remaining
		rec_have = _cha.getPlayerAccess().IsGM ? 0 : _cha.getRecomHave(); // c2 recommendations received
		InventoryLimit = _cha.getInventoryLimit();
		class_id = _cha.getClassId().getId();
		maxCp = _cha.getMaxCp();
		curCp = (int) _cha.getCurrentCp();

		if(_cha.getTeam() < 3)
			team = _cha.getTeam(); // team circle around feet 1= Blue, 2 = red
		else
			team = 1;

		noble = _cha.isNoble() || _cha.isGM() && Config.GM_HERO_AURA ? 1 : 0; // 0x01: symbol on char menu ctrl+I
		hero = _cha.isHero() || _cha.isGM() && Config.GM_HERO_AURA ? 1 : 0; // 0x01: Hero Aura and symbol
		fishing = _cha.isFishing() ? 1 : 0; // Fishing Mode
		_fishLoc = _cha.getFishLoc();
		name_color = _cha.getNameColor();
		running = _cha.isRunning() ? 0x01 : 0x00; // changes the Speed display on Status Window
		pledge_class = _cha.getPledgeClass();
		pledge_type = _cha.getPledgeType();
		title_color = _cha.getTitleColor();
		partyRoom = PartyRoomManager.getInstance().isLeader(_cha);

		_cha.refreshSavedStats();
		if(_cha.isMounted())
			getClient().sendPacket(new Ride(_cha));

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x04);

		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h < 7 ? 7 : _loc.h / 7 * 7); // мальенькая хитрость препятствующая использованию патча на других серверах
		writeD(obj_id);
		writeS(_name);
		writeD(_race);
		writeD(sex);
		writeD(base_class);
		writeD(level);
		writeQ(_exp);
		writeD(_str);
		writeD(_dex);
		writeD(_con);
		writeD(_int);
		writeD(_wit);
		writeD(_men);
		writeD(maxHp);
		writeD(curHp);
		writeD(maxMp);
		writeD(curMp);
		writeD(_sp);
		writeD(curLoad);
		writeD(maxLoad);
		writeD(0x28); // unknown. В снифе бывает 0х28 и 0х14

		for(final byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollObjectId(PAPERDOLL_ID));
		for(final byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollItemId(PAPERDOLL_ID));
		for(final byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollAugmentationId(PAPERDOLL_ID));

		writeD(_patk);
		writeD(_patkspd);
		writeD(_pdef);
		writeD(evasion);
		writeD(accuracy);
		writeD(crit);
		writeD(_matk);
		writeD(_matkspd);
		writeD(_patkspd);
		writeD(_mdef);
		writeD(pvp_flag);
		writeD(karma);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd); // swimspeed
		writeD(_swimWalkSpd); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(move_speed);
		writeF(attack_speed);
		writeF(col_radius);
		writeF(col_height);
		writeD(hair_style);
		writeD(hair_color);
		writeD(face);
		writeD(gm_commands);
		writeS(title);
		writeD(clan_id);
		writeD(clan_crest_id);
		writeD(ally_id);
		writeD(ally_crest_id);
		// 0x40 leader rights
		// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		writeD(_relation);
		writeC(mount_type); // mount type
		writeC(private_store);
		writeC(can_crystalize);
		writeD(pk_kills);
		writeD(pvp_kills);
		writeH(cubics.length);
		for(final L2CubicInstance cubic : cubics)
			writeH(cubic == null ? 0 : cubic.getId());
		writeC(partyRoom ? 0x01 : 0x00); // 1-find party members
		writeD(AbnormalEffect);
		writeC(0x11);
		writeD(ClanPrivs);
		writeH(rec_left);
		writeH(rec_have);
		writeD(mount_id);
		writeH(InventoryLimit);
		writeD(class_id);
		writeD(0x00); // special effects? circles around player...
		writeD(maxCp);
		writeD(curCp);
		writeC(_enchant);
		writeC(team);
		writeD(large_clan_crest_id);
		writeC(noble);
		writeC(hero);
		writeC(fishing);
		writeD(_fishLoc.x);
		writeD(_fishLoc.y);
		writeD(_fishLoc.z);
		writeD(name_color);
		writeC(running);
		writeD(pledge_class);
		writeD(pledge_type);
		writeD(title_color);
		writeD(cw_level);
	}

	public static final byte[] PAPERDOLL_ORDER = {
			Inventory.PAPERDOLL_UNDER,
			Inventory.PAPERDOLL_REAR,
			Inventory.PAPERDOLL_LEAR,
			Inventory.PAPERDOLL_NECK,
			Inventory.PAPERDOLL_RFINGER,
			Inventory.PAPERDOLL_LFINGER,
			Inventory.PAPERDOLL_HEAD,
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_LHAND,
			Inventory.PAPERDOLL_GLOVES,
			Inventory.PAPERDOLL_CHEST,
			Inventory.PAPERDOLL_LEGS,
			Inventory.PAPERDOLL_FEET,
			Inventory.PAPERDOLL_HAIR, // PAPERDOLL_BACK
			Inventory.PAPERDOLL_LRHAND,
			Inventory.PAPERDOLL_HAIR,
			Inventory.PAPERDOLL_DHAIR };
}