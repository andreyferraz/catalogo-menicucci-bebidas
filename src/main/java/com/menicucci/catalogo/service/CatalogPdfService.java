package com.menicucci.catalogo.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import com.menicucci.catalogo.model.Produto;

@Service
public class CatalogPdfService {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(PT_BR);

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public byte[] gerarCatalogoPdf(List<Produto> produtos) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            addHeader(document);
            addIntro(document, produtos.size());
            addProductsTable(document, produtos);
            addFooter(document);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Nao foi possivel gerar o catalogo em PDF.", ex);
        }
    }

    private void addHeader(Document document) throws DocumentException, IOException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 1.15f, 2.85f });

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image logo = Image.getInstance(loadLogoUrl());
        logo.scaleToFit(110, 110);
        logoCell.addElement(logo);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.addElement(new Paragraph("Bebidas Menicucci", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(58, 11, 13))));
        titleCell.addElement(new Paragraph("Catálogo oficial de produtos", FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(116, 95, 85))));
        Paragraph subtitle = new Paragraph("Seleção de produtos pronta para impressão e compartilhamento.", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(116, 95, 85)));
        subtitle.setSpacingBefore(8f);
        titleCell.addElement(subtitle);

        header.addCell(logoCell);
        header.addCell(titleCell);
        document.add(header);

        Paragraph divider = new Paragraph(" ");
        divider.setSpacingBefore(10f);
        divider.setSpacingAfter(10f);
        document.add(divider);
    }

    private void addIntro(Document document, int totalProdutos) throws DocumentException {
        Paragraph intro = new Paragraph(
                "Este catálogo reúne todos os produtos cadastrados no painel administrativo.",
                FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(74, 74, 74)));
        intro.setSpacingAfter(6f);
        document.add(intro);

        Paragraph count = new Paragraph(
                "Total de produtos: " + totalProdutos,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(90, 16, 20)));
        count.setSpacingAfter(14f);
        document.add(count);
    }

    private void addProductsTable(Document document, List<Produto> produtos) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 0.9f, 1.5f, 0.9f, 2.4f, 1.1f });
        table.setHeaderRows(1);

        addHeaderCell(table, "Imagem");
        addHeaderCell(table, "Produto");
        addHeaderCell(table, "Categoria");
        addHeaderCell(table, "Descrição");
        addHeaderCell(table, "Preço");

        for (Produto produto : produtos) {
            addThumbnailCell(table, produto.getImagemUrl());
            addBodyCell(table, safeText(produto.getNome()), true);
            addBodyCell(table, safeText(produto.getCategoria()), false);
            addBodyCell(table, safeText(produto.getDescricao()), false);
            addBodyCell(table, formatPrice(produto.getPreco()), true);
        }

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Gerado automaticamente pelo painel admin da Bebidas Menicucci.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(116, 95, 85)));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(14f);
        document.add(footer);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(new Color(90, 16, 20));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, boolean highlight) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, highlight ? new Color(58, 11, 13) : new Color(42, 23, 19));
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addThumbnailCell(PdfPTable table, String imageName) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Image thumbnail = loadProductThumbnail(imageName);
        if (thumbnail != null) {
            thumbnail.scaleToFit(52, 52);
            cell.addElement(thumbnail);
        } else {
            cell.addElement(new Paragraph("-", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(116, 95, 85))));
        }

        table.addCell(cell);
    }

    private Image loadProductThumbnail(String imageName) {
        try {
            Path imagePath = resolveProductImagePath(imageName);
            if (imagePath == null || !Files.exists(imagePath)) {
                return null;
            }

            BufferedImage sourceImage = readImageForPdf(imagePath);
            if (sourceImage == null) {
                return null;
            }

            BufferedImage thumbnailImage = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = thumbnailImage.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, 96, 96);

                int width = sourceImage.getWidth();
                int height = sourceImage.getHeight();
                float scale = Math.min(96f / width, 96f / height);
                int scaledWidth = Math.round(width * scale);
                int scaledHeight = Math.round(height * scale);
                int x = (96 - scaledWidth) / 2;
                int y = (96 - scaledHeight) / 2;

                graphics.drawImage(sourceImage, x, y, scaledWidth, scaledHeight, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(thumbnailImage, "png", thumbnailOutput);
            return Image.getInstance(thumbnailOutput.toByteArray());
        } catch (Exception ex) {
            return null;
        }
    }

    private BufferedImage readImageForPdf(Path imagePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(imagePath)) {
            BufferedImage image = javax.imageio.ImageIO.read(inputStream);
            if (image != null) {
                return image;
            }
        }

        if (!isWebp(imagePath)) {
            return null;
        }

        Path tempPng = createPrivateTempFile("catalog-pdf-thumb-", ".png");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "dwebp",
                    imagePath.toString(),
                    "-o",
                    tempPng.toString());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            if (exitCode != 0 || !Files.exists(tempPng)) {
                return null;
            }

            try (InputStream pngInput = Files.newInputStream(tempPng)) {
                return javax.imageio.ImageIO.read(pngInput);
            }
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            deleteQuietly(tempPng);
        }
    }

    private Path createPrivateTempFile(String prefix, String suffix) throws IOException {
        Path privateTempDir = Paths.get(System.getProperty("user.home"), ".catalogo-pdf-temp");
        Files.createDirectories(privateTempDir);
        return Files.createTempFile(privateTempDir, prefix, suffix);
    }

    private boolean isWebp(Path imagePath) {
        String fileName = imagePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".webp");
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // ignore cleanup failures
        }
    }

    private Path resolveProductImagePath(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }

        String normalized = imageName.trim();
        if (normalized.startsWith("/uploads/")) {
            normalized = normalized.substring("/uploads/".length());
        } else if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        } else if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return Paths.get(uploadDir, normalized);
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "-" : CURRENCY.format(price);
    }

    private String safeText(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        return text;
    }

    private String loadLogoUrl() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/img/logo-menicucci.png");
        URL url = resource.getURL();
        return url.toString();
    }
}