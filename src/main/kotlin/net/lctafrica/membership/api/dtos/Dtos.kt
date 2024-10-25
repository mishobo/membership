package net.lctafrica.membership.api.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.avro4k.ScalePrecision
import com.sksamuel.avro4k.serializer.BigDecimalSerializer
import com.sksamuel.avro4k.serializer.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import kotlinx.serialization.Serializable
import net.lctafrica.membership.api.domain.ApplicableGender
import net.lctafrica.membership.api.domain.ApplicableMember
import net.lctafrica.membership.api.domain.Beneficiary
import net.lctafrica.membership.api.domain.BeneficiaryType
import net.lctafrica.membership.api.domain.BenefitAccessMode
import net.lctafrica.membership.api.domain.BenefitDistribution
import net.lctafrica.membership.api.domain.CardRequestType
import net.lctafrica.membership.api.domain.Category
import net.lctafrica.membership.api.domain.ChangeLogType
import net.lctafrica.membership.api.domain.Gender
import net.lctafrica.membership.api.domain.Payer
import net.lctafrica.membership.api.domain.PayerType
import net.lctafrica.membership.api.domain.PlanType
import net.lctafrica.membership.api.domain.PreAuthType
import net.lctafrica.membership.api.domain.ProviderTier
import net.lctafrica.membership.api.domain.ServiceGroup
import net.lctafrica.membership.api.domain.VerificationType
import net.lctafrica.membership.api.domain.WaitingPeriod

data class PayerDTO(
    @field:NotBlank(message = "Payer name cannot be blank")
    val name: String,
    @field:NotBlank(message = "Payer contact cannot be blank")
    val contact: String,
    val type: PayerType
)

data class PayerBenefitMappingDTO(
    val payerId: Long,
    val benefitCatalogId: Long,
    @field:NotBlank(message = "Benefit code cannot be blank")
    val code: String
)

data class PayerProviderMappingDTO(
    val payerId: Long,
    val providerId: Long,
    @field:NotBlank(message = "Provider code cannot be blank")
    val code: String
)

data class PlanDTO(
    //val payerId: Long,
    @field:NotBlank(message = "Name of plan cannot be blank")
    val name: String,
    val type: PlanType,
    val benefitAccessMode: BenefitAccessMode
)

data class PolicyDTO(
    val planId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @field:NotBlank(message = "Policy number cannot be blank")
    val policyNumber: String
)

data class CategoryDTO(
    val policyId: Long,
    val categories: MutableList<CatDTO>
)

data class CatDTO(
    @field:NotBlank(message = "Category name cannot be blank")
    val name: String,
    val description: String,
    val jicSchemeCode: Int?,
    val apaSchemeCode: Int?,
    val policyPayerCode: Long?
) {
}

data class BenefitDTO(
    val categoryId: Long,
    @field:NotBlank(message = "Benefit name cannot be blank")
    val name: String,
    val applicableGender: ApplicableGender,
    val applicableMember: ApplicableMember,
    val limit: BigDecimal,
    val suspensionThreshold: BigDecimal,
    val preAuthType: PreAuthType,
    val sharing: BenefitDistribution,
    val coPaymentRequired: Boolean,
    val coPaymentAmount: BigDecimal?,
    val waitingPeriod: WaitingPeriod,
    val parentBenefitId: Long?,
    val payer: Payer,
    val catalogRefId: Long,
    val payerCode: Long?
)

data class BeneficiaryDTO(
    val categoryId: Long,
    @field:NotBlank(message = "Beneficiary name cannot be blank")
    val name: String,
    @field:NotBlank(message = "Member number cannot be blank")
    val memberNumber: String,
    val nhifNumber: String?,
    val dob: LocalDate,
    val gender: Gender,
    val phoneNumber: String?,
    val email: String?,
    val beneficiaryType: BeneficiaryType,
    val principalId: Long?,
    val jicEntityId: Int?,
    val apaEntityId: Int?

)

data class NestedBenefitDTO(
    val parent: BenefitDTO,
    val children: List<BenefitDTO>
)

data class BenefitCatalogDTO(
    @field:NotBlank(message = "Benefit code cannot be blank")
    val code: String,
    @field:NotBlank(message = "Benefit name cannot be blank")
    val name: String,
    val serviceGroup: ServiceGroup
)

