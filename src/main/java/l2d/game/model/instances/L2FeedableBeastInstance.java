package l2d.game.model.instances;

import java.util.ArrayList;

import javolution.util.FastMap;
import l2d.game.ai.CtrlEvent;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.Say2;
import l2d.game.serverpackets.SocialAction;
import l2d.game.tables.NpcTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Rnd;

public class L2FeedableBeastInstance extends L2MonsterInstance
{
	public FastMap<Integer, growthInfo> growthCapableMobs = new FastMap<Integer, growthInfo>();

	ArrayList<Integer> tamedBeasts = new ArrayList<Integer>();
	ArrayList<Integer> feedableBeasts = new ArrayList<Integer>();

	public static FastMap<Integer, Integer> feedInfo = new FastMap<Integer, Integer>();

	private static int GOLDEN_SPICE = 0;
	private static int CRYSTAL_SPICE = 1;
	private static int SKILL_GOLDEN_SPICE = 2188;
	private static int SKILL_CRYSTAL_SPICE = 2189;

	private static String[][] text = new String[][] {
			{
					"What did you just do to me?",
					"You want to tame me, huh?",
					"Do not give me this. Perhaps you will be in danger.",
					"Bah bah. What is this unpalatable thing?",
					"My belly has been complaining. This hit the spot.",
					"What is this? Can I eat it?",
					"You don't need to worry about me.",
					"Delicious food, thanks.",
					"I am starting to like you!",
					"Gulp" },
			{
					"I do not think you have given up on the idea of taming me.",
					"That is just food to me.  Perhaps I can eat your hand too.",
					"Will eating this make me fat? Ha ha",
					"Why do you always feed me?",
					"Do not trust me. I may betray you" },
			{
					"Destroy",
					"Look what you have done!",
					"Strange feeling...! Evil intentions grow in my heart...!",
					"It is happenning!",
					"This is sad...Good is sad...!" } };

	public L2FeedableBeastInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		// Alpen Kookabura

		// x0.5
		growthCapableMobs.put(21451, new growthInfo(0, new int[][][] { { { 21452, 21453, 21454, 21455 } }, { { 21456, 21457, 21458, 21459 } } }, 100));

		// x1
		growthCapableMobs.put(21452, new growthInfo(1, new int[][][] { { { 21460, 21462 } }, {} }, 40));
		growthCapableMobs.put(21453, new growthInfo(1, new int[][][] { { { 21461, 21463 } }, {} }, 40));
		growthCapableMobs.put(21454, new growthInfo(1, new int[][][] { { { 21460, 21462 } }, {} }, 40));
		growthCapableMobs.put(21455, new growthInfo(1, new int[][][] { { { 21461, 21463 } }, {} }, 40));

		growthCapableMobs.put(21456, new growthInfo(1, new int[][][] { {}, { { 21464, 21466 } } }, 40));
		growthCapableMobs.put(21457, new growthInfo(1, new int[][][] { {}, { { 21465, 21467 } } }, 40));
		growthCapableMobs.put(21458, new growthInfo(1, new int[][][] { {}, { { 21464, 21466 } } }, 40));
		growthCapableMobs.put(21459, new growthInfo(1, new int[][][] { {}, { { 21465, 21467 } } }, 40));

		// x2
		growthCapableMobs.put(21460, new growthInfo(2, new int[][][] { { /* x4: */{ 21468, 21824 }, /* tamed: */{ 16017, 16018 } }, {} }, 25));
		growthCapableMobs.put(21461, new growthInfo(2, new int[][][] { { /* x4: */{ 21469, 21825 }, /* tamed: */{ 16017, 16018 } }, {} }, 25));
		growthCapableMobs.put(21462, new growthInfo(2, new int[][][] { { /* x4: */{ 21468, 21824 }, /* tamed: */{ 16017, 16018 } }, {} }, 25));
		growthCapableMobs.put(21463, new growthInfo(2, new int[][][] { { /* x4: */{ 21469, 21825 }, /* tamed: */{ 16017, 16018 } }, {} }, 25));

