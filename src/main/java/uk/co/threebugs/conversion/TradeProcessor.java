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
