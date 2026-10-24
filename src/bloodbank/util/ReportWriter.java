package bloodbank.util;

import bloodbank.exception.BloodBankException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for exporting analytical and audit reports to plain text files (.txt).
 * 
 * Demonstrates:
 * 1. Java File I/O: Using FileWriter and PrintWriter for formatted disk output.
 * 2. Resource Management: Using try-with-resources with a meaningful finally block
 *    to ensure all buffers are safely flushed and reported.
 * 3. Exception Translation: Catching IOException and wrapping it into our custom BloodBankException.
 */
public class ReportWriter {

    private static final String DEFAULT_REPORTS_DIR = "reports";

    /**
     * Exports a list of report lines to a text file on disk.
     *
     * @param reportTitle human-readable title of the report
     * @param lines       the data lines to write into the report
     * @param fileName    the destination filename (e.g. "donation_history.txt")
     * @return the absolute path to the generated report file
     * @throws BloodBankException if any file I/O error occurs
     */
    public static String writeReport(String reportTitle, List<String> lines, String fileName) throws BloodBankException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BloodBankException("Export filename cannot be empty.");
        }

        // Ensure reports directory exists
        File dir = new File(DEFAULT_REPORTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File targetFile = new File(dir, fileName.trim());
        boolean writeSuccessful = false;

        // try-with-resources automatically closes PrintWriter and FileWriter even if an error occurs
        try (FileWriter fw = new FileWriter(targetFile);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Write report header
            pw.println("================================================================================");
            pw.printf("                 BLOOD BANK MANAGEMENT SYSTEM - REPORT%n");
            pw.printf(" Report: %s%n", reportTitle);
            pw.printf(" Generated On: %s%n", timestamp);
            pw.printf(" Total Records: %d%n", lines != null ? lines.size() : 0);
            pw.println("================================================================================");
            pw.println();

            // Write content lines
            if (lines == null || lines.isEmpty()) {
                pw.println("[No records available for this report]");
            } else {
                for (int i = 0; i < lines.size(); i++) {
                    pw.printf("[%02d] %s%n", (i + 1), lines.get(i));
                }
            }

            pw.println();
            pw.println("================================================================================");
            pw.println(" End of Report");
            pw.println("================================================================================");

            writeSuccessful = true;
            return targetFile.getAbsolutePath();

        } catch (IOException e) { // Catch low-level Java I/O exception
            throw new BloodBankException("Failed to export report to file '" + targetFile.getName() + "': " + e.getMessage(), e);
        } finally {
            // A meaningful finally block: logs operational audit status regardless of whether write succeeded or failed
            if (writeSuccessful) {
                System.out.printf("[ReportWriter] Successfully exported report '%s' (%d records) to: %s%n",
                        reportTitle, (lines != null ? lines.size() : 0), targetFile.getPath());
            } else {
                System.err.printf("[ReportWriter] Write operation finished with errors for: %s%n", targetFile.getPath());
            }
        }
    }
}
