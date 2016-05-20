package services.NPCBuffer;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.tables.SkillTable;
import l2d.util.GCArray;
/**
 * 
 * @author Midnex
 *
 */
public class Simplebuffer extends Functions implements ScriptFile
{
	public static GCArray<Integer> _avaiblebuffs = new GCArray<Integer>();

	public static void buff(String[] args)
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;
		
		int skillid = Integer.valueOf(args[0]);
		
		if(npc== null || (npc.getDistance(player) > L2Character.INTERACTION_DISTANCE))
		{
			player.sendMessage("You are to far from npc.");
			return;
		}

		if(!_avaiblebuffs.contains((Integer)skillid))
			return;
		
		L2Skill skill = SkillTable.getInstance().getInfo(skillid,SkillTable.getInstance().getBaseLevel(skillid));
		skill.getEffects(player, player, false, false);
	  player.broadcastPacket(new MagicSkillUse(npc, player, skill.getId(), 1, 500, 0));

	  show("data/html/merchant/20.htm", player);
	}
	
	@Override
	public void onLoad()
	{
		_avaiblebuffs.add(1077); //Focus
		_avaiblebuffs.add(1242); //Death Whisper
		_avaiblebuffs.add(1086); //Haste
		_avaiblebuffs.add(1240); //Guidance
		_avaiblebuffs.add(1045); //Blessed Body
		_avaiblebuffs.add(1048); //Blessed Soul
		_avaiblebuffs.add(1087); //Agility
		_avaiblebuffs.add(1085); //Acumen
		_avaiblebuffs.add(1257); //Decrease Weight
		_avaiblebuffs.add(1068); //Might
		_avaiblebuffs.add(1040); //Shield
		_avaiblebuffs.add(1036); //Magic Barrier
		_avaiblebuffs.add(1268); //Vampiric Rage
		_avaiblebuffs.add(1059); //Empower
		_avaiblebuffs.add(1204); //Wind Walk
		_avaiblebuffs.add(1062); //Berserker Spirit
		_avaiblebuffs.add(1078); //Concentration
		_avaiblebuffs.add(1259); //Resist Shock
		_avaiblebuffs.add(1243); //Bless Shield
		_avaiblebuffs.add(1035); //Mental Shield
			
		// Songs
		/*_avaiblebuffs.add(264); // Song of Earth
		_avaiblebuffs.add(265); // Song of Life
		_avaiblebuffs.add(266); // Song of Water
		_avaiblebuffs.add(267); // Song of Warding
		_avaiblebuffs.add(268); // Song of Wind
		_avaiblebuffs.add(269); // Song of Hunter
		_avaiblebuffs.add(270); // Song of Invocation
		_avaiblebuffs.add(304); // Song of Vitality
		_avaiblebuffs.add(305); // Song of Vengeance
		_avaiblebuffs.add(306); // Song of Flame Guard
		_avaiblebuffs.add(308); // Song of Storm Guard
		_avaiblebuffs.add(349); // Song of Renewal
		_avaiblebuffs.add(363); // Song of Meditation
		_avaiblebuffs.add(364); // Song of Champion
		// Dances
		_avaiblebuffs.add(271); // Dance of Warrior
		_avaiblebuffs.add(272); // Dance of Inspiration
		_avaiblebuffs.add(273); // Dance of Mystic
		_avaiblebuffs.add(274); // Dance of Fire
		_avaiblebuffs.add(275); // Dance of Fury
		_avaiblebuffs.add(276); // Dance of Concentration
		_avaiblebuffs.add(277); // Dance of Light
		_avaiblebuffs.add(307); // Dance of Aqua Guard
		_avaiblebuffs.add(309); // Dance of Earth Guard
		_avaiblebuffs.add(310); // Dance of Vampire
		_avaiblebuffs.add(311); // Dance of Protection
		_avaiblebuffs.add(365); // Dance of Siren

		
		_avaiblebuffs.add(1363); // Chant of Victory
		_avaiblebuffs.add(1413); // Chant of Magnus*/
	}

	@Override
	public void onReload()
	{		
	}

	@Override
	public void onShutdown()
	{		
	}

}
