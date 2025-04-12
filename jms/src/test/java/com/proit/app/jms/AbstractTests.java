package com.proit.app.jms;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = TestApplication.class)
@ImportAutoConfiguration(classes = {JmsAutoConfiguration.class, JacksonAutoConfiguration.class})
public abstract class AbstractTests
{
}
