package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de clasificaciones de edad permitidas en DM_AGE_RATINGS. */
@Entity
@Table(name = "DM_AGE_RATINGS")
public class AgeRatingEntity {
    @Id
    @Column(name = "AGEVALUE", nullable = false)
    private Integer ageValue;

    @Column(name = "LABEL", nullable = false, length = 60)
    private String label;

    protected AgeRatingEntity() {}

    public AgeRatingEntity(Integer ageValue, String label) {
        this.ageValue = ageValue;
        this.label = label;
    }

    public Integer getAgeValue() {
        return ageValue;
    }

    public String getLabel() {
        return label;
    }
}
