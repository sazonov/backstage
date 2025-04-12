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

package com.proit.app.database.configuration.jpa.eclipselink.uuid;

import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sequencing.Sequence;

import java.util.UUID;
import java.util.Vector;

public class UuidSequence extends Sequence
{
	public UuidSequence()
	{
		super();

		shouldAlwaysOverrideExistingValue = false;
	}

	public UuidSequence(String name)
	{
		super(name);
	}

	@Override
	public Object getGeneratedValue(Accessor accessor, AbstractSession writeSession, String seqName)
	{
		return UUID.randomUUID();
	}

	@Override
	public Vector getGeneratedVector(Accessor accessor, AbstractSession writeSession, String seqName, int size)
	{
		return null;
	}

	@Override
	public void onConnect()
	{
	}

	@Override
	public void onDisconnect()
	{
	}

	@Override
	public boolean shouldAcquireValueAfterInsert()
	{
		return false;
	}

	@Override
	public boolean shouldUseTransaction()
	{
		return false;
	}

	@Override
	public boolean shouldUsePreallocation()
	{
		return false;
	}
}
