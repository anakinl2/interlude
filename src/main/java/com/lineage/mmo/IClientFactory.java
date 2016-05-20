package com.lineage.mmo;

public interface IClientFactory<T extends MMOClient<?>>
{
	public T create(MMOConnection<T> con);
}
