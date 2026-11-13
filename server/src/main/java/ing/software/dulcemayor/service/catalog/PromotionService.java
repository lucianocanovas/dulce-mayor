package ing.software.dulcemayor.service.catalog;

import ing.software.dulcemayor.dto.catalog.PromotionDTO;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.util.InputValidator;
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
        InputValidator.validatePromotionRequest(dto);
        String normalizedCode = dto.getCode().trim().toUpperCase();

        promotionRepository.findFirstByCodeIgnoreCaseAndIsDeletedFalse(normalizedCode)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una promoción registrada con el código de cupón '" + normalizedCode + "'.");
                });

        Product product = null;
        if (dto.getProductId() != null && !dto.getProductId().isBlank()) {
            product = productRepository.findByIdAndIsDeletedFalse(dto.getProductId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado para la promoción no existe o fue dado de baja."));
        }

        Promotion promo = Promotion.builder()
                .code(normalizedCode)
                .description(dto.getDescription().trim())
                .discountPercentage(dto.getDiscountPercentage())
                .minimumQuantity(dto.getMinimumQuantity() != null ? dto.getMinimumQuantity() : 1)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .product(product)
                .build();

        return mapToDTO(promotionRepository.save(promo));
    }

    @Transactional
    public PromotionDTO update(String id, PromotionDTO dto) {
        InputValidator.validatePromotionRequest(dto);
        Promotion promo = promotionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada con ID: " + id));

        String normalizedCode = dto.getCode().trim().toUpperCase();
        promotionRepository.findFirstByCodeIgnoreCaseAndIsDeletedFalse(normalizedCode)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Otra promoción ya utiliza el código de cupón '" + normalizedCode + "'.");
                    }
                });

        promo.setCode(normalizedCode);
        promo.setDescription(dto.getDescription().trim());
        promo.setDiscountPercentage(dto.getDiscountPercentage());
        promo.setMinimumQuantity(dto.getMinimumQuantity() != null ? dto.getMinimumQuantity() : 1);
        promo.setStartDate(dto.getStartDate());
        promo.setEndDate(dto.getEndDate());

        if (dto.getProductId() == null || dto.getProductId().isBlank()) {
            promo.setProduct(null);
        } else {
            Product product = productRepository.findByIdAndIsDeletedFalse(dto.getProductId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado para la promoción no existe o fue dado de baja."));
            promo.setProduct(product);
        }

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
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un código de cupón válido.");
        }
        Promotion promo = promotionRepository.findActivePromotionByCode(code.toUpperCase().trim(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("El cupón '" + code.trim().toUpperCase() + "' no existe o ha expirado."));
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
