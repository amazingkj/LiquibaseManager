package com.dbtest.liquibase.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.dbtest.liquibase.service.DataSourceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSourceInitializer implements ApplicationListener<ApplicationReadyEvent> {

	private final DataSourceService dataSourceService;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		dataSourceService.initializeDataSources();
	}
}