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

/**
 * An exception that is thrown by the report-portal importer exception in case
 * an error happens
 */
public class RPImporterException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link RPImporterException}
	 * 
	 * @param msg A {@link String} with a message to add to the exception
	 */
	public RPImporterException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new {@link RPImporterException}
	 * 
	 * @param t A {@link Throwable} to add to the exception
	 */
	public RPImporterException(Throwable t) {
		super(t);
	}

	/**
	 * Creates a new {@link RPImporterException}
	 * 
	 * @param msg A {@link String} with a message to add to the exception
	 * @param t   A {@link Throwable} to add to the exception
	 */
	public RPImporterException(String msg, Throwable t) {
		super(msg, t);
	}
}
