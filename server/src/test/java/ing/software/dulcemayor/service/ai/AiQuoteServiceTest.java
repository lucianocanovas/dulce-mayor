package ing.software.dulcemayor.service.ai;

import ing.software.dulcemayor.dto.ai.AiChatRequestDTO;
import ing.software.dulcemayor.dto.ai.AiChatResponseDTO;
import ing.software.dulcemayor.dto.auth.CustomerProfileDTO;
import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.service.actor.AuthService;
import ing.software.dulcemayor.service.billing.CheckoutService;
import ing.software.dulcemayor.service.catalog.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuoteServiceTest {

    @Mock
    private ProductService productService;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private AuthService authService;
    @Mock
    private CheckoutService checkoutService;
    @Mock
    private UsdExchangeRateService usdExchangeRateService;

    private AiQuoteService aiQuoteService;

    @BeforeEach
    void setUp() {
        aiQuoteService = new AiQuoteService(
                productService,
                promotionRepository,
                authService,
                checkoutService,
                usdExchangeRateService
        );
    }

    @Test
    void chatBuildsWholesaleQuoteFromCatalogAndIncludesUsdConversion() {
        ProductResponseDTO p1 = ProductResponseDTO.builder()
                .id("p1")
                .code("BON-01")
                .name("Bon o Bon x 30")
                .packaging("Caja x 30 u.")
                .unitsPerPack(30)
                .currentPrice(15000.0)
                .currentStock(10)
                .onSale(true)
                .build();

        when(productService.getInStockProducts()).thenReturn(List.of(p1));
        when(promotionRepository.findByIsDeletedFalse()).thenReturn(List.of());
        when(usdExchangeRateService.getOfficialSellRate()).thenReturn(1000.0);

        AiChatRequestDTO request = AiChatRequestDTO.builder()
                .message("Quiero armar un presupuesto por $30.000")
                .build();

        AiChatResponseDTO response = aiQuoteService.chat(request);

        assertNotNull(response);
        assertFalse(response.isAuthenticated());
        assertEquals(1, response.getRecommendedItems().size());
        assertEquals("BON-01", response.getRecommendedItems().get(0).getCode());
        assertEquals(2, response.getRecommendedItems().get(0).getQuantity());
        assertEquals(30000.0, response.getEstimatedTotal());
        assertEquals(30.0, response.getEstimatedTotalUsd());
        assertNotNull(response.getDisclaimer());
    }

    @Test
    void chatAccessesAuthenticatedCustomerInfoAndOrders() {
        when(productService.getInStockProducts()).thenReturn(List.of());
        when(promotionRepository.findByIsDeletedFalse()).thenReturn(List.of());
        when(usdExchangeRateService.getOfficialSellRate()).thenReturn(1000.0);

        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .userId("user-1")
                .firstName("Roberto")
                .lastName("Gómez")
                .fullName("Roberto Gómez")
                .email("cliente@dulcemayor.com.ar")
                .storeName("Kiosco El Trébol")
                .deliveryAddress("Av. San Martín 1420")
                .deliveryCity("Capital")
                .build();

        OrderResponseDTO order = OrderResponseDTO.builder()
                .id("ord-1")
                .orderNumber(1001L)
                .orderDateTime(LocalDateTime.now())
                .status(OrderStatus.DELIVERED)
                .total(47800.0)
                .build();

        when(authService.getCustomerProfile("user-1")).thenReturn(profile);
        when(checkoutService.findCustomerOrders(null, null, "user-1")).thenReturn(List.of(order));

        AiChatRequestDTO request = AiChatRequestDTO.builder()
                .userId("user-1")
                .message("¿Cuál es el estado de mi último pedido?")
                .build();

        AiChatResponseDTO response = aiQuoteService.chat(request);

        assertTrue(response.isAuthenticated());
        assertTrue(response.getReply().contains("Roberto"));
        assertTrue(response.getReply().contains("1001"));
        assertTrue(response.getReply().contains("DELIVERED"));
    }
}
