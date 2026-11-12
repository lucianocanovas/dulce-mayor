package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.CustomerProfileDTO;
import ing.software.dulcemayor.dto.auth.CustomerRegisterRequestDTO;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCustomerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RetailerRepository retailerRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private PointOfSaleRepository pointOfSaleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCustomerCreatesRetailerAndUser() {
        CustomerRegisterRequestDTO request = CustomerRegisterRequestDTO.builder()
                .firstName("Lucía")
                .lastName("Fernández")
                .email("lucia@kiosco.com")
                .password("secreta123")
                .phone("2615550000")
                .storeName("Kiosco Sol")
                .deliveryAddress("Av. San Martín 100")
                .deliveryCity("Capital")
                .build();

        when(userRepository.existsByUsernameAndIsDeletedFalse("lucia@kiosco.com")).thenReturn(false);
        when(passwordEncoder.encode("secreta123")).thenReturn("hashed");
        when(retailerRepository.save(any(Retailer.class))).thenAnswer(inv -> {
            Retailer r = inv.getArgument(0);
            r.setId("ret-1");
            return r;
        });
        when(cityRepository.findByIsDeletedFalse()).thenReturn(List.of(City.builder().id("c1").name("Capital").build()));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointOfSaleRepository.save(any(PointOfSale.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("usr-1");
            return u;
        });

        AuthResponseDTO result = authService.registerCustomer(request);

        assertEquals("usr-1", result.getUserId());
        assertEquals("lucia@kiosco.com", result.getUsername());
        assertEquals("Lucía Fernández", result.getFullName());
        assertEquals(UserRole.RETAILER, result.getRole());
        assertFalse(result.isTemporaryPassword());
    }

    @Test
    void getCustomerProfileReturnsRetailerAndStoreDetails() {
        Retailer retailer = Retailer.builder()
                .id("ret-1")
                .firstName("Roberto")
                .lastName("Gómez")
                .documentNumber("20-28491823-4")
                .phone("2614201122")
                .build();

        User user = User.builder()
                .id("usr-1")
                .username("cliente@dulcemayor.com.ar")
                .role(UserRole.RETAILER)
                .person(retailer)
                .build();

        Address address = Address.builder()
                .street("Av. San Martín 1420")
                .neighborhood("Centro")
                .city(City.builder().name("Capital").build())
                .build();

        PointOfSale pos = PointOfSale.builder()
                .storeName("Kiosco El Trébol")
                .retailer(retailer)
                .address(address)
                .build();

        when(userRepository.findByIdAndIsDeletedFalse("usr-1")).thenReturn(Optional.of(user));
        when(pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse("ret-1")).thenReturn(List.of(pos));

        CustomerProfileDTO profile = authService.getCustomerProfile("usr-1");

        assertEquals("Roberto Gómez", profile.getFullName());
        assertEquals("Kiosco El Trébol", profile.getStoreName());
        assertEquals("Av. San Martín 1420", profile.getDeliveryAddress());
        assertEquals("Capital", profile.getDeliveryCity());
    }
}
