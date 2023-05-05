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

package com.proit.app.repository.generic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

public class CustomJpaRepositoryFactoryBean<T extends JpaRepository<S, I>, S, I extends Serializable> extends JpaRepositoryFactoryBean<T, S, I>
{
	public CustomJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface)
	{
		super(repositoryInterface);
	}

	protected RepositoryFactorySupport createRepositoryFactory(EntityManager em)
	{
		return new CustomJpaRepositoryFactoryFactory(em);
	}

	private static class CustomJpaRepositoryFactoryFactory extends JpaRepositoryFactory
	{
		public CustomJpaRepositoryFactoryFactory(EntityManager em)
		{
			super(em);
		}

		protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata)
		{
			return CustomJpaRepositoryImpl.class;
		}
	}
}

