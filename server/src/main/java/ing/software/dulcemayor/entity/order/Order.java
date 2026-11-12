package ing.software.dulcemayor.entity.order;

import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.base.BaseEntity;
import ing.software.dulcemayor.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private Long orderNumber;

    @Column(name = "order_date_time", nullable = false)
    private LocalDateTime orderDateTime;

    @Column(name = "subtotal", nullable = false)
    private double subtotal;

    @Builder.Default
    @Column(name = "discount", nullable = false)
    private double discount = 0.0;

    @Column(name = "total", nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 50)
    private ing.software.dulcemayor.enums.PaymentType paymentType;

    @Column(name = "source_channel", length = 50)
    private String sourceChannel;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", length = 60)
    private String customerPhone;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "delivery_neighborhood", length = 120)
    private String deliveryNeighborhood;

    @Column(name = "delivery_city", length = 120)
    private String deliveryCity;

    @Column(name = "mp_preference_id", length = 150)
    private String mpPreferenceId;

    @Column(name = "mp_init_point", length = 1000)
    private String mpInitPoint;

    @Column(name = "mp_payment_id", length = 100)
    private String mpPaymentId;

    @Column(name = "mp_payment_status", length = 60)
    private String mpPaymentStatus;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    @Builder.Default
    @Column(name = "stock_deducted", nullable = false)
    private boolean stockDeducted = false;

    @Builder.Default
    @Column(name = "ready_for_dispatch", nullable = false)
    private boolean readyForDispatch = false;

    @Column(name = "dispatch_enabled_at")
    private java.time.LocalDateTime dispatchEnabledAt;

    @Column(name = "payment_note", length = 500)
    private String paymentNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_route_id")
    private ing.software.dulcemayor.entity.logistics.DeliveryRoute deliveryRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_of_sale_id", nullable = false)
    private PointOfSale pointOfSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id")
    private SalesRepresentative salesRep;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();
}
