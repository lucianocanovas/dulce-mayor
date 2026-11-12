package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.AuthResponseDTO;
import ing.software.dulcemayor.landing.dto.CustomerProfileDTO;
import ing.software.dulcemayor.landing.dto.CustomerRegisterRequestDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final BackendApiClient backendApiClient;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) != null) {
            return "redirect:/my-orders";
        }
        model.addAttribute("title", "Ingresar a Mi Cuenta — Dulce Mayor");
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        try {
            AuthResponseDTO auth = backendApiClient.loginCustomer(email, password);
            CustomerProfileDTO profile = backendApiClient.getCustomerProfile(auth.getUserId());
            if (profile == null) {
                profile = new CustomerProfileDTO();
                profile.setUserId(auth.getUserId());
                profile.setEmail(auth.getUsername());
                profile.setFullName(auth.getFullName());
                profile.setRole(auth.getRole());
            }
            session.setAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY, profile);
            session.setAttribute(GlobalModelAttributes.SESSION_AUTH_KEY, auth);
            return "redirect:/my-orders";
        } catch (Exception ex) {
            model.addAttribute("title", "Ingresar a Mi Cuenta — Dulce Mayor");
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Email o contraseña incorrectos.");
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) != null) {
            return "redirect:/my-orders";
        }
        model.addAttribute("title", "Crear Cuenta Mayorista — Dulce Mayor");
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String taxIdOrDni,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(required = false) String deliveryNeighborhood,
            @RequestParam(required = false) String deliveryCity,
            HttpSession session,
            Model model) {
        try {
            CustomerRegisterRequestDTO req = CustomerRegisterRequestDTO.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(password)
                    .phone(phone)
                    .storeName(storeName)
                    .taxIdOrDni(taxIdOrDni)
                    .deliveryAddress(deliveryAddress)
                    .deliveryNeighborhood(deliveryNeighborhood)
                    .deliveryCity(deliveryCity)
                    .build();

            AuthResponseDTO auth = backendApiClient.registerCustomer(req);
            CustomerProfileDTO profile = backendApiClient.getCustomerProfile(auth.getUserId());
            if (profile == null) {
                profile = new CustomerProfileDTO();
                profile.setUserId(auth.getUserId());
                profile.setEmail(auth.getUsername());
                profile.setFullName(auth.getFullName());
                profile.setFirstName(firstName);
                profile.setLastName(lastName);
                profile.setStoreName(storeName);
                profile.setDeliveryAddress(deliveryAddress);
                profile.setDeliveryCity(deliveryCity);
                profile.setRole(auth.getRole());
            }
            session.setAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY, profile);
            session.setAttribute(GlobalModelAttributes.SESSION_AUTH_KEY, auth);
            return "redirect:/my-orders";
        } catch (Exception ex) {
            model.addAttribute("title", "Crear Cuenta Mayorista — Dulce Mayor");
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "No se pudo crear la cuenta.");
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("storeName", storeName);
            model.addAttribute("taxIdOrDni", taxIdOrDni);
            model.addAttribute("deliveryAddress", deliveryAddress);
            model.addAttribute("deliveryNeighborhood", deliveryNeighborhood);
            model.addAttribute("deliveryCity", deliveryCity);
            return "register";
        }
    }

    @PostMapping("/auth/social")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> socialLogin(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        try {
            Map<String, Object> payload = new HashMap<>(body);
            payload.putIfAbsent("provider", "GOOGLE");
            // Si el usuario no existe aún, desde la landing page se registra como cliente (RETAILER)
            payload.putIfAbsent("defaultRole", "RETAILER");

            AuthResponseDTO auth = backendApiClient.socialLogin(payload);
            CustomerProfileDTO profile = backendApiClient.getCustomerProfile(auth.getUserId());
            if (profile == null) {
                profile = new CustomerProfileDTO();
                profile.setUserId(auth.getUserId());
                profile.setEmail(auth.getUsername());
                profile.setFullName(auth.getFullName());
                profile.setRole(auth.getRole());
            }
            session.setAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY, profile);
            session.setAttribute(GlobalModelAttributes.SESSION_AUTH_KEY, auth);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "redirectUrl", "/my-orders",
                    "role", auth.getRole() != null ? auth.getRole() : "RETAILER"
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", ex.getMessage() != null ? ex.getMessage() : "Error al iniciar sesión con Google."
            ));
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
