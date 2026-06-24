package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.service.PptPreviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PPT Preview Controller - converts PPT files to PDF for preview
 */
@Slf4j
@RestController
@RequestMapping("/api/ppt")
public class PptPreviewController {

    @Autowired
    private PptPreviewService pptPreviewService;

    /**
     * Preview PPT as PDF
     * Downloads the PPT from MinIO, converts to PDF using LibreOffice, returns PDF bytes
     */
    @GetMapping("/preview")
    public ResponseEntity<byte[]> previewPpt(@RequestParam String objectName) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }

        log.info("Generating PPT preview: objectName={}", objectName);

        byte[] pdfBytes = pptPreviewService.convertToPdf(objectName);

        String pdfFileName = objectName.substring(0, objectName.lastIndexOf('.')) + ".pdf";
        if (pdfFileName.contains("/")) {
            pdfFileName = pdfFileName.substring(pdfFileName.lastIndexOf('/') + 1);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfFileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    /**
     * Health check for PPT preview service
     */
    @GetMapping("/preview/health")
    public R<Map<String, Object>> previewHealth() {
        return R.ok(Map.of(
                "service", "PptPreviewService",
                "status", "ready"
        ));
    }
}
