package ing.software.dulcemayor.entity.billing;

import ing.software.dulcemayor.entity.base.BaseEntity;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true)
    private Long invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "total", nullable = false)
    private double total;

    @Column(name = "net_amount")
    private Double netAmount;

    @Column(name = "vat_amount")
    private Double vatAmount;

    @Column(name = "invoice_type", length = 10)
    private String invoiceType;

    @Column(name = "afip_code", length = 10)
    private String afipCode;

    @Column(name = "tax_condition_receiver", length = 100)
    private String taxConditionReceiver;

    @Column(name = "cae_number", length = 30)
    private String caeNumber;

    @Column(name = "cae_expiration_date")
    private java.time.LocalDate caeExpirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvoiceStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceDetail> invoiceDetails = new ArrayList<>();
}
