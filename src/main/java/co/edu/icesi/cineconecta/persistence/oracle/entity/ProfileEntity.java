package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Perfil de visualización asociado a una cuenta en PROFILES. */
@Entity
@Table(name = "PROFILES")
public class ProfileEntity {
    public static final String ACTIVE = "A";
    public static final String INACTIVE = "I";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_profiles")
    @SequenceGenerator(name = "seq_profiles", sequenceName = "SEQ_PROFILES", allocationSize = 1)
    @Column(name = "PROFILEID", nullable = false)
    private Long profileId;

    @Column(name = "NAME", nullable = false, length = 60)
    private String name;

    @Column(name = "MAXAGERATING", nullable = false)
    private Integer maxAgeRating;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ISACTIVE", nullable = false, length = 1)
    private String isActive;

    @Column(name = "ACCOUNTS_ACCOUNTID", nullable = false)
    private Long accountId;

    @Column(name = "DM_AGE_RATINGS_AGEVALUE", nullable = false)
    private Integer ageRatingValue;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "DM_PROFILE_STATES_STATECODE", nullable = false, length = 1)
    private String stateCode;

    protected ProfileEntity() {}

    public ProfileEntity(Long profileId, String name, Integer maxAgeRating, String isActive, Long accountId) {
        this.profileId = profileId;
        this.name = name;
        this.maxAgeRating = maxAgeRating;
        this.ageRatingValue = maxAgeRating;
        this.isActive = isActive;
        this.stateCode = isActive;
        this.accountId = accountId;
    }

    public ProfileEntity(String name, Integer maxAgeRating, Long accountId) {
        this(null, name, maxAgeRating, ACTIVE, accountId);
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxAgeRating() {
        return maxAgeRating;
    }

    public String getIsActive() {
        return isActive;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Integer getAgeRatingValue() {
        return ageRatingValue;
    }

    public String getStateCode() {
        return stateCode;
    }

    public boolean isActiveState() {
        return ACTIVE.equalsIgnoreCase(isActive);
    }

    public void updateDetails(String name, Integer maxAgeRating) {
        this.name = name;
        this.maxAgeRating = maxAgeRating;
        this.ageRatingValue = maxAgeRating;
    }

    public void markActive() {
        this.isActive = ACTIVE;
        this.stateCode = ACTIVE;
    }

    public void markInactive() {
        this.isActive = INACTIVE;
        this.stateCode = INACTIVE;
    }
}
