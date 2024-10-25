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
package io.github.alexopa.cukereportportal.exception;

import org.springframework.http.HttpStatusCode;

import io.github.alexopa.cukereportportal.client.ReportPortalClient;
import io.github.alexopa.cukereportportal.client.model.ReportPortalErrorMessage;
import lombok.Getter;

/**
 * An exception that is thrown by the {@link ReportPortalClient} in case an
 * error happens
 */
@Getter
public class ReportPortalClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The {@link HttpStatusCode} of the exception
	 */
	private HttpStatusCode httpStatusCode;

	/**
	 * A {@link ReportPortalErrorMessage} with information about the exception
	 */
	private ReportPortalErrorMessage errorMessage;

	/**
	 * Creates a new {@link ReportPortalClientException}
	 * 
	 * @param httpStatusCode The {@link HttpStatusCode} of the error that occurred
	 * @param errorMessage   A {@link ReportPortalErrorMessage} instance with
	 *                       information about the error
	 */
	public ReportPortalClientException(HttpStatusCode httpStatusCode, ReportPortalErrorMessage errorMessage) {
		this.httpStatusCode = httpStatusCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Creates a new {@link ReportPortalClientException}
	 * 
	 * @param statusCode The {@link HttpStatusCode} of the error that occurred
	 * @param message    A {@link String} with a message about the error. This
	 *                   message will be wrapped in a
	 *                   {@link ReportPortalErrorMessage}
	 */
	public ReportPortalClientException(HttpStatusCode statusCode, String message) {
		this.httpStatusCode = statusCode;
		this.errorMessage = ReportPortalErrorMessage.builder().message(message).build();
	}
}
