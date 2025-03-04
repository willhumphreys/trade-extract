package uk.co.threebugs.conversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriterInitializer {

    public Map<String, BufferedWriter> initializeWriters(List<File> files, Path formattedTradesOutputPath) throws IOException {
        Map<String, BufferedWriter> writers = new HashMap<>();
        for (File file : files) {

            BufferedWriter writer = Files.newBufferedWriter(formattedTradesOutputPath.resolve(file.getName()));
            writer.write("PlaceDateTime,FilledPrice,ClosingPrice,Profit,RunningTotalProfit,State");
            writer.newLine();
            writers.put(file.getName().replace(".csv", ""), writer);
        }
        return writers;
    }

    public Map<String, Integer> initializeRunningTotalProfits(List<File> files) {
        Map<String, Integer> runningTotalProfits = new HashMap<>();
        for (File traderId : files) {
            runningTotalProfits.put(traderId.getName().replace(".csv", ""), 0);
        }
        return runningTotalProfits;
    }

    public void closeWriters(Map<String, BufferedWriter> writers) throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }
}