interface ServiceDto{
    val serviceId: Long
    val serviceGroup: String
}

data class NestedBenefitCatalogs(
    val benefits: List<BenefitCatalogDTO>
)

data class ProviderDTO(
    @field:NotBlank(message = "Provider name cannot be blank")
    val name: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val billsOnHmis:Boolean? = false,
    var billsOnPoral:Boolean? = false,
    val billsOnDevice:Boolean? = false,
    val tier: ProviderTier,
    val mainFacilityId: Long,
    val providerId: Long? = null,
    val verificationType: VerificationType? = VerificationType.OTP
)

data class CountryDTO(
    @field:NotBlank(message = "Country name cannot be blank")
    val name: String
)

data class RegionDTO(
    @field:NotBlank(message = "Region name cannot be blank")
    val name: String,
    val countryId: Long
)

data class WhiteListDTO(
    val benefitId: Long,
    val providerId: Long
)

data class MultipleWhiteListDTO(
    val benefitIds: List<Long>
)

data class ExclusionDTO(
    val categoryId: Long,
    val providerId: Long
)

data class CopayDTO(
    val amount: BigDecimal,
    val categoryId: Long,
    val providerId: Long
)

data class CardDTO(
    @field:NotNull(message = "Policy ID must be provided")
    val policyId: Long,
    @field:NotNull(message = "Request type must be provided")
    val type: CardRequestType,
    val paymentRef: String?
)

@Serializable
data class CreateBenefitDTO(
    var aggregateId: String?,
    val benefitName: String,
    val subBenefits: Set<SubBenefitDTO>,
    val beneficiaries: Set<CreateBeneficiaryDTO>,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val status: String = "ACTIVE",
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val suspensionThreshold: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val balance: BigDecimal,
    val policyNumber: String,
    val payer: SchemePayerDTO,
    val categoryId: Long,
    val benefitId: Long,
    val catalogId: Long
)

data class CreateBenefitDTOAvro(
    var aggregateId: String?,
    val benefitName: String,
    val subBenefits: HashMap<String, List<String>>,
    val beneficiaries: HashMap<CharSequence, CharSequence>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String = "ACTIVE",
    val suspensionThreshold: BigDecimal,
    val balance: BigDecimal,
    val policyNumber: String,
    val payer: SchemePayerDTO,
    val categoryId: Long
)

@Serializable
data class SubBenefitDTO(
    var name: String,
    val status: String = "ACTIVE",
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val balance: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)

    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    @Serializable(with = BigDecimalSerializer::class)
    @ScalePrecision(2, 10)
    val suspensionThreshold: BigDecimal,
    val benefitId: Long,
    val gender: ApplicableGender,
    val memberType: ApplicableMember,
    val catalogId: Long
)

@Serializable
data class CreateBeneficiaryDTO(
    val id: Long,
    val name: String,
    val memberNumber: String,
    val beneficiaryType: BeneficiaryType,
    val email: String?,
    val phoneNumber: String?,
    val gender: Gender,
    val type: BeneficiaryType,
    val jicEntityId: Int?,
    val apaEntityId: Int?
)

@Serializable
data class SchemePayerDTO(
    val payerId: Long,
    val payerName: String
)

@Serializable
data class PayerBenefitCodeMappingDTO(
    val id: Long = 0,
    val code: String
)

@Serializable
data class LinkCardDto(
    val beneficiaryId: Long,
    val phoneNumber: String,
    val link: Boolean,
)

data class BeneficiaryQueryDto(
    var beneficiaryId: Long?,
    var name: String?,
    var memberNumber: String?,
    var nhifNumber: String?,
    var dob: LocalDate?,
    var gender: Gender?,
    var phoneNumber: String?,
    var email: String?,
    var beneficiaryType: BeneficiaryType?,
    var category: Category?,
    var principal: Beneficiary?,
    var processed: Boolean?,
    var processedTime: LocalDateTime?,
    var payer: List<Payer>?
)

fun Beneficiary.toBeneficiaryDto(_payer: List<Payer>?) = BeneficiaryQueryDto(
    beneficiaryId = id,
    name = name,
    memberNumber = memberNumber,
    nhifNumber = nhifNumber,
    dob = dob,
    gender = gender,
    phoneNumber = phoneNumber,
    email = email,
    beneficiaryType = beneficiaryType,
    category = category,
    principal = principal,
    processed = processed,
    processedTime = processedTime,
    payer = _payer
)

