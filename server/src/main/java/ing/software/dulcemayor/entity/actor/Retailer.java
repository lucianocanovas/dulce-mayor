package ing.software.dulcemayor.entity.actor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "retailers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Retailer extends Person {

    @Column(name = "phone", length = 60)
    private String phone;
}
