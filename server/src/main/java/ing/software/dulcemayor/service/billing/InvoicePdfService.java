package ing.software.dulcemayor.service.billing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.entity.billing.Payment;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.billing.PaymentRepository;
import ing.software.dulcemayor.service.billing.factory.InvoiceCreator;
import ing.software.dulcemayor.service.billing.factory.InvoiceFactoryProvider;
import ing.software.dulcemayor.service.billing.strategy.PaymentStrategyFactory;
import ing.software.dulcemayor.service.order.OrderDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates an authentic Argentine electronic invoice PDF (Factura Electrónica AFIP / ARCA)
 * for any billed/invoiced order in Dulce Mayor S.A.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private static final String EMISOR_RAZON_SOCIAL = "DULCE MAYOR S.A.";
    private static final String EMISOR_SUBTITULO = "Distribuidora Mayorista de Golosinas, Chocolates y Afines";
    private static final String EMISOR_DOMICILIO = "Carril Rodríguez Peña 2450 - Godoy Cruz, Mendoza (CP 5501)";
    private static final String EMISOR_COND_IVA = "IVA Responsable Inscripto";
    private static final String EMISOR_CUIT = "30-71589421-9";
    private static final String EMISOR_CUIT_RAW = "30715894219";
    private static final String EMISOR_IIBB = "041-889234-1";
    private static final String EMISOR_INICIO_ACT = "01/03/2018";
    private static final int PUNTO_DE_VENTA = 4;

    private static final Color COLOR_NAVY = new Color(15, 29, 56);
    private static final Color COLOR_DARK = new Color(30, 41, 59);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(148, 163, 184);
    private static final Color COLOR_HEADER_BG = new Color(241, 245, 249);
    private static final Color COLOR_TOTAL_BG = new Color(248, 250, 252);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PaymentAccreditationService paymentAccreditationService;
    private final PaymentRepository paymentRepository;
    private final OrderDtoMapper orderDtoMapper;
    private final InvoiceFactoryProvider invoiceFactoryProvider;
    private final PaymentStrategyFactory paymentStrategyFactory;

    public record GeneratedInvoicePdf(byte[] pdfBytes, String filename, Long invoiceNumber, Long orderNumber) {}

    @Transactional
    public GeneratedInvoicePdf generateInvoicePdfForOrder(String orderId) {
        PaymentAccreditationService.OrderInvoiceBundle bundle = paymentAccreditationService.resolveOrEnsureInvoiceBundle(orderId);
        Order order = bundle.order();
        Invoice invoice = bundle.invoice();

        List<Payment> payments = paymentRepository.findByInvoiceIdAndIsDeletedFalse(invoice.getId());
        Payment primaryPayment = payments.isEmpty() ? null : payments.get(0);

        String customerTaxId = resolveCustomerDocument(order);
        Invoice fallbackInvoice = invoiceFactoryProvider.createInvoiceForOrder(
                order,
                invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : 10001L
        );
        String invoiceLetter = invoice.getInvoiceType() != null && !invoice.getInvoiceType().isBlank()
                ? invoice.getInvoiceType().toUpperCase()
                : fallbackInvoice.getInvoiceType();
        boolean isFacturaA = "A".equalsIgnoreCase(invoiceLetter);
        String invoiceTypeCode = invoice.getAfipCode() != null && !invoice.getAfipCode().isBlank()
                ? invoice.getAfipCode()
                : fallbackInvoice.getAfipCode();
        int afipTipoCmp = isFacturaA ? 1 : 6;

        String ptoVtaFormatted = String.format("%05d", PUNTO_DE_VENTA);
        String compNroFormatted = String.format("%08d", invoice.getInvoiceNumber());
        String filename = String.format("Factura-%s-%s-%s.pdf", invoiceLetter, ptoVtaFormatted, compNroFormatted);

        LocalDateTime emissionDateTime = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDateTime.now();
        LocalDate emissionDate = emissionDateTime.toLocalDate();
        LocalDate dueDate = emissionDate.plusDays(15);
        LocalDate caeDueDate = invoice.getCaeExpirationDate() != null
                ? invoice.getCaeExpirationDate()
                : emissionDate.plusDays(10);
        String caeNumber = invoice.getCaeNumber() != null && !invoice.getCaeNumber().isBlank()
                ? invoice.getCaeNumber()
                : fallbackInvoice.getCaeNumber();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 24f, 24f, 22f, 22f);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font fontTitleBold = new Font(Font.HELVETICA, 14, Font.BOLD, COLOR_NAVY);
            Font fontFacturaTitle = new Font(Font.HELVETICA, 16, Font.BOLD, COLOR_DARK);
            Font fontLetter = new Font(Font.HELVETICA, 26, Font.BOLD, COLOR_DARK);
            Font fontCodeSmall = new Font(Font.HELVETICA, 7, Font.BOLD, COLOR_MUTED);
            Font fontSectionLabel = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_DARK);
            Font fontRegular = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, COLOR_DARK);
            Font fontSmall = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_MUTED);
            Font fontTableHead = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_DARK);
            Font fontTableCell = new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_DARK);
            Font fontTableCellBold = new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_DARK);
            Font fontTotalLabel = new Font(Font.HELVETICA, 9f, Font.BOLD, COLOR_DARK);
            Font fontGrandTotal = new Font(Font.HELVETICA, 11.5f, Font.BOLD, COLOR_NAVY);

            // 1. TOP "ORIGINAL" BANNER
            PdfPTable copyTable = new PdfPTable(1);
            copyTable.setWidthPercentage(100);
            PdfPCell copyCell = new PdfPCell(new Phrase("ORIGINAL", new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_DARK)));
            copyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            copyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            copyCell.setPaddingTop(4f);
            copyCell.setPaddingBottom(5f);
            copyCell.setBackgroundColor(COLOR_HEADER_BG);
            copyCell.setBorderColor(COLOR_BORDER);
            copyCell.setBorderWidth(1f);
            copyTable.addCell(copyCell);
            doc.add(copyTable);

            // 2. MAIN HEADER (EMISOR | LETRA A/B | DATOS DE COMPROBANTE)
            PdfPTable headerTable = new PdfPTable(new float[]{44f, 12f, 44f});
            headerTable.setWidthPercentage(100);

            // Left: Emisor
            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setPadding(8f);
            leftHeader.setBorderColor(COLOR_BORDER);
            leftHeader.setBorderWidth(1f);

            Paragraph brandPara = new Paragraph(EMISOR_RAZON_SOCIAL, fontTitleBold);
            brandPara.setSpacingAfter(2f);
            leftHeader.addElement(brandPara);

            Paragraph subPara = new Paragraph(EMISOR_SUBTITULO, fontSmall);
            subPara.setSpacingAfter(8f);
            leftHeader.addElement(subPara);

            leftHeader.addElement(keyValueLine("Razón Social: ", EMISOR_RAZON_SOCIAL, fontSectionLabel, fontRegular));
            leftHeader.addElement(keyValueLine("Domicilio Comercial: ", EMISOR_DOMICILIO, fontSectionLabel, fontRegular));
            leftHeader.addElement(keyValueLine("Condición frente al IVA: ", EMISOR_COND_IVA, fontSectionLabel, fontRegular));
            leftHeader.addElement(keyValueLine("Contacto Mayorista: ", "administracion@dulcemayor.com.ar | (0261) 420-9000", fontSectionLabel, fontSmall));
            headerTable.addCell(leftHeader);

            // Center: Letra A / B
            PdfPCell centerHeader = new PdfPCell();
            centerHeader.setPaddingTop(6f);
            centerHeader.setPaddingBottom(6f);
            centerHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            centerHeader.setVerticalAlignment(Element.ALIGN_TOP);
            centerHeader.setBorderColor(COLOR_BORDER);
            centerHeader.setBorderWidth(1f);
            centerHeader.setBackgroundColor(COLOR_HEADER_BG);

            Paragraph letterPara = new Paragraph(invoiceLetter, fontLetter);
            letterPara.setAlignment(Element.ALIGN_CENTER);
            centerHeader.addElement(letterPara);

            Paragraph codPara = new Paragraph("COD. " + invoiceTypeCode, fontCodeSmall);
            codPara.setAlignment(Element.ALIGN_CENTER);
            codPara.setSpacingBefore(2f);
            centerHeader.addElement(codPara);
            headerTable.addCell(centerHeader);

            // Right: Factura & Datos Fiscales
            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setPadding(8f);
            rightHeader.setBorderColor(COLOR_BORDER);
            rightHeader.setBorderWidth(1f);

            Paragraph factTitle = new Paragraph("FACTURA", fontFacturaTitle);
            factTitle.setSpacingAfter(4f);
            rightHeader.addElement(factTitle);

            rightHeader.addElement(keyValueLine("Punto de Venta: ", ptoVtaFormatted + "    Comp. Nro: " + compNroFormatted, fontSectionLabel, fontTableCellBold));
            rightHeader.addElement(keyValueLine("Fecha de Emisión: ", emissionDateTime.format(DATE_FMT), fontSectionLabel, fontRegular));
            Paragraph spacer = new Paragraph(" ");
            spacer.setLeading(4f);
            rightHeader.addElement(spacer);
            rightHeader.addElement(keyValueLine("CUIT: ", EMISOR_CUIT, fontSectionLabel, fontRegular));
            rightHeader.addElement(keyValueLine("Ingresos Brutos (ATM Mza): ", EMISOR_IIBB, fontSectionLabel, fontRegular));
            rightHeader.addElement(keyValueLine("Fecha de Inicio de Actividades: ", EMISOR_INICIO_ACT, fontSectionLabel, fontRegular));
            headerTable.addCell(rightHeader);

            doc.add(headerTable);

            // 3. PERIODO FACTURADO & VENCIMIENTO BAR
            PdfPTable periodTable = new PdfPTable(new float[]{36f, 34f, 30f});
            periodTable.setWidthPercentage(100);

            LocalDate orderDate = order.getOrderDateTime() != null ? order.getOrderDateTime().toLocalDate() : emissionDate;
            periodTable.addCell(styledInfoCell("Período Facturado Desde: ", orderDate.format(DATE_FMT), fontSectionLabel, fontRegular));
            periodTable.addCell(styledInfoCell("Hasta: ", emissionDate.format(DATE_FMT), fontSectionLabel, fontRegular));
            periodTable.addCell(styledInfoCell("Fecha de Vto. para el pago: ", dueDate.format(DATE_FMT), fontSectionLabel, fontRegular));
            doc.add(periodTable);

            // 4. RECEPTOR (CLIENTE / PUNTO DE VENTA) BOX
            String customerName = orderDtoMapper.resolveRetailerName(order);
            String storeName = order.getPointOfSale() != null ? order.getPointOfSale().getStoreName() : null;
            String receptorDisplay = (storeName != null && !storeName.isBlank() && !storeName.equalsIgnoreCase(customerName))
                    ? customerName + " — Comercio: " + storeName
                    : customerName;
            String condIvaReceptor = invoice.getTaxConditionReceiver() != null && !invoice.getTaxConditionReceiver().isBlank()
                    ? invoice.getTaxConditionReceiver()
                    : (isFacturaA ? "IVA Responsable Inscripto" : "Responsable Monotributo / Consumidor Final");
            String addressDisplay = resolveFullDeliveryAddress(order);
            String paymentLabel = formatPaymentTypeSpanish(order.getPaymentType());
            String paymentRef = primaryPayment != null && primaryPayment.getReference() != null
                    ? primaryPayment.getReference()
                    : (order.getMpPaymentId() != null ? "MP-" + order.getMpPaymentId() : "ORD-" + order.getOrderNumber());

            PdfPTable clientTable = new PdfPTable(new float[]{45f, 55f});
            clientTable.setWidthPercentage(100);
            clientTable.setSpacingBefore(3f);
            clientTable.setSpacingAfter(6f);

            PdfPCell clientLeft = new PdfPCell();
            clientLeft.setPadding(7f);
            clientLeft.setBorderColor(COLOR_BORDER);
            clientLeft.setBorderWidth(1f);
            clientLeft.addElement(keyValueLine(isFacturaA ? "CUIT: " : "CUIT / DNI: ", formatTaxIdOrDni(customerTaxId), fontSectionLabel, fontRegular));
            clientLeft.addElement(keyValueLine("Condición frente al IVA: ", condIvaReceptor, fontSectionLabel, fontRegular));
            clientLeft.addElement(keyValueLine("Condición de Venta: ", paymentLabel + " (Ref: " + paymentRef + ")", fontSectionLabel, fontRegular));
            clientTable.addCell(clientLeft);

            PdfPCell clientRight = new PdfPCell();
            clientRight.setPadding(7f);
            clientRight.setBorderColor(COLOR_BORDER);
            clientRight.setBorderWidth(1f);
            clientRight.addElement(keyValueLine("Apellido y Nombre / Razón Social: ", receptorDisplay, fontSectionLabel, fontRegular));
            clientRight.addElement(keyValueLine("Domicilio Comercial / Entrega: ", addressDisplay, fontSectionLabel, fontRegular));
            clientRight.addElement(keyValueLine("Orden de Compra / Pedido N°: ", "#" + order.getOrderNumber()
                    + (order.getSourceChannel() != null && "LANDING".equalsIgnoreCase(order.getSourceChannel()) ? " (Canal Tienda Web)" : " (Canal Preventa Mayorista)"),
                    fontSectionLabel, fontRegular));
            clientTable.addCell(clientRight);

            doc.add(clientTable);

            // 5. ITEMS TABLE
            PdfPTable itemsTable = new PdfPTable(new float[]{11f, 34f, 8f, 9f, 12f, 7f, 7f, 12f});
            itemsTable.setWidthPercentage(100);

            String[] headers = {
                    "Código",
                    "Producto / Descripción",
                    "Cant.",
                    "U. Med.",
                    isFacturaA ? "P. Unit. s/IVA" : "Precio Unit.",
                    "% Bonif",
                    "IVA",
                    "Subtotal c/IVA"
            };
            for (String h : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, fontTableHead));
                hCell.setBackgroundColor(COLOR_HEADER_BG);
                hCell.setBorderColor(COLOR_BORDER);
                hCell.setBorderWidth(0.8f);
                hCell.setPadding(5f);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(hCell);
            }

            List<InvoiceDetail> details = invoice.getInvoiceDetails();
            int rowCount = 0;

            if (details != null && !details.isEmpty()) {
                for (InvoiceDetail item : details) {
                    rowCount++;
                    String code = item.getProductCode() != null ? item.getProductCode() : "ART-00";
                    String packaging = (item.getProduct() != null && item.getProduct().getPackaging() != null)
                            ? " (" + item.getProduct().getPackaging() + ")"
                            : "";
                    String name = (item.getProductName() != null ? item.getProductName() : "Producto Mayorista") + packaging;
                    int qty = item.getQuantity();
                    double unitPriceFinal = item.getUnitPrice();
                    double displayedUnitPrice = isFacturaA ? (unitPriceFinal / 1.21) : unitPriceFinal;
                    double lineSubtotalFinal = item.getSubtotal();

                    itemsTable.addCell(tableBodyCell(code, fontTableCell, Element.ALIGN_LEFT));
                    itemsTable.addCell(tableBodyCell(name, fontTableCell, Element.ALIGN_LEFT));
                    itemsTable.addCell(tableBodyCell(formatDec(qty), fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("bultos", fontTableCell, Element.ALIGN_CENTER));
                    itemsTable.addCell(tableBodyCell("$ " + formatMoney(displayedUnitPrice), fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("0,00%", fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("21,0%", fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("$ " + formatMoney(lineSubtotalFinal), fontTableCellBold, Element.ALIGN_RIGHT));
                }
            } else if (order.getOrderDetails() != null) {
                for (OrderDetail od : order.getOrderDetails()) {
                    if (od.isDeleted()) continue;
                    rowCount++;
                    String code = od.getProduct() != null ? od.getProduct().getCode() : "ART-00";
                    String packaging = (od.getProduct() != null && od.getProduct().getPackaging() != null)
                            ? " (" + od.getProduct().getPackaging() + ")"
                            : "";
                    String name = (od.getProduct() != null ? od.getProduct().getName() : "Producto Mayorista") + packaging;
                    double unitPriceFinal = od.getUnitPrice();
                    double displayedUnitPrice = isFacturaA ? (unitPriceFinal / 1.21) : unitPriceFinal;

                    itemsTable.addCell(tableBodyCell(code, fontTableCell, Element.ALIGN_LEFT));
                    itemsTable.addCell(tableBodyCell(name, fontTableCell, Element.ALIGN_LEFT));
                    itemsTable.addCell(tableBodyCell(formatDec(od.getQuantity()), fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("bultos", fontTableCell, Element.ALIGN_CENTER));
                    itemsTable.addCell(tableBodyCell("$ " + formatMoney(displayedUnitPrice), fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("0,00%", fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("21,0%", fontTableCell, Element.ALIGN_RIGHT));
                    itemsTable.addCell(tableBodyCell("$ " + formatMoney(od.getSubtotal()), fontTableCellBold, Element.ALIGN_RIGHT));
                }
            }

            // Fill empty rows so the invoice body has standard A4 visual height
            int minRows = 9;
            for (int i = rowCount; i < minRows; i++) {
                for (int col = 0; col < 8; col++) {
                    PdfPCell empty = new PdfPCell(new Phrase(" ", fontTableCell));
                    empty.setBorderColor(COLOR_BORDER);
                    empty.setBorderWidthLeft(0.5f);
                    empty.setBorderWidthRight(0.5f);
                    empty.setBorderWidthTop(0f);
                    empty.setBorderWidthBottom(i == minRows - 1 ? 0.8f : 0f);
                    empty.setPadding(4.5f);
                    itemsTable.addCell(empty);
                }
            }

            doc.add(itemsTable);

            // 6. SUBTOTALS, IVA BREAKDOWN & TOTAL BOX
            double finalTotal = invoice.getTotal() > 0 ? invoice.getTotal() : order.getTotal();
            double grossSubtotal = order.getSubtotal() > 0 ? order.getSubtotal() : finalTotal;
            double discountAmount = Math.max(0.0, order.getDiscount());
            double netTaxable = invoice.getNetAmount() > 0 ? invoice.getNetAmount() : (finalTotal / 1.21);
            double iva21 = invoice.getVatAmount() > 0 ? invoice.getVatAmount() : (finalTotal - netTaxable);

            PdfPTable summaryOuter = new PdfPTable(new float[]{55f, 45f});
            summaryOuter.setWidthPercentage(100);
            summaryOuter.setSpacingBefore(6f);

            // Left side of totals: observations, coupon & amount in words
            PdfPCell notesCell = new PdfPCell();
            notesCell.setPadding(8f);
            notesCell.setBorderColor(COLOR_BORDER);
            notesCell.setBorderWidth(1f);
            notesCell.setBackgroundColor(COLOR_TOTAL_BG);

            Paragraph obsHeader = new Paragraph("Observaciones y Condiciones Comerciales:", fontSectionLabel);
            obsHeader.setSpacingAfter(3f);
            notesCell.addElement(obsHeader);

            if (discountAmount > 0) {
                String couponLabel = (order.getCouponCode() != null && !order.getCouponCode().isBlank())
                        ? "Cupón aplicado: " + order.getCouponCode()
                        : "Bonificación comercial mayorista";
                notesCell.addElement(new Paragraph("• " + couponLabel + " (-$ " + formatMoney(discountAmount) + ")", fontRegular));
            }
            notesCell.addElement(new Paragraph("• Mercadería controlada y despachada desde Depósito Central Godoy Cruz.", fontSmall));
            notesCell.addElement(new Paragraph("• Estado del Comprobante: PAGADO / ACREDITADO (" + emissionDateTime.format(DATETIME_FMT) + " hs).", fontSmall));

            Paragraph wordsPara = new Paragraph("SON PESOS: " + numberToSpanishWords(Math.round(finalTotal)) + ".-", fontTableCellBold);
            wordsPara.setSpacingBefore(8f);
            notesCell.addElement(wordsPara);
            summaryOuter.addCell(notesCell);

            // Right side of totals: numerical breakdown
            PdfPCell totalsCell = new PdfPCell();
            totalsCell.setPadding(6f);
            totalsCell.setBorderColor(COLOR_BORDER);
            totalsCell.setBorderWidth(1f);
            totalsCell.setBackgroundColor(COLOR_TOTAL_BG);

            PdfPTable totalsInner = new PdfPTable(new float[]{60f, 40f});
            totalsInner.setWidthPercentage(100);

            addTotalRow(totalsInner, "Subtotal Bruto:", "$ " + formatMoney(grossSubtotal), fontRegular, fontRegular);
            if (discountAmount > 0) {
                addTotalRow(totalsInner, "Bonificación / Descuento:", "- $ " + formatMoney(discountAmount), fontRegular, fontRegular);
            }
            addTotalRow(totalsInner, "Importe Neto Gravado:", "$ " + formatMoney(netTaxable), fontRegular, fontRegular);
            addTotalRow(totalsInner, "IVA 21%:", "$ " + formatMoney(iva21), fontRegular, fontRegular);
            addTotalRow(totalsInner, "Importe Otros Tributos:", "$ 0,00", fontRegular, fontRegular);
            addTotalRow(totalsInner, "IMPORTE TOTAL:", "$ " + formatMoney(finalTotal), fontTotalLabel, fontGrandTotal);

            totalsCell.addElement(totalsInner);
            summaryOuter.addCell(totalsCell);

            doc.add(summaryOuter);

            // 7. OFFICIAL AFIP / ARCA FOOTER (QR CODE + CAE)
            PdfPTable footerTable = new PdfPTable(new float[]{18f, 47f, 35f});
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(8f);

            // QR Code Cell
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorderColor(COLOR_BORDER);
            qrCell.setBorderWidth(1f);
            qrCell.setPadding(5f);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            byte[] qrPngBytes = buildAfipQrPngBytes(
                    emissionDate,
                    afipTipoCmp,
                    invoice.getInvoiceNumber(),
                    finalTotal,
                    customerTaxId,
                    caeNumber
            );
            if (qrPngBytes != null) {
                Image qrImg = Image.getInstance(qrPngBytes);
                qrImg.scaleAbsolute(76f, 76f);
                qrImg.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(qrImg);
            } else {
                qrCell.addElement(new Paragraph("AFIP QR", fontSectionLabel));
            }
            footerTable.addCell(qrCell);

            // AFIP / ARCA Authorization Legend Cell
            PdfPCell afipCell = new PdfPCell();
            afipCell.setBorderColor(COLOR_BORDER);
            afipCell.setBorderWidth(1f);
            afipCell.setPadding(8f);
            afipCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph arcaTitle = new Paragraph("ARCA / AFIP — Comprobante Autorizado", new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_NAVY));
            arcaTitle.setSpacingAfter(3f);
            afipCell.addElement(arcaTitle);

            Paragraph disclaimer = new Paragraph(
                    "Esta Administración Federal no se responsabiliza por los datos ingresados en el detalle de la operación.\n"
                            + "Régimen de Factura Electrónica RG AFIP N° 4291 / ARCA Mendoza.",
                    fontSmall
            );
            afipCell.addElement(disclaimer);
            footerTable.addCell(afipCell);

            // CAE & Vto CAE Cell
            PdfPCell caeCell = new PdfPCell();
            caeCell.setBorderColor(COLOR_BORDER);
            caeCell.setBorderWidth(1f);
            caeCell.setPadding(8f);
            caeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            caeCell.setBackgroundColor(COLOR_HEADER_BG);

            Paragraph caeLine = keyValueLine("CAE N°: ", caeNumber, fontSectionLabel, fontTableCellBold);
            caeLine.setAlignment(Element.ALIGN_RIGHT);
            caeCell.addElement(caeLine);

            Paragraph caeVtoLine = keyValueLine("Fecha de Vto. de CAE: ", caeDueDate.format(DATE_FMT), fontSectionLabel, fontTableCellBold);
            caeVtoLine.setAlignment(Element.ALIGN_RIGHT);
            caeVtoLine.setSpacingBefore(4f);
            caeCell.addElement(caeVtoLine);

            Paragraph pageLine = new Paragraph("Pág. 1 / 1", fontSmall);
            pageLine.setAlignment(Element.ALIGN_RIGHT);
            pageLine.setSpacingBefore(6f);
            caeCell.addElement(pageLine);

            footerTable.addCell(caeCell);

            doc.add(footerTable);
            doc.close();

            return new GeneratedInvoicePdf(baos.toByteArray(), filename, invoice.getInvoiceNumber(), order.getOrderNumber());
        } catch (Exception ex) {
            log.error("Failed to generate PDF invoice for order {}: {}", orderId, ex.getMessage(), ex);
            throw new IllegalStateException("No se pudo generar el documento PDF de la factura.", ex);
        }
    }

    private Paragraph keyValueLine(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.setLeading(11.5f);
        p.add(new Chunk(label, labelFont));
        p.add(new Chunk(value != null ? value : "-", valueFont));
        return p;
    }

    private PdfPCell styledInfoCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell(keyValueLine(label, value, labelFont, valueFont));
        cell.setPadding(5.5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1f);
        cell.setBackgroundColor(COLOR_TOTAL_BG);
        return cell;
    }

    private PdfPCell tableBodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4.5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthBottom(0.3f);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lCell.setPadding(3f);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPadding(3f);
        table.addCell(vCell);
    }

    private String resolveCustomerDocument(Order order) {
        PointOfSale pos = order.getPointOfSale();
        if (pos != null) {
            if (pos.getTaxIdOrDni() != null && !pos.getTaxIdOrDni().isBlank() && !pos.getTaxIdOrDni().startsWith("WEB-")) {
                return pos.getTaxIdOrDni().trim();
            }
            Retailer r = pos.getRetailer();
            if (r != null && r.getDocumentNumber() != null && !r.getDocumentNumber().isBlank() && !r.getDocumentNumber().startsWith("WEB-")) {
                return r.getDocumentNumber().trim();
            }
        }
        return "20-34567890-1";
    }

    private String formatTaxIdOrDni(String rawDoc) {
        if (rawDoc == null || rawDoc.isBlank()) {
            return "20-34567890-1";
        }
        String digits = rawDoc.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 10) + "-" + digits.substring(10);
        }
        return rawDoc;
    }

    private String resolveFullDeliveryAddress(Order order) {
        String addr = orderDtoMapper.resolveDeliveryAddress(order);
        String neighborhood = order.getDeliveryNeighborhood();
        String city = order.getDeliveryCity();
        if ((city == null || city.isBlank()) && order.getPointOfSale() != null
                && order.getPointOfSale().getAddress() != null
                && order.getPointOfSale().getAddress().getCity() != null) {
            city = order.getPointOfSale().getAddress().getCity().getName();
        }
        StringBuilder sb = new StringBuilder();
        if (addr != null && !addr.isBlank()) sb.append(addr);
        if (neighborhood != null && !neighborhood.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(neighborhood);
        }
        if (city != null && !city.isBlank()) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(city);
        }
        if (!sb.toString().toLowerCase().contains("mendoza")) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Mendoza");
        }
        return sb.isEmpty() ? "Gran Mendoza, Mendoza" : sb.toString();
    }

    private String formatPaymentTypeSpanish(PaymentType type) {
        return paymentStrategyFactory.formatDisplayNameEs(type);
    }

    private byte[] buildAfipQrPngBytes(
            LocalDate fecha,
            int tipoCmp,
            Long nroCmp,
            double importe,
            String customerDoc,
            String cae
    ) {
        try {
            String digits = customerDoc != null ? customerDoc.replaceAll("\\D", "") : "20345678901";
            int tipoDocRec = digits.length() == 11 ? 80 : 96;
            long nroDocRec = digits.isEmpty() ? 20345678901L : Long.parseLong(digits.substring(0, Math.min(11, digits.length())));
            String json = String.format(Locale.US,
                    "{\"ver\":1,\"fecha\":\"%s\",\"cuit\":%s,\"ptoVta\":%d,\"tipoCmp\":%d,\"nroCmp\":%d,\"importe\":%.2f,\"moneda\":\"PES\",\"ctz\":1,\"tipoDocRec\":%d,\"nroDocRec\":%d,\"tipoCodAut\":\"E\",\"codAut\":%s}",
                    fecha.toString(),
                    EMISOR_CUIT_RAW,
                    PUNTO_DE_VENTA,
                    tipoCmp,
                    nroCmp != null ? nroCmp : 10001L,
                    importe,
                    tipoDocRec,
                    nroDocRec,
                    cae
            );
            String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String afipUrl = "https://www.afip.gob.ar/fe/qr/?p=" + base64Payload;

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(afipUrl, BarcodeFormat.QR_CODE, 180, 180, hints);
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
            return pngOut.toByteArray();
        } catch (Exception ex) {
            log.warn("Could not generate AFIP QR image: {}", ex.getMessage());
            return null;
        }
    }

    private String formatMoney(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "AR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(value);
    }

    private String formatDec(int value) {
        return value + ",00";
    }

    private String numberToSpanishWords(long number) {
        if (number <= 0) return "CERO";
        if (number >= 1_000_000_000L) return String.valueOf(number);

        StringBuilder words = new StringBuilder();
        long millions = number / 1_000_000L;
        long remainder = number % 1_000_000L;

        if (millions > 0) {
            if (millions == 1) {
                words.append("UN MILLÓN ");
            } else {
                words.append(convertThousandsGroup((int) millions)).append(" MILLONES ");
            }
        }

        if (remainder > 0) {
            words.append(convertThousandsGroup((int) remainder));
        }

        return words.toString().trim() + " CON 00/100";
    }

    private String convertThousandsGroup(int n) {
        int thousands = n / 1000;
        int hundreds = n % 1000;
        StringBuilder sb = new StringBuilder();
        if (thousands > 0) {
            if (thousands == 1) {
                sb.append("MIL ");
            } else {
                sb.append(convertHundredsGroup(thousands)).append(" MIL ");
            }
        }
        if (hundreds > 0) {
            sb.append(convertHundredsGroup(hundreds));
        }
        return sb.toString().trim();
    }

    private String convertHundredsGroup(int n) {
        if (n == 100) return "CIEN";
        String[] hundreds = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};
        int h = n / 100;
        int rem = n % 100;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(hundreds[h]).append(" ");
        }
        if (rem > 0) {
            sb.append(convertTensGroup(rem));
        }
        return sb.toString().trim();
    }

    private String convertTensGroup(int n) {
        String[] units = {"", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
                "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
                "VEINTE", "VEINTIÚN", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
        if (n < 30) {
            return units[n];
        }
        String[] tens = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
        int t = n / 10;
        int u = n % 10;
        return u == 0 ? tens[t] : tens[t] + " Y " + units[u];
    }
}
