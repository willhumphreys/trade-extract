package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class S3CsvSplitter {

    private static final String BUCKET_NAME = "mochi-graphs";
    private static final String PREFIX = "btc-1mF/";
    // Change the region if needed.
    private static final Region REGION = Region.US_EAST_1;
    // Columns to filter duplicate rows.
    private static final List<String> FILTER_COLUMNS = List.of(
            "dayofweek", "hourofday", "stop", "limit", "tickoffset", "tradeduration", "outoftime"
    );

    public static void main(String[] args) {
        // Build the S3Client
        try (S3Client s3Client = S3Client.builder().region(REGION).build()) {
            groupAndProcessFiles(s3Client);
        }
    }

    /**
     * Groups S3 keys by scenario, processes each group, and writes a file per scenario.
     *
     * @param s3Client The S3 client.
     */
    public static void groupAndProcessFiles(S3Client s3Client) {
        // List all relevant CSV keys from S3.
        List<String> keys = listS3Keys(s3Client, BUCKET_NAME, PREFIX);
        Map<String, List<String>> scenarioGroups = new HashMap<>();

        // Group keys by scenario. Assuming the key format is:
        // "<PREFIX><scenario>/<other folders>/...csv"
        for (String key : keys) {
            int slashIndex = key.indexOf("/", PREFIX.length());
            if (slashIndex > 0) {
                String scenario = key.substring(PREFIX.length(), slashIndex);
                scenarioGroups.computeIfAbsent(scenario, k -> new ArrayList<>()).add(key);
            } else {
                log.warn("Key does not contain a scenario folder: {}", key);
            }
        }

        // Process each scenario group.
        for (Map.Entry<String, List<String>> entry : scenarioGroups.entrySet()) {
            String scenario = entry.getKey();
            List<String> scenarioKeys = entry.getValue();

            // Download and concatenate CSV content for the group.
            String aggregatedContent = processCsvGroup(s3Client, BUCKET_NAME, scenarioKeys);
            // Remove duplicate rows.
            aggregatedContent = filterDuplicates(aggregatedContent, FILTER_COLUMNS);

            // Write the aggregated CSV to a file (name it using the scenario string).
            Path outputPath = Paths.get(scenario + ".csv");
            try {
                Files.writeString(outputPath, aggregatedContent, StandardCharsets.UTF_8);
                log.info("File written for scenario '{}': {}", scenario, outputPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Error writing file for scenario '{}'", scenario, e);
            }
        }
    }

    /**
     * Lists all CSV keys under the specified prefix in the given S3 bucket.
     *
     * @param s3Client   The S3 client.
     * @param bucketName The S3 bucket name.
     * @param prefix     The key prefix.
     * @return A list of S3 keys that end with ".csv".
     */
    public static List<String> listS3Keys(S3Client s3Client, String bucketName, String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(1000);
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Request request = requestBuilder.build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            for (S3Object object : response.contents()) {
                if (object.key().endsWith(".csv")) {
                    keys.add(object.key());
                }
            }
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);

        return keys;
    }

    /**
     * Downloads and concatenates CSV content from S3 for a list of keys.
     * The first file's header is kept; for subsequent files, the header is removed.
     *
     * @param s3Client   The S3 client.
     * @param bucketName The S3 bucket name.
     * @param keys       A list of S3 keys.
     * @return A concatenated CSV content as a String.
     */
    public static String processCsvGroup(S3Client s3Client, String bucketName, List<String> keys) {
        StringBuilder concatenatedCsv = new StringBuilder();
        boolean isFirstFile = true;

        for (String key : keys) {
            log.info("Downloading file: {}", key);
            String csvContent = downloadCsvContent(s3Client, bucketName, key);
            if (!isFirstFile) {
                // Remove the header (first line) for subsequent files.
                int firstLineBreak = csvContent.indexOf('\n');
                if (firstLineBreak > 0) {
                    csvContent = csvContent.substring(firstLineBreak + 1);
                }
            }
            concatenatedCsv.append(csvContent);
            if (!csvContent.endsWith("\n")) {
                concatenatedCsv.append("\n");
            }
            isFirstFile = false;
        }
        return concatenatedCsv.toString();
    }

    /**
     * Downloads the CSV content from S3 as a String.
     *
     * @param s3Client   The S3 client.
     * @param bucketName The S3 bucket name.
     * @param key        The S3 key for the CSV file.
     * @return The file content as a String.
     */
    public static String downloadCsvContent(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        ResponseBytes<?> objectBytes = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        return objectBytes.asString(StandardCharsets.UTF_8);
    }

    /**
     * Filters out duplicate rows from the concatenated CSV content.
     * The header row is always kept.
     * Duplicate detection is based on the given columns.
     * The header row in the output is re-created with each header value wrapped in double quotes.
     *
     * @param csvData         The concatenated CSV content.
     * @param columnsToCheck  The list of column names to base duplicate filtering on.
     * @return The filtered CSV content as a String.
     */
    // In the filterDuplicates method:
    public static String filterDuplicates(String csvData, List<String> columnsToCheck) {
        // Split the CSV into lines.
        String[] lines = csvData.split("\\r?\\n");
        if (lines.length == 0) {
            return csvData;
        }

        // Parse the header and determine the indices for the columns to check.
        String header = lines[0];
        String[] headers = header.split(",");
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String trimmedHeader = headers[i].trim().replace("\"", "");
            headerIndex.put(trimmedHeader, i);
        }

        List<Integer> filterIndices = new ArrayList<>();
        for (String col : columnsToCheck) {
            if (headerIndex.containsKey(col)) {
                filterIndices.add(headerIndex.get(col));
            } else {
                System.err.println("Warning: Column '" + col + "' not found in header.");
            }
        }

        // Create a new header line wrapping each header value with double quotes.
        String newHeader = Arrays.stream(headers)
                .map(String::trim)
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(","));

        // Use a Set to track rows (based on the joined filter column values) already seen.
        Set<String> seenRows = new HashSet<>();
        StringBuilder filteredOutput = new StringBuilder();
        filteredOutput.append(newHeader).append("\n");

        // To count how many duplicates we skip.
        int duplicateCount = 0;

        // Process each subsequent row.
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            // Skip empty lines.
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] tokens = line.split(",");
            StringBuilder rowKeyBuilder = new StringBuilder();
            for (int idx : filterIndices) {
                if (idx < tokens.length) {
                    rowKeyBuilder.append(tokens[idx].trim()).append("|");
                }
            }
            String rowKey = rowKeyBuilder.toString();
            if (!seenRows.contains(rowKey)) {
                seenRows.add(rowKey);
                filteredOutput.append(line).append("\n");
            } else {
                duplicateCount++;
            }
        }

        // Output the duplicate count.
        log.info("Found {} duplicate rows during filtering.", duplicateCount);
        log.info("Filtered output length: {}", filteredOutput.length());
        return filteredOutput.toString();
    }
}