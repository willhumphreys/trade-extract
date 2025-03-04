package uk.co.threebugs.conversion;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class FileHandler {

    private static final String EXPECTED_HEADER = "traderId,timeToPlace,dayOfWeek,dayOfMonth,month,weekOfYear,placedDateTime,limitPrice,stopPrice,state,filledPrice,exitPrice,direction";

    public void validateHeader(String header) {
        if (header == null || !header.equals(EXPECTED_HEADER)) {
            throw new IllegalArgumentException("Header is not as expected");
        }
    }

    public Map<String, Integer> createHeaderMap(String header) {
        String[] headers = header.split(",");
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i], i);
        }
        return headerMap;
    }

    private int extractFileIndex(File file) {
        var pattern = Pattern.compile("p(\\d+)\\.csv\\.lzo");
        var matcher = pattern.matcher(file.getName());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }
}
