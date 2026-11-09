package ing.software.dulcemayor.entity.actor;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "delivery_agents")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DeliveryAgent extends Employee {
}
