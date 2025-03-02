package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class S3TradesProcessor {

    private static final String TRADES_BUCKET = "mochi-trades";
    private static final Region REGION = Region.US_EAST_1;

    private final S3Client s3Client;
    private final FileHandler fileHandler;

    public S3TradesProcessor() {
        this.s3Client = S3Client.builder().region(REGION).build();
        this.fileHandler = new FileHandler();
    }

    /**
     * For the given symbol and scenario, this method first lists the available years (by listing object prefixes)
     * and then iterates over those years and partitions to download, decompress, and process the trade files.
     *
     * @param symbol    The symbol (e.g. "btc-1mF").
     * @param scenario  The scenario string (e.g. "s_-3000..-100..200___l_100..7500..200___o_-800..800..50___d_14..14..7___out_8..8..4")
     * @param traderIds A set of trader ids to be filtered on.
     */
    public void processTrades(String symbol, String scenario, Set<String> traderIds) {
        // Generate the trade scenario prefix used for key construction.
        String tradeScenario = scenario + "___mw___wc=9";
        // The prefix for listing keys (up to the year folder) is:
        // symbol/tradeScenario/
        String prefix = String.format("%s/%s/", symbol, tradeScenario);

        List<Integer> availableYears = getAvailableYears(prefix);
        if (availableYears.isEmpty()) {
            log.warn("No available years found for prefix: {}", prefix);
            return;
        }

        // Process trade files only for the available years.
        for (int year : availableYears) {
            int partitionIndex = 0;
            while (true) {
                String key = generateTradeKey(symbol, scenario, year, partitionIndex);
                try {
                    GetObjectRequest request = GetObjectRequest.builder().bucket(TRADES_BUCKET).key(key).build();

                    ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
                    Path tempFile = Files.createTempFile("trade", ".lzo");
                    Files.copy(response, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Downloaded trade file: {} (temp: {})", key, tempFile);

                    processTradeFile(tempFile.toFile(), traderIds);

                    Files.deleteIfExists(tempFile);
                    partitionIndex++;
                } catch (NoSuchKeyException e) {
                    log.info("No trade file found for key: {}. Moving to next year...", key);
                    break;
                } catch (SdkClientException e) {
                    log.error("Error while retrieving S3 object for key: {}", key, e);
                    break;
                } catch (IOException e) {
                    log.error("I/O error processing file {}: {}", key, e.getMessage(), e);
                    break;
                }
            }
        }
    }

    /**
     * Lists objects using the prefix and returns a list of years (as integers) for which trade files exist.
     * <p>
     * The method uses the delimiter option to obtain common prefixes (e.g. "btc-1mF/{tradeScenario}/{year}/").
     *
     * @param prefix The S3 prefix for listing (e.g. "btc-1mF/s_-3000..-100..200___l_100..7500..200___o_-800..800..50___d_14..14..7___out_8..8..4/").
     * @return A List of years available for the given prefix.
     */
    private List<Integer> getAvailableYears(String prefix) {
        List<Integer> years = new ArrayList<>();
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(TRADES_BUCKET).prefix(prefix).delimiter("/").build();
        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        // The common prefixes should contain folders for individual years.
        List<String> commonPrefixes = listResponse.commonPrefixes().stream().map(CommonPrefix::prefix).toList();

        for (String cp : commonPrefixes) {
            // Expected format: symbol/tradeScenario/year/
            String[] parts = cp.split("/");
            if (parts.length >= 3) {
                String yearString = parts[2];
                try {
                    int year = Integer.parseInt(yearString);
                    years.add(year);
                } catch (NumberFormatException e) {
                    log.warn("Skipping non-year prefix: {}", cp);
                }
            }
        }
        return years;
    }

    /**
     * Generates the trade file key based on the symbol, scenario, year, and partition index.
     * <p>
     * The key is assumed to follow this pattern:
     * symbol+"/"+ (scenario + "___mw___wc=9") + "/" + year + "/trades--" + (scenario + "___mw___wc=9")
     * + "___" + symbolName + "0_p" + partitionIndex + ".csv.lzo"
     *
     * @param symbol         The symbol (e.g. "btc-1mF").
     * @param scenario       The scenario string.
     * @param year           The year for the trade file.
     * @param partitionIndex The partition index (starting with 0).
     * @return The full S3 key for the trade file.
     */
    private String generateTradeKey(String symbol, String scenario, int year, int partitionIndex) {
        // Append the additional components required by the trade key.
        String tradeScenario = scenario + "___mw___wc=9";
        // Use the full symbol instead of splitting at the hyphen.
        return String.format("%s/%s/%d/trades--%s___%s0_p%d.csv.lzo", symbol, tradeScenario, year, tradeScenario, symbol, partitionIndex);
    }

    /**
     * Decompresses the trade file (using FileHandler) and filters rows based on the traderIds.
     * For matching rows, the line is output to the console.
     *
     * @param file      The local file (in lzo format) to process.
     * @param traderIds A set of trader ids to include.
     * @throws IOException If an I/O error occurs.
     */
    private void processTradeFile(File file, Set<String> traderIds) throws IOException {
        try (BufferedReader reader = fileHandler.getReader(file)) {
            String header = reader.readLine(); // Skip or validate the header.
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                String traderId = line.split(",")[0];
                if (traderIds.contains(traderId)) {
                    System.out.println(line);
                }
            }
        }
    }

    // Optional: shutdown the S3 client if needed.
    public void shutdown() {
        s3Client.close();
    }
}