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
package io.github.alexopa.cukereportportal.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class
 */
@Slf4j
public class Utils {

	/**
	 * Method that returns a {@link File} from the input parameter. The input can be
	 * either an absolute path or a relative path under classpath resources
	 * 
	 * @param f A {@link String} with the file to return
	 * @return A {@link File} from the input parameter
	 */
	public static File getFile(String f) {
		try {
			Path p = Paths.get(f);
			if (p.isAbsolute()) {
				File optionalFile = ResourceUtils.getFile(f);
				if (optionalFile.exists()) {
					return optionalFile;
				}
				throw new FileNotFoundException(f);
			} else {
				return ResourceUtils.getFile("classpath:" + f);
			}
		} catch (FileNotFoundException | InvalidPathException e) {
			log.error("File {} does not exist. Ignoring.", f);
			return null;
		}
	}
}
