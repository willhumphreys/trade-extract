package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class TradeFilterCsvWriter {

    private static final int LONG = 1;
    private static final int SHORT = -1;

    private static Path DATA_PATH;
    private static Path OUTPUT_PATH;

    public static void main(String[] args) {
        if (args.length < 1) {
            log.error("Please provide a symbol as an argument, e.g., gbpusd-1m-btmH or eurusd-1m-btmH");
            System.exit(1);
        }

        String symbol = args[0];
        DATA_PATH = Paths.get("data", symbol);
        OUTPUT_PATH = Paths.get("output6", symbol);
        Path uniqueTradersPath = OUTPUT_PATH.resolve("unique-traders");

        log.info("Processing for symbol: {}", symbol);
        log.info("Input directory: {}", DATA_PATH);
        log.info("Output directory: {}", OUTPUT_PATH);
        log.info("Unique traders directory: {}", uniqueTradersPath);

        TradeFilterCsvWriter writer = new TradeFilterCsvWriter();
        writer.processUniqueTraderFiles(uniqueTradersPath);
    }

    private void processUniqueTraderFiles(Path uniqueTradersPath) {
        TraderFileHandler fileHandler = new TraderFileHandler();
        TradeProcessor tradeProcessor = new TradeProcessor();

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
                Path dataPath = DATA_PATH.resolve(scenarioName);

                // Set the scenario-specific output path and create the 'profits' directory
                Path scenarioOutputPath = OUTPUT_PATH.resolve(scenarioName).resolve("profits");
                TraderScenarioPaths paths = new TraderScenarioPaths(dataPath, scenarioOutputPath, directionLabel, direction);

                tradeProcessor.processTrades(traderIds, paths);
                log.info("Finished processing trader file: {}", traderFilePath);
            } catch (Exception e) {
                log.error("Error processing trader file: {}", traderFilePath, e);
            }
        });
    }
}
