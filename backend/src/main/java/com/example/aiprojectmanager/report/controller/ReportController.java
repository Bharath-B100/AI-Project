package com.example.aiprojectmanager.report.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.report.dto.StatusReportDTO;
import com.example.aiprojectmanager.report.service.PdfReportService;
import com.example.aiprojectmanager.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST endpoints for automated reporting.
 *
 * GET  /api/v1/projects/{projectId}/report/weekly  — JSON weekly status report
 * GET  /api/v1/projects/{projectId}/report/pdf     — PDF download
 */
@RestController
@RequestMapping("/projects/{projectId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService        reportService;
    private final PdfReportService     pdfReportService;
    private final CurrentUserService   currentUserService;

    /**
     * Generates and returns the weekly AI executive status report as JSON.
     */
    @GetMapping("/weekly")
    public ResponseEntity<StatusReportDTO> getWeeklyReport(@PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        StatusReportDTO report = reportService.generateWeeklyReport(projectId, userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Generates a multi-page PDF containing project health, Gantt summary, and budget breakdown,
     * and returns it as a file download attachment.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        StatusReportDTO report = reportService.generateWeeklyReport(projectId, userId);
        byte[] pdf = pdfReportService.generateProjectReportPdf(report);

        String filename = String.format("project_%d_report_%s.pdf",
                projectId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
