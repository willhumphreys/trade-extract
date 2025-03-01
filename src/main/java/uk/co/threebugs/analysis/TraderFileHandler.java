package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TraderFileHandler {

    public List<Path> listTraderFiles(Path uniqueTradersPath) {
        try (var traderIdFilesStream = Files.list(uniqueTradersPath)) {
            return traderIdFilesStream.filter(Files::isRegularFile)
                                      .toList();
        } catch (IOException e) {
            log.error("Error listing unique trader files in directory: {}", uniqueTradersPath, e);
            throw new IllegalStateException("Error listing unique trader files", e);
        }
    }

    public List<String> getTraderIds(Path traderIdFile) throws IOException {
        return Files.readAllLines(traderIdFile)
                    .stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
    }
}
