package ing.software.dulcemayor.service.catalog;

import ing.software.dulcemayor.dto.catalog.ProductRequestDTO;
import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.catalog.StockAdjustmentDTO;
import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.catalog.SubCategory;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.catalog.SubCategoryRepository;
import ing.software.dulcemayor.service.base.BaseService;
import ing.software.dulcemayor.util.InputValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ProductService extends BaseService<Product, ProductRequestDTO, ProductResponseDTO> {

    private final ProductRepository productRepository;
    private final PriceValidityRepository priceValidityRepository;
    private final StockRepository stockRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ExchangeRateService exchangeRateService;

    public ProductService(ProductRepository productRepository,
                          PriceValidityRepository priceValidityRepository,
                          StockRepository stockRepository,
                          SubCategoryRepository subCategoryRepository,
                          ExchangeRateService exchangeRateService) {
        super(productRepository);
        this.productRepository = productRepository;
        this.priceValidityRepository = priceValidityRepository;
        this.stockRepository = stockRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    protected void validate(ProductRequestDTO dto, String id) {
        InputValidator.validateProductRequest(dto, id == null);
        String normalizedCode = dto.getCode().trim().toUpperCase();

        if (id == null) {
            if (productRepository.existsByCodeAndIsDeletedFalse(normalizedCode)) {
                throw new IllegalArgumentException("Ya existe un producto registrado con el código SKU '" + normalizedCode + "'.");
            }
        } else {
            productRepository.findByCodeAndIsDeletedFalse(normalizedCode)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new IllegalArgumentException("Otro producto ya utiliza el código SKU '" + normalizedCode + "'.");
                        }
                    });
        }

        if (dto.getSubCategoryId() != null && !dto.getSubCategoryId().isBlank()) {
            subCategoryRepository.findByIdAndIsDeletedFalse(dto.getSubCategoryId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("La subcategoría seleccionada no existe."));
        }
    }

    @Override
    protected Product toEntity(ProductRequestDTO dto) {
        SubCategory subCategory = null;
        if (dto.getSubCategoryId() != null && !dto.getSubCategoryId().isBlank()) {
            subCategory = subCategoryRepository.findByIdAndIsDeletedFalse(dto.getSubCategoryId().trim()).orElse(null);
        }
        if (subCategory == null) {
            subCategory = subCategoryRepository.findByIsDeletedFalse().stream().findFirst().orElse(null);
        }

        String cleanDesc = (dto.getDescription() != null && !dto.getDescription().trim().isEmpty())
                ? dto.getDescription().trim()
                : null;
        String cleanImg = (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty())
                ? dto.getImageUrl().trim()
                : null;

        return Product.builder()
                .code(dto.getCode().trim().toUpperCase())
                .name(dto.getName().trim())
                .description(cleanDesc)
                .packaging(dto.getPackaging().trim())
                .unitsPerPack(dto.getUnitsPerPack())
                .isOnSale(dto.isOnSale())
                .subCategory(subCategory)
                .imageUrl(cleanImg)
                .build();
    }

    @Override
    protected void updateEntityFromDto(Product entity, ProductRequestDTO dto) {
        entity.setCode(dto.getCode().trim().toUpperCase());
        entity.setName(dto.getName().trim());
        entity.setDescription((dto.getDescription() != null && !dto.getDescription().trim().isEmpty())
                ? dto.getDescription().trim()
                : null);
        entity.setPackaging(dto.getPackaging().trim());
        entity.setUnitsPerPack(dto.getUnitsPerPack());
        entity.setOnSale(dto.isOnSale());

        if (dto.getSubCategoryId() != null && !dto.getSubCategoryId().isBlank()) {
            SubCategory subCategory = subCategoryRepository.findByIdAndIsDeletedFalse(dto.getSubCategoryId().trim()).orElse(null);
            if (subCategory != null) {
                entity.setSubCategory(subCategory);
            }
        }

        // Si se envía vacío o null desde el formulario de edición ("Quitar Foto"), limpiar la imagen
        if (dto.getImageUrl() == null || dto.getImageUrl().trim().isEmpty()) {
            entity.setImageUrl(null);
        } else {
            entity.setImageUrl(dto.getImageUrl().trim());
        }
    }

    @Override
    protected void beforeSave(Product entity, ProductRequestDTO dto) {
        if (dto.getPrice() > 0) {
            double currentRate = exchangeRateService.getActiveExchangeRate();
            double computedUsd = Math.round((dto.getPrice() / currentRate) * 100.0) / 100.0;
            entity.setBasePriceUsd(computedUsd);
        }
    }

    @Override
    protected void afterSave(Product savedEntity, ProductRequestDTO dto) {
        if (dto.getPrice() > 0) {
            double currentRate = exchangeRateService.getActiveExchangeRate();
            double computedUsd = savedEntity.getBasePriceUsd() != null
                    ? savedEntity.getBasePriceUsd()
                    : Math.round((dto.getPrice() / currentRate) * 100.0) / 100.0;

            var activePriceOpt = priceValidityRepository.findActivePriceByProductId(savedEntity.getId());
            if (activePriceOpt.isPresent()) {
                PriceValidity currentPrice = activePriceOpt.get();
                if (Double.compare(currentPrice.getPrice(), dto.getPrice()) != 0) {
                    currentPrice.setEndDate(LocalDate.now());
                    priceValidityRepository.save(currentPrice);

                    PriceValidity newPrice = PriceValidity.builder()
                            .startDate(LocalDate.now())
                            .price(dto.getPrice())
                            .usdPrice(computedUsd)
                            .exchangeRate(currentRate)
                            .reason("Actualización manual de catálogo")
                            .product(savedEntity)
                            .build();
                    priceValidityRepository.save(newPrice);
                }
            } else {
                PriceValidity initialPrice = PriceValidity.builder()
                        .startDate(LocalDate.now())
                        .price(dto.getPrice())
                        .usdPrice(computedUsd)
                        .exchangeRate(currentRate)
                        .reason("Precio inicial de alta en catálogo")
                        .product(savedEntity)
                        .build();
                priceValidityRepository.save(initialPrice);
            }
        }

        if (dto.getInitialStock() >= 0) {
            stockRepository.findByProductIdAndIsDeletedFalse(savedEntity.getId())
                    .ifPresentOrElse(
                            existingStock -> {},
                            () -> {
                                Stock newStock = Stock.builder()
                                        .currentQuantity(dto.getInitialStock())
                                        .note("Stock inicial de alta de producto")
                                        .product(savedEntity)
                                        .build();
                                stockRepository.save(newStock);
                            }
                    );
        }
    }

    @Override
    protected ProductResponseDTO toResponseDto(Product entity) {
        var activePriceOpt = priceValidityRepository.findActivePriceByProductId(entity.getId());
        double currentPrice = activePriceOpt
                .map(PriceValidity::getPrice)
                .orElse(0.0);
        Double activeRate = activePriceOpt
                .map(PriceValidity::getExchangeRate)
                .orElseGet(exchangeRateService::getActiveExchangeRate);
        Double usdPrice = entity.getBasePriceUsd() != null
                ? entity.getBasePriceUsd()
                : activePriceOpt.map(PriceValidity::getUsdPrice).orElse(null);

        int currentStock = stockRepository.findByProductIdAndIsDeletedFalse(entity.getId())
                .map(Stock::getCurrentQuantity)
                .orElse(0);

        String subCatName = entity.getSubCategory() != null ? entity.getSubCategory().getName() : "General";
        String catName = (entity.getSubCategory() != null && entity.getSubCategory().getCategory() != null)
                ? entity.getSubCategory().getCategory().getName()
                : "General";

        return ProductResponseDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .packaging(entity.getPackaging())
                .unitsPerPack(entity.getUnitsPerPack())
                .onSale(entity.isOnSale())
                .subCategoryId(entity.getSubCategory() != null ? entity.getSubCategory().getId() : null)
                .subCategoryName(subCatName)
                .categoryName(catName)
                .currentPrice(currentPrice)
                .basePriceUsd(usdPrice)
                .exchangeRate(activeRate)
                .currentStock(currentStock)
                .imageUrl(entity.getImageUrl())
                .imageBase64(entity.getImageUrl())
                .build();
    }

    @Transactional
    public ProductResponseDTO adjustStock(StockAdjustmentDTO adjustment) {
        InputValidator.validateStockAdjustment(adjustment);

        Stock stock = stockRepository.findByProductIdAndIsDeletedFalse(adjustment.getProductId().trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró registro de stock para el producto seleccionado."));

        int newQuantity = stock.getCurrentQuantity() + adjustment.getQuantityDelta();
        if (newQuantity < 0) {
            throw new IllegalArgumentException("El stock resultante no puede ser negativo (Stock actual: "
                    + stock.getCurrentQuantity() + " u., ajuste solicitado: " + adjustment.getQuantityDelta() + " u.).");
        }

        stock.setCurrentQuantity(newQuantity);
        stock.setNote(adjustment.getNote().trim());
        stockRepository.save(stock);

        return toResponseDto(stock.getProduct());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(int threshold) {
        return stockRepository.findLowStock(threshold).stream()
                .filter(stock -> stock.getProduct() != null && !stock.getProduct().isDeleted())
                .map(stock -> toResponseDto(stock.getProduct()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getInStockProducts() {
        return getAll().stream()
                .filter(p -> p.getCurrentStock() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllSubCategories() {
        return subCategoryRepository.findByIsDeletedFalse().stream()
                .map(sc -> Map.of(
                        "id", sc.getId(),
                        "name", sc.getName(),
                        "categoryName", sc.getCategory() != null ? sc.getCategory().getName() : "General"
                ))
                .toList();
    }
}
