package net.lctafrica.membership.api.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate


enum class BenefitAccessMode {
    CARD, CARDLESS, HYBRID
}

@Entity
@Table(name = "payer")
class Payer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payer_id")
    var id: Long = 0,

    @Column(name = "payer_name", unique = true, nullable = false)
    var name: String,

    @Column(name = "contact")
    var contact: String,

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: PayerType,

    /*@JsonIgnore
    @OneToMany(mappedBy = "payer")
    var plans: List<Plan>,*/

    @JsonIgnore
    @OneToMany(mappedBy = "payer")
    var benefits: List<Benefit> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "payer")
    var benefitMapping: Set<PayerBenefitMapping> = mutableSetOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "payer")
    var providerMapping: Set<PayerProviderMapping> = mutableSetOf()

)

enum class PayerType(val description: String) {
    UNDERWRITER("Insurance service provider"),
    CORPORATE("Self-managing entity"),
    INTERMEDIARY("Broker, Bancassurance or Agency")
}

@Entity
@Table(name = "plan")//, uniqueConstraints = [UniqueConstraint(columnNames = ["plan_name", "payer_id"])])
data class Plan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    var id: Long = 0,

    @Column(name = "plan_name", nullable = false)
    var name: String,

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: PlanType,

    /*@ManyToOne
    @JoinColumn(name = "payer_id", nullable = false)
    val payer: Payer,*/

    @JsonIgnore
    @OneToMany(mappedBy = "plan")
    var policies: List<Policy>,

    @Column(name = "benefit_access_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    var accessMode: BenefitAccessMode
)

enum class PlanType {
    SCHEME, RETAIL
}

@Entity
@Table(name = "policy")
data class Policy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    var id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: Plan,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @JsonIgnore
    @OneToMany(mappedBy = "policy")
    var categories: List<Category> = mutableListOf(),

    @Column(name = "policy_number", unique = true, nullable = false)
    var policyNumber: String,

    @JsonIgnore
    @OneToMany(mappedBy = "policy")
    var cardBatches: List<CardBatch> = mutableListOf(),
) {
    override fun toString(): String {
        return "Policy(id=$id, startDate=$startDate, endDate=$endDate)"
    }

    init {
        require(endDate.isAfter(startDate)) { "Policy start date must come before end date" }
    }
}

@Entity
@Table(name = "category", uniqueConstraints = [UniqueConstraint(columnNames = ["policy_id", "category_name"])])
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    var id: Long = 0,

    @Column(name = "category_name", nullable = false)
    val name: String,

    @Column(name = "description")
    var description: String?,

    @Column(name = "agakhan_insurance_code")
    var agakhanInsuranceCode: String? = "0",

    @Column(name = "agakhan_scheme_code")
    var agakhanSchemeCode: String? = "0",

    @Column(name = "jic_scheme_code")
    var jicSchemeCode: Int?,

    @Column(name = "apa_scheme_code")
    var apaSchemeCode: Int?,

    @Column(name = "policy_payer_code")
    var policyPayerCode: Long?,

    @ManyToOne
    @JoinColumn(name = "policy_id", nullable = false)
    var policy: Policy,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: CategoryStatus = CategoryStatus.UNPROCESSED,

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    var benefits: List<Benefit> = mutableListOf(),


    @JsonIgnore
    @OneToMany(mappedBy = "category")
    var beneficiaries: List<Beneficiary> = mutableListOf(),

    @Column(columnDefinition="tinyint(1) default 1")
    var allowOtpVerificationFailOver: Boolean = true
) {
    override fun toString(): String {
        return "Category(id=$id, name=$name, description=$description)"
    }

    init {
        fun checkNameNotBlank() = name.trim().isNotEmpty()
        require(checkNameNotBlank()) {
            "Category name cannot be blank"
        }
    }
}

enum class CategoryStatus {
    PROCESSED, UNPROCESSED
}

