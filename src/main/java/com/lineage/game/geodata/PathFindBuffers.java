package com.lineage.game.geodata;

import java.util.HashMap;
import java.util.Iterator;

import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.StrTable;

public class PathFindBuffers
{
	private static BufferInfo all_buffers[];

	public static class BufferInfo
	{
		final int MapSize;
		final int sqMapSize;
		final int maxIterations;
		final int index;
		public int overBuffers;
		public int totalUses;
		public int playableUses;
		public double useTimeMillis;
		public GArray<PathFindBuffer> buffers;

		BufferInfo(int mapSize, int idx, int buffersCount)
		{
			overBuffers = 0;
			totalUses = 0;
			playableUses = 0;
			useTimeMillis = 0.0D;
			buffers = new GArray<PathFindBuffer>(buffersCount);
			MapSize = mapSize;
			sqMapSize = mapSize * mapSize;
			index = idx;
			if(sqMapSize <= 10000)
				maxIterations = sqMapSize / 2;
			else if(sqMapSize < 30000)
				maxIterations = sqMapSize / 3;
			else
				maxIterations = sqMapSize / 4;
		}
	}

	public static class PathFindBuffer
	{
		final short hNSWE[] = new short[2];
		final GeoNode nodes[][];
		final BufferInfo info;
		boolean isPlayer;
		boolean inUse;
		Location startpoint;
		Location endpoint;
		Location native_endpoint;
		int offsetX;
		int offsetY;
		private long useStartedNanos;
		GeoNode firstNode;
		GeoNode currentNode;
		GeoNode tempNode;

		PathFindBuffer(BufferInfo inf)
		{
			nodes = new GeoNode[inf.MapSize][inf.MapSize];
			tempNode = new GeoNode();
			info = inf;
		}

		public void free()
		{
			if(!inUse)
				return;

			for(int i = 0; i < nodes.length; i++)
				for(int j = 0; j < nodes[i].length; j++)
					if(nodes[i][j] != null)
						nodes[i][j].free();

			firstNode = null;
			currentNode = null;
			endpoint = null;
			currentNode = null;
			info.totalUses++;
			if(isPlayer)
				info.playableUses++;
			info.useTimeMillis += (System.nanoTime() - useStartedNanos) / 1000000D;
			inUse = false;
		}
	}

	public static class GeoNode
	{
		public int _x;
		public int _y;
		public short _z;
		public short _nswe;
		public double score;
		public double moveCost;
		public boolean closed;
		public GeoNode link;
		public GeoNode parent;

		GeoNode()
		{
			score = 0.0D;
			moveCost = 0.0D;
			closed = false;
			link = null;
			parent = null;
		}

		public void free()
		{
			score = -1D;
			link = null;
			parent = null;
		}

		public static GeoNode initNode(PathFindBuffer buff, int bx, int by, int x, int y, short z, GeoNode parentNode)
		{
			GeoNode result;
			if(buff == null)
			{
				result = new GeoNode();
				result._x = x;
				result._y = y;
				result._z = z;
				result.moveCost = 0;
				result.parent = parentNode;
				result.score = 0.0D;
				result.closed = false;
				return result;
			}
			if(buff.nodes[bx][by] == null)
				buff.nodes[bx][by] = new GeoNode();
			result = buff.nodes[bx][by];
			if(result._x != x || result._y != y || result._z == 0 || Math.abs(z - result._z) > 64)
			{
				GeoEngine.NgetHeightAndNSWE(x, y, z, buff.hNSWE);
				result._x = x;
				result._y = y;
				result._z = buff.hNSWE[0];
				result._nswe = buff.hNSWE[1];
			}
			result.moveCost = 0;
			result.parent = parentNode;
			result.score = 0.0D;
			result.closed = false;
			return result;
		}

		public static GeoNode initNode(PathFindBuffer buff, int bx, int by, Location startPoing, GeoNode parentNode)
		{
			return initNode(buff, bx, by, startPoing.x, startPoing.y, (short) startPoing.z, parentNode);
		}

		public static GeoNode initNode(int x, int y, short z, int mCost, GeoNode parentNode)
		{
			return initNode(null, 0, 0, x, y, z, parentNode);
		}

		public static GeoNode initNode(Location loc, GeoNode parentNode)
		{
			return initNode(null, 0, 0, loc.x, loc.y, (short) loc.z, parentNode);
		}

		public static boolean isNull(GeoNode node)
		{
			return node == null || node.score == -1D;
		}

		public static GeoNode initNodeGeo(PathFindBuffer buff, int bx, int by, int x, int y, short z)
		{
			if(buff.nodes[bx][by] == null)
				buff.nodes[bx][by] = new GeoNode();
			GeoNode result = buff.nodes[bx][by];
			GeoEngine.NgetHeightAndNSWE(x, y, z, buff.hNSWE);
			result._x = x;
			result._y = y;
			result._z = buff.hNSWE[0];
			result._nswe = buff.hNSWE[1];
			result.score = -1D;
			return result;
		}

		public GeoNode reuse(GeoNode old, GeoNode parentNode)
		{
			_x = old._x;
			_y = old._y;
			_z = old._z;
			_nswe = old._nswe;
			moveCost = 0;
			closed = old.closed;
			parent = parentNode;
			return this;
		}

		public void copy(GeoNode old)
		{
			_x = old._x;
			_y = old._y;
			_z = old._z;
			_nswe = old._nswe;
			moveCost = old.moveCost;
			score = old.score;
			closed = old.closed;
		}

		@Override
		public String toString()
		{
			return "GeoNode: " + _x + "\t" + _y + "\t" + _z;
		}
	}

