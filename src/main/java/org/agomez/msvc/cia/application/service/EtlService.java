package org.agomez.msvc.cia.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agomez.msvc.cia.infraestructure.thirdparty.S3FileReaderAdapter;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlService {

    private final S3FileReaderAdapter s3FileReaderAdapter;
    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;

    @Value("classpath:docs/drfelipeossaqa.pdf")
    private Resource resource;

    @Value("${aws.bucketName}")
    private String bucketName;

    @PostConstruct
    public void loadDocs() {
        String downloadedFileWithPath = "/home/felipe/Documents/msvc-cia/src/main/resources/docs";

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            File file = new File(downloadedFileWithPath);
            if (!file.exists()) {
                String objectkey = "drfelipeossaqa.pdf";
                s3FileReaderAdapter.getObjectFromS3(bucketName, objectkey, downloadedFileWithPath);
            }
        });
        // Wait for the thread to complete
        future.join();

        var count = jdbcClient.sql("SELECT COUNT(*) FROM vector_store")
                .query(Integer.class)
                .single();

        if (count == 0) {
            log.info("Loading docs into the vector store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(resource, config);
            var result = pdfReader.get().stream()
                    .peek(document -> log.info("Loading docs: {}", document.getContent()))
                    .toList();
            vectorStore.accept(result);
            log.info("Loaded {} docs into vector store", result.size());
        }
    }

}
