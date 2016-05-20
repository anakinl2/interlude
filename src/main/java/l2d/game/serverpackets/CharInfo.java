package l2d.game.serverpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.instancemanager.PartyRoomManager;
import l2d.game.model.Inventory;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2CubicInstance;
import com.lineage.util.Location;

public class CharInfo extends L2GameServerPacket
{
	private static final Logger _log = Logger.getLogger(CharInfo.class.getName());
	private L2Player _cha;
	private Inventory _inv;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private float _moveMultiplier;
	private Location _loc, _fishLoc;
	private String _name, _title;
	private int _objId, _race, _sex, base_class, pvp_flag, karma, rec_have, rec_left;
	private float speed_move, speed_atack, col_radius, col_height;
	private int hair_style, hair_color, face, abnormal_effect;
	private int clan_id, clan_crest_id, large_clan_crest_id, ally_id, ally_crest_id, class_id;
	private byte _sit, _run, _combat, _dead, _invis, private_store, _enchant;
	private byte _team, _noble, _hero, _fishing, mount_type;
	private int plg_class, pledge_type, clan_rep_score, cw_level, mount_id;
	private int _nameColor, title_color;
	private L2CubicInstance[] cubics;
	private boolean can_writeImpl = false;
	private L2Character _attacker;
	private boolean partyRoom = false;
	private boolean _mount = false;

	protected boolean logHandled()
	{
		return true;
	}

	public CharInfo(final L2Player cha, final L2Player attacker, final boolean mount)
	{
		if(cha == null)
			return;
		_cha = cha;
		_attacker = attacker;
		_mount = mount;
	}

	@Override
	final public void runImpl()
	{
		if(_cha == null || _cha.isInvisible() || _cha.isDeleting())
			return;

		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.equals(_cha))
		{
			_log.severe("You cant send CharInfo about his character to active user!!!");
			Thread.dumpStack();
			return;
		}

		if(_cha.isPolymorphed())
		{
			activeChar.sendPacket(new NpcInfoPoly(_cha, activeChar));
			return;
		}

		if(_cha.isCursedWeaponEquipped())
			cw_level = CursedWeaponsManager.getInstance().getLevel(_cha.getCursedWeaponEquippedId());
		else
			cw_level = 0;

		_name = _cha.getVisName();
		_title = _cha.getTitle();
		clan_id = _cha.getClanId();
		clan_crest_id = _cha.getClanCrestId();
		ally_id = _cha.getAllyId();
		ally_crest_id = _cha.getAllyCrestId();
		large_clan_crest_id = _cha.getClanCrestLargeId();

		if(_mount && _cha.isMounted())
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

		_inv = _cha.getInventory();
		_mAtkSpd = _cha.getMAtkSpd();
		_pAtkSpd = _cha.getPAtkSpd();
		_moveMultiplier = _cha.getMovementSpeedMultiplier();
		_runSpd = (int) (_cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_cha.getWalkSpeed() / _moveMultiplier);
		_flRunSpd = _runSpd;
		_flWalkSpd = _walkSpd;
		_swimSpd = _cha.getSwimSpeed();
		_loc = _cha.getLoc();
		_objId = _cha.getObjectId();
		_race = _cha.getBaseTemplate().race.ordinal();
		_sex = _cha.getSex();
		base_class = _cha.getBaseClassId();
		pvp_flag = _cha.getPvpFlag();
		karma = _cha.getKarma();
		speed_move = _cha.getMovementSpeedMultiplier();
		speed_atack = _cha.getAttackSpeedMultiplier();
		col_radius = _cha.getColRadius();
		col_height = _cha.getColHeight();
		hair_style = _cha.getHairStyle();
		hair_color = _cha.getHairColor();
		face = _cha.getFace();
		if(clan_id > 0 && _cha.getClan() != null)
			clan_rep_score = _cha.getClan().getReputationScore();
		else
			clan_rep_score = 0;
		_sit = _cha.isSitting() ? (byte) 0 : (byte) 1; // standing = 1 sitting = 0
		_run = _cha.isRunning() ? (byte) 1 : (byte) 0; // running = 1 walking = 0
		_combat = _cha.isInCombat() ? (byte) 1 : (byte) 0;
		_dead = _cha.isAlikeDead() ? (byte) 1 : (byte) 0;
		_invis = _cha.isInvisible() ? (byte) 1 : (byte) 0; // invisible = 1 visible = 0
		private_store = (byte) _cha.getPrivateStoreType(); // 1 - sellshop
		cubics = _cha.getCubics().toArray(new L2CubicInstance[0]);
		abnormal_effect = _cha.getAbnormalEffect();
		rec_left = _cha.getRecomLeft();
		rec_have = _cha.getPlayerAccess().IsGM ? 0 : _cha.getRecomHave();
		class_id = _cha.getClassId().getId();

