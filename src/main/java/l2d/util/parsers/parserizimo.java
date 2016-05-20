package l2d.util.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import javolution.util.FastList;
import l2d.blskilas;

public class parserizimo
{
	public static FastList<L2OffHitTime> Hitimes = new FastList<L2OffHitTime>();
	public static FastList<L2Other> Others = new FastList<L2Other>();
	public static FastList<L2PNamePower> NamesPowers = new FastList<L2PNamePower>();

	
	public static FastList<String> all = new FastList<String>();

	
	static String AllStrings = "";

	
	public static void main(String[] args)
	{
		loadOther();
		System.out.println(Others.size());
		loadNames();
		System.out.println(NamesPowers.size());
		loadHitimes();
		System.out.println(Hitimes.size());
		lipdyt();
		write(99);
	}

	
	public static boolean write(int cc)
	{
		try
		{
			// Create file 
			FileWriter fstream = new FileWriter("FORPARSE/bandom"+cc+".txt");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(AllStrings);
			out.close();
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		return true;
	}

	
	
	public static boolean lipdyt()
	{
		int lol = 0;
		int lol2 = 0;

		for(L2Other skill : Others)
		{
		int _id = 0;//
		int _level = 0;//
		int _magiclevel = 0;//
		String _name = "";
		int _isMagic = 0;//
		int _mpconsume = 0;//
		int _hpconsume = 0;//
		int _castRange = -1;//
		int _hitTime = 0;//
		int _power = 0;


			_id = skill._skillId;
			_level = skill._skilllvl;
			_mpconsume = skill._mp_consume;
			_castRange = skill._cast_range;
			_isMagic = skill._is_magic;
			_hpconsume = skill._hp_consume;

		for(L2OffHitTime h : Hitimes)
		{
			if(h._skillId == _id && h._skilllvl == _level)
			{
				_hitTime = h._hittime1;
				_magiclevel = h._mlevel;
				break;
			}
		}
		
		for(L2PNamePower h1 : NamesPowers)
		{
			if(h1._skillId == _id && h1._skilllvl == _level)
			{
				_name = h1._name;
				_power = h1._power;
				break;
			}
		}
		
		System.out.println(lol);
		
		AllStrings += "(" + _id + "," + _level + "," + _magiclevel + ",'" + _name + "'," + _isMagic + "," + _mpconsume + "," + _hpconsume + "," + _castRange + "," + _hitTime + "," + _power + "),\n";
		lol++;
		if(lol == 6000)
		{
			lol=0;
			lol2++;
			write(lol2);
			AllStrings = "";
		}
		}
		return true;
	}

	public static boolean loadOther()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("FORPARSE/skillgrp.txt");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while((strLine = br.readLine()) != null)
			{
				parseOthersString(strLine);
			}
			//Close the input stream
			in.close();
		}
		catch(Exception e)
		{//Catch exception if any
			e.printStackTrace();
		}
		return true;
	}
	
	
	private static boolean parseOthersString(String Other)
	{
		//Syntax skill_id	skill_level	mp_consume	cast_range	is_magic	hp_consume
		String[] splitas;
		splitas = Other.split("	");
		
		int skillId = 0;
		int skilllvl = 0;
		int mp_consume = 0;
		int cast_range = 0;
		int is_magic = 0;
		int hp_consume = 0;
		
		skillId = Integer.parseInt(splitas[0].replaceAll("\\W", ""));
		skilllvl = Integer.parseInt(splitas[1].replaceAll("\\W", ""));
		mp_consume = Integer.parseInt(splitas[2].replaceAll("\\W", ""));
		cast_range = Integer.parseInt(splitas[3].replaceAll("\\W", ""));
		is_magic = Integer.parseInt(splitas[4].replaceAll("\\W", ""));
		hp_consume = Integer.parseInt(splitas[5].replaceAll("\\W", ""));
		
		L2Other a = new L2Other(skillId, skilllvl, mp_consume, cast_range,is_magic,hp_consume);
		Others.add(a);
		//System.out.println(skillId+skilllvl+ mp_consume+ cast_range+is_magic+hp_consume);
		return true;
	}
	
	
	
	
	public static boolean loadNames()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("FORPARSE/skillname.txt");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while((strLine = br.readLine()) != null)
			{
				parseNamesString(strLine);
			}
			//Close the input stream
			in.close();
		}
		catch(Exception e)
		{//Catch exception if any
			e.printStackTrace();
		}
		return true;
	}
	
	
	private static boolean parseNamesString(String Names)
	{
		//Syntax id	level	name	description
		//String Names = "3	1	\"Power Strike\"	\"Gathers power for a fierce strike. Used when equipped with a sword or blunt weapon. Over-hit is possible  Power=25\"";

		String[] splitas;
		splitas = Names.split("\\\"");

		String[] idlevel;
		idlevel = Names.split("	");

		int skillId = 0;
		int skilllvl = 0;
		String name = "";
		int power = 0;

		
		
		skillId = Integer.parseInt(idlevel[0].replaceAll("\\W", ""));
		skilllvl = Integer.parseInt(idlevel[1].replaceAll("\\W", ""));
		name = splitas[1];

		int ip = 0;
		for(String i : splitas)
		{
			if(i.contains("Power="))
			{
				String[] poweras;
				poweras = i.split("Power=");
				
				poweras[1] = poweras[1].replaceAll("\\\"", "").replaceAll("\\W", "");

				power = Integer.parseInt(poweras[1].replaceAll("\\\"", "").replaceAll("\\W", ""));
			}
		}
		//System.out.println(skillId+" "+skilllvl+"   "+name+"   "+power);
		L2PNamePower a = new L2PNamePower(skillId,skilllvl,name,power);
		NamesPowers.add(a);
		return true;
	}
	
	
	public static boolean loadHitimes()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("FORPARSE/skilldata.txt");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while((strLine = br.readLine()) != null)
			{
				parseHitimesString(strLine);
			}
			//Close the input stream
			in.close();
		}
		catch(Exception e)
		{//Catch exception if any
			e.printStackTrace();
		}
		return true;
	}
	
	
	private static boolean parseHitimesString(String Names)
	{
		//Syntax id	level	name	description
		//String Names = "skill_begin	skill_name=[s_triple_slash11]	/*	[Triple Slash]	*/	skill_id=1	level=1	operate_type=A1	magic_level=38	effect={{i_ps_attack_over_hit;431;0}}	operate_cond={{equip_weapon;{dual}}}	is_magic=0	mp_consume1=0	mp_consume2=47	cast_range=40	effective_range=60	skill_hit_time=1.733	skill_cool_time=0.167	skill_hit_cancel_time=0.5	reuse_delay=13	attribute=attr_none	effect_point=-213	target_type=enemy	affect_scope=single	affect_limit={0;0}	next_action=attack	ride_state={@ride_none}	skill_end";


		String[] hittime;
		hittime = Names.split("	");

		int skillId = 0;
		int skilllvl = 0;
		int hittime1 = 0;
		int mlevel = 0;

		skillId = Integer.parseInt(hittime[5].replaceAll("skill_id=", "").replaceAll("\\W", ""));
		skilllvl = Integer.parseInt(hittime[6].replaceAll("level=", ""));

		for(String i : hittime)
		{
			
			if(i.contains("skill_hit_time="))
			{
				String[] poweras;
				poweras = i.split("skill_hit_time=");
				
				//poweras[1]=poweras[1].replaceAll("\\.", "");

				hittime1 = (int)(Double.parseDouble(poweras[1])*1000);
			}
			if(i.contains("magic_level="))
			{
				String[] poweras;
				poweras = i.split("magic_level=");
				
				poweras[1]=poweras[1].replaceAll("\\.", "");
				mlevel = Integer.parseInt(poweras[1]);
			}

		}
		//System.out.println(skillId+" "+skilllvl+"   "+hittime1);
		L2OffHitTime a = new L2OffHitTime(skillId,skilllvl,hittime1,mlevel);
		Hitimes.add(a);
		return true;
	}

}
