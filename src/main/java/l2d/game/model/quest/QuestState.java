package l2d.game.model.quest;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.ExShowQuestMark;
import l2d.game.serverpackets.PlaySound;
import l2d.game.serverpackets.QuestList;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.TutorialEnableClientEvent;
import l2d.game.serverpackets.TutorialShowHtml;
import l2d.game.serverpackets.TutorialShowQuestionMark;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2Item;
import l2d.util.Files;
import l2d.util.Log;
import l2d.util.Rnd;

public final class QuestState
{
	protected static Logger _log = Logger.getLogger(Quest.class.getName());

	/** Player who engaged the quest */
	private L2Player _player;

	/** Quest associated to the QuestState */
	private Quest _quest;

	/** State of the quest */
	private State _state;

	/** List of couples (variable for quest,value of the variable for quest) */
	private ConcurrentHashMap<String, String> _vars;

	/**
	 * Constructor of the QuestState : save the quest in the list of quests of the player.<BR/><BR/>
	 * <U><I>Actions :</U></I><BR/>
	 * <LI>Save informations in the object QuestState created (Quest, Player, Completion, State)</LI>
	 * <LI>Add the QuestState in the player's list of quests by using setQuestState()</LI>
	 * <LI>Add drops gotten by the quest</LI>
	 * <BR/>
	 * 
	 * @param quest
	 *            : quest associated with the QuestState
	 * @param player
	 *            : L2Player pointing out the player
	 * @param state
	 *            : state of the quest
	 */
	public QuestState(final Quest quest, final L2Player player, final State state)
	{
		_quest = quest;
		_player = player;

		// Save the state of the quest for the player in the player's list of quest onwed
		_player.setQuestState(this);

		// set the state of the quest
		_state = state;
	}

	/**
	 * Add XP and SP as quest reward
	 * <br><br>
	 * Метод учитывает рейты!
	 */
	public void addExpAndSp(final long exp, final long sp)
	{
		addExpAndSp(exp, sp, false);
	}

	/**
	 * Add XP and SP as quest reward
	 * <br><br>
	 * Метод учитывает рейты!
	 * 3-ий параметр true/false показывает является ли квест на профессию
	 * и рейты учитываются в завимисомти от параметра RateQuestsRewardOccupationChange
	 */
	public void addExpAndSp(final long exp, final long sp, final boolean rate)
	{
		if(!rate || rate && Config.RATE_QUESTS_OCCUPATION_CHANGE)
			_player.addExpAndSp((long) (exp * getRateQuestsExp()), (long) (sp * getRateQuestsExp()), true, false);
		else
			_player.addExpAndSp(exp, sp, true, false);
	}

	/**
	 * Add player to get notification of characters death
	 * 
	 * @param character
	 *            : L2Character of the character to get notification of death
	 */
	public void addNotifyOfDeath(final L2Character character)
	{
		if(character == null)
			return;
		character.addNotifyQuestOfDeath(this);
	}

	public void addRadar(final int x, final int y, final int z)
	{
		_player.radar.addMarker(x, y, z);
	}

	public void clearRadar()
	{
		_player.radar.removeAllMarkers();
	}

	/**
	 * Destroy element used by quest when quest is exited
	 * 
	 * @param repeatable
	 * @return QuestState
	 */
	public QuestState exitCurrentQuest(final boolean repeatable)
	{
		// Clean drops
		if(_quest.getItems() != null)
			// Go through values of class variable "drops" pointing out mobs that drop for quest
			for(final Integer itemId : _quest.getItems())
			{
				// Get [item from] / [presence of the item in] the inventory of the player
				final L2ItemInstance item = _player.getInventory().getItemByItemId(itemId);
				if(item == null || itemId == 57)
					continue;
				final int count = item.getIntegerLimitedCount();
				// If player has the item in inventory, destroy it (if not gold)
				_player.getInventory().destroyItemByItemId(itemId, count, true);
				_player.getWarehouse().destroyItem(itemId, count);
			}

		// If quest is repeatable, delete quest from list of quest of the player and from database (quest CAN be created again => repeatable)
		if(repeatable)
		{
			_player.delQuestState(_quest.getName());
			Quest.deleteQuestInDb(this);
			_vars = null;
		}
		else
		{ // Otherwise, delete variables for quest and update database (quest CANNOT be created again => not repeatable)
			if(_vars != null && !_vars.isEmpty())
				for(final String var : _vars.keySet())
					if(var != null)
						unset(var);
			setState(_quest.COMPLETED);
			Quest.updateQuestInDb(this);
		}
		_player.sendPacket(new QuestList(_player));
		return this;
	}

