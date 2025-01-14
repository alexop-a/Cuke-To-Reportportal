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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import io.github.alexopa.cukereportportal.config.RPImporterPropertyHandler;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * A utility class
 */
@Slf4j
@UtilityClass
public class Utils {

	/**
	 * Method that returns a {@link File} from the input parameter. The input can be
	 * either an absolute path or a relative path under classpath resources
	 * 
	 * @param f A {@link String} with the file to return
	 * @return A {@link File} from the input parameter
	 */
	public static File getFile(String f) {
		
		// check if it is an absolute path
		Path p = Paths.get(f);
		if (p.isAbsolute()) {
			try {
				File optionalFile = ResourceUtils.getFile(f);
				if (optionalFile.exists()) {
					return optionalFile;
				}
			} catch (FileNotFoundException e) {
				// do nothing
			}
			// if it is absolute path and not found, return null
			log.error("File {} does not exist. Ignoring.", f);
			return null;
		}
        // Load the resource
        Resource resource = new ClassPathResource(f);
        if (!resource.exists()) {
        	log.error("File {} does not exist. Ignoring", f);
            return null;
        }
        // If the resource exists as a file on the filesystem (non-JAR case)
        try {
            return resource.getFile(); // This works only in non-JAR environments
        } catch (Exception e) {
            // Ignore and proceed to handle the JAR case
        }
        
        try {
	        // For JAR execution, copy the resource to a temporary file
	        File tempFile = Files.createTempFile("temp", resource.getFilename()).toFile();
	        tempFile.deleteOnExit();
	        try (FileOutputStream out = new FileOutputStream(tempFile)) {
				IOUtils.copy(resource.getInputStream(), out);
			}
	        // Return the temporary file
	        return tempFile;
        } catch (Exception e) {
        	log.error("Failed to get file {}. Ignoring.", f, e);
			return null;
        }

		/*
		log.info("get file:{}", f);
		try {
			Path p = Paths.get(f);
			log.info("path:{}", p);
			if (p.isAbsolute()) {
				log.info("is absolute");
				File optionalFile = ResourceUtils.getFile(f);
				if (optionalFile.exists()) {
					return optionalFile;
				}
				throw new FileNotFoundException(f);
			} else {
				return ResourceUtils.getFile("classpath:" + f);
			}
		} catch (FileNotFoundException | InvalidPathException e) {
			log.error("File {} does not exist. Ignoring.", f, e);
			return null;
		}
		*/
	}
	
	/**
	 * Method that updates the attributes and adds a rerun attribute. In case rerun
	 * attribute is enabled, then the rerun attribute name is added as a new
	 * attribute to the existing ones
	 * 
	 * @param attributes      A {@link String} with the attributes
	 * @param propertyHandler A {@link RPImporterPropertyHandler} instance
	 * 
	 * @return A {@link String} with the updated attributes
	 */
	public static String enhanceAttributesWithRerun(String attributes, RPImporterPropertyHandler propertyHandler) {
		if (StringUtils.isNotBlank(propertyHandler.getLaunchRerunOf()) && propertyHandler.isRerunAttrbiuteEnabled()) {
			String rerunAttribute = String.format(":%s", propertyHandler.getRerunAttributeName());
			if (StringUtils.isBlank(attributes)) {
				attributes = rerunAttribute;
			} else {
				attributes = String.format("%s;%s", attributes, rerunAttribute);
			}
		}
		return attributes;
	}
}
