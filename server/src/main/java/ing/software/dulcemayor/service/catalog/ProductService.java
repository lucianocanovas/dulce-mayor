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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductService extends BaseService<Product, ProductRequestDTO, ProductResponseDTO> {

    private final ProductRepository productRepository;
    private final PriceValidityRepository priceValidityRepository;
    private final StockRepository stockRepository;
    private final SubCategoryRepository subCategoryRepository;

    public ProductService(ProductRepository productRepository,
                          PriceValidityRepository priceValidityRepository,
                          StockRepository stockRepository,
                          SubCategoryRepository subCategoryRepository) {
        super(productRepository);
        this.productRepository = productRepository;
        this.priceValidityRepository = priceValidityRepository;
        this.stockRepository = stockRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Override
    protected void validate(ProductRequestDTO dto, String id) {
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Product code is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (id == null && productRepository.existsByCodeAndIsDeletedFalse(dto.getCode())) {
            throw new IllegalArgumentException("Product with code " + dto.getCode() + " already exists");
        }
    }

    @Override
    protected Product toEntity(ProductRequestDTO dto) {
        SubCategory subCategory = null;
        if (dto.getSubCategoryId() != null) {
            subCategory = subCategoryRepository.findByIdAndIsDeletedFalse(dto.getSubCategoryId()).orElse(null);
        }

        return Product.builder()
                .code(dto.getCode().trim().toUpperCase())
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .packaging(dto.getPackaging())
                .unitsPerPack(dto.getUnitsPerPack() > 0 ? dto.getUnitsPerPack() : 1)
                .isOnSale(dto.isOnSale())
                .subCategory(subCategory)
                .build();
    }

    @Override
    protected void updateEntityFromDto(Product entity, ProductRequestDTO dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPackaging(dto.getPackaging());
        entity.setUnitsPerPack(dto.getUnitsPerPack() > 0 ? dto.getUnitsPerPack() : entity.getUnitsPerPack());
        entity.setOnSale(dto.isOnSale());
        if (dto.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryRepository.findByIdAndIsDeletedFalse(dto.getSubCategoryId()).orElse(null);
            entity.setSubCategory(subCategory);
        }
    }

    @Override
    protected void afterSave(Product savedEntity, ProductRequestDTO dto) {
        // Handle Price
        if (dto.getPrice() > 0) {
            var activePriceOpt = priceValidityRepository.findActivePriceByProductId(savedEntity.getId());
            if (activePriceOpt.isPresent()) {
                PriceValidity currentPrice = activePriceOpt.get();
                if (Double.compare(currentPrice.getPrice(), dto.getPrice()) != 0) {
                    currentPrice.setEndDate(LocalDate.now());
                    priceValidityRepository.save(currentPrice);

                    PriceValidity newPrice = PriceValidity.builder()
                            .startDate(LocalDate.now())
                            .price(dto.getPrice())
                            .product(savedEntity)
                            .build();
                    priceValidityRepository.save(newPrice);
                }
            } else {
                PriceValidity initialPrice = PriceValidity.builder()
                        .startDate(LocalDate.now())
                        .price(dto.getPrice())
                        .product(savedEntity)
                        .build();
                priceValidityRepository.save(initialPrice);
            }
        }

        // Handle Initial Stock if creating
        if (dto.getInitialStock() >= 0) {
            stockRepository.findByProductIdAndIsDeletedFalse(savedEntity.getId())
                    .ifPresentOrElse(
                            existingStock -> {},
                            () -> {
                                Stock newStock = Stock.builder()
                                        .currentQuantity(dto.getInitialStock())
                                        .note("Initial stock entry")
                                        .product(savedEntity)
                                        .build();
                                stockRepository.save(newStock);
                            }
                    );
        }
    }

    @Override
    protected ProductResponseDTO toResponseDto(Product entity) {
        double currentPrice = priceValidityRepository.findActivePriceByProductId(entity.getId())
                .map(PriceValidity::getPrice)
                .orElse(0.0);

        int currentStock = stockRepository.findByProductIdAndIsDeletedFalse(entity.getId())
                .map(Stock::getCurrentQuantity)
                .orElse(0);

        String subCatName = entity.getSubCategory() != null ? entity.getSubCategory().getName() : "Unassigned";
        String catName = (entity.getSubCategory() != null && entity.getSubCategory().getCategory() != null)
                ? entity.getSubCategory().getCategory().getName()
                : "Unassigned";

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
                .currentStock(currentStock)
                .build();
    }

    @Transactional
    public ProductResponseDTO adjustStock(StockAdjustmentDTO adjustment) {
        Stock stock = stockRepository.findByProductIdAndIsDeletedFalse(adjustment.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found for product: " + adjustment.getProductId()));

        int newQuantity = stock.getCurrentQuantity() + adjustment.getQuantityDelta();
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Resulting stock quantity cannot be negative");
        }

        stock.setCurrentQuantity(newQuantity);
        if (adjustment.getNote() != null) {
            stock.setNote(adjustment.getNote());
        }
        stockRepository.save(stock);

        return toResponseDto(stock.getProduct());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(int threshold) {
        return stockRepository.findLowStock(threshold).stream()
                .map(stock -> toResponseDto(stock.getProduct()))
                .toList();
    }
}
