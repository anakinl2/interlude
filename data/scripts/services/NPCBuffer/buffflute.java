package services.NPCBuffer;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.effects.EffectTemplate;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Files;
import com.lineage.util.GArray;

public class buffflute extends Functions implements ScriptFile
{
	private static GArray<Integer> _avaiblebuffs;

	public static void show(String[] args)
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		String sheme = args[0];
		int page = Integer.valueOf(args[1]);

		switch(page)
		{
			case 1:
				createHtml(player, 1, sheme);
				break;
			case 2:
				createHtml(player, 2, sheme);
				break;
			case 3:
				createHtml(player, 3, sheme);
				break;
			case 4:
				createHtml(player, 4, sheme);
				break;
		}
	}

	public static void main_page(L2Player player)
	{
		String main_page = Files.read("data/scripts/services/NPCBuffer/main_page.htm");
		String one_scheme = Files.read("data/scripts/services/NPCBuffer/one_scheme.htm");
		String shemes_list = "";

		int i = 0;
		
		for(String scheme_name : player.getShemes().keySet())
		{
			i++;
			shemes_list += one_scheme.replaceAll("%scheme%", scheme_name).replaceAll("%schme_id%", i+"");
		}
		
		if(i < 7)
		{
			for(int c = 7-i; c != 0; c--)
			{
				shemes_list += "<br><br>";
			}
		}
		
		main_page = main_page.replaceAll("%schemes%", shemes_list);
		show(main_page, player);
	}

	public static void create_scheme(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		String pending_sheme_name = args[0];

		if(pending_sheme_name.length() > 16)
		{
			player.sendMessage("Scheme name cant be longer than 16chars");
			return;
		}

		if(player.getScheme(pending_sheme_name) != null)
		{
			player.sendMessage("You already have a sheme with that name.");
			return;
		}

		if(player.getShemes().size() > 6)
		{
			player.sendMessage("You can have maximum of 7 schemes.");
			return;
		}

		player.sendMessage("Buff sheme '" + pending_sheme_name + "' created, now you can add buffs to it.");
		player.sendPacket(new PlaySound("ItemSound.quest_accept"));
		player.addScheme(pending_sheme_name);
		main_page(player);
	}

	public static void main_page_s(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		main_page(player);
	}

	public static void cleanbuffs(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		String sheme_name = args[0];

		player.getScheme(sheme_name).clear();
		player.sendPacket(new PlaySound("ItemSound.quest_accept"));
		createHtml(player, player.getLastPage(), sheme_name);
	}

	public static void edit(String[] args)
	{
		L2Player player = (L2Player) self;
		createHtml(player, 1, args[0]);
	}

	public static void addbuff(String[] args)
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		String scheme = args[0];
		int buff = Integer.valueOf(args[1]);
		
		if(player.getScheme(scheme).size() >= player.getBuffLimit())
		{
			player.sendMessage("You cant add more than " + player.getBuffLimit() + " buffs.");
			createHtml(player, player.getLastPage(), scheme);
			return;
		}

		if(!_avaiblebuffs.contains(buff))
		{
			player.sendMessage("Dont cheat NUUB!.");
			return;
		}

		player.sendPacket(new PlaySound("ItemSound.quest_itemget"));
		player.addBuff(scheme, buff);
		createHtml(player, player.getLastPage(), scheme);
	}

	public static void removebuff(String[] args)
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		String scheme = args[0];
		int buff = Integer.valueOf(args[1]);

		if(!_avaiblebuffs.contains(buff))
		{
			player.sendMessage("Dont cheat NUUB!.");
			return;
		}

		player.removeBuff(scheme, buff);
		createHtml(player, player.getLastPage(), scheme);
	}

	public static void createHtml(L2Playable playable, int page, String scheme)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player activeChar = (L2Player) playable;
		GArray<Integer> buffs = activeChar.getScheme(scheme);
		String general_buffs = "HXaxAXaxa try more dumbass!";

		switch(page)
		{
			case 1://general_buffs
				general_buffs = Files.read("data/scripts/services/NPCBuffer/general_buffs.htm");
				break;
			case 2://general_buffs
				general_buffs = Files.read("data/scripts/services/NPCBuffer/dance_buffs.htm");
				break;
			case 3://general_buffs
				general_buffs = Files.read("data/scripts/services/NPCBuffer/song_buffs.htm");
				break;
			case 4://general_buffs
				general_buffs = Files.read("data/scripts/services/NPCBuffer/other_buffs.htm");
				break;
		}

		general_buffs = general_buffs.replaceAll("%bufs_chosen%", buffs.size() + "");
		general_buffs = general_buffs.replaceAll("%bufs_all%", activeChar.getBuffLimit() + "");
		general_buffs = general_buffs.replaceAll("%scheme%", scheme + "");
		
		
		int barup = calculteBar(265, buffs.size(), activeChar.getBuffLimit());
		general_buffs = general_buffs.replaceAll("%bar1up%", "" + barup);
		general_buffs = general_buffs.replaceAll("%bar2up%", "" + (265 - barup));		

		for(int buff : _avaiblebuffs)
		{
			if(!buffs.contains(buff))
			{
				general_buffs = general_buffs.replaceFirst("%color" + buff + "%", "6e6e6a");
				general_buffs = general_buffs.replaceFirst("%link" + buff + "%", "addbuff");
			}
			else
			{
				general_buffs = general_buffs.replaceFirst("%color" + buff + "%", "6692af");
				general_buffs = general_buffs.replaceFirst("%link" + buff + "%", "removebuff");
			}
		}
		activeChar.setLastPage(page);
		Functions.show(general_buffs, activeChar);
	}
	
	public static int calculteBar(int barmax, int buffs, int maxbuffs)
	{
		int c = barmax * (buffs * 100 / maxbuffs) / 100;
		if(c >= barmax)
			return barmax;
		return c;
	}

	public static void buff_player(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		String scheme = args[1];
		if(!conditionsPlayer(player))
			return;
		cancel(player);
		for(int i : player.getScheme(scheme))
		{
			L2Skill skill = SkillTable.getInstance().getInfo(i, SkillTable.getInstance().getBaseLevel(i));
			for(EffectTemplate et : skill.getEffectTemplates())
			{
				Env env = new Env(player, player, skill);
				L2Effect effect = et.getEffect(env);
				effect.setPeriod(61 * 60000);
				player.getEffectList().addEffect(effect);
				player.updateEffectIcons();

				if(player.getPet() != null)
				{
					Env envPet = new Env(player.getPet(), player.getPet(), skill);
					L2Effect effectPet = et.getEffect(envPet);
					effectPet.setPeriod(61 * 60000);
					player.getPet().getEffectList().addEffect(effectPet);
					player.getPet().updateEffectIcons();
				}
			}
		}
		player.broadcastPacket(new MagicSkillUse(player, player, 2240, 1, 0, 0));
		if(player.getPet() != null)
			player.getPet().broadcastPacket(new MagicSkillUse(player.getPet(), player.getPet(), 2240, 1, 0, 0));
	}

	public static void heal()
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		if(!player.isInZone(ZoneType.peace_zone))
		{
			player.sendMessage("You can heal only in peace zone.");
			return;
		}
		
		if(player.isInCombat())
		{
			player.sendMessage("You cant use this in combat!");
			return;
		}

		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.broadcastPacket(new MagicSkillUse(player, player, 4380, 1, 0, 0));
		main_page(player);
	}

	
	public static void cancel(L2Character player)
	{
		if(player == null)
			return;

		for(L2Effect e : player.getEffectList().getAllEffects())
			if(_avaiblebuffs.contains(e.getSkill().getId()) && 	e.getEffected().getKnownSkill(e.getSkill().getId()) == null)
				e.exit();
		
		if(player.getPet()!=null)
		{
			for(L2Effect e : player.getPet().getEffectList().getAllEffects())
				if(_avaiblebuffs.contains(e.getSkill().getId()) && 	e.getEffected().getKnownSkill(e.getSkill().getId()) == null)
					e.exit();
		}
	}
	
	
	public static void cancel()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		cancel(player);
		main_page(player);
	}

	private static boolean conditionsPlayer(L2Player player)
	{
		if(player.isInOlympiadMode())
		{
			player.sendMessage("You cant use this in olympiad!");
			return false;
		}
		if(player.isInCombat())
		{
			player.sendMessage("You cant use this in combat!");
			return false;
		}
		if(player.getTeam() > 0)
		{
			player.sendMessage("You cant use this in events/duels!");
			return false;
		}
		return true;
	}
	
	public static void delete(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		String scheme = args[0];
		player.removeScheme(scheme);
		player.sendPacket(new PlaySound("ItemSound.quest_accept"));
		main_page(player);
	}

	public void onLoad()
	{
		_avaiblebuffs = new GArray<Integer>();		
		_avaiblebuffs.add(1085);//Acumen Casting Spd +30% 
		_avaiblebuffs.add(1304);//Advanced Block Shield P. Def +100% 
		_avaiblebuffs.add(1087);//Agility Evasion +4 
		_avaiblebuffs.add(1354);//Arcane Protection Cancel Resistance +30% and Debuff Resistance +20% 
		_avaiblebuffs.add(1045);//Bless the Body HP +35%, CP +35% 
		_avaiblebuffs.add(1243);//Bless the Shield Shield Def Rate +90% 
		_avaiblebuffs.add(1040);//Shield
		_avaiblebuffs.add(1048);// Bless the Soul MP +35% 
		_avaiblebuffs.add(1311);// Body of Avatar HP +35%, CP +35%, Restore HP added 
		_avaiblebuffs.add(1397);//Clarity MP consupmition for Magical Skills -10% 
		_avaiblebuffs.add(1078);//Concentration Chance of casting interruption -53% 
		_avaiblebuffs.add(1242);//Death Whisper Critical Damage +50% 
		_avaiblebuffs.add(1353);//Divine Protection Darkness Resistance +30% 
		_avaiblebuffs.add(1352);//Elemental Protection Fire Resistance +30%, Water, Wind and Earth Resistance +20% 
		_avaiblebuffs.add(1059);//Empower M. Atk. +75% 
		_avaiblebuffs.add(1077);//Focus Critical Rate +30% 
		_avaiblebuffs.add(1388);//Greater Might P. Atk. +10% 
		_avaiblebuffs.add(1389);//Greater Shield P. Def. +15% 
		_avaiblebuffs.add(1240);//Guidance Acurracy +4 
		_avaiblebuffs.add(1086);//Haste Atk. Spd. +33% 
		_avaiblebuffs.add(1392);//Holy Resistance Sacred Resistance +30% 
		_avaiblebuffs.add(1043);//Holy Weapon Enhances Holy [means sacred] attribute to P. Atk. 
		_avaiblebuffs.add(1032);//Invigor Bleed Resistance +50% 
		_avaiblebuffs.add(1036);//Magic Barrier M. Def. +30% 
		_avaiblebuffs.add(1035);//Mental Shield Hold, Sleep and Derragement Resistance +80% 
		_avaiblebuffs.add(1068);//Might P. Atk. +15% 
		_avaiblebuffs.add(1393);//Unholy Resistance Darkness Resistance +30% 
		_avaiblebuffs.add(1268);//Vampiric Rage Recover 9% of melee physical attack as HP 
		_avaiblebuffs.add(1303);//Wild Magic Magic Critical Rate +300% 
		_avaiblebuffs.add(1204);//Wind Walk Speed +33 
		_avaiblebuffs.add(1062);//Berseker Spirit P. Atk. +8%, M. Atk. +16%, P. Def. -8%, M. Def. -16%, Speed +8, Atk. Spd +8%, Casting Spd. +8% 

		//songai
		_avaiblebuffs.add(364);//Song of Champion Physical Skill Re-use delay -30% and MP cost for physical skills -5% 
		_avaiblebuffs.add(264);//Song of Earth P. Def. +25% 
		_avaiblebuffs.add(306);//Song of Flame Guard Fire Resistance +30% 
		_avaiblebuffs.add(269);//Song of Hunter Critical Rate +100% 
		_avaiblebuffs.add(270);//Song of Invocation Darkness Resistance +20% 
		_avaiblebuffs.add(265);//Song of Life HP regeneration +20% 
		_avaiblebuffs.add(363);//Song of Meditation MP regeneration +20% and MP cost -10% 
		_avaiblebuffs.add(349);//Song of Renewal Skills Re-use delay -30% and MP cost for skills -10% 
		_avaiblebuffs.add(308);//Song of Storm Guard Wind Resistance +30% 
		_avaiblebuffs.add(305);//Song of Vengeance Reflect 20% of melee received damage 
		_avaiblebuffs.add(304);//Song of Vitality HP +30% 
		_avaiblebuffs.add(267);//Song of Warding M. Def. +30% 
		_avaiblebuffs.add(266);//Song of Water Evasion +3 
		_avaiblebuffs.add(268);//Song of Wind Speed +20 

		//dancai
		_avaiblebuffs.add(307);//Dance of Aqua Guard Water Resistance +30% 
		_avaiblebuffs.add(276);//Dance of Concentration Casting Spd +30%, Chance of casting interruption -30% 
		_avaiblebuffs.add(309);//Dance of Earth Guard Earth Resistance +30% 
		_avaiblebuffs.add(274);//Dance of Fire Critical Damage +50% 
		_avaiblebuffs.add(275);//Dance of Fury Atk Spd +15% 
		_avaiblebuffs.add(272);//Dance of Inspiration Accuracy +4 
		_avaiblebuffs.add(277);//Dance of Light Enhances Holy attribute to P. Atk. 
		_avaiblebuffs.add(273);//Dance of Mystic M. Atk. +20% 
		_avaiblebuffs.add(311);//Dance of Protection Fall Damage -30% 
		_avaiblebuffs.add(365);//Dance of Siren Magic Critical Rate +200% 
		_avaiblebuffs.add(310);//Dance of Vampire Recover 8% of melee physical attack as HP 
		_avaiblebuffs.add(271);//Dance of Warrior P. Atk. +12% 

		//special
		_avaiblebuffs.add(1363);//- COV
		_avaiblebuffs.add(1356);//- Prophecy of Fire HP +20%, restore HP added, P.Atk. +10%, P.Def. +20%, Accuracy +4, Speed -10%, Atk. Spd. +20%, Debuff Resistance +10% 
		_avaiblebuffs.add(1355);// - Prophecy of Water MP regen per tick +20%, M.Atk. +20%, M.Def. +20%, Speed -20%, Casting Spd. +20%, Magic Critical Rate +100%, Debuff Resistance +10% 
		_avaiblebuffs.add(1357);// - Prophecy of Wind Accuracy +4, Atk Speed +20%, Recover 5% of melee physical damage as HP, Critical +20% from behind, Critical Damage from behind +20%, Debuff Resistance +10% 
		_avaiblebuffs.add(4699);// - Blessing of Queen Critical Rate +30%, Critical Damage +25% 
		_avaiblebuffs.add(4702);// - Blessing of Seraphim MP Regeneration +35% 
		_avaiblebuffs.add(4700);// - Gift of Queen P. Atk. +10%, Accuracy +2 
		_avaiblebuffs.add(4703);// - Gift of Seraphim Magical Skill Re-use delay -35%
		_avaiblebuffs.add(1414);//- Victories paagrio
		_avaiblebuffs.add(1413);// - Magnu's Chant Max MP +20%, M. Atk +30%, M. Def. +30%, Casting Speed +20%, MP Regeneration +50%, MP Consupmition -20%, Magic Critical Chance +85% 
		_avaiblebuffs.add(1182);//Resist Aqua Aqua Resistance +30% 
		_avaiblebuffs.add(1191);//Resist Fire Fire Resistance +30% 
		_avaiblebuffs.add(1033);//Resist Poison Poison Resistance +50% 
		_avaiblebuffs.add(1259);//Resist Shock Stun Resistance +40% 
		_avaiblebuffs.add(1189);//Resist Wind Wind Resistance +30% 
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}