@Entity
@Table(
    name = "benefit", uniqueConstraints = [
        UniqueConstraint(name = "UNQ_NAME_PER_CATEGORY", columnNames = ["benefit_name", "category_id"]),
        UniqueConstraint(name = "UNQ_REF_PER_CATEGORY", columnNames = ["benefit_ref", "category_id"])]
)
class Benefit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_id")
    var id: Long = 0,

    @Column(name = "benefit_name", nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "benefit_ref")
    var benefitRef: BenefitCatalog,

    @Column(name = "applicable_gender", nullable = false)
    @Enumerated(EnumType.STRING)
    var applicableGender: ApplicableGender,

    @Column(name = "applicable_member", nullable = false)
    @Enumerated(EnumType.STRING)
    var applicableMember: ApplicableMember,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Column(name = "benefit_limit", nullable = false)
    var limit: BigDecimal,

    @Column(name = "suspension_threshold")
    var suspensionThreshold: BigDecimal,

    /*@Column(name = "requires_pre_auth", nullable = false)
    var requiresPreAuth: Boolean,*/

    @Column(name = "pre_auth_type", nullable = true)
    @Enumerated(EnumType.STRING)
    var preAuthType: PreAuthType,

    @Column(name = "sharing", nullable = false)
    @Enumerated(EnumType.STRING)
    var sharing: BenefitDistribution,

    @Column(name = "co_payment_required")
    var coPaymentRequired: Boolean,

    @Column(name = "co_payment_amount")
    var coPaymentAmount: BigDecimal,

    @JsonIgnore
    @OneToMany(mappedBy = "parentBenefit")
    var subBenefits: Set<Benefit>,

    @ManyToOne
    @JoinColumn(name = "parent_benefit_id", nullable = true)
    var parentBenefit: Benefit?,

    @Column(name = "waiting_period")
    @Enumerated(EnumType.STRING)
    var waitingPeriod: WaitingPeriod,

    @Column(name = "processed")
    var processed: Boolean,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @Column(name = "processed_time")
    var processedTime: LocalDateTime?,

    @ManyToOne
    @JoinColumn(name = "payer_id")
    var payer: Payer,
) {
    init {
        fun checkCoPay() = ((coPaymentRequired && coPaymentAmount > BigDecimal.ZERO) or
                (!coPaymentRequired && coPaymentAmount <= BigDecimal.ZERO))

        //fun checkPreAuth() = (!requiresPreAuth.or(requiresPreAuth && (preAuthType is PreAuthType)))
        fun checkPreAuth(): Boolean {
            val pb = this.parentBenefit
            if (pb != null) {
                val pbpre = pb.preAuthType
                return (pbpre == this.preAuthType).or(pbpre == PreAuthType.NONE)
            }
            return true
        }

        fun memberAlignment(): Boolean {
            val pb = this.parentBenefit
            if (pb != null) {
                val pbam = pb.applicableMember
                if (pbam == ApplicableMember.ALL || pbam == this.applicableMember) {
                    return true
                } else {
                    if (pbam == ApplicableMember.PRINCIPAL_AND_SPOUSE &&
                        (this.applicableMember == ApplicableMember.SPOUSE || this.applicableMember == ApplicableMember.PRINCIPAL)
                    ) return true
                }
                return false
            }
            return true
        }

        fun genderAlignment(): Boolean {
            val pb = this.parentBenefit
            if (pb != null) {
                val pbag = pb.applicableGender
                if (pbag == ApplicableGender.ALL || pbag == this.applicableGender) return true
                return false
            }
            return true
        }

        fun distributionAlignment(): Boolean {
            val pb = this.parentBenefit
            if (pb != null) {
                val pbd = pb.sharing
                if (pbd == BenefitDistribution.FAMILY || (pbd == this.sharing)) return true
                return false
            }
            return true
        }

        fun limitAlignment(): Boolean {
            val pb = this.parentBenefit
            if ((pb != null) && (pb.limit < this.limit)) return false
            return true
        }

        fun suspensionLowerThanBalance() = (limit >= suspensionThreshold)

        fun checkPayer(): Boolean {
            val pb = this.parentBenefit
            if (pb != null && (pb.payer.id != this.payer.id)) return false
            return true
        }

        require(checkCoPay()) { "Co-payment is required, therefore co-payment amount must be greater than Zero" }
        require(checkPreAuth()) { "Pre-authorization type for main and sub-benefit are in conflict" }
        require(memberAlignment()) { "Applicable member for main and sub-benefit are in conflict" }
        require(genderAlignment()) { "Applicable gender for main and sub-benefit are in conflict" }
        require(distributionAlignment()) { "Benefit sharing for main and sub-benefit are in conflict" }
        require(limitAlignment()) { "Main benefit limit should be greater than sub-benefit limit" }
        require(suspensionLowerThanBalance()) { "Suspension threshold should not be greater than benefit limit" }
        require(checkPayer()) { "Main benefit payer should be the same as sub-benefit payer" }
    }
}

