package ing.software.dulcemayor.landing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DollarQuoteDTO {
    private double buyRate;
    private double sellRate;
    private double blueBuyRate;
    private double blueSellRate;
    private double appliedRate;
    private String rateType;
    private String source;
    private String updatedAt;
    private int updatedProductsCount;
}

