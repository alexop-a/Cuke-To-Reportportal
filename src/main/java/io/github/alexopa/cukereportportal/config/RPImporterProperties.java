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
package io.github.alexopa.cukereportportal.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An enum with the available properties that can configure the service, with
 * their default values
 */
@RequiredArgsConstructor
@Getter
public enum RPImporterProperties {

	/**
	 * Property that defines the cucumber report json files that will be imported
	 * for a launch
	 */
	RP_IMPORTER_CUCUMBER_JSON_FILES("rp.importer.cucumberJsonFiles", ""),

	/**
	 * Property that defines the launch name
	 */
	RP_IMPORTER_LAUNCH_NAME("rp.importer.launch.name", ""),

	/**
	 * Property that defines the attributes (if any) for the launch. The attributes
	 * should be separated with semi-colon and each attribute should define the
	 * key/value pair separate with colon, ie:
	 * <code>attr1key:value1;attr2key:value2</code>
	 */
	RP_IMPORTER_LAUNCH_ATTRIBUTES("rp.importer.launch.attributes", ""),

	/**
	 * Property that defines the attachments (if any) for the launch. The
	 * attachments should be separated with semi-colon and each attachment should be
	 * the full path of the file, ie:
	 * <code>/home/rp/file-attach1.txt;/home/rp/file-attach2.json</code>
	 */
	RP_IMPORTER_LAUNCH_ATTACHMENTS("rp.importer.launch.attachments", ""),

	/**
	 * Property that defines the description (if any) for the launch
	 */
	RP_IMPORTER_LAUNCH_DESCRIPTION("rp.importer.launch.description", ""),

	/**
	 * Property that defines if the specific launch is a rerun of another launch. In
	 * case that property needs to be used, it should contain the launch uuid of the
	 * initial launch
	 */
	RP_IMPORTER_LAUNCH_RERUN_OF("rp.importer.launch.rerunOf", ""),

	/**
	 * Property that defines the mode of the launch (DEBUG/DEFAULT)
	 */
	RP_IMPORTER_LAUNCH_MODE("rp.importer.launch.mode", RPImporterDefaultValues.DEFAULT_RP_IMPORTER_LAUNCH_MODE),

	/**
	 * Property that defines the number of threads used in parallel for feature
	 * importing.
	 */
	RP_IMPORTER_THREADS_FEATURES("rp.importer.threads.features",
			RPImporterDefaultValues.DEFAULT_RP_IMPORTER_THREADS_FEATURES),

	/**
	 * Property that defines the number of threads used in parallel for scenario
	 * importing. This value is applied for any of the feature thread that is
	 * running
	 */
	RP_IMPORTER_THREADS_SCENARIOS("rp.importer.threads.scenarios",
			RPImporterDefaultValues.DEFAULT_RP_IMPORTER_THREADS_SCENARIOS),

	/**
	 * Property that defines the project name on the ReportPortal instance that this
	 * launch will be imported
	 */
	RP_IMPORTER_REPORTPORTAL_PROJECT_NAME("rp.importer.reportPortal.projectName", ""),

	/**
	 * Property that defines the api key of the project on the ReportPortal instance
	 * that this launch will be imported
	 */
	RP_IMPORTER_REPORTPORTAL_API_KEY("rp.importer.reportPortal.apiKey", ""),

	/**
	 * Property that defines the ReportPortal instance endpoint, ie:
	 * <code>https://demo.reportportal.io</code> (without the api path)
	 */
	RP_IMPORTER_REPORTPORTAL_ENDPOINT("rp.importer.reportPortal.endpoint", ""),
	
	RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED("rp.importer.attributes.rerun.enabled",
			RPImporterDefaultValues.DEFAULT_RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED),

	RP_IMPORTER_ATTRIBUTES_RERUN_NAME("rp.importer.attributes.rerun.name",
			RPImporterDefaultValues.DEFAULT_RP_IMPORTER_ATTRIBUTES_RERUN_NAME);

	private final String propertyName;
	private final String defaultValue;
}
