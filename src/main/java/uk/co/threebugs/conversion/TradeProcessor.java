package uk.co.threebugs.conversion;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

            // Write back the sorted file (header + sorted data)
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write(header);
                writer.newLine();

                for (String line : dataLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            log.info("Sorted file: {}", filePath.getFileName());
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
