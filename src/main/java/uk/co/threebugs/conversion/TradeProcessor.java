package uk.co.threebugs.conversion;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TradeProcessor {

    private final WriterInitializer writerInitializer;
    private final FileHandler fileHandler;
    private final LineProcessor lineProcessor;

    public TradeProcessor() {
        this.writerInitializer = new WriterInitializer();
        this.fileHandler = new FileHandler();
        this.lineProcessor = new LineProcessor();
    }

    public void processTrades(List<File> files, String symbol, String scenario) throws IOException {
        Path formattedTradesOutputPath = Paths.get("output", symbol, scenario, "formatted-trades");

        Files.createDirectories(formattedTradesOutputPath);

        Map<String, BufferedWriter> writers = writerInitializer.initializeWriters(files, formattedTradesOutputPath);
        Map<String, Integer> runningTotalProfits = writerInitializer.initializeRunningTotalProfits(files);

        for (File file : files) {
            log.info("Processing file: {}", file.getName());
            processFile(file, writers, runningTotalProfits, scenario );
        }

        writerInitializer.closeWriters(writers);

        sortOutputFiles(formattedTradesOutputPath);
    }

    /**
     * Reads each output file, sorts the trades by PlaceDateTime (earliest first), and writes them back to the file.
     *
     * @param outputPath The directory containing the formatted trade files
     */
    private void sortOutputFiles(Path outputPath) {
        try {
            Files.list(outputPath).filter(Files::isRegularFile).forEach(this::sortFile);
            log.info("All output files have been sorted by PlaceDateTime");
        } catch (IOException e) {
            log.error("Error sorting output files", e);
        }
    }

    /**
     * Sorts a single file by PlaceDateTime.
     *
     * @param filePath The path to the file to sort
     */
    private void sortFile(Path filePath) {
        try {
            // Read all lines from the file
            List<String> lines = Files.readAllLines(filePath);

            if (lines.size() <= 1) {
                // File is empty or contains only headers - nothing to sort
                return;
            }

            // Extract header
            String header = lines.getFirst();

            // Get index of PlaceDateTime column
            Map<String, Integer> headerMap = fileHandler.createHeaderMap(header);
            int placeDateTimeIndex = headerMap.getOrDefault("PlaceDateTime", 0);
            int profitIndex = headerMap.getOrDefault("Profit", 3);

            // Sort the data lines (excluding header)
            List<String> dataLines = lines.subList(1, lines.size());
            dataLines.sort((line1, line2) -> {
                String[] parts1 = line1.split(",");
                String[] parts2 = line2.split(",");

                if (parts1.length > placeDateTimeIndex && parts2.length > placeDateTimeIndex) {
                    return parts1[placeDateTimeIndex].compareTo(parts2[placeDateTimeIndex]);
                }
                return 0;
            });

            // Recalculate running totals after sorting
            int runningTotal = 0;
            List<String> updatedLines = new ArrayList<>();

            for (String line : dataLines) {
                String[] parts = line.split(",");
                if (parts.length > profitIndex) {
                    int profit = Integer.parseInt(parts[profitIndex]);
                    runningTotal += profit;

                    // Reconstruct the line with the updated running total
                    StringBuilder updatedLine = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) {
                        if (i == profitIndex + 1) { // RunningTotalProfit index
                            updatedLine.append(runningTotal);
                        } else {
                            updatedLine.append(parts[i]);
                        }

                        if (i < parts.length - 1) {
                            updatedLine.append(",");
                        }
                    }
                    updatedLines.add(updatedLine.toString());
                } else {
                    updatedLines.add(line);
                }
            }

            // Write back the sorted file (header + sorted data with updated running totals)
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write(header);
                writer.newLine();

                for (String line : updatedLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            log.info("Sorted file and recalculated running totals: {}", filePath.getFileName());
        } catch (IOException e) {
            log.error("Error sorting file: {}", filePath.getFileName(), e);
        }
    }



    private void processFile(File file, Map<String, BufferedWriter> writers, Map<String, Integer> runningTotalProfits, String scenario) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String header = reader.readLine();
            fileHandler.validateHeader(header);
            Map<String, Integer> headerMap = fileHandler.createHeaderMap(header);

            String line;
            while ((line = reader.readLine()) != null) {
                lineProcessor.processLine(line, headerMap, writers, runningTotalProfits, scenario.contains("short") ? -1 : 1);
            }

        } catch (IOException e) {
            log.error("Error reading file: {}", file.getName(), e);
        }
    }
}
