package com.altoque.altoque.Utils;

import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocumentoGenerator {

    private static final float A4_WIDTH = PDRectangle.A4.getWidth();
    // --- CONSTANTES DE DISEÑO ---
    private static final float MARGINTEXT = 60; // Margen más amplio
    private static final float CONTENT_WIDTH = A4_WIDTH - 2 * MARGINTEXT;

    private static final float MARGINC = 70;
    private static final PDType1Font BOLD_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font REGULAR_FONT = PDType1Font.HELVETICA;
    private static final float MARGIN = 50; // Reducido para dar más espacio a la tabla
    private static final float A4_HEIGHT = PDRectangle.A4.getHeight();
    // Constantes específicas para la tabla del cronograma
    private static final float ROW_HEIGHT = 20; // Mayor altura para más espacio vertical
    private static final float TABLE_WIDTH = A4_WIDTH - 2 * MARGIN;
    // Anchos de columna ajustados para mejor espaciado
    private static final float[] COL_WIDTHS = {40, 90, 100, 100, 100, TABLE_WIDTH - 430};


    // ============================
    // 🟩 DECLARACIÓN JURADA PEP
    // ============================
    public static byte[] generarDeclaracionPEP(Cliente cliente) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // --- DEFINICIÓN DE FUENTES ---
                PDFont titleFont = PDType1Font.HELVETICA_BOLD;
                PDFont bodyFont = PDType1Font.HELVETICA;
                PDFont bodyFontItalic = PDType1Font.HELVETICA_OBLIQUE;

                // Variable para rastrear la posición vertical (Y), comenzando desde arriba.
                float yPosition = 770;

                // --- TÍTULO ---
                addCenteredText(content, "DECLARACIÓN JURADA", titleFont, 16, yPosition);
                yPosition -= 20; // Espacio entre líneas del título
                addCenteredText(content, "PERSONA EXPUESTA POLÍTICAMENTE (PEP)", titleFont, 14, yPosition);
                yPosition -= 50; // Espacio después del título

                // --- CUERPO DEL TEXTO (con ajuste automático de línea) ---
                String declarationText = "Yo, " + cliente.getNombreCliente() + " " + cliente.getApellidoCliente()
                        + ", identificado(a) con DNI N.º " + cliente.getDniCliente() + ", con domicilio en:                                                            "
                        + cliente.getDireccionCliente() + ", declaro bajo juramento lo siguiente:";

                // La función addParagraph se encarga del ajuste de texto y actualiza la posición Y
                yPosition = addParagraph(content, declarationText, bodyFont, 12, MARGINTEXT, yPosition);
                yPosition -= 30; // Espacio antes de la lista

                // --- PUNTOS DE LA DECLARACIÓN ---
                String point1 = "1.  Soy Persona Expuesta Políticamente (PEP) y la información proporcionada en el presente documento es veraz y se encuentra actualizada.";
                yPosition = addParagraph(content, point1, bodyFont, 11, MARGINTEXT + 15, yPosition);
                yPosition -= 20;

                String point2 = "2.  Me comprometo a informar por escrito cualquier modificación a la información declarada, en un plazo no mayor a diez (10) días hábiles de ocurrida.";
                yPosition = addParagraph(content, point2, bodyFont, 11, MARGINTEXT + 15, yPosition);
                yPosition -= 60; // Espacio generoso antes de la fecha

                // --- LUGAR Y FECHA ---
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy");
                String fecha = "Trujillo, " + LocalDate.now().format(dtf);
                addText(content, fecha, bodyFont, 12, A4_WIDTH - MARGINTEXT - (bodyFont.getStringWidth(fecha)/1000*12), yPosition);
                yPosition -= 100;

                // --- SECCIÓN DE FIRMA (Centrada y con líneas dibujadas) ---
                float signatureLineY = yPosition;
                float signatureLineXStart = (A4_WIDTH / 2f) - 120;
                float signatureLineXEnd = (A4_WIDTH / 2f) + 120;
                drawLine(content, signatureLineXStart, signatureLineY, signatureLineXEnd, signatureLineY);

                yPosition -= 15;
                String firmaText = "Firma del Declarante";
                addCenteredText(content, firmaText, bodyFontItalic, 10, yPosition);

                yPosition -= 25;
                String nombreCompleto = cliente.getNombreCliente() + " " + cliente.getApellidoCliente();
                addCenteredText(content, nombreCompleto, PDType1Font.HELVETICA_BOLD, 11, yPosition);

                yPosition -= 15;
                String dni = "DNI N.º: " + cliente.getDniCliente();
                addCenteredText(content, dni, bodyFont, 11, yPosition);

                // --- INFORMACIÓN ADICIONAL SOBRE PEP ---
                yPosition -= 30;
                String infoAdicional = "Información adicional sobre Personas Expuestas Políticamente (PEP):";
                yPosition = addParagraph(content, infoAdicional, bodyFont, 12, MARGINTEXT, yPosition);
                yPosition -= 20;

                String info1 = "Una Persona Expuesta Políticamente (PEP) es aquella que ocupa o ha ocupado una función pública destacada, ya sea a nivel nacional o internacional, y cuya posición puede representar un mayor riesgo de involucramiento en delitos como el lavado de activos o el financiamiento del terrorismo.";
                yPosition = addParagraph(content, info1, bodyFont, 11, MARGINTEXT + 15, yPosition);
                yPosition -= 20;

                String info2 = "Las PEP pueden incluir, entre otros, a jefes de Estado, ministros, miembros del parlamento, jueces de alto nivel, embajadores, oficiales de alto rango de las fuerzas armadas y miembros de órganos de dirección de empresas estatales.";
                yPosition = addParagraph(content, info2, bodyFont, 11, MARGINTEXT + 15, yPosition);
                yPosition -= 20;

                String info3 = "Es importante que las entidades financieras y otras instituciones sujetas a regulaciones de prevención de lavado de activos y financiamiento del terrorismo identifiquen a las PEP y realicen un monitoreo adecuado de sus transacciones.";
                yPosition = addParagraph(content, info3, bodyFont, 11, MARGINTEXT + 15, yPosition);
                yPosition -= 60; // Espacio antes de la firma

                // --- SECCIÓN DE FIRMA (Centrada y con líneas dibujadas) ---
                signatureLineY = yPosition;
                signatureLineXStart = (A4_WIDTH / 2f) - 120;
                signatureLineXEnd = (A4_WIDTH / 2f) + 120;
                drawLine(content, signatureLineXStart, signatureLineY, signatureLineXEnd, signatureLineY);

                yPosition -= 15;
                addCenteredText(content, firmaText, bodyFontItalic, 10, yPosition);

                yPosition -= 25;
                addCenteredText(content, nombreCompleto, PDType1Font.HELVETICA_BOLD, 11, yPosition);

                yPosition -= 15;
                addCenteredText(content, dni, bodyFont, 11, yPosition);
            }

            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar la declaración PEP en PDF",e);
        }
    }

    /**
     * Método de ayuda para añadir texto centrado en la página.
     */
    private static void addCenteredText(PDPageContentStream content, String text, PDFont font, float fontSize, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (A4_WIDTH - textWidth) / 2;
        addText(content, text, font, fontSize, x, y);
    }

    /**
     * Método de ayuda para añadir un bloque de texto (párrafo) con ajuste de línea automático.
     * @return La nueva posición Y después de escribir el párrafo.
     */
    private static float addParagraph(PDPageContentStream content, String text, PDFont font, float fontSize, float x, float y) throws IOException {
        float leading = 1.5f * fontSize; // Espaciado entre líneas (interlineado)
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            float width = font.getStringWidth(currentLine + " " + word) / 1000 * fontSize;
            if (width > CONTENT_WIDTH - (x - MARGIN)) { // Comprueba si la línea excede el ancho de contenido
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        lines.add(currentLine.toString()); // Agrega la última línea

        // Escribe las líneas en el PDF
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        for (String line : lines) {
            content.showText(line);
            content.newLineAtOffset(0, -leading);
        }
        content.endText();

        return y - (lines.size() * leading); // Devuelve la nueva posición Y
    }

    /**
     * Método base para añadir texto en una posición específica.
     */
    private static void addText(PDPageContentStream content, String text, PDFont font, float fontSize, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    /**
     * Método de ayuda para dibujar una línea horizontal.
     */
    private static void drawLine(PDPageContentStream content, float xStart, float yStart, float xEnd, float yEnd) throws IOException {
        content.moveTo(xStart, yStart);
        content.lineTo(xEnd, yEnd);
        content.stroke();
    }

    public static byte[] generarDeclaracionUIT(Cliente cliente, Prestamo prestamo) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Validar que el préstamo supere 1 UIT (valor de ejemplo: S/ 5350)
            if (prestamo.getMonto() <= 5350) {
                throw new IllegalArgumentException(
                        "No se requiere declaración: el monto no supera 1 UIT."
                );
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {

                // --- Configuración de Fuentes y Tamaños ---
                PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font regularFont = PDType1Font.HELVETICA;
                int titleSize = 14;
                int bodySize = 12;

                // --- Variables de Diseño y Posicionamiento ---
                float margin = 70;
                float yPosition = 750; // Posición Y inicial (desde arriba)
                float pageWidth = page.getMediaBox().getWidth();
                float leading = 20f; // Espacio base entre líneas (interlineado)

                // --- 1. Título (Centrado) ---
                String title = "DECLARACIÓN JURADA POR MONTO DE PRÉSTAMO SUPERIOR A 1 UIT";
                float titleWidth = boldFont.getStringWidth(title) / 1000 * titleSize;
                float titleX = (pageWidth - titleWidth) / 2; // Calcular X para centrar

                content.beginText();
                content.setFont(boldFont, titleSize);
                content.newLineAtOffset(titleX, yPosition);
                content.showText(title);
                content.endText();
                yPosition -= leading * 2.5; // Dejar más espacio después del título

                // --- 2. Párrafo de Introducción ---
                // Se divide en dos líneas para un mejor ajuste, en lugar de una sola línea larga.
                String introLine1 = "Yo, " + cliente.getNombreCliente() + " " + cliente.getApellidoCliente()
                        + ", identificado(a) con DNI N.º " + cliente.getDniCliente() + ", con";
                String introLine2 = "domicilio en " + cliente.getDireccionCliente() + ", declaro bajo juramento lo siguiente:";

                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(margin, yPosition);
                content.showText(introLine1);
                yPosition -= leading; // Mover a la siguiente línea
                content.newLineAtOffset(0, -leading);
                content.showText(introLine2);
                content.endText();
                yPosition -= leading * 2; // Dejar más espacio después del párrafo

                // --- 3. Declaraciones Principales ---
                String[] declarations = {
                        "1. El monto solicitado (S/ " + String.format(Locale.US, "%.2f", prestamo.getMonto()) + ") supera una (1) Unidad Impositiva Tributaria (UIT).",
                        "2. Declaro que mi ingreso mensual aproximado es: __________________________ soles.",
                        "3. Declaro que el motivo del préstamo es: _________________________________________.",
                        "4. Afirmo que la información proporcionada es veraz y actualizada."
                };

                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(margin, yPosition);
                for (String line : declarations) {
                    content.showText(line);
                    // Aumentar el espacio entre cada item de la declaración
                    content.newLineAtOffset(0, -leading * 1.5f);
                    yPosition -= leading * 1.5f;
                }
                content.endText();
                yPosition -= leading; // Espacio extra al final de la lista

                // --- 4. Lugar y Fecha ---
                // Formatear la fecha a un formato más legible
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "PE"));
                String formattedDate = today.format(formatter);
                String locationAndDate = "Lugar y fecha: __________________________, " + formattedDate;

                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(margin, yPosition);
                content.showText(locationAndDate);
                content.endText();
                yPosition -= leading * 3; // Dejar un espacio amplio para la firma

                // --- 5. Bloque de Firma (Centrado) ---
                String fullName = cliente.getNombreCliente() + " " + cliente.getApellidoCliente();
                String dni = "DNI N.º: " + cliente.getDniCliente();

                // Línea de firma
                String signatureLine = "__________________________";
                float signatureWidth = regularFont.getStringWidth(signatureLine) / 1000 * bodySize;
                float signatureX = (pageWidth - signatureWidth) / 2;

                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(signatureX, yPosition);
                content.showText(signatureLine);
                content.endText();
                yPosition -= leading;

                // Nombre completo debajo de la firma
                float nameWidth = regularFont.getStringWidth(fullName) / 1000 * bodySize;
                float nameX = (pageWidth - nameWidth) / 2;
                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(nameX, yPosition);
                content.showText(fullName);
                content.endText();
                yPosition -= leading;

                // DNI debajo del nombre
                float dniWidth = regularFont.getStringWidth(dni) / 1000 * bodySize;
                float dniX = (pageWidth - dniWidth) / 2;
                content.beginText();
                content.setFont(regularFont, bodySize);
                content.newLineAtOffset(dniX, yPosition);
                content.showText(dni);
                content.endText();
            }

            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            // En una aplicación real, es mejor usar un logger como SLF4J
            e.printStackTrace();
            throw new RuntimeException("Error al generar la declaración por monto mayor a 1 UIT", e);
        }
    }

    // ============================
    // 🟦 CONTRATO DE PRÉSTAMO > 1 UIT
    // ============================
    public static byte[] generarContratoPrestamo(Prestamo prestamo) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (prestamo == null || prestamo.getCliente() == null) {
                throw new IllegalArgumentException("El préstamo y el cliente asociado no pueden ser nulos.");
            }

            Cliente cliente = prestamo.getCliente();
            double monto = prestamo.getMonto();

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float yPosition = 770;
                float leading = 18f;
                int titleSize = 14;
                int bodySize = 11;
                float contentWidth = A4_WIDTH - 2 * MARGIN;

                // --- 1. Título (Centrado) ---
                String title = "CONTRATO DE PRÉSTAMO PERSONAL DE LIBRE DISPONIBILIDAD";
                float titleWidth = BOLD_FONT.getStringWidth(title) / 1000 * titleSize;
                float titleX = (A4_WIDTH - titleWidth) / 2;
                content.beginText();
                content.setFont(BOLD_FONT, titleSize);
                content.newLineAtOffset(titleX, yPosition);
                content.showText(title);
                content.endText();
                yPosition -= leading * 3;

                // --- 2. Partes del Contrato (con ajuste de texto) ---
                String companyInfo = "Entre AL TOQUE S.A.C., RUC N.º 20612345678, con domicilio en Av. Los Olivos 345, Trujillo, representada por Carlos Ramírez López, DNI N.º 45871234, en adelante 'La Empresa';";
                yPosition = drawWrappedText(content, companyInfo, REGULAR_FONT, bodySize, MARGIN, yPosition, contentWidth, 16f);
                yPosition -= leading; // Espacio extra

                String nombreCompleto = cliente.getNombreCliente() + " " + cliente.getApellidoCliente();
                String clientInfo = "y el(la) señor(a) " + nombreCompleto.trim() + ", identificado(a) con DNI N.º " + cliente.getDniCliente() + ", con domicilio en " + cliente.getDireccionCliente() + ", en adelante denominado(a) 'El Cliente'.";
                yPosition = drawWrappedText(content, clientInfo, REGULAR_FONT, bodySize, MARGIN, yPosition, contentWidth, 16f);
                yPosition -= leading * 2;

                // --- 3. Cláusulas Principales (con ajuste de texto) ---
                String clauseText;
                if (monto > 5150) {
                    clauseText = "La Empresa otorga a El Cliente un préstamo personal por la suma de S/ " + String.format(Locale.US, "%.2f", monto) + ", monto que supera una (1) Unidad Impositiva Tributaria (UIT) vigente. El préstamo es de libre disponibilidad y se sujeta a las cláusulas detalladas en el cronograma de pagos adjunto.";
                } else {
                    clauseText = "La Empresa otorga a El Cliente un préstamo personal por la suma de S/ " + String.format(Locale.US, "%.2f", monto) + ", monto que no supera una (1) Unidad Impositiva Tributaria (UIT). El presente contrato es de carácter simplificado. El Cliente se compromete a devolver el monto prestado en el plazo y bajo las condiciones establecidas en el cronograma de pagos.";
                }
                yPosition = drawWrappedText(content, clauseText, REGULAR_FONT, bodySize, MARGIN, yPosition, contentWidth, 16f);
                yPosition -= leading;

                String agreementText = "Ambas partes firman el presente contrato en señal de conformidad.";
                yPosition = drawWrappedText(content, agreementText, REGULAR_FONT, bodySize, MARGIN, yPosition, contentWidth, 16f);
                yPosition -= leading * 3;

                // --- 4. Lugar y Fecha ---
                content.beginText();
                content.setFont(REGULAR_FONT, bodySize);
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText("Lugar: Trujillo");
                content.newLineAtOffset(0, -leading);
                String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "PE")));
                content.showText("Fecha: " + fecha);
                content.endText();

                // --- 5. Bloque de Firmas (Revisado y Centrado) ---
                float signatureBlockY = yPosition - 100;
                String signatureLine = "_________________________";

                // Firma Izquierda (Empresa)
                content.beginText();
                content.setFont(REGULAR_FONT, 10);
                content.newLineAtOffset(MARGIN + 40, signatureBlockY);
                content.showText(signatureLine);
                content.newLineAtOffset(45, -leading);
                content.showText("Firma del Representante");
                content.newLineAtOffset(-15, -leading);
                content.showText("AL TOQUE S.A.C.");
                content.endText();

                // Firma Derecha (Cliente)
                content.beginText();
                content.setFont(REGULAR_FONT, 10);
                content.newLineAtOffset(A4_WIDTH - MARGIN - 180, signatureBlockY);
                content.showText(signatureLine);
                content.newLineAtOffset(60, -leading);
                content.showText("Firma del Cliente");
                content.newLineAtOffset(-30, -leading);
                content.showText(nombreCompleto.trim());
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el contrato de préstamo", e);
        }
    }

    // Constantes para la tabla

    /**
     * Genera un PDF de cronograma de pagos con diseño mejorado, paginación y totales.
     */
    /**
     * Genera un PDF de cronograma de pagos con diseño mejorado, paginación y totales.
     */
    public static byte[] generarCronogramaPDF(
            String nombreCliente, double monto, double tasaAnual, int meses
    ) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            float currentY = drawHeaderAndClientInfo(content, nombreCliente, monto, tasaAnual, meses);

            double tasaMensual = tasaAnual / 12 / 100;
            double cuota = (monto * tasaMensual) / (1 - Math.pow(1 + tasaMensual, -meses));
            double saldo = monto;

            currentY = drawTableHeaders(content, currentY);

            double totalInteres = 0;
            double totalCapital = 0;
            double totalCuotas = 0; // Variable para sumar el total de las cuotas

            for (int i = 1; i <= meses; i++) {
                double interes = saldo * tasaMensual;
                double capital = cuota - interes;
                saldo -= capital;

                totalInteres += interes;
                totalCapital += capital;
                totalCuotas += cuota; // Acumular el valor de cada cuota

                // --- LÓGICA DE PAGINACIÓN MEJORADA ---
                if (currentY < MARGIN + ROW_HEIGHT) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    currentY = A4_HEIGHT - MARGIN - 30; // Posición Y superior en nueva página
                    currentY = drawTableHeaders(content, currentY); // Dibuja encabezados de nuevo
                }

                String[] rowData = {
                        String.valueOf(i),
                        LocalDate.now().plusMonths(i).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        String.format(Locale.US, "%.2f", cuota),
                        String.format(Locale.US, "%.2f", interes),
                        String.format(Locale.US, "%.2f", capital),
                        String.format(Locale.US, "%.2f", Math.max(saldo, 0))
                };
                currentY = drawTableRow(content, currentY, REGULAR_FONT, 10, rowData, false);
            }

            // Dibuja la fila de totales con la suma de las cuotas
            String[] totalsData = {
                    "", "TOTALES",
                    String.format(Locale.US, "%.2f", totalCuotas),
                    String.format(Locale.US, "%.2f", totalInteres),
                    String.format(Locale.US, "%.2f", totalCapital),
                    ""
            };
            currentY = drawTableRow(content, currentY, BOLD_FONT, 10, totalsData, true); // Fila en negrita

            // --- BLOQUE DE FIRMA MEJORADO ---
            float signatureY = currentY - 80;
            if (signatureY < MARGIN + 50) { // Si no hay espacio, nueva página para la firma
                content.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                content = new PDPageContentStream(document, page);
                signatureY = A4_HEIGHT - MARGIN - 100;
            }

            String signatureLine = "_________________________";
            float signatureWidth = REGULAR_FONT.getStringWidth(signatureLine) / 1000 * 12;
            float signatureX = (A4_WIDTH - signatureWidth) / 2;

            content.beginText();
            content.setFont(REGULAR_FONT, 12);
            content.newLineAtOffset(signatureX, signatureY);
            content.showText(signatureLine);
            content.newLineAtOffset(0, -20);

            float nameWidth = REGULAR_FONT.getStringWidth(nombreCliente) / 1000 * 12;
            content.newLineAtOffset((signatureWidth - nameWidth)/2, 0);
            content.showText(nombreCliente);
            content.endText();


            content.close();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar el cronograma de pagos", e);
        }
    }

    private static float drawHeaderAndClientInfo(PDPageContentStream content, String nombreCliente, double monto, double tasaAnual, int meses) throws IOException {
        float currentY = 780;
        content.beginText();
        content.setFont(BOLD_FONT, 16);
        content.newLineAtOffset(MARGIN, currentY);
        content.showText("FINANCIERA AL TOQUE S.A.C.");
        content.endText();
        currentY -= 30;

        content.beginText();
        content.setFont(BOLD_FONT, 14);
        content.newLineAtOffset(MARGIN, currentY);
        content.showText("CRONOGRAMA DE PAGOS");
        content.endText();
        currentY -= 40;

        content.beginText();
        content.setFont(REGULAR_FONT, 11);
        content.setLeading(18f);
        content.newLineAtOffset(MARGIN, currentY);
        content.showText("Cliente: " + nombreCliente);
        content.newLine();
        content.showText("Monto del préstamo: S/ " + String.format(Locale.US, "%.2f", monto));
        content.newLine();
        content.showText("Tasa de interés anual: " + tasaAnual + "%");
        content.newLine();
        content.showText("Plazo: " + meses + " meses");
        content.newLine();
        content.showText("Fecha de emisión: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        content.endText();
        return currentY - 100;
    }

    private static float drawTableHeaders(PDPageContentStream content, float startY) throws IOException {
        float y = startY;
        float nextX = MARGIN;
        String[] headers = {"N°", "Fecha Vcto.", "Cuota(S/)", "Interés", "Capital", "Saldo"};

        content.setNonStrokingColor(230, 230, 230); // Gris claro de fondo
        content.fillRect(MARGIN, y - ROW_HEIGHT, TABLE_WIDTH, ROW_HEIGHT);
        content.setNonStrokingColor(0, 0, 0); // Texto negro

        for (int i = 0; i < headers.length; i++) {
            float width = COL_WIDTHS[i];
            String text = headers[i];

            content.beginText();
            content.setFont(BOLD_FONT, 10);

            float textWidth = BOLD_FONT.getStringWidth(text) / 1000 * 10;
            float textX = nextX + 5; // Alineación izquierda por defecto
            if (i >= 2) { // Centrado para columnas numéricas
                textX = nextX + (width - textWidth) / 2;
            }
            content.newLineAtOffset(textX, y - 14); // Centrado vertical
            content.showText(text);
            content.endText();
            nextX += width;
        }
        return y - ROW_HEIGHT;
    }

    private static float drawTableRow(PDPageContentStream content, float startY, PDFont font, float fontSize, String[] data, boolean isTotalRow) throws IOException {
        float y = startY;
        float nextX = MARGIN;

        if(isTotalRow) {
            content.setNonStrokingColor(240, 240, 240);
            content.fillRect(MARGIN, y - ROW_HEIGHT, TABLE_WIDTH, ROW_HEIGHT);
            content.setNonStrokingColor(0, 0, 0);
        }

        content.setStrokingColor(200, 200, 200);
        content.setLineWidth(0.5f);
        content.moveTo(MARGIN, y);
        content.lineTo(MARGIN + TABLE_WIDTH, y);
        content.stroke();

        for (int i = 0; i < data.length; i++) {
            float width = COL_WIDTHS[i];
            String text = data[i];

            content.beginText();
            content.setFont(font, fontSize);

            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            float textX = nextX + 5; // Alineación izquierda
            if (i >= 2) { // Alineación derecha para números
                textX = nextX + width - textWidth - 8; // Más padding (8)
            }
            content.newLineAtOffset(textX, y - 14);
            content.showText(text);
            content.endText();
            nextX += width;
        }
        return y - ROW_HEIGHT;
    }

    private static float drawWrappedText(PDPageContentStream content, String text, PDFont font, float fontSize, float x, float y, float maxWidth, float leading) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            float width = font.getStringWidth(currentLine.toString() + " " + word) / 1000 * fontSize;
            if (width < maxWidth) {
                currentLine.append(word).append(" ");
            } else {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word).append(" ");
            }
        }
        lines.add(currentLine.toString().trim());

        content.beginText();
        content.setFont(font, fontSize);
        content.setLeading(leading);
        content.newLineAtOffset(x, y);
        for (String line : lines) {
            content.showText(line);
            content.newLine();
        }
        content.endText();

        return y - (lines.size() * leading);
    }
}