package l2d.ext.network;

public interface IClientFactory<T extends MMOClient<?>>
{
	public T create(MMOConnection<T> con);
}
