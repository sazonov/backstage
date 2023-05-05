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

package com.proit.app.configuration.session;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@Slf4j
@NoArgsConstructor
public class JdkSerializationRedisSerializerNoSerializationEx extends JdkSerializationRedisSerializer
{
	public JdkSerializationRedisSerializerNoSerializationEx(ClassLoader classLoader)
	{
		super(classLoader);
	}

	// workaround for this issue: https://github.com/spring-projects/spring-security/issues/9204
	@Override
	public Object deserialize(byte[] bytes)
	{
		try
		{
			return super.deserialize(bytes);
		}
		catch (SerializationException e)
		{
			if (e.getCause() instanceof SerializationFailedException failedException)
			{
				log.error(failedException.getMessage(), failedException);

				return null;
			}

			throw e;
		}
		catch (SerializationFailedException e)
		{
			if (e.getRootCause() instanceof ClassNotFoundException notFoundException)
			{
				log.error(notFoundException.getMessage(), notFoundException);

				return null;
			}
			throw e;
		}
	}
}
