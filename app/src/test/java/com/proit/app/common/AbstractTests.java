package com.proit.app.common;

import com.proit.app.TestApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = TestApplication.class)
@ImportAutoConfiguration(classes = {JmsAutoConfiguration.class, JacksonAutoConfiguration.class})
public abstract class AbstractTests
{
}
