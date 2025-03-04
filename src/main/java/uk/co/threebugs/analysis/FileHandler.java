package uk.co.threebugs.analysis;

import com.hadoop.compression.lzo.LzopCodec;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.io.*;

@Slf4j
public class FileHandler {

    public BufferedReader getReader(File file) throws IOException {
        var config = new Configuration();
        var codec = new LzopCodec();
        codec.setConf(config);
        InputStream in = codec.createInputStream(new FileInputStream(file));
        return new BufferedReader(new InputStreamReader(in));
    }


}
