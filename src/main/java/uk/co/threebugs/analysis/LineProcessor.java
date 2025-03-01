package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
public class LineProcessor {

    public void processLine(String line, Map<String, Integer> headerMap, Map<String, BufferedWriter> writers, Map<String, Integer> runningTotalProfits, int direction) {
        String[] fields = line.split(",");
        String traderIdField = fields[headerMap.get("traderId")].trim();

        if (line.contains("POISON")) {
            return; // Skip invalid lines
        }

        if (writers.containsKey(traderIdField)) {
            try {
                int exitPrice = Integer.parseInt(fields[headerMap.get("exitPrice")].trim());
                int filledPrice = Integer.parseInt(fields[headerMap.get("filledPrice")].trim());
                String state = fields[headerMap.get("state")].trim();
                int tickProfit = (exitPrice - filledPrice) * direction;

                int placedDateTime = Integer.parseInt(fields[headerMap.get("placedDateTime")]);
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(placedDateTime), ZoneId.of("UTC"));

                int runningTotalProfit = runningTotalProfits.get(traderIdField) + tickProfit;
                runningTotalProfits.put(traderIdField, runningTotalProfit);

                BufferedWriter writer = writers.get(traderIdField);
                writer.write(String.join(",", dateTime.format(DateUtils.DATE_TIME_FORMATTER), String.valueOf(filledPrice), String.valueOf(exitPrice), String.valueOf(tickProfit), String.valueOf(runningTotalProfit), state));
                writer.newLine();
            } catch (NumberFormatException | DateTimeException e) {
                log.error("Error parsing line: {}", line, e);
            } catch (IOException e) {
                log.error("Error writing to file for traderId: {}", traderIdField, e);
            }
        }
    }
}
