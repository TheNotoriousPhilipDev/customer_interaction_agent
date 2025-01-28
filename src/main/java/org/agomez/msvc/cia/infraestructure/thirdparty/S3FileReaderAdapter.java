package org.agomez.msvc.cia.infraestructure.thirdparty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agomez.msvc.cia.application.port.out.S3FileReaderPort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

import java.nio.file.Paths;


@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileReaderAdapter implements S3FileReaderPort {

    private final S3TransferManager s3TransferManager;

    @Override
    public void getObjectFromS3(String bucketName, String key, String downloadedFileWithPath) {

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .destination(Paths.get(downloadedFileWithPath))
                .build();

        FileDownload downloadFile = s3TransferManager.downloadFile(downloadFileRequest);
        CompletedFileDownload downloadResult = downloadFile.completionFuture().join();
        log.info("Content length [{}]", downloadResult.response().contentLength());
    }
}
