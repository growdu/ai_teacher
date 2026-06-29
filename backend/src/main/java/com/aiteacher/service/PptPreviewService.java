package com.aiteacher.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * PPT Preview Service - converts PPT files to PDF for preview
 * Uses LibreOffice to perform the conversion
 */
@Slf4j
@Service
public class PptPreviewService {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${ppt.preview-cache-dir:/tmp/ppt-preview}")
    private String cacheDir;

    @Value("${ppt.libreoffice-path:libreoffice}")
    private String libreOfficePath;

    /**
     * Convert PPT/PPTX file to PDF
     *
     * @param objectName MinIO object name of the PPT file
     * @return byte array of the PDF file
     */
    public byte[] convertToPdf(String objectName) {
        // Validate file extension
        String lowerName = objectName.toLowerCase();
        if (!lowerName.endsWith(".pptx") && !lowerName.endsWith(".ppt")) {
            throw new RuntimeException("Only PPT/PPTX files can be converted to PDF preview");
        }

        String pdfObjectName = objectName.substring(0, objectName.lastIndexOf('.')) + ".pdf";

        try {
            // Download PPT from MinIO to temp file
            Path pptPath = Path.of(cacheDir, "input_" + System.currentTimeMillis() + getExtension(objectName));
            Files.createDirectories(Path.of(cacheDir));
            byte[] pptBytes = getFileBytesWithFallback(objectName);
            Files.write(pptPath, pptBytes);

            // Output PDF path
            String baseName = pptPath.getFileName().toString().replaceFirst("\\.(pptx?)$", "");
            Path pdfPath = Path.of(cacheDir, baseName + ".pdf");

            // Convert using LibreOffice
            boolean success = convertWithLibreOffice(pptPath.toString(), pdfPath.getParent().toString());

            if (!success || !Files.exists(pdfPath)) {
                throw new RuntimeException("LibreOffice conversion failed or PDF not generated");
            }

            byte[] pdfBytes = Files.readAllBytes(pdfPath);

            // Cleanup temp files
            Files.deleteIfExists(pptPath);
            Files.deleteIfExists(pdfPath);

            log.info("Successfully converted PPT to PDF: {}", objectName);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Failed to convert PPT to PDF: {}", e.getMessage(), e);
            throw new RuntimeException("PPT转PDF预览失败: " + e.getMessage(), e);
        }
    }

    private boolean convertWithLibreOffice(String inputPath, String outputDir) throws IOException, InterruptedException {
        // Try to find libreoffice executable
        String soffice = findLibreOffice();
        log.info("Using LibreOffice at: {}", soffice);

        ProcessBuilder pb = new ProcessBuilder(
                soffice,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                outputDir,
                inputPath
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("LibreOffice: {}", line);
            }
        }

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.error("LibreOffice conversion timed out");
            return false;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("LibreOffice exited with code: {}", exitCode);
            return false;
        }

        return true;
    }

    private String findLibreOffice() {
        // Check configured path first
        if (!"libreoffice".equals(libreOfficePath)) {
            if (Files.exists(Path.of(libreOfficePath))) {
                return libreOfficePath;
            }
        }

        // Try common locations
        String[] possiblePaths = {
                "libreoffice",
                "soffice",
                "/usr/bin/libreoffice",
                "/usr/bin/soffice",
                "/opt/libreoffice/program/soffice",
                "/Applications/LibreOffice.app/Contents/MacOS/soffice"
        };

        for (String path : possiblePaths) {
            if (Files.exists(Path.of(path))) {
                return path;
            }
        }

        // Fall back to configured/default (will likely fail if not found)
        return libreOfficePath;
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * Fetch file bytes from MinIO with fallback to direct HTTP access.
     * MinIO SDK may fail with newer server versions (2025+) due to AWS4 signing changes.
     */
    private byte[] getFileBytesWithFallback(String objectName) throws Exception {
        try {
            return fileStorageService.getFileBytes(objectName);
        } catch (Exception sdkError) {
            log.warn("MinIO SDK fetch failed (server may require updated signing): {}, trying direct HTTP fallback", sdkError.getMessage());
            String minioUrl = "http://minio:9000/" + objectName;
            java.net.URL url = new java.net.URL(minioUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("HTTP fetch failed with status: " + conn.getResponseCode());
            }
            try (java.io.InputStream in = conn.getInputStream();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                log.info("Direct HTTP fetch succeeded: {} bytes", out.size());
                return out.toByteArray();
            }
        }
    }
}
