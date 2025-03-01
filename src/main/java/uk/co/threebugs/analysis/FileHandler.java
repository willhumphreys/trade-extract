package uk.co.threebugs.analysis;

import com.hadoop.compression.lzo.LzopCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class FileHandler {

    private static final String EXPECTED_HEADER = "traderId,timeToPlace,dayOfWeek,dayOfMonth,month,weekOfYear,placedDateTime,limitPrice,stopPrice,state,filledPrice,exitPrice,direction";

    public List<File> getSortedFiles(Path start) {
        try (var pathsStream = Files.walk(start)) {
            return pathsStream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .sorted(Comparator.comparingInt(this::extractFileIndex))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error sorting files in directory: {}", start, e);
        }
        return List.of();
    }

    public BufferedReader getReader(File file) throws IOException {
        var config = new Configuration();
        var codec = new LzopCodec();
        codec.setConf(config);
        InputStream in = codec.createInputStream(new FileInputStream(file));
        return new BufferedReader(new InputStreamReader(in));
    }

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
