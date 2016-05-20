package items;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.MercTicketManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.instances.L2ItemInstance;

public class MercTicket implements IItemHandler, ScriptFile
{
	// all the items ids that this handler knowns
	private static final int[] _itemIds = {
	// Gludio
			3960,
			3961,
			3962,
			3963,
			3964,
			3965,
			3966,
			3967,
			3968,
			3969, // Mercenary
			// 3970, 3971, 3972, //Teleporter
			6038,
			6039,
			6040,
			6041,
			6042,
			6043,
			6044,
			6045,
			6046,
			6047, // Greater Mercenary
			6115,
			6116,
			6117,
			6118,
			6119,
			6120,
			6121,
			6122,
			6123,
			6124, // Dawn Mercenary
			6175,
			6176,
			6177,
			6178,
			6179,
			6180,
			6181,
			6182,
			6183,
			6184, // Greater Recruit
			6235,
			6236,
			6237,
			6238,
			6239,
			6240,
			6241,
			6242,
			6243,
			6244, // Recruit
			6295,
			6296, // Nephilim

			// Dion
			3973,
			3974,
			3975,
			3976,
			3977,
			3978,
			3979,
			3980,
			3981,
			3982, // Mercenary
			// 3983, 3984, 3985, //Teleporter
			6051,
			6052,
			6053,
			6054,
			6055,
			6056,
			6057,
			6058,
			6059,
			6060, // Greater Mercenary
			6125,
			6126,
			6127,
			6128,
			6129,
			6130,
			6131,
			6132,
			6133,
			6134, // Dawn Mercenary
			6185,
			6186,
			6187,
			6188,
			6189,
			6190,
			6191,
			6192,
			6193,
			6194, // Greater Recruit
			6245,
			6246,
			6247,
			6248,
			6249,
			6250,
			6251,
			6252,
			6253,
			6254, // Recruit
			6297,
			6298, // Nephilim

			// Giran
			3986,
			3987,
			3988,
			3989,
			3990,
			3991,
			3992,
			3993,
			3994,
			3995, // Mercenary
			// 3996, 3997, 3998, //Teleporter
			6064,
			6065,
			6066,
			6067,
			6068,
			6069,
			6070,
			6071,
			6072,
			6073, // Greater Mercenary
			6135,
			6136,
			6137,
			6138,
			6139,
			6140,
			6141,
			6142,
			6143,
			6144, // Dawn Mercenary
			6195,
			6196,
			6197,
			6198,
			6199,
			6200,
			6201,
			6202,
			6203,
			6204, // Greater Recruit
			6255,
			6256,
			6257,
			6258,
			6259,
			6260,
			6261,
			6262,
			6263,
			6264, // Recruit
			6299,
			6300, // Nephilim

			// Oren
			3999,
			4000,
			4001,
			4002,
			4003,
			4004,
			4005,
			4006,
			4007,
			4008, // Mercenary
			// 4009, 4010, 4011, //Teleporter
			6077,
			6078,
			6079,
			6080,
			6081,
			6082,
			6083,
			6084,
			6085,
			6086, // Greater Mercenary
			6145,
			6146,
			6147,
			6148,
			6149,
			6150,
			6151,
			6152,
			6153,
			6154, // Dawn Mercenary
			6205,
			6206,
			6207,
			6208,
			6209,
			6210,
			6211,
			6212,
			6213,
			6214, // Greater Recruit
			6265,
			6266,
			6267,
			6268,
			6269,
			6270,
			6271,
			6272,
			6273,
			6274, // Recruit
			6301,
			6302, // Nephilim

			// Aden
			4012,
			4013,
			4014,
			4015,
			4016,
			4017,
			4018,
			4019,
			4020,
			4021, // Mercenary
			// 4022, 4023, 4024, 4025, 4026, //Teleporter
			6090,
			6091,
			6092,
			6093,
			6094,
			6095,
			6096,
			6097,
			6098,
			6099, // Greater Mercenary
			6155,
			6156,
			6157,
			6158,
			6159,
			6160,
			6161,
			6162,
			6163,
			6164, // Dawn Mercenary
			6215,
			6216,
			6217,
			6218,
			6219,
			6220,
			6221,
			6222,
			6223,
			6224, // Greater Recruit
			6275,
			6276,
			6277,
			6278,
			6279,
			6280,
			6281,
			6282,
			6283,
			6284, // Recruit
			6303,
			6304, // Nephilim

			// Innadril
			5205,
			5206,
			5207,
			5208,
			5209,
			5210,
			5211,
			5212,
			5213,
			5214, // Mercenary
			// 5215, //Teleporter
			6105,
			6106,
			6107,
			6108,
			6109,
			6110,
			6111,
			6112,
			6113,
			6114, // Greater Mercenary
			6165,
			6166,
			6167,
			6168,
			6169,
			6170,
			6171,
			6172,
			6173,
			6174, // Dawn Mercenary
			6225,
			6226,
			6227,
			6228,
			6229,
			6230,
			6231,
			6232,
			6233,
			6234, // Greater Recruit
			6285,
			6286,
			6287,
			6288,
			6289,
			6290,
			6291,
			6292,
			6293,
			6294, // Recruit
			6305,
			6306, // Nephilim

			// Goddard
			6779,
			6780,
			6781,
			6782,
			6783,
			6784,
			6785,
			6786,
			6787,
			6788, // Mercenary
			// 6789, 6790, 6791, //Teleporter
			6792,
			6793,
			6794,
			6795,
			6796,
			6797,
			6798,
			6799,
			6800,
			6801, // Greater Mercenary
			6802,
			6803,
			6804,
			6805,
			6806,
			6807,
			6808,
			6809,
			6810,
			6811, // Dawn Mercenary
			6812,
			6813,
			6814,
			6815,
			6816,
			6817,
			6818,
			6819,
			6820,
			6821, // Greater Recruit
			6822,
			6823,
			6824,
			6825,
			6826,
			6827,
			6828,
			6829,
			6830,
			6831, // Recruit
			6832,
			6833, // Nephilim

			// Schuttgart
			7918,
			7919,
			7920,
			7921,
			7922,
			7923,
			7924,
			7925,
			7926,
			7927, // Mercenary
			// 7928, 7929, 7930, //Teleporter
			7931,
			7932,
			7933,
			7934,
			7935,
			7936,
			7937,
			7938,
			7939,
			7940, // Greater Mercenary
			7941,
			7942,
			7943,
			7944,
			7945,
			7946,
			7947,
			7948,
			7949,
			7950, // Dawn Mercenary
			7951,
			7952,
			7953,
			7954,
			7955,
			7956,
			7957,
			7958,
			7959,
			7960, // Greater Recruit
			7961,
			7962,
			7963,
			7964,
			7965,
			7966,
			7967,
			7968,
			7969,
			7970, // Recruit
			7971,
			7972, // Nephilim

			// Rune
			7973,
			7974,
			7975,
			7976,
			7977,
			7978,
			7979,
			7980,
			7981,
			7982, // Mercenary
			// 7983, 7984, 7985, 7986, 7987, //Teleporter
			7988,
			7989,
			7990,
			7991,
			7992,
			7993,
			7994,
			7995,
			7996,
			7997, // Greater Mercenary
			7998,
			7999,
			8000,
			8001,
			8002,
			8003,
			8004,
			8005,
			8006,
			8007, // Dawn Mercenary
			8008,
			8009,
			8010,
			8011,
			8012,
			8013,
			8014,
			8015,
			8016,
			8017, // Greater Recruit
			8018,
			8019,
			8020,
			8021,
			8022,
			8023,
			8024,
			8025,
			8026,
			8027, // Recruit
			8028,
			8029 // Nephilim
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		int itemId = item.getItemId();
		Residence castle = CastleManager.getInstance().getCastleByObject(player);

		if(castle == null)
			return;

		int castleId = castle.getId();

		// add check that certain tickets can only be placed in certain castles
		if(MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
			switch(castleId)
			{
				case 1:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Gludio"));
					return;
				case 2:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Dion"));
					return;
				case 3:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Giran"));
					return;
				case 4:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Oren"));
					return;
				case 5:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Aden"));
					return;
				case 6:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Innadril"));
					return;
				case 7:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Goddard"));
					return;
				case 8:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Rune"));
					return;
				case 9:
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnlyIn", player).addString("Schuttgart"));
					return;
				default:
					// player is not in a castle
					player.sendMessage(new CustomMessage("scripts.items.MercTicket.TicketOnly", player));
					return;
			}

		if((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) != L2Clan.CP_CS_MERCENARIES)
		{
			player.sendMessage("You don't have rights to do this.");
			return;
		}

		if(castle.getSiege().isInProgress())
		{
			player.sendMessage(new CustomMessage("scripts.items.MercTicket.SiegeInProgress", player));
			return;
		}

		if(MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()))
		{
			player.sendMessage(new CustomMessage("scripts.items.MercTicket.NoMore", player));
			return;
		}

		if(MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			player.sendMessage(new CustomMessage("scripts.items.MercTicket.NoMoreType", player));
			return;
		}

		MercTicketManager.getInstance().addTicket(item.getItemId(), player);
		player.getInventory().destroyItem(item, 1, true);
	}

	public final int[] getItemIds()
	{
		return _itemIds;
	}

	@Override
	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}
}