data class JubileeCategoryDTO(
    @field:NotNull(message = "Category ID must be provided")
    val categoryId: Long,
    @field:NotNull(message = "MemberNumber must be provided")
    val memberNumber: String,
)

data class JubileeProviderCodeDTO(
    @field:NotNull(message = "Provider ID must be provided")
    val providerId: Long,
    @field:NotNull(message = "Payer ID must be provided")
    val payerId: Long
)
data class JubileeBenefitTO(
    @field:NotNull(message = "Benefit ID must be provided")
    val benefitId: Long,
    @field:NotNull(message = "Policy ID must be provided")
    val policyId: Long
)

@Serializable
data class JubileePayerProviderCodeMappingDTO(
    val id: Long = 0,
    val code: String? = null,
    val providerName:String?
)

@Serializable
data class JubileePayerBenefitNameMappingDTO(
    val id: Long = 0,
    val benefitName: String
)

@Serializable
data class JubileeResponseBenefitNameDTO(
    val benefit:String?,
    val subBenefit: String?
)


@Serializable
data class JubileeResponseMemberDTO(
    val policyPayerCode: Long?,
    val policyEffectiveDate:String?,
    val memberActisureId:Int?,
    val schemeName:String?,
    val memberFullName:String?
)
@Serializable
data class CashierUserDTO(
    val username: String,
    val email: String? = null,
    val password: String? = null,
    val providerId: Long,
    val providerName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class PayerUserDTO(
    val username: String,
    val email: String,
    val password: String,
    val payerId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class AdminUserDTO(
    val username: String,
    val email: String,
    val password: String,
    val payerId: Long,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeCategoryDTO(
    val oldCategoryId: Long,
    val newCategoryId: Long,
    val memberNumber: String,
)

data class DeactivateBenefitDTO(
    val beneficiaryId: Long,
    val categoryId: Long
)


data class UpdateMemberDTO(
    val id:Long,
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val dob: LocalDate?,
)

data class AuditLogDTO(

    var action: String,
    var user: String,
    val time: LocalDate?,
    var data: String?,
    var organisation: String?,
    var reason: String,
    val beneficiaryId: Long? = null,
    val memberNumber: String,
    val type:ChangeLogType?
)

data class BenefitPayersRequestDTO(
    val benefitIds: Collection<Long>
)
interface BenefitPayerMappingDTO {
    val id: Long
    val name: String
    val payer: Payer?
}

data class DeviceRequestDTO(
    @field:NotNull(message = "Provider ID must be provided")
    val providerId: Long,
    @field:NotNull(message = "Device ID must be provided")
    val deviceId: String,
    val imei1: String? = null,
    val imei2: String? = null
)

data class AllocateDeviceRequestDTO(
    @field:NotNull(message = "Provider ID must be provided")
    val providerId: Long,
    @field:NotNull(message = "Device Catalog ID must be provided")
    val deviceCatalogId: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BenefitsResponse(
    @JsonProperty("data")
    val data: MutableList<BeneficiaryBenefit>,

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("msg")
    val msg: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BeneficiaryBenefit(
    val id: Long? = 0,
    val aggregateId: String? = null,
    var status: String? = null,
    val categoryId: Long? = null,
    val beneficiaryId: Long? = null,
)

interface NearbyProvidersDTO {
    val providerId: Long
    val providerName: String
    val latitude:Double
    val longitude:Double
    val distanceInKm:Double
    val createdOn:String
    val regionName:String
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PayerMappings(
    val payerId: Long,
    val payerName: String,
    val benefitCode: String? = null,
    val serviceId: Long,
    val serviceGroup: String,
    val providerCode: String?,
    val providerName: String?,
    val schemeName: String?,
    val policyStartDate: LocalDate,
    val policyEndDate: LocalDate
)


data class ProviderUpdateDTO(
    val providerId:Long,
    val latitude:Double?=null,
    val longitude:Double?=null,
)

data class MultiProviderUpdateDTO(
    val providers: List<ProviderUpdateDTO>
)

interface PayerSearchDTO {
    val scheme: String
    val id:Long?
    val dob:String
    val gender:String
    val categoryId:Long
    val name:String
    val memberNumber:String
    val phoneNumber:String
    val status:String
    val beneficiaryType:String
}
