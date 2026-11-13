package ing.software.dulcemayor.service.catalog;

import ing.software.dulcemayor.dto.catalog.PromotionDTO;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.service.base.BaseService;
import ing.software.dulcemayor.util.InputValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromotionService extends BaseService<Promotion, PromotionDTO, PromotionDTO> {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    public PromotionService(PromotionRepository promotionRepository, ProductRepository productRepository) {
        super(promotionRepository);
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionDTO> findAll() {
        return getAll();
    }

    @Override
    protected void validate(PromotionDTO dto, String id) {
        InputValidator.validatePromotionRequest(dto);
        String normalizedCode = dto.getCode().trim().toUpperCase();

        promotionRepository.findFirstByCodeIgnoreCaseAndIsDeletedFalse(normalizedCode)
                .ifPresent(existing -> {
                    if (id == null) {
                        throw new IllegalArgumentException("Ya existe una promoción registrada con el código de cupón '" + normalizedCode + "'.");
                    } else if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Otra promoción ya utiliza el código de cupón '" + normalizedCode + "'.");
                    }
                });

        if (dto.getProductId() != null && !dto.getProductId().isBlank()) {
            productRepository.findByIdAndIsDeletedFalse(dto.getProductId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado para la promoción no existe o fue dado de baja."));
        }
    }

    @Override
    protected Promotion toEntity(PromotionDTO dto) {
        Product product = resolveProduct(dto.getProductId());
        return Promotion.builder()
                .code(dto.getCode().trim().toUpperCase())
                .description(dto.getDescription().trim())
                .discountPercentage(dto.getDiscountPercentage())
                .minimumQuantity(dto.getMinimumQuantity() != null ? dto.getMinimumQuantity() : 1)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .product(product)
                .build();
    }

    @Override
    protected void updateEntityFromDto(Promotion promo, PromotionDTO dto) {
        promo.setCode(dto.getCode().trim().toUpperCase());
        promo.setDescription(dto.getDescription().trim());
        promo.setDiscountPercentage(dto.getDiscountPercentage());
        promo.setMinimumQuantity(dto.getMinimumQuantity() != null ? dto.getMinimumQuantity() : 1);
        promo.setStartDate(dto.getStartDate());
        promo.setEndDate(dto.getEndDate());
        promo.setProduct(resolveProduct(dto.getProductId()));
    }

    private Product resolveProduct(String productId) {
        if (productId == null || productId.isBlank()) {
            return null;
        }
        return productRepository.findByIdAndIsDeletedFalse(productId.trim())
                .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado para la promoción no existe o fue dado de baja."));
    }

    @Transactional(readOnly = true)
    public PromotionDTO validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un código de cupón válido.");
        }
        Promotion promo = promotionRepository.findActivePromotionByCode(code.toUpperCase().trim(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("El cupón '" + code.trim().toUpperCase() + "' no existe o ha expirado."));
        return toResponseDto(promo);
    }

    @Override
    protected PromotionDTO toResponseDto(Promotion promo) {
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
