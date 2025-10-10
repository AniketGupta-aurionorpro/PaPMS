package com.aurionpro.papms.utils;

import com.aurionpro.papms.entity.Organization;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class PdfStylingHelper {

    // Define a consistent color scheme
    public static final Color PRIMARY_COLOR = new DeviceRgb(0, 51, 102); // Dark Blue
    public static final Color BORDER_COLOR = new DeviceRgb(204, 204, 204); // Light Gray

    /**
     * Creates a styled header cell for tables.
     */
    public static Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBackgroundColor(PRIMARY_COLOR)
                .setFontColor(DeviceRgb.WHITE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(8);
    }

    /**
     * Creates a styled cell for data labels (e.g., "Invoice Number:").
     */
    public static Cell createLabelCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBold()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5);
    }

    /**
     * Creates a styled cell for data values.
     */
    public static Cell createValueCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(alignment)
                .setPadding(5);
    }

    /**
     * Adds the main header to the document, including the organization logo and the document title.
     */
    public static void addLogoAndTitle(Document document, Organization organization, String title) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        headerTable.setBorder(Border.NO_BORDER);

        // --- Logo Cell ---
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER);
        if (organization.getLogoUrl() != null && !organization.getLogoUrl().isEmpty()) {
            try {
                Image logo = new Image(ImageDataFactory.create(new URL(organization.getLogoUrl())));
                logo.setAutoScale(true);
                logo.setMaxHeight(60);
                logoCell.add(logo);
            } catch (MalformedURLException e) {
                log.warn("Invalid logo URL for organization {}: {}", organization.getId(), organization.getLogoUrl());
                logoCell.add(new Paragraph(organization.getCompanyName()).setBold().setFontSize(18));
            } catch (IOException e) {
                log.warn("Could not load image from URL for organization {}: {}", organization.getId(), e.getMessage());
                logoCell.add(new Paragraph(organization.getCompanyName()).setBold().setFontSize(18));
            }
        } else {
            logoCell.add(new Paragraph(organization.getCompanyName()).setBold().setFontSize(18));
        }
        headerTable.addCell(logoCell);

        // --- Title Cell ---
        Cell titleCell = new Cell().add(new Paragraph(title))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setFontSize(26)
                .setFontColor(PRIMARY_COLOR)
                .setBold();
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));
        document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)));
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds a standard footer with page numbers.
     */
    public static void addFooter(Document document) {
        PdfDocument pdfDoc = document.getPdfDocument();
        int numberOfPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(i));
            Rectangle pageSize = pdfDoc.getPage(i).getPageSize();

            // Add a footer line
            canvas.setStrokeColor(BORDER_COLOR)
                    .moveTo(pageSize.getLeft() + 36, pageSize.getBottom() + 30)
                    .lineTo(pageSize.getRight() - 36, pageSize.getBottom() + 30)
                    .stroke();

            // Add page number
            document.showTextAligned(new Paragraph(String.format("Page %d of %d", i, numberOfPages))
                            .setFontSize(8),
                    pageSize.getWidth() / 2, pageSize.getBottom() + 15, i,
                    TextAlignment.CENTER, VerticalAlignment.MIDDLE, 0);
        }
    }
}