enum class WaitingPeriod(val period: Period) {
    ZERO_DAYS(Period.ofDays(0)),
    THIRTY_DAYS(Period.ofDays(30)),
    SIXTY_DAYS(Period.ofDays(60)),
    NINETY_DAYS(Period.ofDays(90)),
    ONE_HUNDRED_EIGHTY_DAYS(Period.ofDays(180)),
    NINE_MONTHS(Period.ofMonths(9))
}

enum class BenefitDistribution {
    FAMILY, INDIVIDUAL
}

enum class PreAuthType(val description: String) {
    HR("HR representative must give the pre-authorization"),
    PAYER("The Underwriter must give the pre-authorization"),
    NONE("No Pre-authorization needed")
}

enum class ApplicableGender {
    MALE, FEMALE, ALL
}

enum class Gender {
    MALE, FEMALE
}

enum class BeneficiaryType {
    PRINCIPAL,
    SPOUSE,
    CHILD,
    PARENT
}

enum class MemberStatus {
    ACTIVE,
    INACTIVE
}

enum class ApplicableMember(val description: String) {
    PRINCIPAL("Applies to principal only"),
    SPOUSE("Applies to spouse only"),
    CHILD("Applies to child only"),
    PRINCIPAL_AND_SPOUSE("Applies to principal and spouse only"),
    PARENT("Applies to parent only"),
    ALL("Applies to all members")
}

@Entity
@Table(
    name = "beneficiary",
    indexes = [Index(columnList = "phone_number")],
    uniqueConstraints = [
        UniqueConstraint(name = "member_number_UNQ", columnNames = ["member_number", "category_id"])
    ]
)
data class Beneficiary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiary_id")
    var id: Long = 0,

    @Column(name = "beneficiary_name", nullable = false)
    var name: String,

    @Column(name = "member_number", nullable = false)
    var memberNumber: String,

    @Column(name = "jic_entity_id", nullable = true)
    var jicEntityId: Int?,

    @Column(name = "apa_entity_id", nullable = true)
    var apaEntityId: Int?,

    @Column(name = "nhif_number", nullable = true)
    var nhifNumber: String?,

    @Column(name = "dob", columnDefinition = "DATE", nullable = false)
    var dob: LocalDate,

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Column(name = "phone_number")
    var phoneNumber: String?,

    @Column(name = "email")
    var email: String?,

    @Column(name = "beneficiary_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var beneficiaryType: BeneficiaryType,

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @JsonIgnore
    @OneToMany(mappedBy = "principal")
    var dependants: List<Beneficiary> = mutableListOf(),

    @ManyToOne
    @JoinColumn(name = "principal_id", nullable = true)
    var principal: Beneficiary?,

    @JoinColumn(name = "can_use_biometrics", nullable = true)
    var canUseBiometrics: Boolean? = null,

    @Column(name = "processed")
    var processed: Boolean,

    @OneToMany(mappedBy = "beneficiaryId", fetch = FetchType.EAGER)
    var changeLog: List<AuditLog>? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @Column(name = "processed_time")
    var processedTime: LocalDateTime?,

    @JsonIgnore
    @OneToMany(mappedBy = "beneficiary")
    var cards: List<BeneficiaryCard> = mutableListOf(),

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: MemberStatus? = MemberStatus.ACTIVE,

    ) {
    override fun toString(): String {
        return "Beneficiary(id=$id, name=$name, memberNumber=$memberNumber)"
    }

    init {

        fun checkPrincipal(): Boolean {
            if (beneficiaryType == BeneficiaryType.PRINCIPAL && principal != null) return false
            if (beneficiaryType != BeneficiaryType.PRINCIPAL && principal == null) return false
            return true
        }

        fun checkNoFutureBirthDay(): Boolean {
            if (dob.isAfter(LocalDate.now())) return false
            return true
        }

        require(checkPrincipal()) {
            "Dependency is incorrectly mapped for $memberNumber. " +
                    "Check that you have included a principal if this is a dependant" +
                    " or no other member has been designated as principal if this is a principal member"
        }

        require(checkNoFutureBirthDay()) { "Date of birth cannot be in the future" }
    }


}

