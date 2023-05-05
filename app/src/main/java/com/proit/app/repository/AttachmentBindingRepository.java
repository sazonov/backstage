/*
 *    Copyright 2019-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.proit.app.repository;

import com.proit.app.configuration.properties.AttachmentProperties;
import com.proit.app.model.domain.Attachment;
import com.proit.app.model.domain.AttachmentBinding;
import com.proit.app.repository.generic.CustomJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
public interface AttachmentBindingRepository extends CustomJpaRepository<AttachmentBinding, String>
{
	@Query("select b.attachment.id, b from AttachmentBinding b where b.attachment.id in :attachmentIds")
	List<Object[]> findByAttachmentIdsRaw(@Param("attachmentIds") Collection<String> attachmentIds);

	default MultiValueMap<String, AttachmentBinding> findByAttachmentIds(Collection<String> attachmentIds)
	{
		LinkedMultiValueMap<String, AttachmentBinding> result = new LinkedMultiValueMap<>();

		if (attachmentIds.isEmpty())
		{
			return result;
		}

		findByAttachmentIdsRaw(attachmentIds).forEach(it -> {
			result.add((String) it[0], (AttachmentBinding) it[1]);
		});

		return result;
	}

	@Query("select a from AttachmentBinding b left join b.attachment a where b.type = :type and b.objectId = :objectId")
	List<Attachment> findAttachmentsByTypeAndObjectId(@Param("type") String type, @Param("objectId") String objectId);

	@Query("select a.id from AttachmentBinding b left join b.attachment a where b.type = :type and b.objectId = :objectId")
	List<String> findAttachmentIdsByTypeAndObjectId(@Param("type") String type, @Param("objectId") String objectId);

	@Query("select b.objectId, a from AttachmentBinding b left join b.attachment a where b.type = :type and b.objectId in :objectIds")
	List<Object[]> findAttachmentsByTypeAndObjectIdsRaw(@Param("type") String type, @Param("objectIds") Collection<String> objectIds);

	default Map<String, List<Attachment>> findAttachmentsByTypeAndObjectIds(String type, Collection<String> objectIds)
	{
		if (objectIds.isEmpty())
		{
			return Collections.emptyMap();
		}

		Map<String, List<Attachment>> result = new HashMap<>();

		findAttachmentsByTypeAndObjectIdsRaw(type, objectIds).forEach(it -> {
			var objectId = (String) it[0];

			if (!result.containsKey(objectId))
			{
				result.put(objectId, new LinkedList<>());
			}

			result.get(objectId).add((Attachment) it[1]);
		});

		return result;
	}

	@Modifying
	int deleteByAttachmentId(String attachmentId);

	@Modifying
	int deleteByAttachmentIdAndUserIdAndTypeAndObjectId(String attachmentId, String userId, String type, String objectId);

	@Modifying
	int deleteByTypeAndObjectId(String type, String objectId);
}
