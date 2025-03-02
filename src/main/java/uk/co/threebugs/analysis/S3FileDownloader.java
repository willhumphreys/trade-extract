package uk.co.threebugs.analysis;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class S3FileDownloader {

    // The bucket name is fixed in this example.
    private static final String BUCKET_NAME = "mochi-graphs";

    // Modify the region to match your S3 bucket's region.
    private static final Region REGION = Region.US_EAST_1;

    private final S3Client s3Client;

    public S3FileDownloader() {
        this.s3Client = S3Client.builder().region(REGION).build();
    }

    // Example usage:
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Please provide a symbol and scenario as arguments.");
            System.exit(1);
        }
        String symbol = args[0];
        String scenario = args[1];

        S3FileDownloader downloader = new S3FileDownloader();

        try {
            Path downloadedFile = downloader.downloadFile(symbol, scenario);
            System.out.println("File downloaded and saved at: " + downloadedFile);
        } catch (IOException e) {
            System.err.println("Failed to download file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            downloader.shutdown();
        }
    }

    /**
     * Downloads a file from S3 based on the given symbol and scenario.
     *
     * @param symbol   The symbol to use in generating the S3 key.
     * @param scenario The scenario to use in generating the S3 key.
     * @return Path The local file path where the file was saved.
     * @throws IOException If an I/O error occurs during file operations.
     */
    public Path downloadFile(String symbol, String scenario) throws IOException {
        // Generate the S3 key using the symbol and scenario.
        // Update this logic with your actual key structure.
        String key = generateKey(symbol, scenario);

        // Define the local file path where the file will be saved.
        // Here we put the file under the system temporary directory.
        Path downloadPath = Paths.get(System.getProperty("java.io.tmpdir"), symbol + "_" + scenario + ".csv");

        // Create the GetObjectRequest
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET_NAME).key(key).build();

        // Download the file from S3 and save to the downloadPath.
        s3Client.getObject(getObjectRequest, downloadPath);
        System.out.println("Download complete: " + downloadPath);

        return downloadPath;
    }

    /**
     * Generates the S3 key based on the provided symbol and scenario.
     *
     * @param symbol   The symbol used to build the key.
     * @param scenario The scenario used to build the key.
     * @return The S3 key as a String.
     */
    private String generateKey(String symbol, String scenario) {

        //s3://mochi-graphs/btc-1mF/s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9/aggregated-btc-1mF_s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9_aggregationQueryTemplate-all.csv.lzo/btc_BestTrades/appt/btc_bestTrades.csv


        String symbolName = symbol.split("-")[0];
        return String.format("%s/%s/aggregated-%s_aggregationQueryTemplate-all.csv.lzo/%s/appt/%s_bestTrades.csv", symbol, scenario, scenario, symbolName, symbolName);
    }

    // Close the S3 client (if needed) when done with the downloader.
    public void shutdown() {
        s3Client.close();
    }
}