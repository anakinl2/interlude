package com.lineage.game.loginservercon.lspackets;

import java.util.logging.Logger;

import com.lineage.game.loginservercon.AttLS;
import com.lineage.game.loginservercon.LSConnection;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 22:11:59
 */
public class RSAKey extends LoginServerBasePacket
{
	private static final Logger log = Logger.getLogger(RSAKey.class.getName());

	public RSAKey(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		getLoginServer().initRSA(readB(128));
		getLoginServer().initCrypt();

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: RSAKey packet readed.");
	}
}
