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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;

import io.github.alexopa.cukereportconverter.model.cuke.CukeEmbedding;
import io.github.alexopa.cukereportconverter.model.cuke.CukeScenario;
import io.github.alexopa.cukereportconverter.model.cuke.CukeStep;
import io.github.alexopa.cukereportconverter.model.cuke.CukeStepResult;
import io.github.alexopa.cukereportconverter.model.cuke.CukeStepSection;
import io.github.alexopa.cukereportportal.client.ReportPortalClient;
import io.github.alexopa.cukereportportal.client.model.AddFileAttachmentProperties;
import io.github.alexopa.cukereportportal.client.model.AddLogProperties;
import io.github.alexopa.cukereportportal.client.model.FinishItemProperties;
import io.github.alexopa.cukereportportal.client.model.StartItemProperties;
import io.github.alexopa.cukereportportal.config.RPImporterPropertyHandler;
import io.github.alexopa.cukereportportal.util.MarkdownUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class that is used to import a {@link CukeScenario} to ReportPortal
 */
@Slf4j
@RequiredArgsConstructor
class CukeScenarioImporter implements Callable<Boolean> {

	private static final String DOCSTRING_DECORATOR = "\n\"\"\"\n";

	private final CukeScenario scenario;
	private final RPImporterPropertyHandler propertyHandler;
	private final ReportPortalClient rpClient;
	private final String launchUuid;
	private final String featureItemUuid;
	
	@Override
	public Boolean call() throws Exception {
		
		log.info("Importing scenario: {}", scenario.getName());
		
		EntryCreatedAsyncRS scenarioItemId = rpClient.startItem(startScenarioProperties(launchUuid, featureItemUuid, scenario));
		
		writeSteps(scenario, scenario.getStartTimestamp(), scenario.getBeforeSteps(), rpClient,
				launchUuid, scenarioItemId.getId());
		writeSteps(scenario, scenario.getStartTimestamp().plusNanos(scenario.getBeforeStepsDuration()),
				scenario.getBackgroundSteps(), rpClient, launchUuid, scenarioItemId.getId());
		writeSteps(scenario,
				scenario.getStartTimestamp().plusNanos(scenario.getBeforeStepsDuration())
						.plusNanos(scenario.getBackgroundStepsDuration()),
				scenario.getScenarioSteps(), rpClient, launchUuid, scenarioItemId.getId());
		writeSteps(scenario,
				scenario.getStartTimestamp().plusNanos(scenario.getBeforeStepsDuration())
						.plusNanos(scenario.getBackgroundStepsDuration())
						.plusNanos(scenario.getScenarioStepsDuration()),
				scenario.getAfterSteps(), rpClient, launchUuid, scenarioItemId.getId());
						
		rpClient.finishItem(finishScenarioProperties(propertyHandler.getLaunchName(), scenarioItemId.getId(), scenario));
		
		return true;
	}

