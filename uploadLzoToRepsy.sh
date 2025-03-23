#!/bin/bash

# Configuration
REPO_URL="https://api.repsy.io/mvn/willhumphreys/default/"
USERNAME="willhumphreys"
GROUP_ID="com.hadoop.gplcompression"
ARTIFACT_ID="hadoop-lzo"
VERSION="0.4.20"
SOURCE_DIR="$HOME/.m2/repository/com/hadoop/gplcompression/hadoop-lzo/0.4.20"


ENV_FILE=".env"

# Load password from .env file
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
    if [ -z "$REPSY_PASSWORD" ]; then
        echo "ERROR: REPSY_PASSWORD not found in $ENV_FILE"
        echo "Please add REPSY_PASSWORD=yourpassword to your $ENV_FILE file"
        exit 1
    fi
    PASSWORD="$REPSY_PASSWORD"
else
    echo "ERROR: $ENV_FILE file not found"
    echo "Please create a $ENV_FILE file with REPSY_PASSWORD=yourpassword"
    exit 1
fi


# Check if the jar and pom files exist
if [ ! -f "$SOURCE_DIR/hadoop-lzo-0.4.20.jar" ]; then
    echo "ERROR: JAR file not found at $SOURCE_DIR/hadoop-lzo-0.4.20.jar"
    exit 1
fi

if [ ! -f "$SOURCE_DIR/hadoop-lzo-0.4.20.pom" ]; then
    echo "ERROR: POM file not found at $SOURCE_DIR/hadoop-lzo-0.4.20.pom"
    exit 1
fi

echo "Uploading hadoop-lzo artifacts to $REPO_URL"

# Create a temporary directory
TEMP_DIR=$(mktemp -d)
TEMP_JAR="$TEMP_DIR/hadoop-lzo-0.4.20.jar"
TEMP_POM="$TEMP_DIR/hadoop-lzo-0.4.20.pom"

# Copy the files to the temporary directory
cp "$SOURCE_DIR/hadoop-lzo-0.4.20.jar" "$TEMP_JAR"
cp "$SOURCE_DIR/hadoop-lzo-0.4.20.pom" "$TEMP_POM"

# Create a temporary settings file
TEMP_SETTINGS_FILE=$(mktemp)

cat > "$TEMP_SETTINGS_FILE" << EOF
<settings>
  <servers>
    <server>
      <id>repsy</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
  </servers>
</settings>
EOF

# Deploy the main JAR with its POM from the temporary location
mvn deploy:deploy-file \
    -DgroupId=$GROUP_ID \
    -DartifactId=$ARTIFACT_ID \
    -Dversion=$VERSION \
    -Dpackaging=jar \
    -Dfile=$TEMP_JAR \
    -DpomFile=$TEMP_POM \
    -DrepositoryId=repsy \
    -Durl=$REPO_URL \
    -DgeneratePom=false \
    --settings "$TEMP_SETTINGS_FILE"

# Check if the upload was successful
RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo "Upload successful!"
    echo "The library can now be accessed at: $REPO_URL$GROUP_ID/$ARTIFACT_ID/$VERSION/"
else
    echo "Upload failed. Please check the error messages."
fi

# Clean up temporary files
rm "$TEMP_SETTINGS_FILE"
rm -rf "$TEMP_DIR"

exit $RESULT