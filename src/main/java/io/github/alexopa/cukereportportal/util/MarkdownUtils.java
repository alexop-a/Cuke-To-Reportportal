/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.alexopa.cukereportportal.util;

import org.apache.commons.lang3.tuple.Pair;

import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 *
 * The class has been copied from the <code>client-java</code> project of
 * <a href=
 * "https://github.com/reportportal/client-java/blob/master/src/main/java/com/epam/reportportal/utils/markdown/MarkdownUtilsTest.java">reportportal</a>
 * 
 * @author Andrei Varabyeu
 */
@UtilityClass
public class MarkdownUtils {

	private static final String MARKDOWN_MODE = "!!!MARKDOWN_MODE!!!";
	private static final char NEW_LINE = '\n';
	private static final String ONE_SPACE = "\u00A0";
	private static final String TABLE_INDENT = "\u00A0\u00A0\u00A0\u00A0";
	private static final String TABLE_COLUMN_SEPARATOR = "|";
	private static final String TABLE_ROW_SEPARATOR = "-";
	private static final String TRUNCATION_REPLACEMENT = "...";
	private static final int PADDING_SPACES_NUM = 2;
	private static final int MAX_TABLE_SIZE = 83;
	private static final int MIN_COL_SIZE = 3;
	private static final String LOGICAL_SEPARATOR = "---";

	/**
	 * Adds special prefix to make log message being processed as markdown
	 *
	 * @param message Message
	 * @return Message with markdown marker
	 */
	public static String asMarkdown(String message) {
		return MARKDOWN_MODE.concat(message);
	}

	/**
	 * Builds markdown representation of some script to be logged to ReportPortal
	 *
	 * @param language Script language
	 * @param script   Script
	 * @return Message to be sent to ReportPortal
	 */
	public static String asCode(String language, String script) {
		return asMarkdown("```" + ofNullable(language).orElse("") + NEW_LINE + script + NEW_LINE + "```");
	}

	private static List<Integer> calculateColSizes(@Nonnull List<List<String>> table) {
		int tableColNum = table.stream().mapToInt(List::size).max().orElse(-1);
		List<Iterator<String>> iterList = table.stream().map(List::iterator).toList();
		return IntStream.range(0, tableColNum)
				.mapToObj(n -> iterList.stream().filter(Iterator::hasNext).map(Iterator::next).toList())
				.map(col -> col.stream().mapToInt(String::length).max().orElse(0)).toList();
	}

	private static int calculateTableSize(@Nonnull List<Integer> colSizes) {
		int colTableSize = colSizes.stream().reduce(Integer::sum).orElse(-1);
		colTableSize += (PADDING_SPACES_NUM + TABLE_COLUMN_SEPARATOR.length()) * colSizes.size() - 1; // Inner columns
																										// grid
		colTableSize += 2; // Outer table grid
		return colTableSize;
	}

	private static <T> List<List<T>> transposeTable(@Nonnull List<List<T>> table) {
		int tableColNum = table.stream().mapToInt(List::size).max().orElse(-1);
		List<Iterator<T>> iterList = table.stream().map(List::iterator).toList();
		return IntStream.range(0, tableColNum)
				.mapToObj(n -> iterList.stream().filter(Iterator::hasNext).map(Iterator::next).toList()).toList();
	}

