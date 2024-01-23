package com.proit.app.dict.exception.dict;

public class DictStorageMigrationException extends DictException
{
	public DictStorageMigrationException(String dictId, String message)
	{
		super("При миграции справочника '%s' произошла ошибка: %s".formatted(dictId, message));
	}

	public DictStorageMigrationException(String dictId, Throwable throwable)
	{
		super("При миграции справочника '%s' произошла ошибка.".formatted(dictId), throwable);
	}
}
