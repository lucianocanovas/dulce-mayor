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

    @GetMapping("/profile")
    public String profilePage(
            @RequestParam(required = false) String updated,
            @RequestParam(required = false) String passwordChanged,
            @RequestParam(required = false) String storeAdded,
            HttpSession session,
            Model model) {
        Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
        if (!(attr instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        CustomerProfileDTO fresh = backendApiClient.getCustomerProfile(customer.getUserId());
        if (fresh != null) {
            session.setAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY, fresh);
            model.addAttribute("currentCustomer", fresh);
            customer = fresh;
        }
        model.addAttribute("title", "Mi Perfil — Dulce Mayor");
        model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
        model.addAttribute("updated", updated != null);
        model.addAttribute("passwordChanged", passwordChanged != null);
        model.addAttribute("storeAdded", storeAdded != null);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String taxIdOrDni,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(required = false) String deliveryNeighborhood,
            @RequestParam(required = false) String deliveryCity,
            HttpSession session,
            Model model) {
        Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
        if (!(attr instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("phone", phone != null ? phone : "");
            payload.put("taxIdOrDni", taxIdOrDni != null ? taxIdOrDni : "");
            payload.put("storeName", storeName != null ? storeName : "");
            payload.put("deliveryAddress", deliveryAddress != null ? deliveryAddress : "");
            payload.put("deliveryNeighborhood", deliveryNeighborhood != null ? deliveryNeighborhood : "");
            payload.put("deliveryCity", deliveryCity != null ? deliveryCity : "");

            CustomerProfileDTO updated = backendApiClient.updateCustomerProfile(customer.getUserId(), payload);
            session.setAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY, updated);
            return "redirect:/profile?updated=1";
        } catch (Exception ex) {
            model.addAttribute("title", "Mi Perfil — Dulce Mayor");
            model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
            model.addAttribute("profileError", ex.getMessage() != null ? ex.getMessage() : "No se pudo actualizar el perfil.");
            return "profile";
        }
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {
        Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
        if (!(attr instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("title", "Mi Perfil — Dulce Mayor");
            model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
            model.addAttribute("passwordError", "La nueva contraseña debe tener al menos 6 caracteres.");
            return "profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("title", "Mi Perfil — Dulce Mayor");
            model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
            model.addAttribute("passwordError", "Las contraseñas nuevas no coinciden.");
            return "profile";
        }
        try {
            backendApiClient.changePassword(customer.getUserId(), currentPassword, newPassword);
            return "redirect:/profile?passwordChanged=1";
        } catch (Exception ex) {
            model.addAttribute("title", "Mi Perfil — Dulce Mayor");
            model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
            model.addAttribute("passwordError", ex.getMessage() != null ? ex.getMessage() : "La contraseña actual es incorrecta.");
            return "profile";
        }
    }

    @PostMapping("/profile/stores")
    public String addStore(
            @RequestParam String storeName,
            @RequestParam String address,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String phone,
            HttpSession session,
            Model model) {
        Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
        if (!(attr instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("storeName", storeName);
            payload.put("address", address);
            payload.put("neighborhood", neighborhood != null ? neighborhood : "Centro");
            payload.put("city", city != null ? city : "Mendoza Capital");
            payload.put("phone", phone != null ? phone : customer.getPhone());

            backendApiClient.createCustomerStore(customer.getUserId(), payload);
            return "redirect:/profile?storeAdded=1";
        } catch (Exception ex) {
            model.addAttribute("title", "Mi Perfil — Dulce Mayor");
            model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
            model.addAttribute("storeError", ex.getMessage() != null ? ex.getMessage() : "No se pudo guardar el local.");
            return "profile";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