	@Nonnull
	private static List<Integer> adjustColSizes(@Nonnull List<Integer> colSizes, int maxTableSize) {
		int colTableSize = calculateTableSize(colSizes);
		if (maxTableSize >= colTableSize) {
			return colSizes;
		}
		List<Pair<Integer, Integer>> colsBySize = IntStream.range(0, colSizes.size())
				.mapToObj(i -> Pair.of(colSizes.get(i), i)).sorted().collect(Collectors.toList());
		Collections.reverse(colsBySize);
		int sizeToShrink = colTableSize - maxTableSize;
		for (int i = 0; i < sizeToShrink; i++) {
			for (int j = 0; j < colsBySize.size(); j++) {
				Pair<Integer, Integer> currentCol = colsBySize.get(j);
				if (currentCol.getKey() <= MIN_COL_SIZE) {
					continue;
				}
				Pair<Integer, Integer> nextCol = colsBySize.size() > j + 1 ? colsBySize.get(j + 1) : Pair.of(0, 0);
				if (currentCol.getKey() >= nextCol.getKey()) {
					colsBySize.set(j, Pair.of(currentCol.getKey() - 1, currentCol.getValue()));
					break;
				}
			}
		}
		return colsBySize.stream().sorted(Map.Entry.comparingByValue()).map(Pair::getKey).toList();
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string.
	 *
	 * @param table        a table object
	 * @param maxTableSize maximum size in characters of result table, cells will be
	 *                     truncated
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table, int maxTableSize) {
		List<Integer> colSizes = calculateColSizes(table);
		boolean transpose = colSizes.size() > table.size() && calculateTableSize(colSizes) > maxTableSize;
		List<List<String>> printTable = transpose ? transposeTable(table) : table;
		if (transpose) {
			colSizes = calculateColSizes(printTable);
		}
		colSizes = adjustColSizes(colSizes, maxTableSize);
		int tableSize = calculateTableSize(colSizes);
		boolean addPadding = tableSize <= maxTableSize;
		boolean header = !transpose;
		StringBuilder result = new StringBuilder();
		for (List<String> row : printTable) {
			result.append(TABLE_INDENT).append(TABLE_COLUMN_SEPARATOR);
			for (int i = 0; i < row.size(); i++) {
				String cell = row.get(i);
				int colSize = colSizes.get(i);
				if (colSize < cell.length()) {
					if (TRUNCATION_REPLACEMENT.length() < colSize) {
						cell = cell.substring(0, colSize - TRUNCATION_REPLACEMENT.length()) + TRUNCATION_REPLACEMENT;
					} else {
						cell = cell.substring(0, colSize);
					}
				}
				int padSize = colSize - cell.length() + (addPadding ? PADDING_SPACES_NUM : 0);
				int lSpace = padSize / 2;
				int rSpace = padSize - lSpace;
				IntStream.range(0, lSpace).forEach(j -> result.append(ONE_SPACE));
				result.append(cell);
				IntStream.range(0, rSpace).forEach(j -> result.append(ONE_SPACE));
				result.append(TABLE_COLUMN_SEPARATOR);
			}
			if (header) {
				header = false;
				result.append(NEW_LINE);
				result.append(TABLE_INDENT).append(TABLE_COLUMN_SEPARATOR);
				for (int i = 0; i < row.size(); i++) {
					int maxSize = colSizes.get(i) + (addPadding ? PADDING_SPACES_NUM : 0);
					IntStream.range(0, maxSize).forEach(j -> result.append(TABLE_ROW_SEPARATOR));
					result.append(TABLE_COLUMN_SEPARATOR);
				}
			}
			result.append(NEW_LINE);
		}
		return result.toString().trim();
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table) {
		return formatDataTable(table, MAX_TABLE_SIZE);
	}

	/**
	 * Converts a table represented as Map to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final Map<String, String> table) {
		List<List<String>> toFormat = new ArrayList<>();
		List<String> keys = new ArrayList<>(table.keySet());
		toFormat.add(keys);
		toFormat.add(keys.stream().map(table::get).toList());
		return formatDataTable(toFormat);
	}

	/**
	 * Formats two strings in a new string, with one part on each line separated by
	 * the {@link #LOGICAL_SEPARATOR}
	 * 
	 * @param firstPart  The first part of the string
	 * @param secondPart The second part of the string
	 * @return the formatted string
	 */
	public static String asTwoParts(@Nonnull String firstPart, @Nonnull String secondPart) {
		return firstPart + NEW_LINE + LOGICAL_SEPARATOR + NEW_LINE + secondPart;
	}
}
