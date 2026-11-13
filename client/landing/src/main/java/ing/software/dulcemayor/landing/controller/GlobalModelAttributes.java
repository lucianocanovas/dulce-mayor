package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.AuthResponseDTO;
import ing.software.dulcemayor.landing.dto.CustomerProfileDTO;
import ing.software.dulcemayor.landing.dto.DollarQuoteDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    public static final String SESSION_CUSTOMER_KEY = "DULCE_MAYOR_CUSTOMER";
    public static final String SESSION_AUTH_KEY = "DULCE_MAYOR_AUTH";

    private final BackendApiClient backendApiClient;

    @ModelAttribute("dollarQuote")
    public DollarQuoteDTO dollarQuote() {
        return backendApiClient.getDollarQuote(false);
    }

    @Value("${portal.central.url:http://localhost:5173}")
    private String centralPortalUrl;

    @Value("${portal.order.url:http://localhost:5174}")
    private String orderPortalUrl;

    @Value("${auth0.domain:}")
    private String auth0Domain;

    @Value("${auth0.client-id:}")
    private String auth0ClientId;

    @ModelAttribute("currentCustomer")
    public CustomerProfileDTO currentCustomer(HttpSession session) {
        Object attr = session.getAttribute(SESSION_CUSTOMER_KEY);
        return (attr instanceof CustomerProfileDTO profile) ? profile : null;
    }

    @ModelAttribute("auth0Domain")
    public String auth0Domain() {
        return auth0Domain;
    }

    @ModelAttribute("auth0ClientId")
    public String auth0ClientId() {
        return auth0ClientId;
    }

    @ModelAttribute("portalLabel")
    public String portalLabel(HttpSession session) {
        CustomerProfileDTO profile = currentCustomer(session);
        if (profile == null || profile.getRole() == null) {
            return null;
        }
        return switch (profile.getRole()) {
            case "MANAGER", "ADMINISTRATIVE" -> "Ir al Panel Central";
            case "PRESALE_AGENT" -> "Ir al Portal Preventista";
            case "DELIVERY_AGENT" -> "Ir al Portal Distribuidor";
            default -> null; // RETAILER (cliente) no tiene acceso a los portales internos
        };
    }

    @ModelAttribute("portalUrl")
    public String portalUrl(HttpSession session) {
        CustomerProfileDTO profile = currentCustomer(session);
        if (profile == null || profile.getRole() == null) {
            return null;
        }
        String baseUrl = switch (profile.getRole()) {
            case "MANAGER", "ADMINISTRATIVE" -> centralPortalUrl;
            case "PRESALE_AGENT", "DELIVERY_AGENT" -> orderPortalUrl;
            default -> null;
        };
        if (baseUrl == null) {
            return null;
        }

        String ssoParam = buildSsoHash(session, profile);
        return ssoParam != null ? baseUrl + "#sso=" + ssoParam : baseUrl;
    }

    private String buildSsoHash(HttpSession session, CustomerProfileDTO profile) {
        try {
            Object authObj = session.getAttribute(SESSION_AUTH_KEY);
            String userId = profile.getUserId();
            String username = profile.getEmail();
            String fullName = profile.getFullName();
            String role = profile.getRole();
            boolean tempPass = false;
            String token = "jwt-landing-sso";

            if (authObj instanceof AuthResponseDTO auth) {
                if (auth.getUserId() != null) userId = auth.getUserId();
                if (auth.getUsername() != null) username = auth.getUsername();
                if (auth.getFullName() != null) fullName = auth.getFullName();
                if (auth.getRole() != null) role = auth.getRole();
                tempPass = auth.isTemporaryPassword();
                if (auth.getToken() != null) token = auth.getToken();
            }

            String json = String.format(
                    "{\"userId\":\"%s\",\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"temporaryPassword\":%s,\"token\":\"%s\",\"timestamp\":%d}",
                    escapeJson(userId),
                    escapeJson(username),
                    escapeJson(fullName),
                    escapeJson(role),
                    tempPass,
                    escapeJson(token),
                    System.currentTimeMillis()
            );
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
