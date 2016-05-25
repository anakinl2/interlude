package com.lineage.ext.listeners.engine;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.PropertyChangeListener;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.ext.listeners.MethodInvokeListener;
import com.lineage.ext.listeners.events.PropertyEvent;

/**
 * Интерфейс для движка слушателей.
 * Идея заключается в том что для каждого объекта можно добавить свой движок слушателей.
 * В результате мы получаем гибкую систему для управления событиями в сервере.
 *
 * @author Death
 */
public interface ListenerEngine<T>
{
	/**
	 * Добавляет слушатель свойсв в общую коллекцию.
	 * @param listener слушатель
	 */
	void addPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Убирает слушаеть свойств с общей коллекции.
	 * @param listener слушаетль
	 */
	void removePropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Добавляет слушатель свойств определенному свойсву
	 * @param value свойство
	 * @param listener слушатель
	 */
	void addPropertyChangeListener(PropertyCollection value, PropertyChangeListener listener);

	/**
	 * Убирает слушатель свойств у определенного свойства
	 * @param value свойство
	 * @param listener слушатель
	 */
	void removePropertyChangeListener(PropertyCollection value, PropertyChangeListener listener);

	/**
	 * Запускает уведомление всех слушателей об изменении свойства
	 * Используется стандартный класс PropertyChangeEvent
	 * @param value свойство
	 * @param source объект от которого запущено
	 * @param oldValue старое значение
	 * @param newValue новое значение
	 */
	void firePropertyChanged(PropertyCollection value, T source, Object oldValue, Object newValue);

	/**
	 * Запускает уведомление всех слушателей об изменении свойства
	 * @param event Определенный ивенд для передачи.
	 */
	void firePropertyChanged(PropertyEvent event);

	/**
	 * Добавляет свойство в коллекцию.
	 * @param property свойство
	 * @param value значение
	 */
	void addProperty(PropertyCollection property, Object value);

	/**
	 * Возвращает значение свойства
	 * @param property свойство
	 * @return значение
	 */
	Object getProperty(PropertyCollection property);

	/**
	 * Возвращает обьект - владельца даного движка слушателей
	 * @return владелец инстанса слушаетелей
	 */
	T getOwner();

	/**
	 * Добавляет слушатель на вызов определенный метод
	 * @param listener слушатель
	 */
	void addMethodInvokedListener(MethodInvokeListener listener);

	/**
	 * Убирает определенный слушатель методов
	 * @param listener слушатель
	 */
	void removeMethodInvokedListener(MethodInvokeListener listener);

	/**
	 * Добавляет слушатель на вызов определенный метод
	 * @param listener слушатель
	 * @param methodName имя метода
	 */
	void addMethodInvokedListener(MethodCollection methodName, MethodInvokeListener listener);

	/**
	 * Убирает определенный слушатель методов
	 * @param listener слушатель
	 * @param methodName имя метода
	 */
	void removeMethodInvokedListener(MethodCollection methodName, MethodInvokeListener listener);

	/**
	 * Вызывает слушатели и делает им нотифай события
	 * @param event событие
	 */
	void fireMethodInvoked(MethodEvent event);

	/**
	 * Запускает нотифай слушателям что был вызван метод.
	 * @param methodName имя метода
	 * @param source источник у кого он был вызван
	 * @param args аргументы метода
	 */
	void fireMethodInvoked(MethodCollection methodName, T source, Object[] args);
}
