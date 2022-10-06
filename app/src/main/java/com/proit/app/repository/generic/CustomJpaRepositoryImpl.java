/*
 *    Copyright 2019-2022 the original author or authors.
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

package com.proit.app.repository.generic;

import com.proit.app.exception.ObjectNotFoundException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public class CustomJpaRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements CustomJpaRepository<T, ID>
{
	private final EntityManager em;
	private final Class<T> domainClass;
	private final String selectAllIdsQuery;
	private final Class<ID> idClass;

	public CustomJpaRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager)
	{
		super(entityInformation, entityManager);

		this.em = entityManager;
		this.domainClass = entityInformation.getJavaType();

		SingularAttribute<? super T, ?> idAttribute = entityInformation.getIdAttribute();

		if (idAttribute == null)
		{
			throw new RuntimeException("failed to get id attribute");
		}

		this.idClass = (Class<ID>) idAttribute.getJavaType();
		this.selectAllIdsQuery = String.format("select t.%s from %s t", idAttribute.getName(), entityInformation.getEntityName());
	}

	public T findByIdEx(ID id)
	{
		return findById(id).orElseThrow(() -> new ObjectNotFoundException(domainClass, id));
	}

	public T findByIdNoEx(ID id)
	{
		return findById(id).orElse(null);
	}

	public List<ID> findAllIds()
	{
		return em.createQuery(selectAllIdsQuery, idClass).getResultList();
	}

	public List<T> findAll(Collection<ID> ids)
	{
		return findAll((root, query, cb) -> root.get("id").in(ids));
	}

	@Transactional
	@Override
	public void deleteById(@NonNull ID id)
	{
		delete(findById(id).orElseThrow(() -> new ObjectNotFoundException(domainClass, id)));
	}

	public EntityManager getEntityManager()
	{
		return em;
	}

	public void lock(T entity, LockModeType lockModeType)
	{
		em.lock(entity, lockModeType);
	}

	public void detach(T entity)
	{
		em.detach(entity);
	}

	public Query createQuery(String queryString)
	{
		return em.createQuery(queryString);
	}

	public TypedQuery<T> createQuery(String queryString, Class<T> clazz)
	{
		return em.createQuery(queryString, clazz);
	}
}

