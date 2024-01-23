package com.proit.app.utils;

import lombok.experimental.UtilityClass;
import org.springframework.boot.SpringApplication;

@UtilityClass
public class SpringApplicationUtils
{
	public static void runWithBufferingApplicationStartup(String[] args, Class<?>... primarySources)
	{
		var app = new SpringApplication(primarySources);

		app.setApplicationStartup(ActuatorUtils.buildBufferingApplicationStartup());
		app.run(args);
	}
}
