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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import io.github.alexopa.cukereportportal.exception.RPImporterException;
import io.github.alexopa.cukereportportal.util.Utils;
import io.github.alexopa.reportportalclient.rpmodel.Mode;
import lombok.extern.slf4j.Slf4j;

/**
 * Class that is responsible to initialize the service properties. It also
 * provides methods to access the properties.
 */
@Slf4j
public class RPImporterPropertyHandler {

	private static final String PROPERTIES_FILE = "rp-cucumber-import.properties";

	private Properties props = new Properties();

	/**
	 * Creates a new {@link RPImporterPropertyHandler} instance. The default
	 * properties file is used for initialization.
	 */
	public RPImporterPropertyHandler() {
		initProperties(PROPERTIES_FILE, new Properties());
	}

	/**
	 * Creates a new {@link RPImporterPropertyHandler} instance.
	 * 
	 * @param extraProps A {@link Properties} object with the properties to be used
	 *                   for initialization. The extra properties will be merged on
	 *                   top of the properties defined in the default properties
	 *                   file
	 */
	public RPImporterPropertyHandler(Properties extraProps) {
		initProperties(PROPERTIES_FILE, extraProps);
	}

	/**
	 * Creates a new {@link RPImporterPropertyHandler} instance.
	 * 
	 * @param propsFile A {@link String} with the properties file to be used for
	 *                  initialization
	 */
	public RPImporterPropertyHandler(String propsFile) {
		initProperties(propsFile, new Properties());
	}

	/**
	 * Creates a new {@link RPImporterPropertyHandler} instance.
	 * 
	 * @param propsFile  A {@link String} with the properties file to be used for
	 *                   initialization
	 * @param extraProps A {@link Properties} object with the properties to be used
	 *                   for initialization. The extra properties will be merged on
	 *                   top of the properties defined in the <code>propsFile</code>
	 */
	public RPImporterPropertyHandler(String propsFile, Properties extraProps) {
		initProperties(propsFile, extraProps);
	}

	private void initProperties(String propsFile, Properties extraProps) {
		Properties propsFromFile = initPropsFromFile(propsFile);
		Properties propsFromSystemProperties = initPropsFromSystem();

		props = mergeProperties(propsFromFile, propsFromSystemProperties, extraProps);
	}

	private Properties mergeProperties(Properties... properties) {
		return Stream.of(properties).collect(Properties::new, Map::putAll, Map::putAll);
	}

	private Properties initPropsFromSystem() {
		Properties systemProperties = System.getProperties();

		Properties overridesFromSystem = new Properties();
		for (RPImporterProperties prop : RPImporterProperties.values()) {
			Optional.ofNullable(systemProperties.getProperty(prop.getPropertyName()))
					.ifPresent(v -> overridesFromSystem.setProperty(prop.getPropertyName(), v));
		}

		return overridesFromSystem;
	}

