package ing.software.dulcemayor.landing.dto;

import lombok.Data;

@Data
public class PaymentConfigDTO {
    private boolean mercadoPagoEnabled;
    private boolean sandboxEnabled = true;
    private String publicKey;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private String bankName;
}