	private void writeSteps(CukeScenario scenario, LocalDateTime sectionStartTime, List<CukeStep> steps, ReportPortalClient rpClient, String launchUuid, String scenarioUuid) {
		if (steps == null || steps.isEmpty()) {
			return;
		}
		
		EntryCreatedAsyncRS stepsContainerItemId = rpClient.startItem(startStepsContainerProperties(launchUuid, scenarioUuid, steps.get(0), sectionStartTime));
		
		for (CukeStep step : steps) {
			
			if (step.getBeforeSteps() != null) {
				for (CukeStep beforeStep: step.getBeforeSteps()) {
					EntryCreatedAsyncRS beforestepItemId = rpClient
							.startItem(startStepProperties(launchUuid, stepsContainerItemId.getId(), beforeStep, sectionStartTime));
					rpClient.finishItem(finishStepProperties(launchUuid, beforestepItemId.getId(), sectionStartTime, beforeStep.getResult().name()));
					sectionStartTime = sectionStartTime.plusNanos(beforeStep.getDuration());
				}
			}
			
			EntryCreatedAsyncRS stepItemId = rpClient
					.startItem(startStepProperties(launchUuid, stepsContainerItemId.getId(), step, sectionStartTime));
			
			if (step.getTableData() != null) {
				String stepLog = MarkdownUtils.formatDataTable(step.getTableData());
				if (StringUtils.isNotBlank(stepLog)) {
					rpClient.addLog(AddLogProperties.builder().launchId(launchUuid).itemId(stepItemId.getId())
							.level("INFO").time(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC))).message(stepLog)
							.build());
				}
			}
			if (StringUtils.isNotBlank(step.getDocString())) {
				rpClient.addLog(AddLogProperties.builder().launchId(launchUuid).itemId(stepItemId.getId()).level("INFO")
						.time(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
						.message(DOCSTRING_DECORATOR + step.getDocString() + DOCSTRING_DECORATOR).build());
			}	
			if (StringUtils.isNotBlank(step.getErrorMessage())) {
				rpClient.addLog(AddLogProperties.builder().launchId(launchUuid).itemId(stepItemId.getId())
						.level("ERROR").time(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
						.message(step.getErrorMessage()).build());
			}
			
			if (step.getEmbeddings() != null) {
				for (CukeEmbedding embedding: step.getEmbeddings()) {
					if (embedding.getMimeType().equals("image/png")) {
						
						byte[] decodedImg = Base64.getDecoder()
			                    .decode(embedding.getData().getBytes(StandardCharsets.UTF_8));
						File tmpImgFile = null;
						try {
							tmpImgFile = File.createTempFile("rp_" + embedding.getName().replace(" ", "_") + "_", "_img-attach.png", new File("/tmp"));
							tmpImgFile.deleteOnExit();
						} catch (IOException e) {
							log.error("Could not create tmp file for attachment under /tmp folder:", e);
							continue;
						}
						try {
							Files.write(Paths.get(tmpImgFile.getAbsolutePath()), decodedImg);
						} catch (IOException e) {
							log.error("Could not write tmp file for attachment under /tmp folder:", e);
							continue;
						}
						
						rpClient.addFileAttachment(AddFileAttachmentProperties.builder().launchUuid(launchUuid)
								.itemUuid(stepItemId.getId()).level("INFO")
								.time(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
								.message(tmpImgFile.getName()).fullPath(tmpImgFile.getAbsolutePath()).build());
					} else if (embedding.getMimeType().equals("text/plain")) {
						byte[] decodedData = Base64.getDecoder()
			                    .decode(embedding.getData().getBytes(StandardCharsets.UTF_8));
						String text = new String(decodedData);
						
						
						rpClient.addLog(AddLogProperties.builder().launchId(launchUuid).itemId(stepItemId.getId())
								.level("INFO").time(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
								.message(String.format("%s: %s", embedding.getName(), text)).build());
					}
				}
			}
			
			sectionStartTime = sectionStartTime.plusNanos(step.getDuration());

			if (step.getAfterSteps() != null) {
				for (CukeStep afterStep: step.getAfterSteps()) {
					EntryCreatedAsyncRS afterStepItemId = rpClient
							.startItem(startStepProperties(launchUuid, stepsContainerItemId.getId(), afterStep, sectionStartTime));
					rpClient.finishItem(finishStepProperties(launchUuid, afterStepItemId.getId(), sectionStartTime, afterStep.getResult().name()));
					sectionStartTime = sectionStartTime.plusNanos(afterStep.getDuration());
				}
			}
			
			rpClient.finishItem(finishStepProperties(launchUuid, stepItemId.getId(), sectionStartTime, step.getResult().name()));
			
		}

		rpClient.finishItem(finishStepProperties(launchUuid, stepsContainerItemId.getId(), sectionStartTime,
				steps.stream().allMatch(s -> s.getResult() == CukeStepResult.PASSED) ? "passed" : "failed"));
		
	}
	
	private StartItemProperties startScenarioProperties(String launchUuid, String featureUuid, CukeScenario scenario) {
		return StartItemProperties.builder()
				.launchUuid(launchUuid)
				.parentUuid(featureUuid)
				.name(String.format("%s: %s", scenario.getType().getText(), scenario.getName()))
				.startTime(Date.from(scenario.getStartTimestamp().toInstant(ZoneOffset.UTC)))
				.attributes(scenario.getTags().stream().collect(Collectors.joining(";")))
				.type("STEP")
				.description(scenario.getDescription())
				.codeRef(String.format("%s:%s", scenario.getParent().getCodeRef(), scenario.getLine()))
				.build();
	}
	
	private FinishItemProperties finishScenarioProperties(String launchUuid, String scenarioUuid, CukeScenario scenario) {
		return FinishItemProperties.builder()
				.launchUuid(launchUuid)
				.itemUuid(scenarioUuid)
				.endTime(Date.from(scenario.getStartTimestamp().plusNanos(scenario.getTotalDuration()).toInstant(ZoneOffset.UTC)))
				.status(scenario.getResult().name())
				.build();
	}
	
	private StartItemProperties startStepsContainerProperties(String launchUuid, String scenarioUuid, CukeStep step, LocalDateTime sectionStartTime) {
		return StartItemProperties.builder()
				.launchUuid(launchUuid)
				.parentUuid(scenarioUuid)
				.name(generateStepContainerName(step))
				.startTime(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
				.type(getStepType(step))
				.hasStats(false)
				.build();
	}
	
	private StartItemProperties startStepProperties(String launchUuid, String scenarioUuid, CukeStep step, LocalDateTime sectionStartTime) {
		return StartItemProperties.builder()
				.launchUuid(launchUuid)
				.parentUuid(scenarioUuid)
				.name(generateStepName(step))
				.startTime(Date.from(sectionStartTime.toInstant(ZoneOffset.UTC)))
				.type(getStepType(step))
				.hasStats(false)
				.codeRef(String.format("%s:%s", step.getParent().getParent().getCodeRef(), step.getLine()))
				.build();
	}
	
	private FinishItemProperties finishStepProperties(String launchUuid, String stepUuid, LocalDateTime endTime, String status) {
		return FinishItemProperties.builder()
				.launchUuid(launchUuid)
				.itemUuid(stepUuid)
				.endTime(Date.from(endTime.toInstant(ZoneOffset.UTC)))
				.status(status)
				.build();
	}
	
	private String generateStepContainerName(CukeStep step) {
		if (step.getStepSection() == CukeStepSection.BEFORE_SCENARIO) {
			return "Before Hooks";
		} else if (step.getStepSection() == CukeStepSection.BACKGROUND) {
			return "Background";
		} else if (step.getStepSection() == CukeStepSection.SCENARIO) {
			return "Scenario";
		} else if (step.getStepSection() == CukeStepSection.AFTER_SCENARIO) {
			return "After Hooks";
		}
		return "";
	}
	
	private String generateStepName(CukeStep step) {
		if (step.getStepSection() == CukeStepSection.BEFORE_SCENARIO) {
			return String.format("%s: %s", "Before Hook", step.getMatch().getLocation());
		} else if (step.getStepSection() == CukeStepSection.BACKGROUND) {
			return String.format("%s %s", step.getKeyword().trim(), step.getName());
		} else if (step.getStepSection() == CukeStepSection.BEFORE_STEP) {
			return String.format("%s: %s", "Before Step", step.getMatch().getLocation());
		} else if (step.getStepSection() == CukeStepSection.AFTER_STEP) {
			return String.format("%s: %s", "After Step", step.getMatch().getLocation());
		} else if (step.getStepSection() == CukeStepSection.SCENARIO) {
			return String.format("%s %s", step.getKeyword().trim(), step.getName());
		} else if (step.getStepSection() == CukeStepSection.AFTER_SCENARIO) {
			return String.format("%s: %s", "After Hook", step.getMatch().getLocation());
		}
		return "";
	}
	
	private String getStepType(CukeStep step) {
		if (step.getStepSection() == CukeStepSection.BEFORE_SCENARIO) {
			return "before_test";
		} else if (step.getStepSection() == CukeStepSection.BACKGROUND) {
			return "test";
		} else if (step.getStepSection() == CukeStepSection.SCENARIO) {
			return "test";
		} else if (step.getStepSection() == CukeStepSection.AFTER_SCENARIO) {
			return "after_test";
		} else if (step.getStepSection() == CukeStepSection.BEFORE_STEP) {
			return "before_method";
		} else if (step.getStepSection() == CukeStepSection.AFTER_STEP) {
			return "after_method";
		}
		throw new NotImplementedException("Missing return value for step type: " + step.getStepSection());
	}
}
