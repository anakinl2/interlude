package com.lineage.db;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import com.lineage.Config;

/**
 * <p>При работе с пулами коннектов иногда возникает ситуация - когда выбираешь весь пул до предела и
 * при этом коннекты не закрываются а требуется получить еще один коннект. В этом случае программа
 * зависает. Так бывает если в процессе выполнения одного запроса при переборке результатов вызывается
 * другая функция, которая также берет коннект из базы данных. Таких вложений может быть много. И коннекты
 * не отпускаются, пока не выполнятся самые глубокие запросы. DBCP и C3P0 висли при этом - опробовано на
 * практике.
 * </p>
 * <p>Для того чтобы избежать этой коллизии пишется оболочка для коннекта, которой коннект
 * делегирует все свои методы. Эта оболочка хранится в локальном пуле коннектов и если коннект запрашивается
 * в потоке - для которого был уже открыт коннект и еще не закрыт, то возвращаем его.
 * </p>
 * Эту возможность можно отключить выставив в настройках сервера UseDatabaseLayer = false;
 */
public class L2DatabaseFactory
{
	private static L2DatabaseFactory _instance;
	private ComboPooledDataSource _source;

	//список используемых на данный момент коннектов
	private final Hashtable<String, ThreadConnection> Connections = new Hashtable<String, ThreadConnection>();

	static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());

	public L2DatabaseFactory() throws SQLException
	{
		_instance = this;
		try
		{
			if(Config.DATABASE_MAX_CONNECTIONS < 2)
			{
				Config.DATABASE_MAX_CONNECTIONS = 2;
				_log.warning("at least " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
			}

			Class.forName(Config.DATABASE_DRIVER).newInstance();

			if(Config.DEBUG)
				_log.fine("Database Connection Working");

			_source = new ComboPooledDataSource();
			_source.setDriverClass(Config.DATABASE_DRIVER); //loads the jdbc driver
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD); // the settings below are optional -- c3p0 can work with defaults
			_source.setAutoCommitOnClose(true);
			_source.setInitialPoolSize(1);
			_source.setMinPoolSize(1);
			_source.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
			_source.setAcquireRetryAttempts(0);// try to obtain Connections indefinitely (0 = never quit)
			_source.setAcquireRetryDelay(100);// 500 miliseconds wait before try to acquire connection again
			_source.setCheckoutTimeout(0); // 0 = wait indefinitely for new connection
			_source.setAcquireIncrement(5); // if pool is exhausted, get 5 more Connections at a time
			_source.setMaxStatements(100);
			_source.setIdleConnectionTestPeriod(Config.DATABASE_IDLE_TEST_PERIOD); // test idle connection every 1 minute
			_source.setMaxIdleTime(Config.DATABASE_MAX_IDLE_TIMEOUT); // remove unused connection after 10 minutes
			_source.setNumHelperThreads(5);
			_source.setBreakAfterAcquireFailure(false);

			/* Test the connection */
			_source.getConnection().close();
		}
		catch(SQLException x)
		{
			if(Config.DEBUG)
				_log.fine("Database Connection FAILED");
			// rethrow the exception
			throw x;
		}
		catch(Exception e)
		{
			if(Config.DEBUG)
				_log.fine("Database Connection FAILED");
			throw new SQLException("could not init DB connection:" + e);
		}
	}

	public static L2DatabaseFactory getInstance() throws SQLException
	{
		if(_instance == null)
			new L2DatabaseFactory();
		return _instance;
	}

	public ThreadConnection getConnection() throws SQLException
	{
		ThreadConnection connection;
		if(Config.USE_DATABASE_LAYER)
		{
			String key = generateKey();
			//Пробуем получить коннект из списка уже используемых. Если для данного потока уже открыт
			//коннект - не мучаем пул коннектов, а отдаем этот коннект.
			connection = Connections.get(key);
			if(connection == null)
				try
				{
					//не нашли - открываем новый
					connection = new ThreadConnection(_source.getConnection());
				}
				catch(SQLException e)
				{
					_log.warning("Couldn't create connection. Cause: " + e.getMessage());
				}
			else
				//нашли - увеличиваем счетчик использования
				connection.updateCounter();

			//добавляем коннект в список
			if(connection != null)
				synchronized (Connections)
				{
					Connections.put(key, connection);
				}
		}
		else
			connection = new ThreadConnection(_source.getConnection());
		return connection;
	}

	public Hashtable<String, ThreadConnection> getConnections()
	{
		return Connections;
	}

	public void shutdown()
	{
		_source.close();
		Connections.clear();
		try
		{
			DataSources.destroy(_source);
		}
		catch(SQLException e)
		{
			_log.log(Level.INFO, "", e);
		}
	}

	/**
	 * Генерация ключа для хранения коннекта
	 *
	 * Ключ равен хэш-коду текущего потока
	 *
	 * @return сгенерированный ключ.
	 */
	public String generateKey()
	{
		return String.valueOf(Thread.currentThread().hashCode());
	}
}