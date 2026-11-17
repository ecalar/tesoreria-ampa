package com.ampa.tesoreria.util;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportUtils {

    private ExportUtils() {}

    /** Genera CSV a partir de cabeceras + filas (ya convertidas a String) */
    public static byte[] toCsv(List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            // BOM para que Excel abra el CSV con acentos correctamente
            writer.write('\uFEFF');

            writer.write(String.join(";", headers));
            writer.write("\n");

            for (List<String> row : rows) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(";");
                    sb.append(escapeCsv(row.get(i)));
                }
                writer.write(sb.toString());
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generando CSV", e);
        }
        return out.toByteArray();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Genera un Excel .xlsx */
    public static byte[] toExcel(String titulo, List<String> headers, List<List<String>> rows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(titulo.length() > 30 ? titulo.substring(0, 30) : titulo);

            // Estilo cabecera
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (List<String> row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.size(); i++) {
                    r.createCell(i).setCellValue(row.get(i));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }

    /** Genera un PDF con una tabla */
    public static byte[] toPdf(String titulo, List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(titulo));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h));
                c.setGrayFill(0.9f);
                table.addCell(c);
            }

            for (List<String> row : rows) {
                for (String v : row) {
                    table.addCell(v == null ? "" : v);
                }
            }

            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    /** Formatea un BigDecimal como moneda (sin símbolo) */
    public static String money(BigDecimal v) {
        if (v == null) return "0,00";
        return String.format("%.2f", v).replace('.', ',');
    }

    /** Formatea un porcentaje */
    public static String pct(BigDecimal v) {
        if (v == null) return "0";
        return String.format("%s", v.stripTrailingZeros().toPlainString()).replace('.', ',');
    }
}