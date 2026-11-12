package ing.software.dulcemayor.repository.billing;

import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends BaseRepository<Invoice> {
    Optional<Invoice> findByOrderIdAndIsDeletedFalse(String orderId);
    Optional<Invoice> findByInvoiceNumberAndIsDeletedFalse(Long invoiceNumber);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(i.invoiceNumber), 10000) FROM Invoice i")
    Long findMaxInvoiceNumber();
}
