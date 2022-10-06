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
