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

import lombok.experimental.UtilityClass;

/**
 * Class that defines the default values of the service properties
 */
@UtilityClass
public class RPImporterDefaultValues {

	/**
	 * The default value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_MODE}
	 * property
	 */
	protected static final String DEFAULT_RP_IMPORTER_LAUNCH_MODE = "DEBUG";

	/**
	 * The default value of
	 * {@link RPImporterProperties#RP_IMPORTER_THREADS_FEATURES} property
	 */
	protected static final String DEFAULT_RP_IMPORTER_THREADS_FEATURES = "1";

	/**
	 * The default value of
	 * {@link RPImporterProperties#RP_IMPORTER_THREADS_SCENARIOS} property
	 */
	protected static final String DEFAULT_RP_IMPORTER_THREADS_SCENARIOS = "1";
	
	/**
	 * The default value of
	 * {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED} property
	 */
	protected static final String DEFAULT_RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED = "false";

	/**
	 * The default value of
	 * {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_NAME} property
	 */
	protected static final String DEFAULT_RP_IMPORTER_ATTRIBUTES_RERUN_NAME = "rerun";
	
}
