package com.proit.app.cache.configuration.distributed;

import com.proit.app.cache.configuration.properties.CacheProperties;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class DistributedOperationsHelper
{
	boolean isPropagatingPut(Set<CacheProperties.DistributedCacheOperation> operations)
	{
		return operations != null && operations.contains(CacheProperties.DistributedCacheOperation.ANY);
	}
}
