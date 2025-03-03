# ThreeBugs Analysis Project

This project is a Java-based tool for processing and analyzing trade data. It includes functionality to compress scenario directories into ZIP files and upload them to an AWS S3 bucket. The application leverages the Apache Commons CLI library for handling command-line arguments.

## Features

- **Scenario Compression and Uploading:** Compress a directory containing scenario files into a ZIP archive and upload it to an S3 bucket.
- **Command-Line Interface:** Pass the symbol and scenario as arguments to the application.
- **Output Directory Management:** Automatically creates the output directory at startup if it does not exist.
- **AWS Integration:** Uses the AWS SDK for S3 interactions.
- **Gradle Build:** Managed using Gradle with support for Java 21.

## Prerequisites

- **Java SDK 21** or later.
- **Gradle** (the project uses the Gradle wrapper, so installing Gradle separately is optional).
- **AWS Credentials:** Ensure you have valid AWS credentials configured (for example, via the AWS CLI or environment variables) for S3 operations.

## Getting Started

1. **Clone the Repository**

   ```bash
   gh repo clone willhumphreys/trade-extract
   ```

2. **Build the Project**

   Use the Gradle wrapper to build the project:

   ```bash
   ./gradlew build
   ```

3. **Run the Application**

   The application requires two arguments: `symbol` and `scenario`. These are passed using the Apache Commons CLI options.

   Example:

   ```bash
   java -jar build/libs/threebugs-analysis-1.0-SNAPSHOT.jar --symbol BTC --scenario --symbol btc-1mF --scenario s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9
   ```

   In this example:
   - `BTC` is the symbol value.
   - `btc-1mF --scenario s_-3000..-100..400___l_100..7500..400___o_-800..800..100___d_14..14..7___out_8..8..4___mw___wc=9` is the scenario value.

   The output directory will be created automatically if it does not already exist.

## Dependencies

- **AWS SDK for S3**: For interacting with AWS S3.
- **Apache Commons CLI**: For parsing and validating command-line arguments.
- **Lombok**: To simplify logging and data class creation.
- **JUnit Jupiter, AssertJ, and Mockito**: For testing.

## Project Structure

- `Runner.java`: Contains the main method and command-line arguments handling using Apache Commons CLI.
- `S3ExtractsUploader.java`: Provides functionality to compress directories to ZIP files and upload them to S3.
- `build.gradle.kts`: Gradle Kotlin DSL build file that manages dependencies and compilation settings.

## Contributing

Feel free to fork the repository and submit pull requests. For any issues or feature requests, please open an issue on the repository.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.