package watermarking.core;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for generating test reports from watermark evaluation results.
 * Creates Excel spreadsheets showing test results and comparative analyses.
 */
public class WatermarkTestReport {

    /**
     * Generates an Excel report from a list of test results.
     *
     * @param results The test results to include in the report
     * @param outputPath File path where the report should be saved
     * @throws IOException If there is an error writing the report
     */
    public static void generateReport(List<WatermarkResult> results, String outputPath) throws IOException {
        Logger.info("Generating watermark test report to: " + outputPath);

        // Create a new workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create a sheet for all results
            Sheet resultsSheet = workbook.createSheet("Test Results");

            // Create header row
            Row headerRow = resultsSheet.createRow(0);
            String[] headers = {
                    "Test ID", "Timestamp", "Attack", "Parameters", "Method",
                    "Component", "Parameter", "BER", "NC", "PSNR", "WNR",
                    "Quality Rating", "Robustness", "Watermark Config"
            };

            // Add header cells with styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (WatermarkResult result : results) {
                Row row = resultsSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(result.getTestId());
                row.createCell(1).setCellValue(result.getTimestamp());
                row.createCell(2).setCellValue(result.getAttackName());
                row.createCell(3).setCellValue(result.getAttackParameters());
                row.createCell(4).setCellValue(result.getMethod());
                row.createCell(5).setCellValue(result.getComponent());
                row.createCell(6).setCellValue(result.getParameter());

                // Format numeric values
                Cell berCell = row.createCell(7);
                berCell.setCellValue(result.getBer());

                Cell ncCell = row.createCell(8);
                ncCell.setCellValue(result.getNc());

                Cell psnrCell = row.createCell(9);
                psnrCell.setCellValue(result.getPsnr());

                Cell wnrCell = row.createCell(10);
                wnrCell.setCellValue(result.getWnr());

                row.createCell(11).setCellValue(result.getQualityRating());
                row.createCell(12).setCellValue(result.getRobustnessLevel());
                row.createCell(13).setCellValue(result.getWatermarkConfig());
            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                resultsSheet.autoSizeColumn(i);
            }

            // Create comparison sheet for attacks
            createComparisonSheet(workbook, results, "Attack Comparison",
                    result -> result.getAttackName() + " (" + result.getAttackParameters() + ")");

            // Create comparison sheet for methods
            createComparisonSheet(workbook, results, "Method Comparison",
                    result -> result.getMethod() + " (" + result.getComponent() + ", " + result.getParameter() + ")");

            // Create comparison sheet for watermark configurations
            createComparisonSheet(workbook, results, "Watermark Config Comparison",
                    WatermarkResult::getWatermarkConfig);

            // Write the workbook to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }

            Logger.info("Watermark test report successfully generated to: " + outputPath);
        }
    }

    /**
     * Creates a comparison sheet that groups results by a specific attribute.
     *
     * @param workbook The workbook to add the sheet to
     * @param results The test results to include
     * @param sheetName The name for the new sheet
     * @param groupKeyExtractor Function to extract the grouping key from a result
     */
    private static void createComparisonSheet(Workbook workbook, List<WatermarkResult> results,
                                              String sheetName, java.util.function.Function<WatermarkResult, String> groupKeyExtractor) {
        Sheet sheet = workbook.createSheet(sheetName);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Group", "Average BER", "Best BER", "Worst BER", "Average NC",
                "Test Count", "Quality Distribution"
        };

        // Style the header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Group results
        Map<String, List<WatermarkResult>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(groupKeyExtractor));

        // Generate comparison data
        int rowNum = 1;
        for (Map.Entry<String, List<WatermarkResult>> entry : groupedResults.entrySet()) {
            String groupName = entry.getKey();
            List<WatermarkResult> groupResults = entry.getValue();

            // Calculate statistics
            double avgBer = groupResults.stream()
                    .mapToDouble(WatermarkResult::getBer)
                    .average().orElse(0);

            double minBer = groupResults.stream()
                    .mapToDouble(WatermarkResult::getBer)
                    .min().orElse(0);

            double maxBer = groupResults.stream()
                    .mapToDouble(WatermarkResult::getBer)
                    .max().orElse(0);

            double avgNc = groupResults.stream()
                    .mapToDouble(WatermarkResult::getNc)
                    .average().orElse(0);

            int count = groupResults.size();

            // Count quality ratings
            Map<String, Long> qualityCounts = groupResults.stream()
                    .collect(Collectors.groupingBy(
                            WatermarkResult::getQualityRating, Collectors.counting()));

            String qualityDistribution = qualityCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(s -> {
                        // Sort by quality level
                        switch (s) {
                            case "Excellent": return 0;
                            case "Very Good": return 1;
                            case "Good": return 2;
                            case "Fair": return 3;
                            case "Poor": return 4;
                            case "Failed": return 5;
                            default: return 6;
                        }
                    })))
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));

            // Create row
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(groupName);

            // Format numeric cells
            Cell avgBerCell = row.createCell(1);
            avgBerCell.setCellValue(avgBer);

            Cell minBerCell = row.createCell(2);
            minBerCell.setCellValue(minBer);

            Cell maxBerCell = row.createCell(3);
            maxBerCell.setCellValue(maxBer);

            Cell avgNcCell = row.createCell(4);
            avgNcCell.setCellValue(avgNc);

            row.createCell(5).setCellValue(count);
            row.createCell(6).setCellValue(qualityDistribution);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Exports a list of results to a CSV file.
     * Simpler alternative to Excel for quick analysis.
     *
     * @param results The test results to export
     * @param outputPath File path where the CSV should be saved
     * @throws IOException If there is an error writing the file
     */
    public static void exportToCsv(List<WatermarkResult> results, String outputPath) throws IOException {
        Logger.info("Exporting watermark test results to CSV: " + outputPath);

        StringBuilder csv = new StringBuilder();

        // Add header
        csv.append("TestID,Timestamp,Attack,Parameters,Method,Component,Parameter,BER,NC,PSNR,WNR,QualityRating,Robustness,WatermarkConfig\n");

        // Add data rows
        for (WatermarkResult result : results) {
            csv.append(result.getTestId()).append(",");
            csv.append(escapeForCsv(result.getTimestamp())).append(",");
            csv.append(escapeForCsv(result.getAttackName())).append(",");
            csv.append(escapeForCsv(result.getAttackParameters())).append(",");
            csv.append(escapeForCsv(result.getMethod())).append(",");
            csv.append(escapeForCsv(result.getComponent())).append(",");
            csv.append(escapeForCsv(result.getParameter())).append(",");
            csv.append(result.getBer()).append(",");
            csv.append(result.getNc()).append(",");
            csv.append(result.getPsnr()).append(",");
            csv.append(result.getWnr()).append(",");
            csv.append(escapeForCsv(result.getQualityRating())).append(",");
            csv.append(escapeForCsv(result.getRobustnessLevel())).append(",");
            csv.append(escapeForCsv(result.getWatermarkConfig())).append("\n");
        }

        // Write to file
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(csv.toString().getBytes());
        }

        Logger.info("CSV export completed successfully: " + outputPath);
    }

    /**
     * Escapes a string for CSV output.
     *
     * @param value The string to escape
     * @return Properly escaped string for CSV
     */
    private static String escapeForCsv(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}