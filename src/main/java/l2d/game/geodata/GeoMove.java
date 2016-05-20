package l2d.game.geodata;

import java.util.ArrayList;

import com.lineage.Config;
import l2d.game.model.L2Object;
import com.lineage.util.Location;

public class GeoMove
{
	private static final ArrayList<Location> emptyTargetRecorder = new ArrayList<Location>();
	private static final ArrayList<Location> emptyMovePath = new ArrayList<Location>();

	public static ArrayList<Location> findPath(int x, int y, int z, Location target, L2Object obj)
	{
		if(Math.abs(z - target.z) > 256)
			return emptyTargetRecorder;

		z = GeoEngine.getHeight(x, y, z);
		target.setZ(GeoEngine.getHeight(target));
		PathFind n = new PathFind(x, y, z, target.x, target.y, target.z, obj);

		if(n.getPath() == null || n.getPath().isEmpty())
			return emptyTargetRecorder;

		ArrayList<Location> targetRecorder = new ArrayList<Location>();

		targetRecorder.add(new Location(x, y, z));

		for(PathFindBuffers.GeoNode p : n.getPath())
			targetRecorder.add(new Location(p._x, p._y, p._z).geo2world());

		targetRecorder.add(target);

		if(Config.PATH_CLEAN)
			pathClean(targetRecorder);

		return targetRecorder;
	}

	public static ArrayList<Location> findMovePath(int x, int y, int z, Location target, L2Object obj)
	{
		return getNodePath(findPath(x, y, z, target, obj));
	}

	public static ArrayList<Location> getNodePath(ArrayList<Location> path)
	{
		int size = path.size();
		if(size <= 1)
			return emptyMovePath;
		ArrayList<Location> result = new ArrayList<Location>();
		for(int i = 1; i < size; i++)
		{
			Location p2 = path.get(i);
			Location p1 = path.get(i - 1);
			ArrayList<Location> moveList = GeoEngine.MoveList(p1.x, p1.y, p1.z, p2.x, p2.y, true);
			if(moveList == null)
				return emptyMovePath;
			if(!moveList.isEmpty())
				result.addAll(moveList);
		}
		return result;
	}

	
	private static void pathClean(ArrayList<Location> path)
	{
		int size = path.size();
		if(size > 2)
			for(int i = 2; i < size; i++)
			{
				Location p3 = path.get(i); // точка конца движения
				Location p2 = path.get(i - 1); // точка в середине, кандидат на вышибание
				Location p1 = path.get(i - 2); // точка начала движения
				if(p1.equals(p2) || p3.equals(p2) || IsPointInLine(p1, p2, p3)) // если вторая точка совпадает с первой/третьей или на одной линии с ними - она не нужна
				{
					path.remove(i - 1); // удаляем ее
					size--; // отмечаем это в размере массива
					i = Math.max(2, i - 2); // сдвигаемся назад, FIXME: может я тут не совсем прав
				}
			}

		int current = 0;
		int sub;
		while(current < path.size() - 2)
		{
			Location one = path.get(current);
			sub = current + 2;
			while(sub < path.size())
			{
				Location two = path.get(sub);
				if(one.equals(two) || GeoEngine.canMoveWithCollision(one.x, one.y, one.z, two.x, two.y, two.z)) //canMoveWithCollision  /  canMoveToCoord
					while(current + 1 < sub)
					{
						path.remove(current + 1);
						sub--;
					}
				sub++;
			}
			current++;
		}
	}
	
	private static boolean IsPointInLine(Location p1, Location p2, Location p3)
	{
		// Все 3 точки на одной из осей X или Y.
		if(p1.x == p3.x && p3.x == p2.x || p1.y == p3.y && p3.y == p2.y)
			return true;
		// Условие ниже выполнится если все 3 точки выстроены по диагонали.
		// Это работает потому, что сравниваем мы соседние точки (расстояния между ними равны, важен только знак).
		// Для случая с произвольными точками работать не будет.
		if((p1.x - p2.x) * (p1.y - p2.y) == (p2.x - p3.x) * (p2.y - p3.y))
			return true;
		return false;
	}
}
