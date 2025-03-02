package uk.co.threebugs.analysis;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3ExtractsUploader {

    private final S3Client s3Client;

    public S3ExtractsUploader(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Compresses the provided scenario directory into a ZIP file and uploads the archive
     * to the 'mochi-trade-extracts' bucket with the specified S3 key.
     *
     * @param scenarioDir The local directory containing the scenario files.
     * @param s3Key       The S3 key to use when uploading the compressed ZIP.
     */
    public void compressAndPushScenarioZip(Path scenarioDir, String s3Key) {
        if (!Files.isDirectory(scenarioDir)) {
            log.warn("Scenario directory does not exist or is not a directory: {}", scenarioDir);
            return;
        }

        // Create a temporary file to hold the ZIP archive.
        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("scenario", ".zip");
            compressDirectoryToZip(scenarioDir, tempZip);
            log.info("Successfully compressed {} into temporary ZIP: {}", scenarioDir, tempZip);

            // Upload the compressed archive to S3.
            String extractsBucket = "mochi-trade-extracts";
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(extractsBucket)
                    .key(s3Key)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(tempZip));
            log.info("Uploaded compressed ZIP as key {} to bucket {}", s3Key, extractsBucket);
        } catch (IOException e) {
            log.error("Error during ZIP compression/upload for directory {}: {}", scenarioDir, e.getMessage(), e);
        } finally {
            // Clean up the temporary ZIP file.
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException e) {
                    log.warn("Could not delete temporary ZIP file {}: {}", tempZip, e.getMessage());
                }
            }
        }
    }

    /**
     * Loops through all scenario directories in a given symbol directory, compressing and uploading each.
     * The S3 key for each uploaded ZIP is in the format "symbol/scenarioName.zip",
     * where "symbol" is the name of the symbol directory.
     *
     * @param symbolDir The parent directory representing a symbol that contains scenario subdirectories.
     */
    public void compressAndPushAllScenarios(Path symbolDir) {
        if (!Files.isDirectory(symbolDir)) {
            log.warn("Symbol directory does not exist or is not a directory: {}", symbolDir);
            return;
        }

        // The symbol is taken as the name of the symbol directory.
        String symbol = symbolDir.getFileName().toString();
        try (Stream<Path> scenarios = Files.list(symbolDir)) {
            scenarios.filter(Files::isDirectory)
                    .forEach(scenarioDir -> {
                        String scenarioName = scenarioDir.getFileName().toString();
                        // Create an S3 key that mirrors the directory structure: symbol/scenarioName.zip
                        String s3Key = symbol + "/" + scenarioName + ".zip";
                        compressAndPushScenarioZip(scenarioDir, s3Key);
                    });
        } catch (IOException e) {
            log.error("Error listing scenarios in symbol directory {}: {}", symbolDir, e.getMessage(), e);
        }
    }

    /**
     * Compresses the given directory into a ZIP archive.
     *
     * @param sourceDir The directory to compress.
     * @param zipFile   The path of the resulting ZIP file.
     * @throws IOException if any I/O error occurs during compression.
     */
    private void compressDirectoryToZip(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Determine the relative path from the source directory
                    Path relativePath = sourceDir.relativize(file);
                    // Replace system-specific file separators with '/'
                    String zipEntryName = relativePath.toString().replace(file.getFileSystem().getSeparator(), "/");
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);

                    // Read the file and write to the ZIP output stream
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()))) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = bis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }

                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}