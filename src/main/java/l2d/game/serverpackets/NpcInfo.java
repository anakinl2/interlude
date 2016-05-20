package l2d.game.serverpackets;

import com.lineage.Config;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Summon;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Location;

public class NpcInfo extends L2GameServerPacket
{
	// ddddddddddddddddddffffdddcccccSSddd dddddccffddddccd
	private boolean can_writeImpl = false;
	private L2Character _cha;
	private L2Summon _summon;
	private int _npcObjId, _npcId, running, incombat, dead, team;
	private int _runSpd, _walkSpd, _mAtkSpd, _pAtkSpd, _rhand, _lhand;
	private int karma, pvp_flag, _abnormalEffect, clan_crest_id, ally_crest_id;
	private double colHeight, colRadius, currentColHeight, currentColRadius;
	// private int _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private boolean _isAttackable;
	private Location _loc;
	private String _name = "";
	private String _title = "";
	private boolean _isShowSpawnAnimation = false;

	private L2Character _attacker;

	public NpcInfo(final L2NpcInstance cha, final L2Character attacker)
	{
		if(cha == null)
			return;

		_cha = cha;
		_attacker = attacker;
		_npcId = cha.getTemplate().displayId != 0 ? cha.getTemplate().displayId : cha.getTemplate().npcId;
		_isAttackable = cha.isAutoAttackable(attacker);
		_rhand = cha.getRightHandItem();
		_lhand = cha.getLeftHandItem();
		if(Config.SERVER_SIDE_NPC_NAME || cha.getTemplate().displayId != 0)
			_name = cha.getName();
		if(Config.SERVER_SIDE_NPC_TITLE || cha.getTemplate().displayId != 0)
			_title = _title + cha.getTitle();
		_isShowSpawnAnimation = cha.isShowSpawnAnimation();
		can_writeImpl = true;
	}

	public NpcInfo(final L2Summon cha, final L2Character attacker)
	{
		if(cha == null)
			return;
		if(cha.getPlayer() != null && cha.getPlayer().isInvisible())
			return;

		_cha = cha;
		_attacker = attacker;
		_summon = cha;
		_npcId = cha.getTemplate().npcId;
		_isAttackable = cha.isAutoAttackable(attacker); // (cha.getKarma() > 0);
		_rhand = 0;
		_lhand = 0;
		if(Config.SERVER_SIDE_NPC_NAME || cha.isPet())
			_name = _cha.getName();
		_title = cha.getTitle();
		can_writeImpl = true;
	}

	public NpcInfo(final L2Summon cha, final L2Character attacker, final boolean isShowSpawnAnimation)
	{
		this(cha, attacker);
		_isShowSpawnAnimation = isShowSpawnAnimation;
	}

	@Override
	final public void runImpl()
	{
		if(!can_writeImpl)
			return;

		currentColHeight = colHeight = _cha.getColHeight();
		currentColRadius = colRadius = _cha.getColRadius();
		if(_cha.getEffectList().getEffectByType(EffectType.Grow) != null)
		{
			currentColHeight = (int) (currentColHeight / 1.2);
			currentColRadius = (int) (currentColRadius / 1.2);
		}
		_npcObjId = _cha.getObjectId();
		_loc = _cha.getLoc();
		_mAtkSpd = _cha.getMAtkSpd();
		clan_crest_id = _cha.getClanCrestId();
		ally_crest_id = _cha.getAllyCrestId();

		_runSpd = _cha.getRunSpeed();
		_walkSpd = _cha.getWalkSpeed();
		karma = _cha.getKarma();
		pvp_flag = _cha.getPvpFlag();
		_pAtkSpd = _cha.getPAtkSpd();
		running = _cha.isRunning() ? 1 : 0;
		incombat = _cha.isInCombat() ? 1 : 0;
		dead = _cha.isAlikeDead() ? 1 : 0;
		_abnormalEffect = _cha.getAbnormalEffect();

		if(_cha instanceof L2Summon)
		{
			if(_cha.getTeam() < 3)
				team = _cha.getTeam();
			else if(_attacker == null || _attacker.getTeam() == 0)
				team = 0;
			else if(_attacker.getTeam() == _cha.getTeam())
				team = 1;
			else
				team = 2;
		}
		else
			team = _cha.getTeam();
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x16);
		// ddddddddddddddddddffffdddcccccSSddddddddccffddddccd
		writeD(_npcObjId);
		writeD(_npcId + 1000000); // npctype id c4
		writeD(_isAttackable ? 1 : 0);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
		writeD(0x00);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd /* _swimRunSpd *//* 0x32 */); // swimspeed
		writeD(_walkSpd/* _swimWalkSpd *//* 0x32 */); // swimspeed
		writeD(_runSpd/* _flRunSpd */);
		writeD(_walkSpd/* _flWalkSpd */);
		writeD(_runSpd/* _flyRunSpd */);
		writeD(_walkSpd/* _flyWalkSpd */);
		writeF(1.1); // Interlude
		writeF(_pAtkSpd / 277.478340719);
		writeF(colRadius);
		writeF(colHeight);
		writeD(_rhand); // right hand weapon
		writeD(0);
		writeD(_lhand); // left hand weapon
		writeC(1); // name above char 1=true ... ??
		writeC(running);
		writeC(incombat);
		writeC(dead);
		writeC(_isShowSpawnAnimation ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
		writeS(_name);
		writeS(_title);
		writeD(0); // как-то связано с тайтлом, если не 0 скрывать?
		writeD(pvp_flag);
		writeD(karma); // hmm karma ??
		writeD(_abnormalEffect); // C2
		writeD(0); // clan id (клиентом не используется, но требуется для показа значка)
		writeD(clan_crest_id); // clan crest id
		writeD(0); // ally id (клиентом не используется, но требуется для показа значка)
		writeD(ally_crest_id); // ally crest id
		writeC(0x00); // C2
		writeC(team); // team aura 1-blue, 2-red
		writeF(currentColRadius); // тут что-то связанное с colRadius
		writeF(currentColHeight); // тут что-то связанное с colHeight
		writeD(0x00); // C4
		writeD(0x00); // как-то связано с высотой
	}

	@Override
	public String getType()
	{
		return super.getType() + (_cha != null ? " about " + _cha : "");
	}
}