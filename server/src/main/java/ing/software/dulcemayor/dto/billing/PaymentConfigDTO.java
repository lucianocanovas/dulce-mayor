package ing.software.dulcemayor.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfigDTO {
    private boolean mercadoPagoEnabled;
    private boolean sandboxEnabled;
    private String publicKey;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private String bankName;
}

