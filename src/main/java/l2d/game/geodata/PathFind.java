package l2d.game.geodata;

import java.util.ArrayList;

import com.lineage.Config;
import l2d.game.model.L2Object;
import com.lineage.util.Location;

public class PathFind
{
	private static final byte NSWE_NONE = 0, EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8, NSWE_ALL = 15;
	private ArrayList<PathFindBuffers.GeoNode> path;
	private PathFindBuffers.PathFindBuffer buff;

	public PathFind(int x, int y, int z, int destX, int destY, int destZ, L2Object obj)
	{
		Location startpoint = Config.PATHFIND_BOOST == 0 ? new Location(x, y, z) : GeoEngine.moveCheckWithCollision(x, y, z, destX, destY, true);
		Location native_endpoint = new Location(destX, destY, destZ);
		Location endpoint = Config.PATHFIND_BOOST == 2 && Math.abs(destZ - z) <= 200 ? GeoEngine.moveCheckBackwardWithCollision(destX, destY, destZ, startpoint.x, startpoint.y, true) : native_endpoint.clone();
		startpoint.world2geo();
		native_endpoint.world2geo();
		endpoint.world2geo();
		startpoint.setZ(GeoEngine.NgetHeight(startpoint.x, startpoint.y, startpoint.z));
		endpoint.setZ(GeoEngine.NgetHeight(endpoint.x, endpoint.y, endpoint.z));
		int xdiff = Math.abs(endpoint.x - startpoint.x);
		int ydiff = Math.abs(endpoint.y - startpoint.y);
		if((buff = PathFindBuffers.alloc(64 + Math.max(xdiff, ydiff), obj.isPlayer() || obj.isPet() || obj.isSummon(), startpoint, endpoint, native_endpoint)) != null)
		{
			path = findPath();
			buff.free();
		}
	}

	private PathFindBuffers.GeoNode getBestOpenNode()
	{
		PathFindBuffers.GeoNode bestNodeLink = null;
		PathFindBuffers.GeoNode oldNode = buff.firstNode;
		PathFindBuffers.GeoNode nextNode = buff.firstNode.link;

		while(nextNode != null)
		{
			if(bestNodeLink == null || nextNode.score < bestNodeLink.link.score)
				bestNodeLink = oldNode;
			oldNode = nextNode;
			nextNode = nextNode.link;
		}

		if(bestNodeLink != null)
		{
			PathFindBuffers.GeoNode bestNode = bestNodeLink.link;
			bestNodeLink.link.closed = true;
			bestNodeLink.link = bestNode.link;
			if(bestNode == buff.currentNode)
				buff.currentNode = bestNodeLink;
			return bestNode;
		}

		return null;
	}

	private ArrayList<PathFindBuffers.GeoNode> tracePath(PathFindBuffers.GeoNode f)
	{
		ArrayList<PathFindBuffers.GeoNode> nodes = new ArrayList<PathFindBuffers.GeoNode>();
		do
		{
			nodes.add(0, f);
			f = f.parent;
		} while(f.parent != null);
		return nodes;
	}

	public ArrayList<PathFindBuffers.GeoNode> findPath()
	{
		buff.firstNode = PathFindBuffers.GeoNode.initNode(buff, buff.startpoint.x - buff.offsetX, buff.startpoint.y - buff.offsetY, buff.startpoint, null);
		PathFindBuffers.GeoNode nextNode = buff.firstNode, finish = null;
		buff.firstNode.closed = true;
		int i = buff.info.maxIterations;
		while(nextNode != null && i-- > 0)
		{
			if((finish = handleNode(nextNode)) != null)
				return tracePath(finish);
			nextNode = getBestOpenNode();
		}

		return null;
	}

