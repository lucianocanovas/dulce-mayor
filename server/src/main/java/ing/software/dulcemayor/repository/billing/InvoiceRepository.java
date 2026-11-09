package ing.software.dulcemayor.repository.billing;

import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends BaseRepository<Invoice> {
    Optional<Invoice> findByOrderIdAndIsDeletedFalse(String orderId);
    Optional<Invoice> findByInvoiceNumberAndIsDeletedFalse(Long invoiceNumber);
}
