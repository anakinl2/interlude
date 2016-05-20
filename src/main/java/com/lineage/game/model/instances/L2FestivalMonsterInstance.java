package com.lineage.game.model.instances;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2NpcTemplate;

public class L2FestivalMonsterInstance extends L2MonsterInstance
{
	protected int _bonusMultiplier = 1;

	/**
	 * Constructor of L2FestivalMonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2FestivalMonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param L2NpcTemplate Template to apply to the NPC
	 */
	public L2FestivalMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public void setOfferingBonus(int bonusMultiplier)
	{
		_bonusMultiplier = bonusMultiplier;
	}

	/**
	 * All mobs in the festival really don't need random animation.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	/**
	 * Actions:
	 * <li>Check if the killing object is a player, and then find the party they belong to.</li>
	 * <li>Add a blood offering item to the leader of the party.</li>
	 * <li>Update the party leader's inventory to show the new item addition.</li>
	 */
	@Override
	public void doItemDrop(L2Character topDamager)
	{
		if(!(topDamager instanceof L2Playable))
			return;

		L2Player killingChar = topDamager.getPlayer();
		L2Party associatedParty = killingChar.getParty();

		if(associatedParty == null)
			return;

		L2Player partyLeader = associatedParty.getPartyLeader();
		L2ItemInstance bloodOfferings = ItemTable.getInstance().createItem(SevenSignsFestival.FESTIVAL_OFFERING_ID);

		bloodOfferings.setCount(_bonusMultiplier);
		partyLeader.getInventory().addItem(bloodOfferings);
		if(_bonusMultiplier > 1)
			partyLeader.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(SevenSignsFestival.FESTIVAL_OFFERING_ID).addNumber(_bonusMultiplier));
		else
			partyLeader.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(SevenSignsFestival.FESTIVAL_OFFERING_ID));
	}
}