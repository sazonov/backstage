package com.proit.app.model.dto.scheduledjob.request;

import com.proit.app.model.dto.scheduledjob.JobTriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
public class RescheduleJobRequest
{
	@NotBlank
	@Schema(description = "Наименование работы", required = true)
	private String jobName;

	@NotNull
	@Schema(description = "Тип триггера (CRON/INTERVAL)", required = true)
	private JobTriggerType triggerType;

	@NotBlank
	@Schema(description = "Выражение для триггера", required = true)
	private String expression;
}
