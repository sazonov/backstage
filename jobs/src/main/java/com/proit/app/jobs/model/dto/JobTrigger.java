package com.proit.app.jobs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
@Schema(description = "План расписания для указанной задачи")
public class JobTrigger
{
	@Schema(description = "Тип триггера (CRON/INTERVAL)")
	private JobTriggerType triggerType;

	@Schema(description = "Выражение для триггера")
	private String expression;
}
