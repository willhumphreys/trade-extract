package uk.co.threebugs.analysis;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.file.Path;

@Value
@AllArgsConstructor
public class TraderScenarioPaths {
    /**
     * Helper class to hold the paths and information for each trader scenario.
     */

    String scenario;
    Path scenarioOutputPath;
    String directionLabel;
    int direction;


}

