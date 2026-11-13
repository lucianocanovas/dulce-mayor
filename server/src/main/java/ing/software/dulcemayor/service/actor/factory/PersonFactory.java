package ing.software.dulcemayor.service.actor.factory;

import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Person;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.DeliveryAgentRepository;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creational Pattern (Factory Method / Simple Factory for Actor Hierarchy).
 * Encapsulates polymorphic instantiation and persistence of Person subclasses
 * (SalesRepresentative, DeliveryAgent, Retailer, Employee) based on UserRole,
 * eliminating scattered role-based conditionals across UserService and AuthService.
 */
@Component
@RequiredArgsConstructor
public class PersonFactory {

    private final EmployeeRepository employeeRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final RetailerRepository retailerRepository;

    public Person createAndSaveStaffByRole(
            UserRole role,
            String firstName,
            String lastName,
            String documentNumber,
            String employeeIdNumber) {

        String cleanFirst = firstName != null ? firstName.trim() : "";
        String cleanLast = lastName != null ? lastName.trim() : "";
        String cleanDoc = documentNumber != null ? documentNumber.trim() : "";

        if (role == UserRole.PRESALE_AGENT) {
            return salesRepRepository.save(SalesRepresentative.builder()
                    .firstName(cleanFirst)
                    .lastName(cleanLast)
                    .documentType(DocumentType.DNI)
                    .documentNumber(cleanDoc)
                    .employeeIdNumber(employeeIdNumber)
                    .build());
        }

        if (role == UserRole.DELIVERY_AGENT) {
            return deliveryAgentRepository.save(DeliveryAgent.builder()
                    .firstName(cleanFirst)
                    .lastName(cleanLast)
                    .documentType(DocumentType.DNI)
                    .documentNumber(cleanDoc)
                    .employeeIdNumber(employeeIdNumber)
                    .build());
        }

        return employeeRepository.save(Employee.builder()
                .firstName(cleanFirst)
                .lastName(cleanLast)
                .documentType(DocumentType.DNI)
                .documentNumber(cleanDoc)
                .employeeIdNumber(employeeIdNumber)
                .build());
    }

    public Retailer createAndSaveRetailer(
            String firstName,
            String lastName,
            String documentNumber,
            String phone) {

        return retailerRepository.save(Retailer.builder()
                .firstName(firstName != null ? firstName.trim() : "")
                .lastName(lastName != null ? lastName.trim() : "")
                .documentType(DocumentType.DNI)
                .documentNumber(documentNumber != null ? documentNumber.trim() : "00000000")
                .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                .build());
    }
}
