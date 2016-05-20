package l2d;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import l2d.util.GArray;
import l2d.util.Util;

public class tests
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//2023 - small firework
		//2043 - even smaller
		
		
		///long nextEventTime = System.currentTimeMillis()+900000;
		///long test=  (nextEventTime - System.currentTimeMillis())/60000;
		
		//double 0.6;

	//The 0 symbol shows a digit or 0 if no digit present
		////NumberFormat formatter = new DecimalFormat("0.#");
		///String s = formatter.format(75);  // -001235
		// notice that the number was rounded up

		 GArray<Integer> _avaiblebuffs = new GArray<Integer>();
		int[] buffs = {1085,1304,1087};
		
		
		
		/*String additional = "<td width=\"10\"></td>";
		
		for(int c = 7-3; c != 0; c--)
		{
			System.out.println("<tr>");
			System.out.println("<td width=\"10\"></td>");
			System.out.println("<td><img src=\"icon.skill1085\" width=32 height=13></td>");
			System.out.println("<td width=\"100\"><font color=\"6e6e6a\"><a action=\"bypass -h scripts_services.NPCBuffer.buffflute:cancel\">Acumghjghjen</a></font></td>");
			
			System.out.println("<td><img src=\"icon.skill1304\" width=32 height=13></td>");
			System.out.println("<td width=\"100\"><font color=\"6e6e6a\"><a action=\"bypass -h scripts_services.NPCBuffer.buffflute:cancel\">55555</a></font></td>");*/
			System.out.println(Util.convertDateToString(System.currentTimeMillis()/1000));
		}		

}
