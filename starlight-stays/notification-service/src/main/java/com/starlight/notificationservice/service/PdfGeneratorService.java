package com.starlight.notificationservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.starlight.notificationservice.model.Notification;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    private static final Color GOLD_ACCENT = new Color(212, 175, 55);
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    public byte[] generateLuxuryVoucherPdf(Notification n) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, GOLD_ACCENT);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_MUTED);
            Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, DARK_BG);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_BG);
            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 13, GOLD_ACCENT);

            // Header Banner
            Paragraph brandTitle = new Paragraph("✦ STARLIGHT STAYS & RESORTS ✦", titleFont);
            brandTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(brandTitle);

            Paragraph docType = new Paragraph("OFFICIAL CELESTIAL RESERVATION VOUCHER & ITINERARY", subtitleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(18);
            document.add(docType);

            // Voucher Code Box Table
            PdfPTable codeTable = new PdfPTable(1);
            codeTable.setWidthPercentage(100);
            PdfPCell codeCell = new PdfPCell();
            codeCell.setBackgroundColor(new Color(248, 250, 252));
            codeCell.setBorderColor(GOLD_ACCENT);
            codeCell.setBorderWidth(1.5f);
            codeCell.setPadding(12);

            Paragraph codeP = new Paragraph("DIGITAL VOUCHER KEY: " + (n.getVoucherCode() != null ? n.getVoucherCode() : "STARLIGHT-VIP-001"), codeFont);
            codeP.setAlignment(Element.ALIGN_CENTER);
            codeCell.addElement(codeP);

            Paragraph securityHash = new Paragraph("CRYPTOGRAPHIC VERIFICATION HASH: " + (n.getSecurityHash() != null ? n.getSecurityHash() : "0x7F9A8B2C4D6E"), FontFactory.getFont(FontFactory.COURIER, 8, TEXT_MUTED));
            securityHash.setAlignment(Element.ALIGN_CENTER);
            codeCell.addElement(securityHash);
            codeTable.addCell(codeCell);
            codeTable.setSpacingAfter(18);
            document.add(codeTable);

            // Details Table
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1, 1});

            addTableCell(detailsTable, "GUEST RESIDENT", n.getGuestName() != null ? n.getGuestName() : "VIP Resident", boldFont, bodyFont);
            addTableCell(detailsTable, "BOOKING REFERENCE", "#STR-" + (n.getBookingId() != null ? n.getBookingId() : "1001"), boldFont, bodyFont);
            addTableCell(detailsTable, "SUITE SELECTION", n.getRoomName() != null ? n.getRoomName() : "Celestial Penthouse Observatory", boldFont, bodyFont);
            addTableCell(detailsTable, "RESERVATION STATUS", "CONFIRMED & GUARANTEED", boldFont, bodyFont);
            addTableCell(detailsTable, "TOTAL INVESTED", "$" + (n.getTotalPrice() != null ? n.getTotalPrice() : 0.0) + " USD (All Taxes & Service Included)", boldFont, bodyFont);
            addTableCell(detailsTable, "ISSUED AT", n.getCreatedAt() != null ? n.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")) : "Instant Issuance", boldFont, bodyFont);

            detailsTable.setSpacingAfter(20);
            document.add(detailsTable);

            // Curated VIP Itinerary Section
            Paragraph itineraryHeader = new Paragraph("CURATED 3-DAY RESIDENCE EXPERIENCE", sectionHeaderFont);
            itineraryHeader.setSpacingAfter(8);
            document.add(itineraryHeader);

            PdfPTable itinTable = new PdfPTable(3);
            itinTable.setWidthPercentage(100);
            itinTable.setWidths(new float[]{1, 1, 1});

            addItineraryDayCell(itinTable, "DAY 1: ARRIVAL & STELLAR WELCOME", "• Sky-Helipad private transfer\n• Vintage Dom Pérignon uncorking\n• Twilight plunge pool & astronomy tour");
            addItineraryDayCell(itinTable, "DAY 2: OCEAN & GASTRONOMY", "• Private yacht charter to coral reef\n• 5-Course Chef Antoine rooftop tasting\n• Celestial Spa aromatherapy massage");
            addItineraryDayCell(itinTable, "DAY 3: FAREWELL VISTAS", "• Sunrise breakfast on terrace\n• Late check-out with head butler\n• Rolls-Royce VIP airport transfer");

            itinTable.setSpacingAfter(25);
            document.add(itinTable);

            // Footer Note
            Paragraph footer = new Paragraph("Present this digital voucher upon arrival at the VIP Sky-Lobby. Head Butler Lord Alistair is assigned to your suite.\nStarlight Stays Luxury Hotels & Resorts Mesh • Singapore • Maldives • Zurich • Tokyo", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF voucher", e);
        }
    }

    private void addTableCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED)));
        cell.addElement(new Paragraph(value, valFont));
        table.addCell(cell);
    }

    private void addItineraryDayCell(PdfPTable table, String dayTitle, String schedule) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(203, 213, 225));
        cell.addElement(new Paragraph(dayTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DARK_BG)));
        Paragraph p = new Paragraph(schedule, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
        p.setSpacingBefore(4);
        cell.addElement(p);
        table.addCell(cell);
    }
}