@Entity
@Table(
    name = "benefit_tracker", uniqueConstraints = [
        UniqueConstraint(columnNames = ["beneficiary_id", "benefit_id"], name = "Benefit_Beneficiary_UNQ"),
        UniqueConstraint(columnNames = ["aggregateId"], name = "BenefitAggregateUNQ")]
)
data class SharedBenefitTracker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_tracker_id")
    var id: Long = 0,

    @ManyToOne()
    @JoinColumn(name = "beneficiary_id")
    var beneficiary: Beneficiary,

    @ManyToOne()
    @JoinColumn(name = "benefit_id")
    var benefit: Benefit,

    @Column(name = "aggregateId")
    var aggregateId: String
)


@Entity
@Table(
    name = "benefit_catalog", uniqueConstraints = [
        UniqueConstraint(columnNames = ["service_name"], name = "Service_Name_UNQ"),
        UniqueConstraint(columnNames = ["service_code"], name = "Service_Code_UNQ")
    ]
)
data class BenefitCatalog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    var id: Long?,

    @Column(name = "service_code")
    var code: String,

    @Column(name = "service_name")
    var name: String,

    @Column(name = "service_group")
    @Enumerated(EnumType.STRING)
    var serviceGroup: ServiceGroup,

    @JsonIgnore
    @OneToMany(mappedBy = "benefit")
    var whitelist: Set<Whitelist> = mutableSetOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "benefit")
    var payerMapping: Set<PayerBenefitMapping> = mutableSetOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "benefitRef")
    var benefits: List<Benefit> = mutableListOf()
)

enum class ServiceGroup {
    OUTPATIENT, INPATIENT, DENTAL, OPTICAL, COVID, MATERNITY
}

@Entity
@Table(
    name = "provider", uniqueConstraints = [
        UniqueConstraint(columnNames = ["provider_name"], name = "Service_Name_UNQ")
    ]
)
data class Provider(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    var id: Long? = 0,

    @Column(name = "provider_name")
    var name: String? = null,

    @Column(name = "latitude")
    var latitude: Double? = 0.00,

    @Column(name = "longitude")
    var longitude: Double? = 0.00,

    @Enumerated(EnumType.STRING)
    var tier: ProviderTier? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "mainFacility")
    var branches: List<Provider> = mutableListOf(),

    @ManyToOne
    @JoinColumn(name = "main_facility_id")
    var mainFacility: Provider? = null,

    @JsonIgnore
    @OneToMany
    var whitelist: Set<Whitelist> = mutableSetOf(),

    @ManyToOne
    @JoinColumn(name = "region_id")
    var region: Region? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "provider")
    var payerMapping: Set<PayerProviderMapping> = mutableSetOf(),

    var baseUrl: String? = null,

    var billingStation: Boolean = false,
    var billsOnPortal: Boolean = true,
    var billsOnHmis: Boolean = false,
    var billsOnDevice: Boolean = false,
    var billsOnHmisAutomaticClose: Boolean? = false,
    @Column(columnDefinition="tinyint(1) default 1")
    var canUseOtpVerificationFailOver: Boolean = true,

    @Column(name = "verification_type")
    @Enumerated(EnumType.STRING)
    var verificationType: VerificationType? = null,

    @Column(name = "invoice_number_type")
    @Enumerated(EnumType.STRING)
    var invoiceNumberType: InvoiceNumberType? = null,

    @Column(name = "provider_middleware")
    @Enumerated(EnumType.STRING)
    val providerMiddleware: MIDDLEWARENAME? = null,

    var createdOn: LocalDateTime = LocalDateTime.now()
)

