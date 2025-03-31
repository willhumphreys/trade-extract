package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

@Slf4j
public class S3TradesProcessor {

    private static final String TRADES_BUCKET = System.getenv("MOCHI_TRADES_BUCKET") != null ? System.getenv("MOCHI_TRADES_BUCKET") : "mochi-prod-backtest-trades";

    private final S3Client s3Client;
    private final FileHandler fileHandler;

    public S3TradesProcessor(S3Client s3Client) {
        this.s3Client = s3Client;
        this.fileHandler = new FileHandler();
    }

    public void processTrades(String symbol, String scenario, Set<String> traderIds) throws IOException {
        // Construct the prefix using the actual key structure
        String prefix = String.format("%s/%s/", symbol, scenario);

        Path output = Paths.get("output", symbol, scenario, "raw");

        // List all objects under the prefix
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(TRADES_BUCKET)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse;
        do {
            listResponse = s3Client.listObjectsV2(listRequest);

            // Process each object that ends with .lzo
            for (S3Object s3Object : listResponse.contents()) {
                String key = s3Object.key();
                if (key.endsWith(".lzo")) {
                    try {
                        GetObjectRequest request = GetObjectRequest.builder()
                                .bucket(TRADES_BUCKET)
                                .key(key)
                                .build();

                        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
                        Path tempFile = Files.createTempFile("trade", ".lzo");
                        Files.copy(response, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Downloaded trade file: {} (temp: {})", key, tempFile);

                        processTradeFile(tempFile.toFile(), traderIds, output);

                        Files.deleteIfExists(tempFile);
                    } catch (SdkClientException e) {
                        log.error("Error while retrieving S3 object for key: {}", key, e);
                    } catch (IOException e) {
                        log.error("I/O error processing file {}: {}", key, e.getMessage(), e);
                    }
                }
            }

            // Continue with next batch of objects if there are more
            listRequest = ListObjectsV2Request.builder()
                    .bucket(TRADES_BUCKET)
                    .prefix(prefix)
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();

        } while (listResponse.isTruncated());

        String header = "tradeId,traderId,timeToPlace,dayOfWeek,dayOfMonth,month,weekOfYear,placedDateTime,limitPrice,stopPrice,state,filledPrice,exitPrice,direction";
        addHeaderToFiles(Arrays.stream(output.toFile().listFiles()).map(File::toPath).toList(), header);
    }

    public void addHeaderToFiles(Collection<Path> files, String header) throws IOException {
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            // Check if the header is already present
            if (lines.isEmpty() || !lines.getFirst().equals(header)) {
                List<String> newLines = new ArrayList<>();
                newLines.add(header);
                newLines.addAll(lines);
                Files.write(file, newLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }

    /**
     * Decompresses and processes the trade file by filtering rows based on the provided traderIds.
     * For each trader, the corresponding trade rows are written to a separate file under a directory
     * structure based on the symbol and scenario.
     *
     * @param file      The local file (already decompressed) to process.
     * @param traderIds The set of trader IDs to include.
     * @param outputDir
     * @throws IOException If an I/O error occurs.
     */
    private void processTradeFile(File file, Set<String> traderIds, Path outputDir) throws IOException {
        // Map to collect trade lines grouped by traderId.
        Map<String, List<String>> traderTrades = new HashMap<>();

        try (BufferedReader reader = fileHandler.getReader(file)) {
            String line;

            String header = reader.readLine();

            String[] headerArray = header.split(",");

            int traderIdIndex = 1;

            if(!headerArray[traderIdIndex].trim().equalsIgnoreCase("traderid")) {
                throw new IllegalArgumentException("Invalid header: " + header);
            }

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split the line on commas; the first token is the traderId.
                String[] tokens = line.split(",");
                if (tokens.length < 1) {
                    continue;
                }
                String traderId = tokens[traderIdIndex].trim();

                // Only include trades for the specified traderIds.
                if (traderIds.contains(traderId)) {
                    traderTrades.computeIfAbsent(traderId, k -> new ArrayList<>()).add(line);
                }
            }

            // Prepare output directory based on symbol and scenario.
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Inside processTradeFile, after collecting trades for each trader:
            for (Map.Entry<String, List<String>> entry : traderTrades.entrySet()) {
                String traderId = entry.getKey();
                List<String> trades = entry.getValue();
                Path traderFile = outputDir.resolve(traderId + ".csv");

                // Open the file in append mode, creating it if it doesn't exist.
                try (BufferedWriter writer = Files.newBufferedWriter(traderFile, StandardCharsets.UTF_8, CREATE, APPEND)) {
                    for (String trade : trades) {
                        writer.write(trade);
                        writer.newLine();
                    }
                }

                log.info("Appended {} trades for trader {} to file {}", trades.size(), traderId, traderFile);
            }

        }
    }
}