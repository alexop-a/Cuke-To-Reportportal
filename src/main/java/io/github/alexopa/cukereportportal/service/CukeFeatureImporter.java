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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import io.github.alexopa.cukereportconverter.model.cuke.CukeFeature;
import io.github.alexopa.cukereportconverter.model.cuke.CukeScenario;
import io.github.alexopa.cukereportconverter.model.cuke.CukeScenarioResult;
import io.github.alexopa.cukereportportal.config.RPImporterPropertyHandler;
import io.github.alexopa.reportportalclient.RPClient;
import io.github.alexopa.reportportalclient.model.testitem.FinishTestItemProperties;
import io.github.alexopa.reportportalclient.model.testitem.StartTestItemProperties;
import io.github.alexopa.reportportalclient.rpmodel.EntryCreatedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class that is used to import a {@link CukeFeature} to ReportPortal
 */
@Slf4j
@RequiredArgsConstructor 
class CukeFeatureImporter implements Callable<Boolean> {

	private final RPImporterPropertyHandler propertyHandler;
	private final String launchUuid;
	private final CukeFeature cukeFeature;
	private final RPClient rpClient;

	@Override
	public Boolean call() throws Exception {
		log.info("Importing feature: {}", cukeFeature.getName());

		EntryCreatedResponse featureItemId = rpClient.startItem(startFeatureProperties(launchUuid, cukeFeature));

		ExecutorService executorService = Executors.newFixedThreadPool(propertyHandler.getThreadsScenarios());
		List<Future<Boolean>> listOfScenarios = new ArrayList<>();

		for (CukeScenario scenario : cukeFeature.getScenarios()) {
			CukeScenarioImporter cukeScenarioImporter = new CukeScenarioImporter(scenario, propertyHandler, rpClient,
					launchUuid, featureItemId.getId());
			listOfScenarios.add(executorService.submit(cukeScenarioImporter));
		}

		for (Future<Boolean> sc : listOfScenarios) {

			try {
				sc.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Failed to get result from CukeScenarioImporter", e);
			}
		}
		executorService.shutdown();

		rpClient.finishItem(finishFeatureProperties(launchUuid, featureItemId.getId(), cukeFeature));

		return true;
	}

	private StartTestItemProperties startFeatureProperties(String launchUuid, CukeFeature feature) {
		return StartTestItemProperties.builder().launchUuid(launchUuid)
				.name(String.format("Feature: %s", feature.getName()))
				.startTime(Date.from(feature.getMinScenarioStartTime().toInstant(ZoneOffset.UTC)))
				.attributes(feature.getTags().stream().collect(Collectors.joining(";"))).type("STORY")
				.description(feature.getDescription()).codeRef(feature.getCodeRef()).build();
	}

	private FinishTestItemProperties finishFeatureProperties(String launchUuid, String featureUuid, CukeFeature feature) {
		return FinishTestItemProperties.builder().launchUuid(launchUuid).itemUuid(featureUuid)
				.endTime(Date.from(feature.getMaxScenarioEndTime().toInstant(ZoneOffset.UTC)))
				.status(feature.getScenarios().stream().allMatch(s -> s.getResult() == CukeScenarioResult.PASSED)
						? "passed"
						: "failed")
				.build();
	}

}