enum class ProviderTier {
    TIER_ONE, TIER_TWO, TIER_THREE
}

enum class MIDDLEWARENAME {
    AVENUE, MATER, NAIROBIHOSPITAL, GETRUDES, MPSHAH, METROPOLITAN, AGAKHANKISUMU,
    AGAKHANMOMBASA, AGAKHANNAIROBI, AGAKHANNAIROBITEST, NONE, AKUH, GETRUDESTEST
}

enum class VerificationType {
    BIOMETRIC, OTP
}

enum class BillingStationType {
    MULTIPLE, SINGLE
}

enum class InvoiceNumberType {
    SAME, VARIED
}

@Entity
@Table(
    name = "provider_exclusion",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["category_id", "provider_id"], name = "Category_Exclusion_UNQ")
    ]
)
data class ProviderExclusion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exclusion_id")
    var id: Long?,

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider,

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: Status,

    @Column(name = "processed")
    var processed: Boolean = false,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @Column(name = "processed_time")
    var processedTime: LocalDateTime? = null
)

enum class Status {
    ACTIVE, INACTIVE
}

@Entity
@Table(
    name = "copayment", uniqueConstraints = [
        UniqueConstraint(columnNames = ["category_id", "provider_id"], name = "Copayment_UNQ")
    ]
)
data class Copayment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "copayment_id")
    var id: Long?,

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider,

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Column(name = "amount", nullable = false)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: Status,

    @Column(name = "processed")
    var processed: Boolean,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @Column(name = "processed_time")
    var processedTime: LocalDateTime?
)

@Entity
@Table(
    name = "service_whitelist"
)
data class Whitelist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "whitelist_id")
    var id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "benefit_catalog_id", nullable = false)
    var benefit: BenefitCatalog,

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider
)


@Entity
@Table(name = "country")
data class Country(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    var id: Long = 0,

    @Column(name = "country_name", unique = true, nullable = false)
    var name: String,

    @JsonIgnore
    @OneToMany(mappedBy = "country")
    var regions: Set<Region> = mutableSetOf()
)

@Entity
@Table(
    name = "region",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["region_name", "country_id"], name = "region_UNQ")
    ]
)
data class Region(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    var id: Long = 0,

    @Column(name = "region_name", nullable = false)
    var name: String,

    @ManyToOne
    @JoinColumn(name = "country_id")
    var country: Country,

    @JsonIgnore
    @OneToMany(mappedBy = "region")
    var providers: Set<Provider> = mutableSetOf()

)

@Entity
@Table(
    name = "payer_benefit_mapping",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["payerId", "benefitId"], name = "payer_benefit_mapping_UNQ")
    ]
)
data class PayerBenefitMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payer_benefit_mapping_id")
    var id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "benefitId")
    var benefit: BenefitCatalog,

    @ManyToOne
    @JoinColumn(name = "payerId")
    var payer: Payer,

    @Column(name = "code", nullable = false)
    var code: String
)

@Entity
@Table(
    name = "payer_provider_mapping",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["payerId", "providerId"], name = "payer_provider_mapping_UNQ")
    ]
)
data class PayerProviderMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payer_provider_mapping_id")
    var id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "payerId")
    var payer: Payer? = null,

    @ManyToOne
    @JoinColumn(name = "providerId")
    var provider: Provider? = null,

    @Column(name = "code")
    var code: String? = null
)

