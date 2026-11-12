package ing.software.dulcemayor.landing.dto;

import lombok.Data;

@Data
public class CheckoutResponseDTO {
    private String orderId;
    private Long orderNumber;
    private String status;
    private String paymentType;
    private double subtotal;
    private double discount;
    private double total;
    private String preferenceId;
    private String initPoint;
    private String qrPayload;
    private String qrImageBase64;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private String bankName;
    private boolean readyForDispatch;
    private String message;
}

