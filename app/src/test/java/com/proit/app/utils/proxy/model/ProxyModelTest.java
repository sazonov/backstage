package com.proit.app.utils.proxy.model;

import com.proit.app.utils.proxy.ForceProxy;
import com.proit.app.utils.proxy.NoProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProxyModelTest
{
	public static class AbstractAbstractEntity
	{
		private String field1;
	}

	public static class AbstractEntity extends AbstractAbstractEntity
	{
		private String field2;
	}

	public static class Entity extends AbstractEntity
	{
		private String field3;
	}

	public static class EntityWithNoProxyField
	{
		@NoProxy
		private String field;
	}

	public static class EntityWithNoProxyTypeField
	{
		private NoProxyType field;
	}

	@NoProxy
	public static class NoProxyType
	{
		private String field;
	}

	/**
	 * Поля, которые помечены как static/final/transient, а также поля, название которых начинается с "_"
	 * не проксируются
	 */
	public static class SpecifiedFieldsEntity
	{
		private final String finalField = "";
		private static String staticField;
		private transient String transientField;
		private String _field;
	}

	public static class CollectionFieldsEntity
	{
		private Set<String> field;
	}

	public static class MapFieldsEntity
	{
		private Map<String, String> field;
	}

	public static class ArrayFieldsEntity
	{
		private String[] field;
	}

	public static class EntityFieldsEntity
	{
		private EntityClass field;
	}

	public static class EntityForceProxyFieldsEntity
	{
		@ForceProxy
		private EntityClass field;
	}

	@Test
	public void constructorClazzTest()
	{
		var item = new ProxyModel(AbstractAbstractEntity.class);

		assertEquals(AbstractAbstractEntity.class, item.getClazz());
	}

	@Test
	public void constructorWithoutInheritanceTest()
	{
		var item = new ProxyModel(AbstractAbstractEntity.class);

		assertEquals(1, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithInheritanceTest()
	{
		var item = new ProxyModel(AbstractEntity.class);

		assertEquals(2, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithMultipleInheritanceTest()
	{
		var item = new ProxyModel(Entity.class);

		assertEquals(3, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithNoProxyTypeFieldTest()
	{
		var item = new ProxyModel(EntityWithNoProxyTypeField.class);

		assertEquals(0, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithNoProxyFieldTest()
	{
		var item = new ProxyModel(EntityWithNoProxyField.class);

		assertEquals(0, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithSpecifiedFieldsEntity()
	{
		var item = new ProxyModel(SpecifiedFieldsEntity.class);

		assertEquals(0, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithCollectionFieldsEntity()
	{
		var item = new ProxyModel(CollectionFieldsEntity.class);

		assertEquals(CollectionFieldProxy.class, item.getFieldProxies().get(0).getClass());
	}

	@Test
	public void constructorWithMapFieldsEntity()
	{
		var item = new ProxyModel(MapFieldsEntity.class);

		assertEquals(MapFieldProxy.class, item.getFieldProxies().get(0).getClass());
	}

	@Test
	public void constructorWithArrayFieldsEntity()
	{
		assertThrows(RuntimeException.class, () -> new ProxyModel(ArrayFieldsEntity.class));
	}

	@Test
	public void constructorWithEntityFieldsEntity()
	{
		var item = new ProxyModel(EntityFieldsEntity.class);

		assertEquals(0, item.getFieldProxies().size());
	}

	@Test
	public void constructorWithEntityForceProxyFieldsEntity()
	{
		var item = new ProxyModel(EntityForceProxyFieldsEntity.class);

		assertEquals(EntityFieldProxy.class, item.getFieldProxies().get(0).getClass());
	}

	@Test
	public void constructorWithBasicFieldsEntity()
	{
		var item = new ProxyModel(AbstractAbstractEntity.class);

		assertEquals(BasicFieldProxy.class, item.getFieldProxies().get(0).getClass());
	}

	@Test
//	TODO: Покрыть тестами функциональность наследников AbstractFieldProxy в соответствующих классах
	public void processWithBasicFieldsEntity()
	{
		var model = new ProxyModel(AbstractAbstractEntity.class);

		var item = new AbstractAbstractEntity();
		item.field1 = "field1";

		var result = (AbstractAbstractEntity) model.process(item);

		assertEquals("field1", result.field1);
	}
}