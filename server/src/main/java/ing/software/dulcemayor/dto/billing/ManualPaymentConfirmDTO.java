package ing.software.dulcemayor.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualPaymentConfirmDTO {
    private String paymentType;
    private String reference;
    private String note;
}

