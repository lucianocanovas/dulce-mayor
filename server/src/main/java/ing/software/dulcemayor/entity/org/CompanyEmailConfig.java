package ing.software.dulcemayor.entity.org;

import ing.software.dulcemayor.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "company_email_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompanyEmailConfig extends BaseEntity {

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "port", length = 10)
    private String port;

    @Column(name = "smtp_host", length = 150)
    private String smtpHost;

    @Column(name = "tls", nullable = false)
    private boolean tls;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesaler_id")
    private Wholesaler company;
}
