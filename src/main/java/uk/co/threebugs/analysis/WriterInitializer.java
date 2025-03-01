package uk.co.threebugs.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriterInitializer {

    public Map<String, BufferedWriter> initializeWriters(List<String> traderIds, TraderScenarioPaths paths) throws IOException {
        Map<String, BufferedWriter> writers = new HashMap<>();
        for (String traderId : traderIds) {
            String outputFileName = String.format("trader-profit-%s-%s.csv", traderId, paths.getDirectionLabel());
            BufferedWriter writer = Files.newBufferedWriter(paths.getScenarioOutputPath().resolve(outputFileName));
            writer.write("PlaceDateTime,FilledPrice,ClosingPrice,Profit,RunningTotalProfit,State");
            writer.newLine();
            writers.put(traderId, writer);
        }
        return writers;
    }

    public Map<String, Integer> initializeRunningTotalProfits(List<String> traderIds) {
        Map<String, Integer> runningTotalProfits = new HashMap<>();
        for (String traderId : traderIds) {
            runningTotalProfits.put(traderId, 0);
        }
        return runningTotalProfits;
    }

    public void closeWriters(Map<String, BufferedWriter> writers) throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }
}
