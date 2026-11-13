package ing.software.dulcemayor.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceValidityResponseDTO {
    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String categoryName;
    private String packaging;
    private double price;
    private Double usdPrice;
    private Double exchangeRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyValid;
    private String validityStatusLabel;
    private String reason;
}

