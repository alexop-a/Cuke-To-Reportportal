/*
 * (C) Copyright 2024 Andreas Alexopoulos (https://alexop-a.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.alexopa.cukereportportal.service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import io.github.alexopa.cukereportconverter.model.cuke.CukeFeature;
import io.github.alexopa.cukereportconverter.model.cuke.CukeTestRun;
import io.github.alexopa.cukereportconverter.service.CukeConverter;
import io.github.alexopa.cukereportportal.config.RPImporterPropertyHandler;
import io.github.alexopa.cukereportportal.util.Utils;
import io.github.alexopa.reportportalclient.RPClient;
import io.github.alexopa.reportportalclient.config.RPClientConfig;
import io.github.alexopa.reportportalclient.model.launch.FinishLaunchProperties;
import io.github.alexopa.reportportalclient.model.launch.StartLaunchProperties;
import io.github.alexopa.reportportalclient.model.log.AddFileAttachmentProperties;
import io.github.alexopa.reportportalclient.rpmodel.FinishLaunchResponse;
import io.github.alexopa.reportportalclient.rpmodel.StartLaunchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class that is used to import cucumber json reports to ReportPortal
 */
@Slf4j
@RequiredArgsConstructor
public class ReportPortalImporter {

	private final RPImporterPropertyHandler propertyHandler;

	/**
	 * Method that imports cucumber json files to ReportPortal. The report files to
	 * be imported will be retrieved from the properties
	 * 
	 * @return A {@link String} with the launch uuid that was created
	 */
	public String importCucumberReports() {
		List<String> jsonReports = propertyHandler.getCucumberJsonFiles();
		CukeConverter cukeConverter = new CukeConverter();
		CukeTestRun testRun = cukeConverter.convertToTestRun(
				jsonReports.stream().map(Utils::getFile).filter(Objects::nonNull).collect(Collectors.toList()));
		return importReport(testRun);
	}

	/**
	 * Method that imports a {@link CukeTestRun} instance to ReportPortal. In this
	 * case, even if cucumber report files are defined in properties, they are
	 * ignored.
	 * 
	 * @param testRun The {@link CukeTestRun} instance to import
	 * @return A {@link String} with the launch uuid that was created
	 */
	public String importReport(CukeTestRun testRun) {

		if (CollectionUtils.isEmpty(testRun.getFeatures())) {
			log.warn("No feature exists for the test-run. Cannot import...");
			return null;
		}

		RPClientConfig rpClientConfig = new RPClientConfig();
		rpClientConfig.setEndpoint(propertyHandler.getReportPortalEndpoint());
		rpClientConfig.setApiKey(propertyHandler.getReportPortalApiKey());
		rpClientConfig.setProject(propertyHandler.getReportPortalProjectName());
		RPClient rpClient = new RPClient(rpClientConfig);
		
		StartLaunchResponse launchRS = rpClient.startLaunch(launchProperties(testRun));
		log.info("Importing reports in new launch with uuid {}", launchRS.getId());

		propertyHandler.getLaunchAttachments().stream().filter(StringUtils::isNoneBlank).map(Utils::getFile)
				.filter(Objects::nonNull)
				.forEach(f -> rpClient
						.addFileAttachment(AddFileAttachmentProperties.builder().launchUuid(launchRS.getId())
								.level("INFO").time(Date.from(testRun.getStartTime().toInstant(ZoneOffset.UTC)))
								.message(f.getName()).fullPath(f.getAbsolutePath()).build()));

		ExecutorService executorService = Executors.newFixedThreadPool(propertyHandler.getThreadsFeatures());
		List<Future<Boolean>> listOfFuture = new ArrayList<>();

		for (CukeFeature f : testRun.getFeatures()) {
			CukeFeatureImporter cukeFeatureImporter = new CukeFeatureImporter(propertyHandler, launchRS.getId(), f,
					rpClient);
			listOfFuture.add(executorService.submit(cukeFeatureImporter));
		}

		for (Future<Boolean> fut : listOfFuture) {
			try {
				fut.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Failed to get result from CukeFeatureImporter", e);
				Thread.currentThread().interrupt();
			}

		}
		executorService.shutdown();

		FinishLaunchResponse finishRs = rpClient.finishLaunch(FinishLaunchProperties.builder().launchUuid(launchRS.getId())
				.endTime(Date.from(testRun.getEndTime().toInstant(ZoneOffset.UTC))).build());

		log.info("Finishing import of launch {}. Link: {}", launchRS.getId(), finishRs.getLink());

		return launchRS.getId();
	}

	private StartLaunchProperties launchProperties(CukeTestRun testRun) {
		return StartLaunchProperties.builder().name(propertyHandler.getLaunchName())
				.description(propertyHandler.getLaunchDescription())
				.startTime(Date.from(testRun.getStartTime().toInstant(ZoneOffset.UTC)))
				.attributes(propertyHandler.getLaunchAttributes()).rerunOf(propertyHandler.getLaunchRerunOf())
				.mode(propertyHandler.getLaunchMode()).build();
	}

}