	public void abortQuest()
	{
		_quest.onAbort(this);
		exitCurrentQuest(true);
	}

	/**
	 * Return the value of the variable of quest represented by "var"
	 * 
	 * @param var
	 *            : name of the variable of quest
	 * @return Object
	 */
	public String get(final String var)
	{
		if(_vars == null)
			return null;
		return _vars.get(var);
	}

	public ConcurrentHashMap<String, String> getVars()
	{
		final ConcurrentHashMap<String, String> result = new ConcurrentHashMap<String, String>();
		if(_vars != null)
			result.putAll(_vars);
		return result;
	}

	/**
	 * Return the value of the variable of quest represented by "var"
	 * 
	 * @param var
	 *            : String designating the variable for the quest
	 * @return int
	 */
	public int getInt(final String var)
	{
		int varint = 0;
		try
		{
			varint = Integer.parseInt(_vars.get(var));
		}
		catch(final Exception e)
		{
			_log.finer(getPlayer().getName() + ": variable " + var + " isn't an integer: " + varint + e);
		}
		return varint;
	}

	/**
	 * Return item number which is equipped in selected slot
	 * 
	 * @return int
	 */
	public int getItemEquipped(final int loc)
	{
		return getPlayer().getInventory().getPaperdollItemId(loc);
	}

	/**
	 * Return the L2Player
	 * 
	 * @return L2Player
	 */
	public L2Player getPlayer()
	{
		return _player;
	}

	/**
	 * Return the quest
	 * 
	 * @return Quest
	 */
	public Quest getQuest()
	{
		return _quest;
	}

	/**
	 * Return the quantity of one sort of item hold by the player
	 * 
	 * @param itemId
	 *            : ID of the item wanted to be count
	 * @return int
	 */
	public int getQuestItemsCount(final int itemId)
	{
		return _player.getInventory().getCountOf(itemId);
	}

	public int getQuestItemsCount(final int[] itemsIds)
	{
		int result = 0;
		for(final int id : itemsIds)
			result += getQuestItemsCount(id);
		return result;
	}

	public long getQuestItemsCount(final short[] itemsIds)
	{
		long result = 0;
		for(final int id : itemsIds)
			result += getQuestItemsCount(id);
		return result;
	}

	/**
	 * Return the QuestTimer object with the specified name
	 * 
	 * @return QuestTimer<BR> Return null if name does not exist
	 */
	public final QuestTimer getQuestTimer(final String name)
	{
		return getQuest().getQuestTimer(name, null, getPlayer());

	}

	/**
	 * Return the state of the quest
	 * 
	 * @return State
	 */
	public State getState()
	{
		return _state;
	}

	/**
	 * Return ID of the state of the quest
	 * 
	 * @return String
	 */
	public String getStateId()
	{
		return _state.getName();
	}

	/**
	 * Добавить предмет игроку
	 * By default if item is adena rates 'll be applyed, else no
	 * 
	 * @param itemId
	 * @param count
	 */
	public void giveItems(final int itemId, final int count)
	{
		if(itemId == 57)
			giveItems(itemId, count, 0, true);
		else
			giveItems(itemId, count, 0, false);
	}

	/**
	 * Добавить предмет игроку
	 * 
	 * @param itemId
	 * @param count
	 * @param rate
	 *            - учет квестовых рейтов
	 */
	public void giveItems(final int itemId, final int count, final boolean rate)
	{
		giveItems(itemId, count, 0, rate);
	}

