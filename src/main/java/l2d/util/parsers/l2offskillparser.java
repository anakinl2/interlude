package l2d.util.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javolution.util.FastList;
import l2d.blskilas;

public class l2offskillparser
{

	static String AllStrings = "";
	//public static FastMap<Integer,Integer> AllPowers = new FastMap<Integer,Integer>();
	public static FastList<blskilas> AllPowers = new FastList<blskilas>();

	public static int lines, filecc;

	public static boolean parsePowers()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("FORPARSE/SKc6.txt");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while((strLine = br.readLine()) != null)
			{
				parseString2(strLine);
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

	private static boolean parseString2(String kaka)
	{
		//String kaka = "1	11	\"a,Inflicts 3 quick slashes on a target. An equipped Dual-Sword Weapon is required to use this skill.  Shield defense is ignored. Over-hit is possible   @@Power=760";
		//String[] power;
		//power = kaka.split("Power");

		////////////////////////////
		String[] forid;/////////////
		forid = kaka.split("@@");///
		////////////////////////////

		////////////////////////////
		String[] forid2;/////////////
		forid2 = kaka.split("	");///
		////////////////////////////

		////////////////////////////
		String[] forname;/////////////
		forname = kaka.split("a,");///
		////////////////////////////

		//System.out.println(power.length +" <<<<<<<<");  110+	

		int skillId = 0;
		int skilllvl = 0;
		int poweras = 0;
		String name = "FIX.MY.NAME";

		for(String powersd : forid)
		{
			skillId = Integer.parseInt(forid2[0].replaceAll("\\D", ""));
			skilllvl = Integer.parseInt(forid2[1].replaceAll("\\D", ""));
			name = forname[1].replaceAll("	", "").replaceAll("0", "").replaceAll("/", "");

			if(powersd.contains("Power="))
			{
				String temp;

				temp = powersd.replaceAll("Power=", "").replaceAll("\\D", "");
				poweras = Integer.parseInt(temp);
			}

		}

		blskilas a = new blskilas(skillId, skilllvl, poweras, name);
		AllPowers.add(a);

		return true;
	}

	public static void main(String[] args) throws IOException
	{

		parsePowers();
		System.out.println(AllPowers.size());

		//parseString("1");
		//System.out.println((int) (((double)1.733)*1000));

		//System.out.println((int) ((double)1.733*1000));

	}

	private static boolean parseString(String skilas)
	{

		//String skilas = "skill_begin	skill_name=[s_long_range_shot1]	/*	[Long Shot]	*/	skill_id=113	level=1	operate_type=P	magic_level=20	effect={{p_attack_range;200;diff}}	skill_end";
		/**
		 * Constructor:
		 * skillname
		 * skill_id
		 * level
		 * power
		 * magic_level
		 * is_magic
		 * mp_consume
		 * cast_range
		 * skill_hit_time
		 */

		String[] temp;
		temp = skilas.split("	");

		int id = 0;
		int level = 0;
		int magiclevel = 0;
		String name = "";
		int isMagic = 0;
		int mpconsume = 0;
		int hpconsume = 0;
		int castRange = -1;
		int hitTime = 0;
		int power = 0;
		// String a = "",b = "",c = "",d = "",e = "",f = "",g= "",h= "",i= "",j="";
		//c = "'"+temp[2].replaceAll("\\[", "").replaceAll("\\]", ""+"',");

		for(String splitas : temp)
		{
			String tempas = "";

			if(splitas.contains("skill_id="))
			{
				tempas = splitas.replaceAll("skill_id=", "");
				id = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}
			if(splitas.startsWith("level="))
			{
				tempas = splitas.replaceAll("level=", "");
				level = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}
			if(splitas.contains("is_magic="))
			{
				tempas = splitas.replaceAll("is_magic=", "");
				isMagic = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}

			if(splitas.contains("mp_consume1="))
			{
				tempas = splitas.replaceAll("mp_consume1=", "");
				mpconsume = Integer.parseInt(tempas);
			}
			if(splitas.contains("mp_consume2="))
			{
				tempas = splitas.replaceAll("mp_consume2=", "");
				mpconsume = mpconsume + Integer.parseInt(tempas);
			}

			if(splitas.contains("hp_consume="))
			{
				tempas = splitas.replaceAll("hp_consume=", "");
				hpconsume = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}

			if(splitas.contains("cast_range="))
			{
				tempas = splitas.replaceAll("cast_range=", "");
				castRange = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}

			if(splitas.contains("skill_hit_time="))
			{
				tempas = splitas.replaceAll("skill_hit_time=", "");
				hitTime = (int) (Double.parseDouble(tempas) * 1000);
			}

			if(id > 0 && level > 0)
			{
				for(blskilas skilasukas : AllPowers)
				{
					if(skilasukas.getSkillId() == id && skilasukas.getSkilllvl() == level)
					{
						power = skilasukas.getPower();
						name = skilasukas.getName();
					}
				}
			}

			if(splitas.contains("magic_level="))
			{
				tempas = splitas.replaceAll("magic", "");
				tempas = tempas.replaceAll("level=", "");
				tempas = tempas.replaceAll("_", "");

				if(level < 101)
					magiclevel = Integer.parseInt(tempas.replaceAll("\\D", ""));
			}
			tempas = "";
		}

		AllStrings += "(" + id + "," + level + "," + magiclevel + ",'" + name + "'," + isMagic + "," + mpconsume + "," + hpconsume + "," + castRange + "," + hitTime + "," + power + "),\n";

		return true;
	}

	public static boolean containsOnlyNumbers(String str)
	{

		//It can't contain only numbers if it's null or empty...
		if(str == null || str.length() == 0)
			return false;

		for(int i = 0; i < str.length(); i++)
		{

			//If we find a non-digit character we return false.
			if(!Character.isDigit(str.charAt(i)))
				return false;
		}

		return true;
	}

	public static boolean write(String html, int filecc2)
	{
		try
		{
			// Create file 
			FileWriter fstream = new FileWriter("FORPARSE/bandom" + filecc2 + ".txt");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(html);
			out.close();
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		return true;
	}

}