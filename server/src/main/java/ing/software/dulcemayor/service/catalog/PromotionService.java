package ing.software.dulcemayor.service.catalog;

import ing.software.dulcemayor.dto.catalog.PromotionDTO;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<PromotionDTO> findAll() {
        return promotionRepository.findByIsDeletedFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromotionDTO create(PromotionDTO dto) {
        Product product = null;
        if (dto.getProductId() != null && !dto.getProductId().isBlank()) {
            product = productRepository.findByIdAndIsDeletedFalse(dto.getProductId())
                    .orElse(null);
        }

        Promotion promo = Promotion.builder()
                .code(dto.getCode().toUpperCase().trim())
                .description(dto.getDescription())
                .discountPercentage(dto.getDiscountPercentage())
                .minimumQuantity(dto.getMinimumQuantity() != null ? dto.getMinimumQuantity() : 1)
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now())
                .endDate(dto.getEndDate() != null ? dto.getEndDate() : LocalDate.now().plusMonths(1))
                .product(product)
                .build();

        return mapToDTO(promotionRepository.save(promo));
    }

    @Transactional
    public void delete(String id) {
        Promotion promo = promotionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada"));
        promo.setDeleted(true);
        promotionRepository.save(promo);
    }

    @Transactional(readOnly = true)
    public PromotionDTO validateCode(String code) {
        Promotion promo = promotionRepository.findActivePromotionByCode(code.toUpperCase().trim(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("El cupón '" + code + "' no existe o ha expirado."));
        return mapToDTO(promo);
    }

    private PromotionDTO mapToDTO(Promotion promo) {
        LocalDate today = LocalDate.now();
        boolean isActive = !promo.isDeleted() &&
                (promo.getStartDate() == null || !today.isBefore(promo.getStartDate())) &&
                (promo.getEndDate() == null || !today.isAfter(promo.getEndDate()));

        return PromotionDTO.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .description(promo.getDescription())
                .discountPercentage(promo.getDiscountPercentage())
                .minimumQuantity(promo.getMinimumQuantity())
                .startDate(promo.getStartDate())
                .endDate(promo.getEndDate())
                .productId(promo.getProduct() != null ? promo.getProduct().getId() : null)
                .productName(promo.getProduct() != null ? promo.getProduct().getName() : null)
                .active(isActive)
                .build();
    }
}
