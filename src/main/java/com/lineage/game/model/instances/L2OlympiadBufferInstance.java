package com.lineage.game.model.instances;

import java.io.File;
import java.util.StringTokenizer;

import javolution.util.FastList;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.ValidateLocation;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2NpcTemplate;

public class L2OlympiadBufferInstance extends L2NpcInstance
{
	private int buffers_count = 0;

	public L2OlympiadBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2Player player)
	{
		if(this != player.getTarget())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			if(!isInRange(player, INTERACTION_DISTANCE))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else if(buffers_count > 4)
				showChatWindow(player, 1);
			else
				showChatWindow(player, 0);
			player.sendActionFailed();
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(buffers_count > 4)
			showChatWindow(player, 1);

		if(command.startsWith("Buff"))
		{
			int id = 0;
			int lvl = 0;
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			id = Integer.parseInt(st.nextToken());
			lvl = Integer.parseInt(st.nextToken());
			L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
			FastList<L2Character> target = new FastList<L2Character>();
			target.add(player);
			broadcastPacket(new MagicSkillUse(this, player, id, lvl, 0, 0));
			callSkill(skill, target, true);
			buffers_count++;
			if(buffers_count > 4)
				showChatWindow(player, 1);
			else
				showChatWindow(player, 0);
		}
		else
			showChatWindow(player, 0);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom;
		if(val == 0)
			pom = "buffer";
		else
			pom = "buffer-" + val;
		String temp = "data/html/olympiad/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
}