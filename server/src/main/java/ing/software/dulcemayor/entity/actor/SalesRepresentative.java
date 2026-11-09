package ing.software.dulcemayor.entity.actor;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sales_representatives")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SalesRepresentative extends Employee {
}
