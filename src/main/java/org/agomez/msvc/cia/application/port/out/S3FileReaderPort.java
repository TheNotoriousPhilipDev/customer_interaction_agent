package org.agomez.msvc.cia.application.port.out;

public interface S3FileReaderPort {

    void getObjectFromS3(String bucketName, String key, String downloadedFileWithPath);
}
