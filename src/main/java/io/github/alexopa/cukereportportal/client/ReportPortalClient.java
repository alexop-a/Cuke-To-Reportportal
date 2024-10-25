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
package io.github.alexopa.cukereportportal.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.FinishLaunchRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.alexopa.cukereportportal.client.model.AddFileAttachmentProperties;
import io.github.alexopa.cukereportportal.client.model.AddLogProperties;
import io.github.alexopa.cukereportportal.client.model.FinishItemProperties;
import io.github.alexopa.cukereportportal.client.model.FinishLaunchProperties;
import io.github.alexopa.cukereportportal.client.model.ReportPortalErrorMessage;
import io.github.alexopa.cukereportportal.client.model.StartItemProperties;
import io.github.alexopa.cukereportportal.client.model.StartLaunchProperties;
import io.github.alexopa.cukereportportal.exception.ReportPortalClientException;
import io.github.alexopa.cukereportportal.util.AttributeParser;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is a client for ReportPortal. It provides methods to send requests
 * to a ReportPortal instance
 */
@Slf4j
public class ReportPortalClient {

	private final RestClient rpClient;
	private final String projectName;
	private final String apiKey;

	private final UriComponentsBuilder startLaunchUri;
	private final UriComponentsBuilder finishLaunchUri;
	private final UriComponentsBuilder startItemUri;
	private final UriComponentsBuilder startNestedItemUri;
	private final UriComponentsBuilder finishItemUri;
	private final UriComponentsBuilder addLogUri;