	/**
	 * Добавить предмет игроку
	 * 
	 * @param itemId
	 * @param count
	 * @param enchantlevel
	 * @param rate
	 *            - учет квестовых рейтов
	 */
	public void giveItems(final int itemId, int count, final int enchantlevel, final boolean rate)
	{
		if(count <= 0)
			count = 1;

		if(rate)
			count = (int) (count * getRateQuestsReward());

		// Get template of item
		final L2Item template = ItemTable.getInstance().getTemplate(itemId);
		if(template == null)
			return;

		if(template.isStackable())
		{
			final L2ItemInstance item = ItemTable.getInstance().createItem(itemId);

			// Set quantity of item
			item.setCount(count);

			// Add items to player's inventory
			_player.getInventory().addItem(item);

			if(enchantlevel > 0)
				item.setEnchantLevel(enchantlevel);

			Log.LogItem(_player, Log.GetQuestItem, item);
		}
		else
			for(int i = 0; i < count; i++)
			{
				final L2ItemInstance item = ItemTable.getInstance().createItem(itemId);

				// Set quantity of item
				item.setCount(1);

				// Add items to player's inventory
				_player.getInventory().addItem(item);

				if(enchantlevel > 0)
					item.setEnchantLevel(enchantlevel);

				Log.LogItem(_player, Log.GetQuestItem, item);
			}

		// If item for reward is gold, send message of gold reward to client
		if(template.getItemId() == 57)
			_player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_ADENA).addNumber(count));
		else if(count > 1)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
			smsg.addItemName(template.getItemId());
			smsg.addNumber(count);
			_player.sendPacket(smsg);
		}
		else
		{
			final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1);
			smsg.addItemName(template.getItemId());
			_player.sendPacket(smsg);
		}

		final StatusUpdate su = new StatusUpdate(_player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, _player.getCurrentLoad());
		_player.sendPacket(su);
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param count
	 *            количество при рейтах 1х
	 * @param calcChance
	 *            шанс при рейтах 1х, в процентах
	 * @return количество вещей для дропа, может быть 0
	 */
	public int rollDrop(final int count, final double calcChance, final boolean prof)
	{
		if(calcChance <= 0 || count <= 0)
			return 0;
		return rollDrop(count, count, calcChance, prof);
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param min
	 *            минимальное количество при рейтах 1х
	 * @param max
	 *            максимальное количество при рейтах 1х
	 * @param calcChance
	 *            шанс при рейтах 1х, в процентах
	 * @param prof
	 *            - учитывать дроп по параметру "рейт дропа для квестов на профу"
	 * @return количество вещей для дропа, может быть 0
	 */
	public int rollDrop(final int min, final int max, double calcChance, final boolean prof)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0)
			return 0;
		int dropmult = 1;
		calcChance *= getRateQuestsDrop(prof);
		if(calcChance > 100)
		{
			if((int) Math.ceil(calcChance / 100) <= calcChance / 100)
				calcChance = Math.nextUp(calcChance);
			dropmult = (int) Math.ceil(calcChance / 100);
			calcChance = calcChance / dropmult;
		}
		return Rnd.chance(calcChance) ? Rnd.get(min * dropmult, max * dropmult) : 0;
	}

	public float getRateQuestsDrop(final boolean prof)
	{
		if(prof)
			return Config.RATE_QUESTS_DROP_PROF + _player.getBonus().RATE_QUESTS_DROP;
		return Config.RATE_QUESTS_DROP + _player.getBonus().RATE_QUESTS_DROP;
	}

	public float getRateQuestsReward()
	{
		return Config.RATE_QUESTS_REWARD + _player.getBonus().RATE_QUESTS_REWARD;
	}

	public float getRateQuestsExp()
	{
		return Config.RATE_QUESTS_EXP + _player.getBonus().RATE_QUESTS_REWARD;
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * проверяет максимум, а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param min
	 *            минимальное количество при рейтах 1х
	 * @param max
	 *            максимальное количество при рейтах 1х
	 * @param limit
	 *            максимум таких вещей
	 * @param calcChance
	 * @return true если после выполнения количество достигло лимита
	 */
	public boolean rollAndGive(final int itemId, final int min, final int max, final int limit, final double calcChance)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0 || limit <= 0 || itemId <= 0)
			return false;
		return rollAndGive(itemId, min, max, limit, calcChance, false);
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * проверяет максимум, а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param min
	 *            минимальное количество при рейтах 1х
	 * @param max
	 *            максимальное количество при рейтах 1х
	 * @param limit
	 *            максимум таких вещей
	 * @param calcChance
	 * @param QuestProf
	 * @return true если после выполнения количество достигло лимита
	 */
	public boolean rollAndGive(final int itemId, final int min, final int max, final int limit, final double calcChance, final boolean prof)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0 || limit <= 0 || itemId <= 0)
			return false;
		int count = rollDrop(min, max, calcChance, prof);
		if(count > 0)
		{
			final int alreadyCount = getQuestItemsCount(itemId);
			if(alreadyCount + count > limit)
				count = limit - alreadyCount;
			if(count > 0)
			{
				giveItems(itemId, count, false);
				if(count + alreadyCount < limit)
					playSound(Quest.SOUND_ITEMGET);
				else
				{
					playSound(Quest.SOUND_MIDDLE);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param min
	 *            минимальное количество при рейтах 1х
	 * @param max
	 *            максимальное количество при рейтах 1х
	 * @param calcChance
	 */
	public void rollAndGive(final int itemId, final int min, final int max, final double calcChance)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0 || itemId <= 0)
			return;
		rollAndGive(itemId, min, max, calcChance, false);
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param min
	 *            минимальное количество при рейтах 1х
	 * @param max
	 *            максимальное количество при рейтах 1х
	 * @param prof
	 *            - учитывать дроп по параметру "рейт дропа для квестов на профу"
	 * @param calcChance
	 */
	public void rollAndGive(final int itemId, final int min, final int max, final double calcChance, final boolean prof)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0 || itemId <= 0)
			return;
		final int count = rollDrop(min, max, calcChance, prof);
		if(count > 0)
		{
			giveItems(itemId, count, false);
			playSound(Quest.SOUND_ITEMGET);
		}
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param count
	 *            количество при рейтах 1х
	 * @param calcChance
	 */
	public boolean rollAndGive(final int itemId, final int count, final double calcChance)
	{
		if(calcChance <= 0 || count <= 0 || itemId <= 0)
			return false;
		return rollAndGive(itemId, count, calcChance, false);
	}

	/**
	 * Этот метод рассчитывает количество дропнутых вещей в зависимости от рейтов и дает их,
	 * а так же проигрывает звук получения вещи.
	 * <br><br>
	 * Следует учесть, что контроль за верхним пределом вещей в квестах, в которых
	 * нужно набить определенное количество предметов не осуществляется.
	 * <br><br>
	 * Ни один из передаваемых параметров не должен быть равен 0
	 * 
	 * @param itemId
	 *            id вещи
	 * @param count
	 *            количество при рейтах 1х
	 * @param calcChance
	 * @param prof
	 *            - учитывать дроп по параметру "рейт дропа для квестов на профу"
	 */
	public boolean rollAndGive(final int itemId, final int count, final double calcChance, final boolean prof)
	{
		if(calcChance <= 0 || count <= 0 || itemId <= 0)
			return false;
		final int countToDrop = rollDrop(count, calcChance, prof);
		if(countToDrop > 0)
		{
			giveItems(itemId, countToDrop, false);
			playSound(Quest.SOUND_ITEMGET);
			return true;
		}
		return false;
	}

	/**
	 * Return true if quest completed, false otherwise
	 * 
	 * @return boolean
	 */
	public boolean isCompleted()
	{
		return getStateId().equalsIgnoreCase("Completed");
	}

	/**
	 * Return true if quest started, false otherwise
	 * 
	 * @return boolean
	 */
	public boolean isStarted()
	{
		return !(getStateId().equals("Start") || getStateId().equals("Completed"));
	}

	public boolean isCreated()
	{
		return getStateId().equalsIgnoreCase("Start");
	}

	public void killNpcByObjectId(final int _objId)
	{
		final L2Object obj = L2World.findObject(_objId);
		if(obj instanceof L2NpcInstance)
			((L2NpcInstance) obj).doDie(null);
		else
			_log.warning("Attemp to kill object that is not npc in quest " + getQuest().getQuestIntId());
	}

	/**
	 * Return value of parameter "val" after adding the couple (var,val) in class variable "vars".<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Initialize class variable "vars" if is null</LI>
	 * <LI>Initialize parameter "val" if is null</LI>
	 * <LI>Add/Update couple (var,val) in class variable FastMap "vars"</LI>
	 * <LI>If the key represented by "var" exists in FastMap "vars", the couple (var,val) is updated in the database. The key is known as
	 * existing if the preceding value of the key (given as result of function put()) is not null.<BR>
	 * If the key doesn't exist, the couple is added/created in the database</LI>
	 * 
	 * @param var
	 *            : String indicating the name of the variable for quest
	 * @param val
	 *            : String indicating the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	public String set(final String var, String val)
	{
		if(_vars == null)
			_vars = new ConcurrentHashMap<String, String>();
		if(val == null)
			val = "";
		_vars.put(var, val);
		Quest.updateQuestVarInDb(this, var, val);
		if(var.equals("cond"))
		{
			_player.sendPacket(new QuestList(_player));
			if(!val.equals("0") && isStarted())
				_player.sendPacket(new ExShowQuestMark(getQuest().getQuestIntId()));
		}
		return val;
	}

	/**
	 * Add parameter used in quests.
	 * 
	 * @param var
	 *            : String pointing out the name of the variable for quest
	 * @param val
	 *            : String pointing out the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	String setInternal(final String var, String val)
	{
		if(_vars == null)
			_vars = new ConcurrentHashMap<String, String>();
		if(val == null)
			val = "";
		_vars.put(var, val);
		return val;
	}

	/**
	 * Return state of the quest after its initialization.<BR><BR>
	 * <U><I>Actions :</I></U>
	 * <LI>Remove drops from previous state</LI>
	 * <LI>Set new state of the quest</LI>
	 * <LI>Add drop for new state</LI>
	 * <LI>Update information in database</LI>
	 * <LI>Send packet QuestList to client</LI>
	 * 
	 * @param state
	 * @return object
	 */
	public Object setState(final State state)
	{
		if(state == null)
			return null;

		_state = state;

		if(isStarted())
			_player.sendPacket(new ExShowQuestMark(getQuest().getQuestIntId()));

		Quest.updateQuestInDb(this);
		_player.sendPacket(new QuestList(_player));
		return state;
	}

	public void removeRadar(final int x, final int y, final int z)
	{
		_player.radar.removeMarker(x, y, z);
	}

	/**
	 * Send a packet in order to play sound at client terminal
	 * 
	 * @param sound
	 */
	public void playSound(final String sound)
	{
		_player.sendPacket(new PlaySound(sound));
	}

	public void playTutorialVoice(final String voice)
	{
		_player.sendPacket(new PlaySound(2, voice, 0, 0, _player.getLoc()));
	}

	public void onTutorialClientEvent(final int number)
	{
		_player.sendPacket(new TutorialEnableClientEvent(number));
	}

	public void showQuestionMark(final int number)
	{
		_player.sendPacket(new TutorialShowQuestionMark(number));
	}

	public void showTutorialHTML(String html)
	{
		String text = Files.read("data/scripts/quests/_255_Tutorial/" + html);
		if(text == null || text.equalsIgnoreCase(""))
			text = "<html><body>File data/scripts/quests/_255_Tutorial/" + html + " not found or file is empty.</body></html>";
		_player.sendPacket(new TutorialShowHtml(text));
	}

	/**
	 * Start a timer for quest.<BR><BR>
	 * 
	 * @param name
	 *            <BR> The name of the timer. Will also be the value for event of onEvent
	 * @param time
	 *            <BR> The milisecond value the timer will elapse
	 */
	public void startQuestTimer(final String name, final long time)
	{
		getQuest().startQuestTimer(name, time, null, getPlayer());
	}

	/**
	 * Удаляет указанные предметы из инвентаря игрока, и обновляет инвентарь
	 * 
	 * @param itemId
	 *            : id удаляемого предмета
	 * @param count
	 *            : число удаляемых предметов<br>
	 *            Если count передать -1, то будут удалены все указанные предметы.
	 * @return Количество удаленных предметов
	 */
	public int takeItems(final int itemId, int count)
	{
		// Get object item from player's inventory list
		final L2ItemInstance item = _player.getInventory().getItemByItemId(itemId);
		if(item == null)
			return 0;
		// Tests on count value in order not to have negative value
		if(count < 0 || count > item.getCount())
			count = item.getIntegerLimitedCount();

		// Destroy the quantity of items wanted
		_player.getInventory().destroyItemByItemId(itemId, count, true);
		// Send message of destruction to client
		if(itemId == 57)
			_player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(count));
		else if(count == 1)
			_player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(itemId));
		else
			_player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(itemId).addNumber(count));

		return count;
	}

	public int takeAllItems(final int itemId)
	{
		return takeItems(itemId, -1);
	}

	public long takeAllItems(final int[] itemsIds)
	{
		long result = 0;
		for(final int id : itemsIds)
			result += takeAllItems(id);
		return result;
	}

	public long takeAllItems(final short[] itemsIds)
	{
		long result = 0;
		for(final int id : itemsIds)
			result += takeAllItems(id);
		return result;
	}

	public long takeAllItems(final Collection<Integer> itemsIds)
	{
		long result = 0;
		for(final int id : itemsIds)
			result += takeAllItems(id);
		return result;
	}

	/**
	 * Remove the variable of quest from the list of variables for the quest.<BR><BR>
	 * <U><I>Concept : </I></U>
	 * Remove the variable of quest represented by "var" from the class variable FastMap "vars" and from the database.
	 * 
	 * @param var
	 *            : String designating the variable for the quest to be deleted
	 * @return String pointing out the previous value associated with the variable "var"
	 */
	public String unset(final String var)
	{
		if(_vars == null || var == null)
			return null;
		final String old = _vars.remove(var);
		if(old != null)
			Quest.deleteQuestVarInDb(this, var);
		return old;
	}

	private boolean checkPartyMember(final L2Player member, final State st, final int maxrange, final L2Object rangefrom)
	{
		if(member == null)
			return false;
		if(rangefrom != null && maxrange > 0 && !member.isInRange(rangefrom, maxrange))
			return false;
		if(st != null)
		{
			final QuestState qs = member.getQuestState(getQuest().getName());
			if(qs == null || qs.getState() != st)
				return false;
		}
		return true;
	}

	public FastList<L2Player> getPartyMembers(final State st, final int maxrange, final L2Object rangefrom)
	{
		final FastList<L2Player> result = new FastList<L2Player>();
		final L2Party party = getPlayer().getParty();
		if(party == null)
		{
			if(checkPartyMember(getPlayer(), st, maxrange, rangefrom))
				result.add(getPlayer());
			return result;
		}

		for(final L2Player _member : party.getPartyMembers())
			if(checkPartyMember(_member, st, maxrange, rangefrom))
				result.add(getPlayer());

		return result;
	}

	public L2Player getRandomPartyMember()
	{
		return getRandomPartyMember(null);
	}

	public L2Player getRandomPartyMember(final State st)
	{
		return getRandomPartyMember(st, 0, null);
	}

	public L2Player getRandomPartyMember(final State st, final int maxrangefromplayer)
	{
		return getRandomPartyMember(st, maxrangefromplayer, getPlayer());
	}

	public L2Player getRandomPartyMember(final State st, final int maxrange, final L2Object rangefrom)
	{
		final FastList<L2Player> list = getPartyMembers(st, maxrange, rangefrom);
		if(list.size() == 0)
			return null;
		return list.get(Rnd.get(list.size()));
	}

	/**
	 * Add spawn for player instance
	 * Return object id of newly spawned npc
	 */
	public L2NpcInstance addSpawn(final int npcId)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, 0);
	}

	public L2NpcInstance addSpawn(final int npcId, final int despawnDelay)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, despawnDelay);
	}

	public L2NpcInstance addSpawn(final int npcId, final int x, final int y, final int z)
	{
		return addSpawn(npcId, x, y, z, 0, false, 0);
	}

	/**
	 * Add spawn for player instance
	 * Will despawn after the spawn length expires
	 * Return object id of newly spawned npc
	 */
	public L2NpcInstance addSpawn(final int npcId, final int x, final int y, final int z, final int despawnDelay)
	{
		return addSpawn(npcId, x, y, z, 0, false, despawnDelay);
	}

	/**
	 * Add spawn for player instance
	 * Return object id of newly spawned npc
	 */
	public L2NpcInstance addSpawn(final int npcId, final int x, final int y, final int z, final int heading, final boolean randomOffset, final int despawnDelay)
	{
		return getQuest().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay);
	}

	public L2NpcInstance findTemplate(final int npcId)
	{
		for(final L2Spawn spawn : SpawnTable.getInstance().getSpawnTable().values())
			if(spawn != null && spawn.getNpcId() == npcId)
				return spawn.getLastSpawn();
		return null;
	}

	public int calculateLevelDiffForDrop(final int mobLevel, final int player)
	{
		if(!Config.DEEPBLUE_DROP_RULES)
			return 0;
		return Math.max(player - mobLevel - Config.DEEPBLUE_DROP_MAXDIFF, 0);
	}
}