		if(_cha.getTeam() < 3)
			_team = (byte) _cha.getTeam(); // team circle around feet 1 = Blue, 2 = red
		else if(_attacker == null || _attacker.getTeam() == 0)
			_team = 0;
		else if(_attacker.getTeam() == _cha.getTeam())
			_team = 1;
		else
			_team = 2;

		_noble = _cha.isNoble() ? (byte) 1 : (byte) 0; // 0x01: symbol on char menu ctrl+I
		_hero = _cha.isHero() || _cha.isGM() && Config.GM_HERO_AURA ? (byte) 1 : (byte) 0; // 0x01: Hero Aura
		_fishing = _cha.isFishing() ? (byte) 1 : (byte) 0;
		_fishLoc = _cha.getFishLoc();
		_nameColor = _cha.getNameColor(); // New C5
		plg_class = _cha.getPledgeClass();
		pledge_type = _cha.getPledgeType();
		title_color = _cha.getTitleColor();
		partyRoom = PartyRoomManager.getInstance().isLeader(_cha);

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x03);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h); // ?
		writeD(_objId);
		writeS(_name);
		writeD(_race);
		writeD(_sex);
		writeD(base_class);

		for(final byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollItemId(PAPERDOLL_ID));

		for(final byte PAPERDOLL_ID : PAPERDOLL_ORDER)
			writeD(_inv.getPaperdollAugmentationId(PAPERDOLL_ID));

		writeD(pvp_flag);
		writeD(karma);

		writeD(_mAtkSpd);
		writeD(_pAtkSpd);

		writeD(pvp_flag);
		writeD(karma);

		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimSpd/* 0x32 */); // swimspeed
		writeD(_swimSpd/* 0x32 */); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(speed_move); // _cha.getProperMultiplier()
		writeF(speed_atack); // _cha.getAttackSpeedMultiplier()
		writeF(col_radius);
		writeF(col_height);
		writeD(hair_style);
		writeD(hair_color);
		writeD(face);
		writeS(_title);
		writeD(clan_id);
		writeD(clan_crest_id);
		writeD(ally_id);
		writeD(ally_crest_id);

		writeD(0);

		writeC(_sit);
		writeC(_run);
		writeC(_combat);
		writeC(_dead);
		writeC(_invis);
		writeC(mount_type); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		writeC(private_store);
		writeH(cubics.length);
		for(final L2CubicInstance cubic : cubics)
			writeH(cubic == null ? 0 : cubic.getId());
		writeC(partyRoom ? 0x01 : 0x00); // find party members
		writeD(abnormal_effect);
		writeC(rec_left);
		writeH(rec_have);
		writeD(mount_id);
		writeD(class_id);
		writeD(0); // ?
		writeC(_enchant);

		writeC(_team);
		writeD(large_clan_crest_id);
		writeC(_noble);
		writeC(_hero);

		writeC(_fishing);
		writeD(_fishLoc.x);
		writeD(_fishLoc.y);
		writeD(_fishLoc.z);

		writeD(_nameColor);
		writeD(_loc.h);
		writeD(plg_class);
		writeD(pledge_type);
		writeD(title_color);
		writeD(cw_level);
		//writeD(clan_rep_score);
		//writeD(_transform);
		//writeD(_agathion);
	}

	public static final byte[] PAPERDOLL_ORDER = {
			Inventory.PAPERDOLL_UNDER,
			Inventory.PAPERDOLL_HEAD,
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_LHAND,
			Inventory.PAPERDOLL_GLOVES,
			Inventory.PAPERDOLL_CHEST,
			Inventory.PAPERDOLL_LEGS,
			Inventory.PAPERDOLL_FEET,
			Inventory.PAPERDOLL_HAIR,
			Inventory.PAPERDOLL_LRHAND,
			Inventory.PAPERDOLL_HAIR,
			Inventory.PAPERDOLL_DHAIR };
}