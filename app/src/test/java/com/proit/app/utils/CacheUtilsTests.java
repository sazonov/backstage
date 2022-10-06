package com.proit.app.utils;

import com.proit.app.utils.proxy.ForceProxy;
import com.proit.app.utils.proxy.NoProxy;
import com.proit.app.utils.proxy.ReadOnlyObjectProxyFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CacheUtilsTests
{
	@Data
	@NoArgsConstructor
	public static class CacheItem
	{
		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class InnerItem
		{
			private String id;
		}

		private String id;

		@NoProxy
		private String ignoredId;

		private List<String> ignoredList;

		@ForceProxy
		private List<String> basicList;

		@ForceProxy
		private List<InnerItem> complexList;

		private Map<String, String> ignoredMap;

		@ForceProxy
		private Map<String, String> basicMap;

		@ForceProxy
		private Map<String, InnerItem> complexMap;

		public CacheItem(String id)
		{
			this.id = id;
			this.ignoredId = id;

			this.ignoredList = List.of(id);
			this.basicList = List.of(id);
			this.complexList = List.of(new InnerItem(id));

			this.ignoredMap = Map.of(id, id);
			this.basicMap = Map.of(id, id);
			this.complexMap = Map.of(id, new InnerItem(id));
		}
	}

	@Test
	public void checkReadOnlyProxy()
	{
		// TODO: проверки для @Entity
		var sourceItem = new CacheItem(UUID.randomUUID().toString());
		var cachedItem = ReadOnlyObjectProxyFactory.createProxy(sourceItem, CacheItem.class);

		assertEquals(sourceItem.getId(), cachedItem.getId());
		assertNotNull(sourceItem.getIgnoredId());
		assertNull(cachedItem.getIgnoredId());

		assertNotNull(sourceItem.getIgnoredList());
		assertNull(cachedItem.getIgnoredList());
		assertIterableEquals(sourceItem.getBasicList(), cachedItem.getBasicList());
		assertIterableEquals(sourceItem.getComplexList(), cachedItem.getComplexList());

		assertNotNull(sourceItem.getIgnoredMap());
		assertNull(cachedItem.getIgnoredMap());
		assertIterableEquals(sourceItem.getBasicMap().entrySet(), cachedItem.getBasicMap().entrySet());
		assertIterableEquals(sourceItem.getComplexMap().entrySet(), cachedItem.getComplexMap().entrySet());
	}
}
