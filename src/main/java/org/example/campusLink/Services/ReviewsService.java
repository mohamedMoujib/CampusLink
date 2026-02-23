package org.example.campusLink.Services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.campusLink.entities.Reviews;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewsService {

    private ReviewsDAO reviewsDAO = new ReviewsDAO();
    private TrustPointsService trustService = new TrustPointsService();

    // ===================== CREATE =====================

    public void addReview(Reviews r) {

        if (reviewsDAO.existsByStudentAndService(
                r.getStudentId(), r.getReservationId())) {
            throw new IllegalStateException("Avis déjà existant");
        }

        reviewsDAO.save(r);

        try {
            trustService.applyPoints(
                    r.getPrestataireId(),
                    r.getRating()
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== READ =====================

    public Reviews getReview(int id) {
        return reviewsDAO.findById(id);
    }

    public List<Reviews> getReviewsByStudentWithDetails(int studentId) {
        return reviewsDAO.findByStudentWithDetails(studentId);
    }

    public List<Reviews> getReviewsByTutor(int tutorId) {
        return reviewsDAO.findByTutorWithDetails(tutorId);
    }

    public List<Reviews> getAllReviewsWithDetails() {
        return reviewsDAO.findAllWithDetails();
    }

    // ===================== REPORT MANAGEMENT =====================

    public void reportReview(int reviewId, String reason) {
        reviewsDAO.reportReview(reviewId, reason);
    }

    public void unreportReview(int reviewId) {
        reviewsDAO.unreportReview(reviewId);
    }

    public int getReportedReviewsCount() {
        return reviewsDAO.getReportedReviewsCount();
    }

    // ===================== UPDATE =====================

    public void updateReview(int id, int newRating, String newComment) {

        Reviews old = reviewsDAO.findById(id);
        if (old == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        int diff = newRating - old.getRating();

        old.setRating(newRating);
        old.setComment(newComment);
        reviewsDAO.update(old);

        try {
            trustService.applyPoints(
                    old.getPrestataireId(),
                    diff
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== DELETE =====================

    public void deleteReview(int id) {

        Reviews r = reviewsDAO.findById(id);
        if (r == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        reviewsDAO.delete(id);

        try {
            trustService.applyPoints(
                    r.getPrestataireId(),
                    -r.getRating()
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== GET TRUST POINTS =====================

    public int getTrustPoints(int userId) {
        return reviewsDAO.getTrustPointsByUserId(userId);
    }

    // ===================== 🔥 EXPORT TO CSV =====================

    public String exportToCSV(List<Reviews> reviews) {
        try {
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);

            // En-têtes
            String[] headers = {
                    "ID", "Étudiant", "Prestataire", "Service", "Note",
                    "Commentaire", "Réservation ID", "Signalé", "Raison", "Date signalement"
            };
            csvWriter.writeNext(headers);

            // Données
            for (Reviews review : reviews) {
                String[] data = {
                        String.valueOf(review.getId()),
                        review.getStudentName() != null ? review.getStudentName() : "N/A",
                        review.getPrestataireName() != null ? review.getPrestataireName() : "N/A",
                        review.getServiceTitle() != null ? review.getServiceTitle() : "N/A",
                        String.valueOf(review.getRating()),
                        review.getComment(),
                        String.valueOf(review.getReservationId()),
                        review.isReported() ? "Oui" : "Non",
                        review.getReportReason() != null ? review.getReportReason() : "",
                        review.getReportedAt() != null ?
                                review.getReportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
                };
                csvWriter.writeNext(data);
            }

            csvWriter.close();
            return stringWriter.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ===================== 🔥 EXPORT TO EXCEL =====================

    public byte[] exportToExcel(List<Reviews> reviews) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Avis");

            // 🔥 Style pour l'en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // 🔥 Utiliser explicitement Apache POI Font
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 🔥 Style pour les cellules signalées
            CellStyle reportedStyle = workbook.createCellStyle();
            reportedStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            reportedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // En-têtes
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID", "Étudiant", "Prestataire", "Service", "Note",
                    "Commentaire", "Réservation ID", "Signalé", "Raison", "Date signalement"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowNum = 1;
            for (Reviews review : reviews) {
                Row row = sheet.createRow(rowNum++);
                CellStyle rowStyle = review.isReported() ? reportedStyle : null;

                createCell(row, 0, review.getId(), rowStyle);
                createCell(row, 1, review.getStudentName() != null ? review.getStudentName() : "N/A", rowStyle);
                createCell(row, 2, review.getPrestataireName() != null ? review.getPrestataireName() : "N/A", rowStyle);
                createCell(row, 3, review.getServiceTitle() != null ? review.getServiceTitle() : "N/A", rowStyle);
                createCell(row, 4, review.getRating(), rowStyle);
                createCell(row, 5, review.getComment(), rowStyle);
                createCell(row, 6, review.getReservationId(), rowStyle);
                createCell(row, 7, review.isReported() ? "Oui" : "Non", rowStyle);
                createCell(row, 8, review.getReportReason() != null ? review.getReportReason() : "", rowStyle);
                createCell(row, 9, review.getReportedAt() != null ?
                        review.getReportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "", rowStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir en bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }

        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    // ===================== 🔥 EXPORT TO PDF =====================

    public byte[] exportToPDF(List<Reviews> reviews) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // 🔥 Titre - Utiliser explicitement iText Font
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Rapport des Avis - CampusLink", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 🔥 Date
            com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph date = new Paragraph("Généré le : " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Largeurs des colonnes
            float[] columnWidths = {5f, 12f, 12f, 15f, 7f, 25f, 8f, 7f, 15f, 12f};
            table.setWidths(columnWidths);

            // 🔥 En-têtes
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
            String[] headers = {
                    "ID", "Étudiant", "Prestataire", "Service", "Note",
                    "Commentaire", "Rés. ID", "Signalé", "Raison", "Date signal."
            };

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(79, 70, 229));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // 🔥 Données
            com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (Reviews review : reviews) {
                BaseColor bgColor = review.isReported() ?
                        new BaseColor(254, 243, 199) : BaseColor.WHITE;

                addTableCell(table, String.valueOf(review.getId()), cellFont, bgColor);
                addTableCell(table, review.getStudentName() != null ? review.getStudentName() : "N/A", cellFont, bgColor);
                addTableCell(table, review.getPrestataireName() != null ? review.getPrestataireName() : "N/A", cellFont, bgColor);
                addTableCell(table, review.getServiceTitle() != null ? review.getServiceTitle() : "N/A", cellFont, bgColor);
                addTableCell(table, String.valueOf(review.getRating()), cellFont, bgColor);
                addTableCell(table, truncate(review.getComment(), 100), cellFont, bgColor);
                addTableCell(table, String.valueOf(review.getReservationId()), cellFont, bgColor);
                addTableCell(table, review.isReported() ? "Oui" : "Non", cellFont, bgColor);
                addTableCell(table, review.getReportReason() != null ? truncate(review.getReportReason(), 50) : "", cellFont, bgColor);
                addTableCell(table, review.getReportedAt() != null ?
                        review.getReportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) : "", cellFont, bgColor);
            }

            document.add(table);

            // Statistiques
            Paragraph stats = new Paragraph("\n\nStatistiques :",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(stats);

            long positive = reviews.stream().filter(r -> r.getRating() > 0).count();
            long negative = reviews.stream().filter(r -> r.getRating() < 0).count();
            long reported = reviews.stream().filter(Reviews::isReported).count();

            Paragraph statsContent = new Paragraph(
                    "Total des avis : " + reviews.size() + "\n" +
                            "Avis positifs : " + positive + "\n" +
                            "Avis négatifs : " + negative + "\n" +
                            "Avis signalés : " + reported,
                    FontFactory.getFont(FontFactory.HELVETICA, 10)
            );
            document.add(statsContent);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addTableCell(PdfPTable table, String text, com.itextpdf.text.Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}