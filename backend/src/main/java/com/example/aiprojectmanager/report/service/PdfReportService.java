package com.example.aiprojectmanager.report.service;

import com.example.aiprojectmanager.report.dto.StatusReportDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a multi-page PDF project health report using pure Java without any
 * third-party PDF library dependency (to keep the pom clean and compilable on
 * any machine).  The output is a hand-crafted minimal PDF 1.4 byte stream that
 * renders correctly in every modern PDF viewer.
 *
 * Pages produced:
 *   1. Cover — project name, report date, overall status colour
 *   2. Schedule & Progress — actual vs expected, task counts, variance
 *   3. Budget Health — spend vs budget, remaining, health category
 *   4. Team & Risk — workload summary, open risks, delay probability
 *   5. AI Executive Summary & Recommendations
 */
@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateProjectReportPdf(StatusReportDTO r) {
        try {
            MinimalPdfWriter pdf = new MinimalPdfWriter();

            // ── Page 1: Cover ──────────────────────────────────────────────
            pdf.newPage();
            pdf.drawRect(0, 720, 595, 120, hexToRgb(statusHex(r.getOverallStatusColor())));
            pdf.text(r.getProjectName(), 40, 790, 22, 1, 1, 1);
            pdf.text("Weekly Status Report  •  " + (r.getReportDate() != null ? r.getReportDate().format(DATE_FMT) : ""), 40, 762, 12, 1, 1, 1);
            pdf.text("Methodology: " + r.getMethodology() + "   |   Status: " + r.getOverallStatusColor(), 40, 744, 10, 1, 1, 1);

            pdf.text("Executive Summary", 40, 700, 14, 0.1, 0.1, 0.1);
            pdf.wrappedText(r.getExecutiveSummary(), 40, 682, 515, 10);

            // ── Page 2: Schedule & Progress ────────────────────────────────
            pdf.newPage();
            pdf.sectionHeader("Schedule & Progress", 770);
            int row = 740;
            row = pdf.kv("Overall Health",       r.getScheduleHealth(),                 row); row -= 4;
            row = pdf.kv("Actual Progress",       fmt1(r.getActualProgressPct())   + "%", row);
            row = pdf.kv("Expected Progress",     fmt1(r.getExpectedProgressPct()) + "%", row);
            row = pdf.kv("Variance",              fmt1(r.getProgressVariancePct()) + "%", row); row -= 4;
            row = pdf.kv("Total Tasks",           String.valueOf(r.getTotalTasks()),        row);
            row = pdf.kv("Completed",             String.valueOf(r.getCompletedTasks()),    row);
            row = pdf.kv("Overdue",               String.valueOf(r.getOverdueTasks()),      row);
            row = pdf.kv("Blocked",               String.valueOf(r.getBlockedTasks()),      row);

            if (r.getMilestones() != null && !r.getMilestones().isEmpty()) {
                row -= 14;
                pdf.text("Milestone Snapshot", 40, row, 11, 0.2, 0.2, 0.6); row -= 14;
                for (StatusReportDTO.MilestoneSnapshot m : r.getMilestones()) {
                    String line = m.getName() + " — " + fmt1(m.getCompletionPct()) + "% (" + m.getStatus() + ")";
                    if (m.getTargetDate() != null) line += "  Target: " + m.getTargetDate().format(DATE_FMT);
                    pdf.text(line, 50, row, 9, 0.3, 0.3, 0.3); row -= 12;
                    if (row < 60) break;
                }
            }

            // ── Page 3: Budget ─────────────────────────────────────────────
            pdf.newPage();
            pdf.sectionHeader("Budget Health", 770);
            row = 740;
            row = pdf.kv("Budget Status",        r.getBudgetHealth(),                    row); row -= 4;
            row = pdf.kv("Approved Budget",       "₹ " + fmt2(r.getApprovedBudget()),     row);
            row = pdf.kv("Actual Cost",           "₹ " + fmt2(r.getActualCost()),          row);
            row = pdf.kv("Remaining Budget",      "₹ " + fmt2(r.getRemainingBudget()),     row);
            pdf.kv("Budget Used",                 fmt1(r.getBudgetUsedPct()) + "%",        row);

            // ── Page 4: Team & Risk ────────────────────────────────────────
            pdf.newPage();
            pdf.sectionHeader("Team & Risk", 770);
            row = 740;
            row = pdf.kv("Total Team Members",    String.valueOf(r.getTotalTeamMembers()),     row);
            row = pdf.kv("Overloaded Members",    String.valueOf(r.getOverloadedMembers()),    row);
            row = pdf.kv("Avg Utilisation",       fmt1(r.getAvgUtilizationPct()) + "%",       row); row -= 8;
            row = pdf.kv("Overall Risk Level",    r.getOverallRiskLevel(),                    row);
            row = pdf.kv("Open Risks",            String.valueOf(r.getOpenRisks()),            row);
            row = pdf.kv("Critical Risks",        String.valueOf(r.getCriticalRisks()),        row);
            pdf.kv("Delay Probability",           fmt1(r.getDelayProbabilityPct()) + "%",     row);

            // ── Page 5: Recommendations ────────────────────────────────────
            pdf.newPage();
            pdf.sectionHeader("AI Recommendations & Next Steps", 770);
            row = 740;
            row = pdf.bulletList("Key Accomplishments", r.getKeyAccomplishments(), row);
            row -= 10;
            row = pdf.bulletList("Active Blockers",     r.getActiveBlockers(),     row);
            row -= 10;
            pdf.bulletList("Next Steps",                r.getNextStepRecommendations(), row);

            return pdf.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String statusHex(String color) {
        if ("RED".equals(color))   return "#dc2626";
        if ("AMBER".equals(color)) return "#d97706";
        return "#059669"; // GREEN
    }

    private static double[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new double[]{
            Integer.parseInt(hex.substring(0,2), 16) / 255.0,
            Integer.parseInt(hex.substring(2,4), 16) / 255.0,
            Integer.parseInt(hex.substring(4,6), 16) / 255.0
        };
    }

    private static String fmt1(double v) { return String.format("%.1f", v); }
    private static String fmt2(double v) { return String.format("%,.2f", v); }

    // ── Inner minimal PDF writer ──────────────────────────────────────────────

    private static class MinimalPdfWriter {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final List<Integer>          offsets = new java.util.ArrayList<>();
        private final List<String>           streams = new java.util.ArrayList<>();
        private final List<StringBuilder>    pages   = new java.util.ArrayList<>();
        private StringBuilder                cur     = null;

        MinimalPdfWriter() { write("%PDF-1.4\n"); }

        void newPage() {
            if (cur != null) streams.add(cur.toString());
            cur = new StringBuilder();
            pages.add(cur);
        }

        // ── Drawing primitives ─────────────────────────────────────────────

        void drawRect(float x, float y, float w, float h, double[] rgb) {
            cur.append(String.format("%.2f %.2f %.2f rg %.1f %.1f %.1f %.1f re f\n",
                    rgb[0], rgb[1], rgb[2], x, y, w, h));
        }

        void text(String s, float x, float y, float size, double r, double g, double b) {
            cur.append(String.format("BT /F1 %.1f Tf %.2f %.2f %.2f rg %.1f %.1f Td (%s) Tj ET\n",
                    size, r, g, b, x, y, escapePdf(s)));
        }

        void wrappedText(String s, float x, float startY, float maxW, float size) {
            if (s == null) return;
            String[] words = s.split("\\s+");
            StringBuilder line = new StringBuilder();
            float y = startY;
            for (String w : words) {
                if (line.length() + w.length() > (int)(maxW / (size * 0.52))) {
                    text(line.toString().trim(), x, y, size, 0.2, 0.2, 0.2);
                    y -= (size + 3);
                    line = new StringBuilder();
                }
                line.append(w).append(' ');
            }
            if (!line.isEmpty()) text(line.toString().trim(), x, y, size, 0.2, 0.2, 0.2);
        }

        void sectionHeader(String title, float y) {
            drawRect(30, y - 4, 535, 22, new double[]{0.2, 0.2, 0.6});
            text(title, 40, y + 4, 13, 1, 1, 1);
        }

        int kv(String key, String value, int y) {
            text(key + ":", 50, y, 10, 0.35, 0.35, 0.35);
            text(value,     260, y, 10, 0.1,  0.1,  0.1);
            return y - 15;
        }

        int bulletList(String title, List<String> items, int y) {
            if (items == null || items.isEmpty()) return y;
            text(title, 40, y, 11, 0.2, 0.2, 0.6); y -= 14;
            for (String item : items) {
                wrappedText("•  " + item, 52, y, 490, 9);
                y -= 22;
                if (y < 60) break;
            }
            return y;
        }

        // ── PDF serialisation ──────────────────────────────────────────────

        byte[] toBytes() {
            if (cur != null) streams.add(cur.toString());

            // Write font resource object (obj 1)
            offsets.add(buf.size()); write("1 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            // Write page content streams and page objects
            int pageCount = streams.size();
            int baseObj   = 2; // content streams start at obj 2
            List<Integer> pageObjs = new java.util.ArrayList<>();

            for (int i = 0; i < pageCount; i++) {
                // content stream
                byte[] streamBytes = streams.get(i).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                offsets.add(buf.size());
                int csObj = baseObj + i * 2;
                write(csObj + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
                writeBytes(streamBytes);
                write("\nendstream\nendobj\n");

                // page object
                offsets.add(buf.size());
                int pgObj = csObj + 1;
                pageObjs.add(pgObj);
                write(pgObj + " 0 obj\n<< /Type /Page /Parent " + (baseObj + pageCount * 2) + " 0 R"
                        + " /MediaBox [0 0 595 842]"
                        + " /Contents " + csObj + " 0 R"
                        + " /Resources << /Font << /F1 1 0 R >> >> >>\nendobj\n");
            }

            // Pages dict
            int pagesObj = baseObj + pageCount * 2;
            offsets.add(buf.size());
            StringBuilder kids = new StringBuilder("[");
            for (int pg : pageObjs) kids.append(pg).append(" 0 R ");
            kids.append("]");
            write(pagesObj + " 0 obj\n<< /Type /Pages /Kids " + kids + " /Count " + pageCount + " >>\nendobj\n");

            // Catalog
            int catObj = pagesObj + 1;
            offsets.add(buf.size());
            write(catObj + " 0 obj\n<< /Type /Catalog /Pages " + pagesObj + " 0 R >>\nendobj\n");

            // xref
            int xrefOffset = buf.size();
            int totalObjs  = catObj + 1;
            write("xref\n0 " + totalObjs + "\n");
            write("0000000000 65535 f \n");
            for (int off : offsets) write(String.format("%010d 00000 n \n", off));

            write("trailer\n<< /Size " + totalObjs + " /Root " + catObj + " 0 R >>\n");
            write("startxref\n" + xrefOffset + "\n%%EOF\n");
            return buf.toByteArray();
        }

        private void write(String s) {
            try { buf.write(s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)); }
            catch (Exception ignored) {}
        }

        private void writeBytes(byte[] b) {
            try { buf.write(b); } catch (Exception ignored) {}
        }

        private static String escapePdf(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", " ");
        }
    }
}