	PathFindBuffers()
	{}

	public static void initBuffers(String s)
	{

		HashMap<Integer, Integer> conf_data = new HashMap<Integer, Integer>();
		String str[] = s.split(";");
		for(int i = 0; i < str.length; i++)
		{
			String e = str[i];
			String k[];
			if(!e.isEmpty() && (k = e.split("x")).length == 2)
				conf_data.put(Integer.valueOf(k[1]), Integer.valueOf(k[0]));
		}

		BufferInfo _allbuffers[] = new BufferInfo[conf_data.size()];
		for(int idx = 0; !conf_data.isEmpty(); idx++)
		{
			Integer lowestKey = null;
			Iterator<Integer> it = conf_data.keySet().iterator();
			do
			{
				if(!it.hasNext())
					break;
				Integer ke = it.next();
				if(lowestKey == null || lowestKey.intValue() > ke.intValue())
					lowestKey = ke;
			} while(true);

			_allbuffers[idx] = new BufferInfo(lowestKey, idx, conf_data.remove(lowestKey).intValue());
		}

		all_buffers = _allbuffers;
	}

	public static boolean resizeBuffers(int MapSize, int newCapacity)
	{
		if(newCapacity < 1)
			return false;
		for(int i = 0; i < all_buffers.length; i++)
			if(MapSize == all_buffers[i].MapSize)
			{
				if(newCapacity == all_buffers[i].buffers.getCapacity())
					return true;
				GArray<PathFindBuffer> new_buffers = new GArray<PathFindBuffer>(newCapacity);
				synchronized (all_buffers[i])
				{
					for(; all_buffers[i].buffers.size() > newCapacity; all_buffers[i].buffers.removeLast());
					new_buffers.addAll(all_buffers[i].buffers);
					all_buffers[i].buffers = new_buffers;
				}
				return true;
			}

		return false;
	}

	private static PathFindBuffer alloc(BufferInfo fine_buffer)
	{
		synchronized (fine_buffer)
		{
			for(Iterator<?> i = fine_buffer.buffers.iterator(); i.hasNext();)
			{
				PathFindBuffer b = (PathFindBuffer) i.next();
				if(!b.inUse)
				{
					b.inUse = true;
					return b;
				}
			}

			PathFindBuffer result = new PathFindBuffer(fine_buffer);
			if(fine_buffer.buffers.size() < fine_buffer.buffers.getCapacity())
			{
				result.inUse = true;
				fine_buffer.buffers.add(result);
			}
			else
				fine_buffer.overBuffers++;
			return result;
		}
	}

	public static PathFindBuffer alloc(int mapSize, boolean isPlayer, Location startpoint, Location endpoint, Location native_endpoint)
	{
		if(mapSize % 2 > 0)
			mapSize--;
		BufferInfo fine_buffer = null;
		int i = 0;
		do
		{
			if(i >= all_buffers.length)
				break;
			if(mapSize <= all_buffers[i].MapSize)
			{
				fine_buffer = all_buffers[i];
				mapSize = all_buffers[i].MapSize;
				break;
			}
			i++;
		} while(true);
		if(fine_buffer == null)
			return null;
		else
		{
			PathFindBuffer result = alloc(fine_buffer);
			result.useStartedNanos = System.nanoTime();
			result.isPlayer = isPlayer;
			result.startpoint = startpoint;
			result.endpoint = endpoint;
			result.native_endpoint = native_endpoint;
			result.offsetX = startpoint.x - mapSize / 2;
			result.offsetY = startpoint.y - mapSize / 2;
			return result;
		}
	}

	public static StrTable getStats()
	{
		StrTable table = new StrTable("PathFind Buffers Stats");
		long pathFindsTotal = 0L;
		long pathFindsPlayable = 0L;
		double allTimeMillis = 0.0D;
		BufferInfo arr$[] = all_buffers;
		int len$ = arr$.length;
		for(int i$ = 0; i$ < len$; i$++)
		{
			BufferInfo buff = arr$[i$];
			pathFindsTotal += buff.totalUses;
			pathFindsPlayable += buff.playableUses;
			allTimeMillis += buff.useTimeMillis;
			long inUse = 0L;
			synchronized (buff)
			{
				Iterator<?> i = buff.buffers.iterator();
				do
				{
					if(!i.hasNext())
						break;
					PathFindBuffer b = (PathFindBuffer) i.next();
					if(b.inUse)
						inUse++;
				} while(true);
			}
			table.set(buff.index, "MapSize", new StringBuilder().append(buff.MapSize).append("x").append(buff.MapSize).toString());
			table.set(buff.index, "Use", Long.valueOf(inUse));
			table.set(buff.index, "Uses", Integer.valueOf(buff.totalUses));
			table.set(buff.index, "Alloc", new StringBuilder().append(buff.buffers.size()).append(" of ").append(buff.buffers.getCapacity()).toString());
			table.set(buff.index, "unbuf", Integer.valueOf(buff.overBuffers));
			if(buff.totalUses > 0)
				table.set(buff.index, "Avg ms", String.format("%1.3f", new Object[] { Double.valueOf(buff.useTimeMillis / buff.totalUses) }));
		}

		table.addTitle(new StringBuilder().append("Uses Total / Playable  : ").append(pathFindsTotal).append(" / ").append(pathFindsPlayable).toString());
		table.addTitle(new StringBuilder().append("Time Total(s) / Avg(ms): ").append(String.format("%1.2f", new Object[] { Double.valueOf(allTimeMillis / 1000D) })).append(" / ").append(String.format("%1.3f", new Object[] { Double.valueOf(allTimeMillis / pathFindsTotal) })).toString());
		return table;
	}

}
