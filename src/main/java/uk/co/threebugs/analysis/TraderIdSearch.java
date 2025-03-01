package uk.co.threebugs.analysis;

import com.hadoop.compression.lzo.LzopCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TraderIdSearch {

    private int cumulativeProfit = 0;
    private int maxCumulativeProfit = 0; // Peak
    private int minCumulativeProfit = 0; // Trough

    private int maxStoppedCount;
    private int currentStoppedCount;

    private int currentLimitCount = 0;
    private int maxLimitCount = 0;

    private int winningCount = 0;
    private int losingCount = 0;

    private int consecutiveWinningCount = 0;
    private int consecutiveLosingCount = 0;

    private int maxConsecutiveWinningCount = 0;
    private int maxConsecutiveLosingCount = 0;

    private int maxDrawdown = 0; // maximum loss in ticks from a peak
    private int maxMeltUp = 0; // maximum profit in ticks from a trough

    private BufferedWriter csvWriter;


    public static void main(String[] args) {
        TraderIdSearch traderIdSearch = new TraderIdSearch();
        traderIdSearch.setup();
        List<File> files = traderIdSearch.getSortedFiles("/home/will/eurusd");
        traderIdSearch.processFiles(files, "101025");
    }

    private void setup() {
        maxStoppedCount = 0;
        currentStoppedCount = 0;

        currentLimitCount = 0;
        maxLimitCount = 0;

        // Initialize CSV writer
        try {
            csvWriter = new BufferedWriter(new FileWriter("output.csv"));
            // Write CSV header
            csvWriter.write("PlaceDateTime,MaxDrawdown,MaxMeltUp");
            csvWriter.newLine();
        } catch (IOException e) {
            log.error("Error writing to CSV file", e);
        }
    }

    private List<File> getSortedFiles(String directoryPath) {
        try {
            try (var stream = Files.walk(Paths.get(directoryPath))) {
                return stream.filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .sorted(Comparator.comparingInt(this::extractFileIndex))
                        .toList();
            }
        } catch (IOException e) {
           log.error("Error sorting files in directory: {}", directoryPath, e);
        }

        return Collections.emptyList(); // Return empty list on exception
    }

    private void processFiles(List<File> files, String traderId) {
        files.forEach(file -> processFile(file, traderId));
    }

    // Assuming that `extractFileIndex` and `processFile` methods are also refactored to be instance methods.


    private int extractFileIndex(File file) {
        Pattern pattern = Pattern.compile("p(\\d+)\\.csv\\.lzo");
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE; // If no match, place it at the end
    }

    private void processFile(File file, String traderId) {
        System.out.println("Processing file: " + file.getName());

        Configuration config = new Configuration();
        LzopCodec codec = new LzopCodec();
        codec.setConf(config);

        try (InputStream in = codec.createInputStream(new FileInputStream(file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String line;
            reader.readLine(); // Skip the header line

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String traderIdField = fields[0].trim();

                if (traderIdField.equals(traderId)) {
                    int exitPrice = Integer.parseInt(fields[11].trim());
                    int filledPrice = Integer.parseInt(fields[10].trim());
                    int tickProfit = exitPrice - filledPrice;

                    int placedDateTime = Integer.parseInt(fields[6]);

                    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(placedDateTime), ZoneId.of("UTC"));


                    cumulativeProfit += tickProfit;

                    // Update peak and trough
                    if (cumulativeProfit > maxCumulativeProfit) {
                        maxCumulativeProfit = cumulativeProfit;
                    }
                    if (cumulativeProfit < minCumulativeProfit) {
                        minCumulativeProfit = cumulativeProfit;
                    }

                    // Calculate drawdown and melt up
                    int possibleDrawdown = maxCumulativeProfit - cumulativeProfit;
                    int possibleMeltUp = cumulativeProfit - minCumulativeProfit;

                    // Update max drawdown and max melt up if changed, and write to CSV
                    boolean drawdownChanged = false;
                    boolean meltUpChanged = false;

                    if (possibleDrawdown > maxDrawdown) {
                        maxDrawdown = possibleDrawdown;
                        System.out.println("New max drawdown: " + maxDrawdown);
                        drawdownChanged = true;
                    }
                    if (possibleMeltUp > maxMeltUp) {
                        maxMeltUp = possibleMeltUp;
                        System.out.println("New max melt up: " + maxMeltUp);
                        meltUpChanged = true;
                    }

                    // Write to CSV only if either drawdown or melt-up changed
                    if (drawdownChanged || meltUpChanged) {
                        writeCsv(dateTime, maxDrawdown, maxMeltUp);
                    }

                    // Write to CSV
                    writeCsv(dateTime, maxDrawdown, maxMeltUp);

                    // Update winning/losing counts
                    if (tickProfit > 0) {
                        winningCount++;
                        consecutiveWinningCount++;
                        consecutiveLosingCount = 0;
                    } else {
                        losingCount++;
                        consecutiveLosingCount++;
                        consecutiveWinningCount = 0;
                    }

                    // Update max consecutive counts
                    if (consecutiveWinningCount > maxConsecutiveWinningCount) {
                        maxConsecutiveWinningCount = consecutiveWinningCount;
                        System.out.println("New max consecutive winning count: " + maxConsecutiveWinningCount);
                    }
                    if (consecutiveLosingCount > maxConsecutiveLosingCount) {
                        maxConsecutiveLosingCount = consecutiveLosingCount;
                        System.out.println("New max consecutive losing count: " + maxConsecutiveLosingCount);
                    }

                    // Update STOPPED and LIMIT counts
                    String state = fields[9].trim();
                    if (state.equals("STOPPED")) {
                        currentStoppedCount++;
                        maxStoppedCount = Math.max(maxStoppedCount, currentStoppedCount);
                    } else if (state.equals("LIMIT")) {
                        currentLimitCount++;
                        maxLimitCount = Math.max(maxLimitCount, currentLimitCount);
                    } else {
                        currentStoppedCount = 0;
                        currentLimitCount = 0;
                    }
                }
            }

            // Print final results
            System.out.println("Maximum consecutive STOPPED trades for traderId " + traderId + ": " + maxStoppedCount);
            System.out.println("Maximum consecutive LIMIT trades for traderId " + traderId + ": " + maxLimitCount);
            System.out.println("Maximum consecutive winning trades for traderId " + traderId + ": " + maxConsecutiveWinningCount);
            System.out.println("Maximum consecutive losing trades for traderId " + traderId + ": " + maxConsecutiveLosingCount);
            System.out.println("Winning trades for traderId " + traderId + ": " + winningCount);
            System.out.println("Losing trades for traderId " + traderId + ": " + losingCount);
            System.out.println("Max drawdown: " + maxDrawdown);
            System.out.println("Max melt up: " + maxMeltUp);
        } catch (IOException e) {
            log.error("Error processing file: {}", file.getName(), e);
        }
    }

    private void writeCsv(LocalDateTime dateTime, int maxDrawdown, int maxMeltUp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            csvWriter.write(dateTime.format(formatter) + "," + maxDrawdown + "," + maxMeltUp);
            csvWriter.newLine();
        } catch (IOException e) {
            log.error("Error writing to CSV file", e);
        }
    }
}