	private Properties initPropsFromFile(String f) {

		Properties propsFromFile = new Properties();

		ClassLoader classLoader = getClass().getClassLoader();

		InputStream inStream = null;
		inStream = classLoader.getResourceAsStream(f);
		if (inStream == null) {
			log.warn("{} file is missing.", f);
			return propsFromFile;
		}

		URL url = classLoader.getResource(f);
		if (url != null) {
			File file = Utils.getFile(url.getFile());
			FileInputStream propsInput = null;
			try {
				propsInput = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				throw new RPImporterException(String.format("Failed to read properties file %s", f));
			}
			try {
				propsFromFile.load(propsInput);
			} catch (IOException e) {
				throw new RPImporterException(String.format("Failed to process properties file %s", f));
			}
		}

		return propsFromFile;
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_CUCUMBER_JSON_FILES}
	 * property
	 * 
	 * @return a <code>List</code> of {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_CUCUMBER_JSON_FILES} property
	 */
	public List<String> getCucumberJsonFiles() {
		return getPropertyAsList(RPImporterProperties.RP_IMPORTER_CUCUMBER_JSON_FILES);
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_NAME}
	 * property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_NAME} property
	 */
	public String getLaunchName() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_LAUNCH_NAME.getPropertyName());
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_ATTRIBUTES}
	 * property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_ATTRIBUTES} property
	 */
	public String getLaunchAttributes() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_LAUNCH_ATTRIBUTES.getPropertyName());
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_ATTACHMENTS}
	 * property
	 * 
	 * @return a <code>List</code> of {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_ATTACHMENTS} property
	 */
	public List<String> getLaunchAttachments() {
		return getPropertyAsList(RPImporterProperties.RP_IMPORTER_LAUNCH_ATTACHMENTS);
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_DESCRIPTION}
	 * property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_DESCRIPTION} property
	 */
	public String getLaunchDescription() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_LAUNCH_DESCRIPTION.getPropertyName());
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_RERUN_OF}
	 * property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_RERUN_OF} property
	 */
	public String getLaunchRerunOf() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_LAUNCH_RERUN_OF.getPropertyName());
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_LAUNCH_MODE}
	 * property
	 * 
	 * @return a {@link Mode} instance with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_LAUNCH_MODE} property
	 */
	public Mode getLaunchMode() {
		return Mode.valueOf(props.getProperty(RPImporterProperties.RP_IMPORTER_LAUNCH_MODE.getPropertyName()));
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_THREADS_FEATURES}
	 * property
	 * 
	 * @return an <code>int</code> with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_THREADS_FEATURES} property
	 */
	public int getThreadsFeatures() {
		return getPropertyAsInteger(RPImporterProperties.RP_IMPORTER_THREADS_FEATURES);
	}

	/**
	 * Returns value of {@link RPImporterProperties#RP_IMPORTER_THREADS_SCENARIOS}
	 * property
	 * 
	 * @return an <code>int</code> with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_THREADS_SCENARIOS} property
	 */
	public int getThreadsScenarios() {
		return getPropertyAsInteger(RPImporterProperties.RP_IMPORTER_THREADS_SCENARIOS);
	}

	/**
	 * Returns value of
	 * {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_PROJECT_NAME} property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_PROJECT_NAME}
	 *         property
	 */
	public String getReportPortalProjectName() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_REPORTPORTAL_PROJECT_NAME.getPropertyName());
	}

	/**
	 * Returns value of
	 * {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_API_KEY} property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_API_KEY}
	 *         property
	 */
	public String getReportPortalApiKey() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_REPORTPORTAL_API_KEY.getPropertyName());
	}

	/**
	 * Returns value of
	 * {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_ENDPOINT} property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_REPORTPORTAL_ENDPOINT}
	 *         property
	 */
	public String getReportPortalEndpoint() {
		return props.getProperty(RPImporterProperties.RP_IMPORTER_REPORTPORTAL_ENDPOINT.getPropertyName());
	}
	
	/**
	 * Returns value of
	 * {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED} property
	 * 
	 * @return a <code>boolean</code> with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED}
	 *         property
	 */
	public boolean isRerunAttrbiuteEnabled() {
		return getPropertyAsBoolean(RPImporterProperties.RP_IMPORTER_ATTRIBUTES_RERUN_ENABLED);
	}

	/**
	 * Returns value of
	 * {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_NAME} property
	 * 
	 * @return a {@link String} with the value of
	 *         {@link RPImporterProperties#RP_IMPORTER_ATTRIBUTES_RERUN_NAME}
	 *         property
	 */
	public String getRerunAttributeName() {
		return getPropertyAsString(RPImporterProperties.RP_IMPORTER_ATTRIBUTES_RERUN_NAME);
	}

	private String getPropertyAsString(RPImporterProperties prop) {
		String value = props.getProperty(prop.getPropertyName());
		return Optional.ofNullable(value).orElse(prop.getDefaultValue());
	}
	
	private int getPropertyAsInteger(RPImporterProperties prop) {
		String value = props.getProperty(prop.getPropertyName());
		return null != value ? Integer.valueOf(value) : Integer.valueOf(prop.getDefaultValue());
	}

	private boolean getPropertyAsBoolean(RPImporterProperties prop) {
		String value = props.getProperty(prop.getPropertyName());
		return null != value ? Boolean.valueOf(value) : Boolean.valueOf(prop.getDefaultValue());
	}
	
	private List<String> getPropertyAsList(RPImporterProperties prop) {
		String value = props.getProperty(prop.getPropertyName());
		return null != value ? Arrays.asList(value.split(";")) : Collections.emptyList();
	}
}