	/**
	 * Creates a new {@link ReportPortalClient} instance for a specific project on
	 * ReportPortal
	 * 
	 * @param projectName A {@link String} with the project name
	 * @param apiKey      A {@link String} with the api key for the project
	 * @param endpoint    A {@link String} with the endpoint of the ReportPortal
	 *                    instance
	 */
	public ReportPortalClient(final String projectName, final String apiKey, final String endpoint) {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(
				Collections.singletonList(new MediaType("text", "html", StandardCharsets.UTF_8)));

		List<HttpMessageConverter<?>> c = new ArrayList<>();
		c.add(converter);
		c.add(new MappingJackson2HttpMessageConverter());

		this.rpClient = RestClient.builder().requestFactory(getClientHttpRequestFactory())
				.messageConverters(converters -> converters.addAll(c))
				.defaultStatusHandler(new ReportPortalErrorHandler()).build();
		this.projectName = projectName;
		this.apiKey = apiKey;

		startLaunchUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}")
				.path("/launch");
		finishLaunchUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}")
				.path("/launch").path("/{launchUuid}").path("/finish");
		startItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}").path("/item");
		startNestedItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}")
				.path("/item").path("/{parentUuid}");
		finishItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}").path("/item")
				.path("/{itemUuid}");
		addLogUri = UriComponentsBuilder.fromHttpUrl(endpoint).path("api/v1/").path("{projectName}").path("/log");
	}

	private ClientHttpRequestFactory getClientHttpRequestFactory() {
		HttpClient httpClient = HttpClientBuilder.create().useSystemProperties().disableRedirectHandling().build();

		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		clientHttpRequestFactory.setConnectTimeout(5000);
		clientHttpRequestFactory.setConnectionRequestTimeout(10000);
		return clientHttpRequestFactory;
	}

	/**
	 * Starts a new launch on ReportPortal
	 * 
	 * @param props A {@link StartLaunchProperties} object with the properties of
	 *              the launch to start
	 * @return A {@link StartLaunchRS} object with the response from ReportPortal
	 */
	public StartLaunchRS startLaunch(StartLaunchProperties props) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(props.getName());
		if (StringUtils.isNotBlank(props.getRerunOf())) {
			rq.setRerun(true);
			rq.setRerunOf(props.getRerunOf());
		}
		rq.setStartTime(props.getStartTime());
		Optional.ofNullable(props.getMode()).ifPresent(rq::setMode);
		Optional.ofNullable(props.getDescription()).ifPresent(rq::setDescription);
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));

		ResponseEntity<StartLaunchRS> rs = rpClient
				.post()
				.uri(startLaunchUri.buildAndExpand(projectName).toUri())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(StartLaunchRS.class);

		return rs.getBody();
	}

	/**
	 * Finishes a launch on ReportPortal
	 * 
	 * @param props A {@link FinishLaunchProperties} object with the properties of
	 *              the launch to finish
	 * @return A {@link FinishLaunchRS} object with the response from ReportPortal
	 */
	public FinishLaunchRS finishLaunch(FinishLaunchProperties props) {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(props.getEndTime());

		ResponseEntity<FinishLaunchRS> rs = rpClient
				.put()
				.uri(finishLaunchUri.buildAndExpand(projectName, props.getLaunchUuid()).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(FinishLaunchRS.class);

		return rs.getBody();
	}

	/**
	 * Starts a new item on ReportPortal
	 * 
	 * @param props A {@link StartItemProperties} object with the properties of the
	 *              item to start
	 * @return An {@link EntryCreatedAsyncRS} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedAsyncRS startItem(StartItemProperties props) {
		StartTestItemRQ rq = new StartTestItemRQ();
		Optional.ofNullable(props.getDescription()).ifPresent(d -> rq.setDescription(d));
		Optional.ofNullable(props.getCodeRef()).ifPresent(c -> rq.setCodeRef(c));
		rq.setName(props.getName());
		rq.setStartTime(props.getStartTime());
		rq.setType(props.getType());
		rq.setLaunchUuid(props.getLaunchUuid());
		Optional.ofNullable(props.getHasStats()).ifPresent(s -> rq.setHasStats(s));
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));

		URI uri = null;
		if (StringUtils.isBlank(props.getParentUuid())) {
			uri = startItemUri.buildAndExpand(projectName).toUri();
		} else {
			uri = startNestedItemUri.buildAndExpand(projectName, props.getParentUuid()).toUri();
		}

		ResponseEntity<EntryCreatedAsyncRS> rs = rpClient
				.post()
				.uri(uri)
				.accept(MediaType.ALL)
				.header("Authorization", "Bearer " + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedAsyncRS.class);

		return rs.getBody();
	}

	/**
	 * Finishes an item on ReportPortal
	 * 
	 * @param props A {@link FinishItemProperties} object with the properties of the
	 *              item to finish
	 * @return An {@link EntryCreatedAsyncRS} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedAsyncRS finishItem(FinishItemProperties props) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(props.getEndTime());
		rq.setLaunchUuid(props.getLaunchUuid());
		rq.setStatus(props.getStatus());

		ResponseEntity<EntryCreatedAsyncRS> rs = rpClient
				.put()
				.uri(finishItemUri.buildAndExpand(projectName, props.getItemUuid()).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedAsyncRS.class);

		return rs.getBody();
	}

	/**
	 * Adds a log message to an item
	 * 
	 * @param props An {@link AddLogProperties} object with the properties of the
	 *              log message to add
	 * @return An {@link EntryCreatedAsyncRS} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedAsyncRS addLog(AddLogProperties props) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setLaunchUuid(props.getLaunchId());
		rq.setItemUuid(props.getItemId());
		rq.setLevel(props.getLevel());
		rq.setLogTime(props.getTime());
		rq.setMessage(props.getMessage());

		ResponseEntity<EntryCreatedAsyncRS> rs = rpClient
				.post()
				.uri(addLogUri.buildAndExpand(projectName).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedAsyncRS.class);

		return rs.getBody();
	}

	/**
	 * Adds a file attachment to launch or item
	 * 
	 * @param props An {@link AddFileAttachmentProperties} object with the
	 *              properties of the attachment to add
	 * @return An {@link EntryCreatedAsyncRS} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedAsyncRS addFileAttachment(AddFileAttachmentProperties props) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setLaunchUuid(props.getLaunchUuid());
		Optional.ofNullable(props.getItemUuid()).ifPresent(it -> rq.setItemUuid(it));
		rq.setLevel(props.getLevel());
		rq.setLogTime(props.getTime());
		rq.setMessage(props.getMessage());

		SaveLogRQ.File file = new SaveLogRQ.File();
		file.setName(props.getMessage());
		rq.setFile(file);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("json_request_part", Arrays.asList(rq));
		parts.add("file", new FileSystemResource(props.getFullPath()));

		ResponseEntity<EntryCreatedAsyncRS> rs = rpClient
				.post()
				.uri(addLogUri.buildAndExpand(projectName).toUri())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(parts)
				.retrieve()
				.toEntity(EntryCreatedAsyncRS.class);

		return rs.getBody();
	}

	private class ReportPortalErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			HttpStatusCode statusCode = response.getStatusCode();
			return statusCode.is3xxRedirection() || statusCode.is4xxClientError() || statusCode.is5xxServerError();
		}

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().is3xxRedirection()) {
				throw new ReportPortalClientException(response.getStatusCode(),
						"Redirection responses are not expected. Please check if server is running properly");
			}

			InputStream res = response.getBody();
			ObjectMapper objectMapper = new ObjectMapper();
			ReportPortalErrorMessage errorMessage = null;
			try {
				errorMessage = objectMapper.readValue(res, ReportPortalErrorMessage.class);
			} catch (Exception e) {
				try {
					String error = objectMapper.readValue(res, String.class);
					errorMessage = new ReportPortalErrorMessage();
					errorMessage.setMessage(error);
					errorMessage.setT(e);
				} catch (Exception e1) {
					errorMessage = new ReportPortalErrorMessage();
					errorMessage.setMessage("Failed to parse response as String");
					errorMessage.setT(e1);
				}
			}
			throw new ReportPortalClientException(response.getStatusCode(), errorMessage);
		}
	}
}