		growthCapableMobs.put(21464, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21468, 21824 }, /* tamed: */{ 16017, 16018 } } }, 25));
		growthCapableMobs.put(21465, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21469, 21825 }, /* tamed: */{ 16017, 16018 } } }, 25));
		growthCapableMobs.put(21466, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21468, 21824 }, /* tamed: */{ 16017, 16018 } } }, 25));
		growthCapableMobs.put(21467, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21469, 21825 }, /* tamed: */{ 16017, 16018 } } }, 25));

		// Alpen Buffalo

		// x0.5
		growthCapableMobs.put(21470, new growthInfo(0, new int[][][] { { { 21471, 21472, 21473, 21474 } }, { { 21475, 21476, 21477, 21478 } } }, 100));

		// x1
		growthCapableMobs.put(21471, new growthInfo(1, new int[][][] { { { 21479, 21481 } }, {} }, 40));
		growthCapableMobs.put(21472, new growthInfo(1, new int[][][] { { { 21480, 21482 } }, {} }, 40));
		growthCapableMobs.put(21473, new growthInfo(1, new int[][][] { { { 21479, 21481 } }, {} }, 40));
		growthCapableMobs.put(21474, new growthInfo(1, new int[][][] { { { 21480, 21482 } }, {} }, 40));

		growthCapableMobs.put(21475, new growthInfo(1, new int[][][] { {}, { { 21483, 21485 } } }, 40));
		growthCapableMobs.put(21476, new growthInfo(1, new int[][][] { {}, { { 21484, 21486 } } }, 40));
		growthCapableMobs.put(21477, new growthInfo(1, new int[][][] { {}, { { 21483, 21485 } } }, 40));
		growthCapableMobs.put(21478, new growthInfo(1, new int[][][] { {}, { { 21484, 21486 } } }, 40));

		// x2
		growthCapableMobs.put(21479, new growthInfo(2, new int[][][] { { /* x4: */{ 21487, 21826 }, /* tamed: */{ 16013, 16014 } }, {} }, 25));
		growthCapableMobs.put(21480, new growthInfo(2, new int[][][] { { /* x4: */{ 21488, 21827 }, /* tamed: */{ 16013, 16014 } }, {} }, 25));
		growthCapableMobs.put(21481, new growthInfo(2, new int[][][] { { /* x4: */{ 21487, 21826 }, /* tamed: */{ 16013, 16014 } }, {} }, 25));
		growthCapableMobs.put(21482, new growthInfo(2, new int[][][] { { /* x4: */{ 21488, 21827 }, /* tamed: */{ 16013, 16014 } }, {} }, 25));

		growthCapableMobs.put(21483, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21487, 21826 }, /* tamed: */{ 16013, 16014 } } }, 25));
		growthCapableMobs.put(21484, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21488, 21827 }, /* tamed: */{ 16013, 16014 } } }, 25));
		growthCapableMobs.put(21485, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21487, 21826 }, /* tamed: */{ 16013, 16014 } } }, 25));
		growthCapableMobs.put(21486, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21488, 21827 }, /* tamed: */{ 16013, 16014 } } }, 25));

		// Alpen Cougar

		// x0.5
		growthCapableMobs.put(21489, new growthInfo(0, new int[][][] { { { 21490, 21491, 21492, 21493 } }, { { 21494, 21495, 21496, 21497 } } }, 100));

		// x1
		growthCapableMobs.put(21490, new growthInfo(1, new int[][][] { { { 21498, 21500 } }, {} }, 40));
		growthCapableMobs.put(21491, new growthInfo(1, new int[][][] { { { 21499, 21501 } }, {} }, 40));
		growthCapableMobs.put(21492, new growthInfo(1, new int[][][] { { { 21498, 21500 } }, {} }, 40));
		growthCapableMobs.put(21493, new growthInfo(1, new int[][][] { { { 21499, 21501 } }, {} }, 40));

		growthCapableMobs.put(21494, new growthInfo(1, new int[][][] { {}, { { 21502, 21504 } } }, 40));
		growthCapableMobs.put(21495, new growthInfo(1, new int[][][] { {}, { { 21503, 21505 } } }, 40));
		growthCapableMobs.put(21496, new growthInfo(1, new int[][][] { {}, { { 21502, 21504 } } }, 40));
		growthCapableMobs.put(21497, new growthInfo(1, new int[][][] { {}, { { 21503, 21505 } } }, 40));

		// x2
		growthCapableMobs.put(21498, new growthInfo(2, new int[][][] { { /* x4: */{ 21506, 21828 }, /* tamed: */{ 16015, 16016 } }, {} }, 25));
		growthCapableMobs.put(21499, new growthInfo(2, new int[][][] { { /* x4: */{ 21507, 21829 }, /* tamed: */{ 16015, 16016 } }, {} }, 25));
		growthCapableMobs.put(21500, new growthInfo(2, new int[][][] { { /* x4: */{ 21506, 21828 }, /* tamed: */{ 16015, 16016 } }, {} }, 25));
		growthCapableMobs.put(21501, new growthInfo(2, new int[][][] { { /* x4: */{ 21507, 21829 }, /* tamed: */{ 16015, 16016 } }, {} }, 25));

		growthCapableMobs.put(21502, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21506, 21828 }, /* tamed: */{ 16015, 16016 } } }, 25));
		growthCapableMobs.put(21503, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21507, 21829 }, /* tamed: */{ 16015, 16016 } } }, 25));
		growthCapableMobs.put(21504, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21506, 21828 }, /* tamed: */{ 16015, 16016 } } }, 25));
		growthCapableMobs.put(21505, new growthInfo(2, new int[][][] { {}, { /* x4: */{ 21507, 21829 }, /* tamed: */{ 16015, 16016 } } }, 25));

		for(Integer i = 16013; i <= 16018; i++)
			tamedBeasts.add(i);
		for(Integer i = 16013; i <= 16019; i++)
			feedableBeasts.add(i);
		for(Integer i = 21451; i <= 21507; i++)
			feedableBeasts.add(i);
		for(Integer i = 21824; i <= 21829; i++)
			feedableBeasts.add(i);
	}

	private void spawnNext(L2Player player, int growthLevel, int food)
	{
		int npcId = getNpcId();
		int nextNpcId = 0;

		if(growthLevel == 2)
		{
			// if tamed, the mob that will spawn depends on the class type (fighter/mage) of the player!
			if(Rnd.chance(50))
				if(player.getClassId().isMage())
					nextNpcId = growthCapableMobs.get(npcId).spice[food][1][1];
				else
					nextNpcId = growthCapableMobs.get(npcId).spice[food][1][0];
			// if not tamed, there is a small chance that have "mad cow" disease. That is a stronger-than-normal animal that attacks its feeder
			else if(player.getClassId().isMage())
				nextNpcId = growthCapableMobs.get(npcId).spice[food][0][1];
			else
				nextNpcId = growthCapableMobs.get(npcId).spice[food][0][0];
		}
		else
			// all other levels of growth are straight-forward
			nextNpcId = growthCapableMobs.get(npcId).spice[food][0][Rnd.get(growthCapableMobs.get(npcId).spice[food][0].length)];

		// remove the feedinfo of the mob that got despawned, if any
		feedInfo.remove(getObjectId());

		// despawn the old mob
		if(growthCapableMobs.get(npcId).growth_level == 0)
			onDecay();
		else
			deleteMe();

		// if this is finally a trained mob, then despawn any other trained mobs that the player might have and initialize the Tamed Beast.
		if(tamedBeasts.contains(nextNpcId))
		{
			L2TamedBeastInstance oldTrained = player.getTrainedBeast();
			if(oldTrained != null)
				oldTrained.doDespawn();

			L2NpcTemplate template = NpcTable.getTemplate(nextNpcId);
			L2TamedBeastInstance nextNpc = new L2TamedBeastInstance(IdFactory.getInstance().getNextId(), template, player, food == 0 ? SKILL_GOLDEN_SPICE : SKILL_CRYSTAL_SPICE, getLoc());

			int objectId = nextNpc.getObjectId();

			QuestState st = player.getQuestState("_020_BringUpWithLove");
			if(st != null && Rnd.chance(5) && st.getQuestItemsCount(7185) == 0)
			{
				st.giveItems(7185, 1);
				st.set("cond", "2");
			}

			// also, perform a rare random chat
			int rand = Rnd.get(10);
			if(rand > 4)
				return;

			if(rand == 0)
				broadcastPacket(new Say2(objectId, 0, nextNpc.getName(), player.getName() + ", will you show me your hideaway?"));
			else if(rand == 1)
				broadcastPacket(new Say2(objectId, 0, nextNpc.getName(), player.getName() + ", whenever I look at spice, I think about you."));
			else if(rand == 2)
				broadcastPacket(new Say2(objectId, 0, nextNpc.getName(), player.getName() + ", you do not need to return to the village.  I will give you strength"));
			else if(rand == 3)
				broadcastPacket(new Say2(objectId, 0, nextNpc.getName(), "Thanks, " + player.getName() + ".  I hope I can help you"));
			else if(rand == 4)
				broadcastPacket(new Say2(objectId, 0, nextNpc.getName(), player.getName() + ", what can I do to help you?"));
		}
		// if not trained, the newly spawned mob will automatically be agro against its feeder (what happened to "never bite the hand that feeds you" anyway?!)
		else
		{
			// spawn the new mob
			L2Spawn spawn = spawn(nextNpcId, getX(), getY(), getZ());
			if(spawn == null)
				return;

			L2MonsterInstance nextNpc = (L2MonsterInstance) spawn.getLastSpawn();

			// register the player in the feedinfo for the mob that just spawned
			feedInfo.put(nextNpc.getObjectId(), player.getObjectId());

			broadcastPacket(new Say2(nextNpc.getObjectId(), 0, nextNpc.getName(), text[growthLevel][Rnd.get(text[growthLevel].length)]));

			nextNpc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, player, 99999);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		feedInfo.remove(getObjectId());
		super.doDie(killer);
	}

	private class growthInfo
	{
		public int growth_level;
		public int growth_chance;
		public int[][][] spice;

		public growthInfo(int level, int[][][] sp, int chance)
		{
			growth_level = level;
			spice = sp;
			growth_chance = chance;
		}
	}

	public L2Spawn spawn(int npcId, int x, int y, int z)
	{
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(npcId);
			if(template == null)
				return null;
			L2Spawn spawn = new L2Spawn(template);
			spawn.setId(npcId);
			if(getPlayer() != null)
				spawn.setHeading(getPlayer().getHeading());
			else
				spawn.setHeading(0);
			spawn.setLocx(x);
			spawn.setLocy(y);
			spawn.setLocz(z + 20);
			spawn.stopRespawn();
			spawn.setAmount(1);
			spawn.doSpawn(true);
			return spawn;
		}
		catch(Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}
		return null;
	}

	public void onSkillUse(L2Player player, int skill_id)
	{
		// gather some values on local variables
		int npcId = getNpcId();
		// check if the npc and skills used are valid
		if(!feedableBeasts.contains(npcId))
			return;
		if(skill_id != SKILL_GOLDEN_SPICE && skill_id != SKILL_CRYSTAL_SPICE)
			return;

		int food = GOLDEN_SPICE;
		if(skill_id == SKILL_CRYSTAL_SPICE)
			food = CRYSTAL_SPICE;

		int objectId = getObjectId();
		// display the social action of the beast eating the food.
		broadcastPacket(new SocialAction(objectId, 2));

		// if this pet can't grow, it's all done.
		if(growthCapableMobs.containsKey(npcId))
		{
			// do nothing if this mob doesn't eat the specified food (food gets consumed but has no effect).
			if(growthCapableMobs.get(npcId).spice[food].length == 0)
				return;

			// more value gathering on local variables
			int growthLevel = growthCapableMobs.get(npcId).growth_level;

			if(growthLevel > 0)
				// check if this is the same player as the one who raised it from growth 0.
				// if no, then do not allow a chance to raise the pet (food gets consumed but has no effect).
				if(feedInfo.get(objectId) != null && feedInfo.get(objectId) != player.getObjectId())
					return;

			// Polymorph the mob, with a certain chance, given its current growth level
			if(Rnd.chance(growthCapableMobs.get(npcId).growth_chance))
				spawnNext(player, growthLevel, food);
		}
		else if(tamedBeasts.contains(npcId))
			if(skill_id == ((L2TamedBeastInstance) this).getFoodType())
			{
				((L2TamedBeastInstance) this).onReceiveFood();
				String[] mytext = new String[] {
						"Refills! Yeah!",
						"I am such a gluttonous beast, it is embarrassing! Ha ha",
						"Your cooperative feeling has been getting better and better.",
						"I will help you!",
						"The weather is really good.  Wanna go for a picnic?",
						"I really like you! This is tasty...",
						"If you do not have to leave this place, then I can help you.",
						"What can I helped you with?",
						"I am not here only for food!",
						"Yam, yam, yam, yam, yam!" };
				broadcastPacket(new Say2(objectId, 0, getName(), mytext[Rnd.get(mytext.length)]));
			}
	}
}