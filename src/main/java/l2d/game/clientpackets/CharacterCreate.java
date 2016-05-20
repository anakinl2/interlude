package l2d.game.clientpackets;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2d.Config;
import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Player;
import l2d.game.model.L2ShortCut;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.base.ClassId;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.quest.Quest;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.CharacterCreateFail;
import l2d.game.serverpackets.CharacterCreateSuccess;
import l2d.game.serverpackets.CharacterSelectionInfo;
import l2d.game.tables.CharNameTable;
import l2d.game.tables.CharTemplateTable;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import l2d.game.templates.L2CharTemplate;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2PlayerTemplate;
import l2d.util.Util;

/**
 * @author Felixx
 */
public class CharacterCreate extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(CharacterCreate.class.getName());

	private String _name;
	@SuppressWarnings("unused")
	private int _race;
	private int _sex;
	private int _classId;
	private int _hairStyle;
	private int _hairColor;
	private int _face;

	@Override
	public void readImpl()
	{
		_name = readS(); // Имя
		_race = readD(); // Расса
		_sex = readD(); // Пол
		_classId = readD(); // ID Класса
		readD(); // int
		readD(); // str
		readD(); // con
		readD(); // men
		readD(); // dex
		readD(); // wit
		_hairStyle = readD(); // Стиль Волос
		_hairColor = readD(); // Цвет Волос
		_face = readD(); // Лицо
	}

	@Override
	public void runImpl()
	{
		// Подтверждено: May 30, 2009 - Gracia Final - Игроки могут создать персов с названиями, состоящими всего из 1,2,3 комбинаций букв/чисел.
		if(_name.length() < 1 || _name.length() > 16)
		{
			if(Config.DEBUG)
				_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Your title cannot exceed 16 characters in length. Please try again.");

			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_16_ENG_CHARS));
			return;
		}

		// Подтверждено: May 30, 2009 - Gracia Final

		if(!Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			if(Config.DEBUG)
				_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Incorrect name. Please try again.");

			sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_INCORRECT_NAME));
			return;
		}

		L2Player newChar = null;
		L2CharTemplate template = null;

		synchronized (CharNameTable.getInstance())
		{
			if(CharNameTable.getInstance().accountCharNumber(getClient().getLoginName()) >= 8)
			{
				sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if(CharNameTable.getInstance().doesCharNameExist(_name))
			{
				sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_NAME_ALREADY_EXISTS));
				if(Config.DEBUG)
					_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: This name already exists.");
				return;
			}

			for(ClassId cid : ClassId.values())
				if(cid.getId() == _classId && cid.getLevel() != 1)
					return;

			newChar = L2Player.create(_classId, (byte) _sex, getClient().getLoginName(), _name, (byte) _hairStyle, (byte) _hairColor, (byte) _face);
		}

		boolean my_sex = _sex == 0 ? false : true;
		template = CharTemplateTable.getInstance().getTemplate(_classId, my_sex);

		newChar.setCurrentHp(template.baseHpMax, true);
		newChar.setCurrentCp(template.baseCpMax);
		newChar.setCurrentMp(template.baseMpMax);

		if(newChar == null)
			return;

		newChar.setConnected(false);

		sendPacket(new CharacterCreateSuccess());

		initNewChar(getClient(), newChar);
	}

	private boolean isValidName(String text)
	{
		boolean result = true;
		String test = text;
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch(PatternSyntaxException e) // В случае незаконного образца
		{
			_log.warning("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		Matcher regexp = pattern.matcher(test);
		if(!regexp.matches())
			result = false;
		return result;
	}

	private void initNewChar(L2GameClient client, L2Player newChar)
	{
		L2PlayerTemplate template = newChar.getTemplate();

		L2Player.restoreCharSubClasses(newChar);

		if(Config.STARTING_ADENA > 0)
			newChar.addAdena(Config.STARTING_ADENA);

		if(Config.ENABLE_STARTING_ITEM)
		{
			int[] itemIds = Config.STARTING_ITEM_ID_LIST;
			int[] counts = Config.STARTING_ITEM_COUNT_LIST;
			for(int i = 0; i < itemIds.length; i++)
				if(itemIds[i] != 0)
					if(counts[i] != 0)
						newChar.getInventory().addItem(itemIds[i], counts[i], 0, "Char Create");
					else
						_log.config("STARTING ITEMS: Count for ItemId = " + itemIds[i] + "In Pozition " + i);
		}

		newChar.setXYZInvisible(template.spawnX, template.spawnY, template.spawnZ);

		newChar.setTitle("");

		ItemTable itemTable = ItemTable.getInstance();
		for(L2Item i : template.getItems())
		{
			L2ItemInstance item = itemTable.createItem(i.getItemId());
			newChar.getInventory().addItem(item);

			if(item.getItemId() == 5588) // tutorial book
				newChar.registerShortCut(new L2ShortCut(11, 0, L2ShortCut.TYPE_ITEM, item.getObjectId(), 0));

			if(item.isEquipable() && (newChar.getActiveWeaponItem() == null || item.getItem().getType2() != L2Item.TYPE2_WEAPON))
				newChar.getInventory().equipItem(item);
		}

		for(L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId()))
			newChar.addSkill(SkillTable.getInstance().getInfo(skill.id, skill.skillLevel), true);

		if(newChar.getSkillLevel(1001) > 0) // Soul Cry
			newChar.registerShortCut(new L2ShortCut(1, 0, L2ShortCut.TYPE_SKILL, 1001, 1));
		if(newChar.getSkillLevel(1177) > 0) // Wind Strike
			newChar.registerShortCut(new L2ShortCut(1, 0, L2ShortCut.TYPE_SKILL, 1177, 1));
		if(newChar.getSkillLevel(1216) > 0) // Self Heal
			newChar.registerShortCut(new L2ShortCut(2, 0, L2ShortCut.TYPE_SKILL, 1216, 1));

		// add attack, take, sit shortcut
		newChar.registerShortCut(new L2ShortCut(0, 0, L2ShortCut.TYPE_ACTION, 2, 0));
		newChar.registerShortCut(new L2ShortCut(3, 0, L2ShortCut.TYPE_ACTION, 5, 0));
		newChar.registerShortCut(new L2ShortCut(10, 0, L2ShortCut.TYPE_ACTION, 0, 0));

		startTutorialQuest(newChar);

		L2GameClient.saveCharToDisk(newChar);
		newChar.deleteMe();

		CharacterSelectionInfo CSInfo = new CharacterSelectionInfo(client.getLoginName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(CSInfo);
		client.setCharSelection(CSInfo.getCharInfo());

	}

	public static void startTutorialQuest(L2Player player)
	{
		Quest q = QuestManager.getQuest(255);
		if(q != null)
			q.newQuestState(player);
	}
}