package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    public void processTrades(List<String> traderIds, TraderScenarioPaths paths) throws IOException {
        Files.createDirectories(paths.getScenarioOutputPath());

        Map<String, BufferedWriter> writers = writerInitializer.initializeWriters(traderIds, paths);
        Map<String, Integer> runningTotalProfits = writerInitializer.initializeRunningTotalProfits(traderIds);

        List<File> files = fileHandler.getSortedFiles(paths.getDataPath());

        for (File file : files) {
            log.info("Processing file: {}", file.getName());
            processFile(file, writers, runningTotalProfits, paths);
        }

        writerInitializer.closeWriters(writers);
    }

    private void processFile(File file, Map<String, BufferedWriter> writers, Map<String, Integer> runningTotalProfits, TraderScenarioPaths paths) {
        try (BufferedReader reader = fileHandler.getReader(file)) {
            String header = reader.readLine();
            fileHandler.validateHeader(header);
            Map<String, Integer> headerMap = fileHandler.createHeaderMap(header);

            String line;
            while ((line = reader.readLine()) != null) {
                lineProcessor.processLine(line, headerMap, writers, runningTotalProfits, paths.getDirection());
            }

        } catch (IOException e) {
            log.error("Error reading file: {}", file.getName(), e);
        }
    }
}
