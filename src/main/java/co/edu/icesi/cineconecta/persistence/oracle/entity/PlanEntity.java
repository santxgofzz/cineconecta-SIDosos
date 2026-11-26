package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Plan de suscripción comercializado en PLANS. */
@Entity
@Table(name = "PLANS")
public class PlanEntity {
    @Id
    @Column(name = "PLANID", nullable = false)
    private Long planId;

    @Column(name = "PRICE", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "MAXPROFILES", nullable = false)
    private Integer maxProfiles;

    @Column(name = "NAME", nullable = false, length = 60, unique = true)
    private String name;

    protected PlanEntity() {}

    public PlanEntity(Long planId, BigDecimal price, Integer maxProfiles, String name) {
        this.planId = planId;
        this.price = price;
        this.maxProfiles = maxProfiles;
        this.name = name;
    }

    public Long getPlanId() {
        return planId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getMaxProfiles() {
        return maxProfiles;
    }

    public String getName() {
        return name;
    }
}
