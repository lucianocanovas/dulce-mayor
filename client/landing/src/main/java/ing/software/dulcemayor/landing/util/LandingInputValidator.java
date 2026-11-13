package ing.software.dulcemayor.landing.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validaciones en el servidor de la Landing Page (:8081) previas al envío al Backend REST (:8080).
 */
public final class LandingInputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\s.'\\-()]+$"
    );

    private static final Pattern PHONE_CHARS_PATTERN = Pattern.compile(
            "^[0-9+\\-()\\s.]+$"
    );

    private static final Set<String> VALID_CUIT_PREFIXES = Set.of(
            "20", "23", "24", "27", "30", "33", "34"
    );

    private static final Set<String> ALLOWED_CITIES = Set.of(
            "mendoza capital",
            "godoy cruz",
            "guaymallén",
            "guaymallen",
            "maipú",
            "maipu",
            "las heras",
            "luján de cuyo",
            "lujan de cuyo"
    );

    private LandingInputValidator() {
    }

    public static void validateStoreForm(
            String storeName,
            String address,
            String neighborhood,
            String city,
            String ownerName,
            String phone,
            String contactEmail,
            String taxIdOrDni,
            String deliveryNotes) {
        validateStoreName(storeName, true);
        validateAddress(address, true);
        validateNeighborhood(neighborhood);
        validateCity(city, true);
        validateRecipientName(ownerName, "El nombre y apellido de quien recibe", true);
        validatePhone(phone, true);
        if (hasText(contactEmail)) {
            validateEmail(contactEmail, false);
        }
        if (hasText(taxIdOrDni)) {
            validateTaxIdOrDni(taxIdOrDni, false);
        }
        validateNotes(deliveryNotes);
    }

    public static void validateProfileForm(
            String firstName,
            String lastName,
            String phone,
            String taxIdOrDni,
            String storeName,
            String deliveryAddress,
            String deliveryNeighborhood,
            String deliveryCity) {
        validatePersonPartName(firstName, "El nombre");
        validatePersonPartName(lastName, "El apellido");
        if (hasText(phone)) {
            validatePhone(phone, false);
        }
        if (hasText(taxIdOrDni)) {
            validateTaxIdOrDni(taxIdOrDni, false);
        }
        if (hasText(storeName)) {
            validateStoreName(storeName, false);
        }
        if (hasText(deliveryAddress)) {
            validateAddress(deliveryAddress, false);
        }
        if (hasText(deliveryNeighborhood)) {
            validateNeighborhood(deliveryNeighborhood);
        }
        if (hasText(deliveryCity)) {
            validateCity(deliveryCity, false);
        }
    }

    public static void validateStoreName(String storeName, boolean required) {
        if (!hasText(storeName)) {
            if (required) {
                throw new IllegalArgumentException("El nombre del local o comercio es obligatorio.");
            }
            return;
        }
        String trimmed = storeName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new IllegalArgumentException("El nombre del local debe tener entre 2 y 100 caracteres.");
        }
        rejectHtmlTags(trimmed, "El nombre del local");
        if (!trimmed.matches(".*[\\p{L}0-9].*")) {
            throw new IllegalArgumentException("El nombre del local debe contener letras o números válidos.");
        }
    }

    public static void validateAddress(String address, boolean required) {
        if (!hasText(address)) {
            if (required) {
                throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
            }
            return;
        }
        String trimmed = address.trim();
        if (trimmed.length() < 5 || trimmed.length() > 150) {
            throw new IllegalArgumentException("La dirección de entrega debe tener entre 5 y 150 caracteres (ej: Av. San Martín 1450).");
        }
        rejectHtmlTags(trimmed, "La dirección de entrega");
        if (!trimmed.matches(".*\\p{L}.*")) {
            throw new IllegalArgumentException("La dirección de entrega debe incluir el nombre de la calle o avenida.");
        }
    }

    public static void validateNeighborhood(String neighborhood) {
        if (!hasText(neighborhood)) {
            return;
        }
        String trimmed = neighborhood.trim();
        if (trimmed.length() < 2 || trimmed.length() > 80) {
            throw new IllegalArgumentException("El barrio o referencia debe tener entre 2 y 80 caracteres.");
        }
        rejectHtmlTags(trimmed, "El barrio o referencia");
    }

    public static void validateCity(String city, boolean required) {
        if (!hasText(city)) {
            if (required) {
                throw new IllegalArgumentException("El departamento o localidad es obligatorio.");
            }
            return;
        }
        if (!ALLOWED_CITIES.contains(city.trim().toLowerCase())) {
            throw new IllegalArgumentException(
                    "Seleccioná un departamento válido de nuestra zona de cobertura (Mendoza Capital, Godoy Cruz, Guaymallén, Maipú o Las Heras)."
            );
        }
    }

    public static void validatePersonPartName(String name, String fieldLabel) {
        if (!hasText(name)) {
            throw new IllegalArgumentException(fieldLabel + " es obligatorio.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 60) {
            throw new IllegalArgumentException(fieldLabel + " debe tener entre 2 y 60 caracteres.");
        }
        rejectHtmlTags(trimmed, fieldLabel);
        if (!PERSON_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(fieldLabel + " solo puede contener letras y espacios.");
        }
    }

    public static void validateRecipientName(String name, String fieldLabel, boolean required) {
        if (!hasText(name)) {
            if (required) {
                throw new IllegalArgumentException(fieldLabel + " es obligatorio.");
            }
            return;
        }
        String trimmed = name.trim();
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldLabel + " debe tener entre 3 y 100 caracteres.");
        }
        rejectHtmlTags(trimmed, fieldLabel);
        if (!PERSON_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(fieldLabel + " solo puede contener letras, espacios y paréntesis.");
        }
    }

    public static void validatePhone(String phone, boolean required) {
        if (!hasText(phone)) {
            if (required) {
                throw new IllegalArgumentException("El teléfono / WhatsApp de contacto es obligatorio.");
            }
            return;
        }
        String trimmed = phone.trim();
        if (trimmed.length() > 30 || !PHONE_CHARS_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El teléfono / WhatsApp contiene caracteres inválidos (solo números, +, - y espacios).");
        }
        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            throw new IllegalArgumentException("El teléfono / WhatsApp debe tener entre 7 y 15 dígitos numéricos (ej: 261-555-0000).");
        }
    }

    public static void validateEmail(String email, boolean required) {
        if (!hasText(email)) {
            if (required) {
                throw new IllegalArgumentException("El correo electrónico es obligatorio.");
            }
            return;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 120 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El formato del correo electrónico no es válido (ej: cliente@kiosco.com).");
        }
    }

    public static void validateTaxIdOrDni(String taxIdOrDni, boolean required) {
        if (!hasText(taxIdOrDni)) {
            if (required) {
                throw new IllegalArgumentException("El CUIT o DNI es obligatorio.");
            }
            return;
        }
        String trimmed = taxIdOrDni.trim();
        if (trimmed.startsWith("CLI-") || trimmed.startsWith("WEB-") || trimmed.startsWith("SOC-")) {
            return;
        }
        if (!trimmed.matches("^[0-9.\\-\\s]+$")) {
            throw new IllegalArgumentException("El CUIT o DNI solo puede contener números, puntos o guiones.");
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() == 7 || digits.length() == 8) {
            return;
        }
        if (digits.length() == 11) {
            String prefix = digits.substring(0, 2);
            if (!VALID_CUIT_PREFIXES.contains(prefix)) {
                throw new IllegalArgumentException("El prefijo del CUIT/CUIL no es válido (debe comenzar con 20, 23, 24, 27, 30, 33 o 34).");
            }
            return;
        }
        throw new IllegalArgumentException(
                "El CUIT o DNI no es válido. Ingresá un DNI de 7 u 8 dígitos o un CUIT de 11 dígitos (ej: 20-34567890-1)."
        );
    }

    public static void validateNotes(String notes) {
        if (!hasText(notes)) {
            return;
        }
        String trimmed = notes.trim();
        if (trimmed.length() > 250) {
            throw new IllegalArgumentException("Las notas de entrega no pueden superar los 250 caracteres.");
        }
        rejectHtmlTags(trimmed, "Las notas de entrega");
    }

    private static void rejectHtmlTags(String value, String fieldLabel) {
        if (value.contains("<") || value.contains(">")) {
            throw new IllegalArgumentException(fieldLabel + " contiene caracteres no permitidos.");
        }
    }

    private static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}

