package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class TradeFilterCsvWriter {

    private static final int LONG = 1;
    private static final int SHORT = -1;

//    private static Path DATA_PATH;
    private static Path OUTPUT_PATH;

    public static void main(String[] args) throws IOException {

        String symbol = args[0];



        TradeFilterCsvWriter writer = new TradeFilterCsvWriter();
        writer.processUniqueTraderFiles(symbol);
    }

    private void processUniqueTraderFiles(String symbol) throws IOException {
        TraderFileHandler fileHandler = new TraderFileHandler();
        TradeProcessor tradeProcessor = new TradeProcessor();




        String scenario = "1mF";

        Path uniqueTradersPath = new S3FileDownloader().downloadFile(symbol, scenario);

        //s3://mochi-graphs/btc-1mF/s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9/aggregated-btc-1mF_s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9_aggregationQueryTemplate-all.csv.lzo/btc_BestTrades/appt/btc_bestTrades.csv

        // Check if the directory exists, and create it if it doesn't
        try {
            if (!Files.exists(uniqueTradersPath) || uniqueTradersPath.toFile().list().length == 0) {
                throw new IllegalStateException("Unique traders directory does not exist or is empty: %s".formatted(uniqueTradersPath));
            }
        } catch (Exception e) {
            log.error("Failed to create directory: {}", uniqueTradersPath, e);
            throw new IllegalStateException("Could not create required directory", e);
        }

        // List trader files
        List<Path> traderFiles = fileHandler.listTraderFiles(uniqueTradersPath);

        traderFiles.forEach(traderFilePath -> {
            try {
                log.info("Processing trader file: {}", traderFilePath);
                List<String> traderIds = fileHandler.getTraderIds(traderFilePath);

                // Determine the direction based on the file name
                int direction = traderFilePath.toString().contains("short") ? SHORT : LONG;
                String directionLabel = direction == SHORT ? "short" : "long";
                String scenarioName = traderFilePath.getFileName().toString().replace("_unique_trader_ids.txt", "");
//                Path dataPath = DATA_PATH.resolve(scenarioName);

                // Set the scenario-specific output path and create the 'profits' directory
                Path scenarioOutputPath = OUTPUT_PATH.resolve(scenarioName).resolve("profits");
                TraderScenarioPaths paths = new TraderScenarioPaths(scenarioName, scenarioOutputPath, directionLabel, direction);

                tradeProcessor.processTrades(traderIds, paths);
                log.info("Finished processing trader file: {}", traderFilePath);
            } catch (Exception e) {
                log.error("Error processing trader file: {}", traderFilePath, e);
            }
        });
    }
}
