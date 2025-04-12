/*
 *    Copyright 2019-2024 the original author or authors.
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

package com.proit.app.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface CustomJpaRepository<T, I extends Serializable> extends JpaRepository<T, I>
{
	/**
	 * То же, что и {@link #findById(Object)}, но кидает исключение, если объект не найден.
	 */
	T findByIdEx(I id);

	/**
	 * То же, что и {@link #findById(Object)}, но возвращает null, если объект не найден.
	 */
	T findByIdNoEx(I id);

	List<T> findAll(Collection<I> ids);

	/**
	 * Получение списка всех идентификаторов объектов в репозитории.
	 */
	List<I> findAllIds();

	/**
	 * Возвращает используемый {@link EntityManager}.
	 */
	EntityManager getEntityManager();

	/**
	 * Прокси для {@link EntityManager#lock(Object, LockModeType).}.
	 */
	void lock(T entity, LockModeType lockModeType);

	/**
	 * Прокси для {@link EntityManager#detach(Object).}.
	 */
	void detach(T entity);

	/**
	 * Прокси для {@link EntityManager#createQuery(String).}
	 */
	Query createQuery(String queryString);

	/**
	 * Прокси для {@link EntityManager#createQuery(String, Class).}
	 */
	TypedQuery<T> createQuery(String queryString, Class<T> clazz);
}

