package ing.software.dulcemayor.dto.billing;

import ing.software.dulcemayor.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {
    private String invoiceId;
    private Long invoiceNumber;
    private LocalDateTime invoiceDate;
    private double total;
    private InvoiceStatus status;
    private String orderId;
    private Long orderNumber;
    private String customerName;
    private String storeName;
    private String paymentType;
    private String paymentReference;
    private List<InvoiceDetailDTO> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceDetailDTO {
        private String id;
        private String productId;
        private String productCode;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double subtotal;
    }
}