@Entity
@Table(
    name = "card_batch"
)
data class CardBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    val id: Long = 0,

    @CreatedDate
    @Column(name = "created_date", nullable = false)
    var createdAt: LocalDate,

    @LastModifiedDate
    @Column(name = "modified_at")
    var modifiedAt: LocalDateTime? = null,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: CardStatus = CardStatus.REQUESTED,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    var type: CardRequestType,

    @ManyToOne
    @JoinColumn
    var policy: Policy,

    @JsonIgnore
    @OneToMany(mappedBy = "batch")
    var beneficiaryCards: List<BeneficiaryCard> = mutableListOf(),

    @Column(name = "payment_ref")
    var paymentRef: String? = null
) {
    private fun checkReprintHasPayment(): Boolean {
        if (type == CardRequestType.REPRINT && paymentRef.isNullOrEmpty()) return false
        return true
    }

    init {
        require(checkReprintHasPayment()) { "Reprint requests must have a payment" }
    }
}

enum class CardStatus {
    REQUESTED, PRINTED, ISSUED
}

enum class ChangeLogType {
    CATEGORY_UPDATE, MEMBER_UPDATE, MEMBERSTATUS_UPDATE, BIOMETRICS_UPDATE, BENEFIT_UPDATE,
    PREAUTH_UPDATE, NONE
}

enum class CardRequestType {
    NEW, REPRINT
}

@Entity
@Table(
    name = "beneficiary_card"
)
data class BeneficiaryCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    val id: Long = 0,

    @ManyToOne
    var beneficiary: Beneficiary,

    @ManyToOne
    var batch: CardBatch
)


@Entity
@Table(
    name = "beneficiary_link",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["beneficiary_id", "phone_number"], name = "link_UNQ")
    ]
)
data class BeneficiaryLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    var linkId: Long? = 0,

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "phone_number")
    var phoneNumber: String,

    @NotNull
    var status: Boolean,

    @JsonIgnore
    @JoinColumn(name = "beneficiary_id", referencedColumnName = "beneficiary_id")
    @ManyToOne
    var beneficiary: Beneficiary,

    @NotNull
    @Column(name = "linked_on")
    var linkedOn: LocalDateTime = LocalDateTime.now(),

    @NotNull
    @Column(name = "updated_on")
    var updatedOn: LocalDateTime? = null
)


@Entity
@Table(name = "audit_log")
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0,

    @Column(name = "action")
    var action: String,

    @Column(name = "user")
    var user: String,

    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "time", updatable = false, nullable = true)
    val time: LocalDate? = null,

    @Column(name = "data", columnDefinition = "LONGTEXT")
    var data: String?,

    @Column(name = "organisation")
    var organisation: String?,

    @Column(name = "reason")
    var reason: String,

    @Column(name = "member_number")
    var memberNumber: String,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    var type: ChangeLogType = ChangeLogType.NONE,

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "beneficiary_id", nullable = true)
    val beneficiaryId: Beneficiary? = null,
)

@Entity
@Table(name = "device_catalog")
data class DeviceCatalog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_catalog_id", nullable = false)
    var id: Long = 0,

    @Column(name = "device_id", unique = true, nullable = false)
    var deviceId: String,

    @Column(name = "first_imei", unique = true)
    var firstImei: String? = null,

    @Column(name = "second_imei", unique = true)
    var secondImei: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: Status,

    @Column(name = "note")
    var note: String? = null,

    @Column(name = "registered_on")
    var registeredOn: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "device_allocation")
data class DeviceAllocation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id", nullable = false)
    var id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "device_catalog_id", nullable = false)
    var deviceCatalog: DeviceCatalog,

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: Provider,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: AllocationStatus,

    @Column(name = "note")
    var note: String? = null,

    @Column(name = "date_allocated")
    var dateAllocated: LocalDateTime = LocalDateTime.now(),

    @Column(name = "date_deallocated")
    var dateDeallocated: LocalDateTime? = null
)

enum class AllocationStatus {
    ALLOCATED, DEALLOCATED
}