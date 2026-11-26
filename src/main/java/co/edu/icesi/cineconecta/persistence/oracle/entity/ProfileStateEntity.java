package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Catálogo de estados de perfil en DM_PROFILE_STATES ('A' activo, 'I' inactivo). */
@Entity
@Table(name = "DM_PROFILE_STATES")
public class ProfileStateEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "STATECODE", nullable = false, length = 1)
    private String stateCode;

    @Column(name = "DESCRIPTION", nullable = false, length = 60)
    private String description;

    protected ProfileStateEntity() {}

    public ProfileStateEntity(String stateCode, String description) {
        this.stateCode = stateCode;
        this.description = description;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getDescription() {
        return description;
    }
}
