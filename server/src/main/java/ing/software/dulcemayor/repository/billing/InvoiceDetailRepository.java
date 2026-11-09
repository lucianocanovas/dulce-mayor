package ing.software.dulcemayor.repository.billing;

import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDetailRepository extends BaseRepository<InvoiceDetail> {
    List<InvoiceDetail> findByInvoiceIdAndIsDeletedFalse(String invoiceId);
}
