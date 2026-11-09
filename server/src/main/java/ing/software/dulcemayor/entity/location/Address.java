package ing.software.dulcemayor.entity.location;

import ing.software.dulcemayor.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Address extends BaseEntity {

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "street_number", length = 50)
    private String streetNumber;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "block_or_floor", length = 50)
    private String blockOrFloor;

    @Column(name = "apartment_unit", length = 50)
    private String apartmentUnit;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
}