	public PathFindBuffers.GeoNode handleNode(PathFindBuffers.GeoNode node)
	{
		PathFindBuffers.GeoNode result = null;
		int clX = node._x;
		int clY = node._y;
		short clZ = node._z;
		byte NSWE = GeoEngine.NgetNSWE(clX, clY, clZ);

		if(Config.PATHFIND_DIAGONAL)
		{
			if((NSWE & SOUTH) == SOUTH && (NSWE & EAST) == EAST)
			{
				getHeightAndNSWE(clX + 1, clY, clZ);
				if((buff.hNSWE[1] & SOUTH) == SOUTH)
				{
					getHeightAndNSWE(clX, clY + 1, clZ);
					if((buff.hNSWE[1] & EAST) == EAST)
					{
						result = getNeighbour(clX + 1, clY + 1, node, true);
						if(result != null)
							return result;
					}
				}
			}

			if((NSWE & SOUTH) == SOUTH && (NSWE & WEST) == WEST)
			{
				getHeightAndNSWE(clX - 1, clY, clZ);
				if((buff.hNSWE[1] & SOUTH) == SOUTH)
				{
					getHeightAndNSWE(clX, clY + 1, clZ);
					if((buff.hNSWE[1] & WEST) == WEST)
					{
						result = getNeighbour(clX - 1, clY + 1, node, true);
						if(result != null)
							return result;
					}
				}
			}

			if((NSWE & 0x8) == 8 && (NSWE & EAST) == EAST)
			{
				getHeightAndNSWE(clX + 1, clY, clZ);
				if((buff.hNSWE[1] & NORTH) == NORTH)
				{
					getHeightAndNSWE(clX, clY - 1, clZ);
					if((buff.hNSWE[1] & EAST) == EAST)
					{
						result = getNeighbour(clX + 1, clY - 1, node, true);
						if(result != null)
							return result;
					}
				}
			}

			if((NSWE & NORTH) == NORTH && (NSWE & WEST) == WEST)
			{
				getHeightAndNSWE(clX - 1, clY, clZ);
				if((buff.hNSWE[1] & NORTH) == NORTH)
				{
					getHeightAndNSWE(clX, clY - 1, clZ);
					if((buff.hNSWE[1] & WEST) == WEST)
					{
						result = getNeighbour(clX - 1, clY - 1, node, true);
						if(result != null)
							return result;
					}
				}
			}
		}

		if((NSWE & EAST) == EAST)
		{
			result = getNeighbour(clX + 1, clY, node, false);
			if(result != null)
				return result;
		}
		if((NSWE & WEST) == WEST)
		{
			result = getNeighbour(clX - 1, clY, node, false);
			if(result != null)
				return result;
		}
		if((NSWE & SOUTH) == SOUTH)
		{
			result = getNeighbour(clX, clY + 1, node, false);
			if(result != null)
				return result;
		}
		if((NSWE & NORTH) == NORTH)
			result = getNeighbour(clX, clY - 1, node, false);
		return result;
	}

	public PathFindBuffers.GeoNode getNeighbour(int x, int y, PathFindBuffers.GeoNode from, boolean d)
	{
		int nX = x - buff.offsetX, nY = y - buff.offsetY;

		if(nX >= buff.info.MapSize || nX < 0 || nY >= buff.info.MapSize || nY < 0)
			return null;

		boolean isOldNull = PathFindBuffers.GeoNode.isNull(buff.nodes[nX][nY]);

		if(!isOldNull && buff.nodes[nX][nY].closed)
			return null;

		PathFindBuffers.GeoNode n = isOldNull ? PathFindBuffers.GeoNode.initNode(buff, nX, nY, x, y, from._z, from) : buff.tempNode.reuse(buff.nodes[nX][nY], from);

		int height = Math.abs(n._z - from._z);

		if(height > Config.MAX_Z_DIFF || n._nswe == NSWE_NONE)
			return null;

		double weight = d ? 1.414213562373095D * Config.WEIGHT0 : Config.WEIGHT0;

		if(n._nswe != NSWE_ALL || height > 16)
			weight = Config.WEIGHT1;

		if(buff.isPlayer || Config.SIMPLE_PATHFIND_FOR_MOBS)
		{
			getHeightAndNSWE(x + 1, y, n._z);
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += Config.WEIGHT2;
			getHeightAndNSWE(x - 1, y, n._z);
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += Config.WEIGHT2;
			getHeightAndNSWE(x, y + 1, n._z);
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += Config.WEIGHT2;
			getHeightAndNSWE(x, y - 1, n._z);
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += Config.WEIGHT2;
		}

		int diffx = buff.endpoint.x - x;
		int diffy = buff.endpoint.y - y;
		int dz = Math.abs(buff.endpoint.z - n._z);

		n.moveCost += from.moveCost + weight;
		n.score = n.moveCost + Math.sqrt(diffx * diffx + diffy * diffy + dz * dz / 256);
		if(x == buff.endpoint.x && y == buff.endpoint.y && dz < 64)
			return n;
		if(isOldNull)
		{
			if(buff.currentNode == null)
				buff.firstNode.link = n;
			else
				buff.currentNode.link = n;
			buff.currentNode = n;
		}
		else if(n.moveCost < buff.nodes[nX][nY].moveCost)
			buff.nodes[nX][nY].copy(n);
		return null;
	}

	private void getHeightAndNSWE(int x, int y, short z)
	{
		int nX = x - buff.offsetX;
		int nY = y - buff.offsetY;
		if(nX >= buff.info.MapSize || nX < 0 || nY >= buff.info.MapSize || nY < 0)
		{
			buff.hNSWE[1] = NSWE_NONE;
			return;
		}
		PathFindBuffers.GeoNode n = buff.nodes[nX][nY];

		if(n == null)
			n = PathFindBuffers.GeoNode.initNodeGeo(buff, nX, nY, x, y, z);

		buff.hNSWE[0] = n._z;
		buff.hNSWE[1] = n._nswe;
	}

	public ArrayList<PathFindBuffers.GeoNode> getPath()
	{
		return path;
	}
}
