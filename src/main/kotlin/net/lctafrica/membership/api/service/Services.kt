package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.AuditLog
import net.lctafrica.membership.api.domain.Beneficiary
import net.lctafrica.membership.api.domain.BeneficiaryLink
import net.lctafrica.membership.api.domain.Benefit
import net.lctafrica.membership.api.domain.BenefitCatalog
import net.lctafrica.membership.api.domain.CardBatch
import net.lctafrica.membership.api.domain.Category
import net.lctafrica.membership.api.domain.Copayment
import net.lctafrica.membership.api.domain.Country
import net.lctafrica.membership.api.domain.DeviceAllocation
import net.lctafrica.membership.api.domain.DeviceCatalog
import net.lctafrica.membership.api.domain.Payer
import net.lctafrica.membership.api.domain.PayerBenefitMapping
import net.lctafrica.membership.api.domain.PayerProviderMapping
import net.lctafrica.membership.api.domain.PayerType
import net.lctafrica.membership.api.domain.Plan
import net.lctafrica.membership.api.domain.Policy
import net.lctafrica.membership.api.domain.Provider
import net.lctafrica.membership.api.domain.ProviderExclusion
import net.lctafrica.membership.api.domain.ProviderTier
import net.lctafrica.membership.api.domain.Region
import net.lctafrica.membership.api.domain.ServiceGroup
import net.lctafrica.membership.api.domain.Whitelist
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.util.Result
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.data.domain.Page
import org.springframework.web.multipart.MultipartFile
import java.util.*

interface IPayerService {
    fun findAll(): Result<MutableList<Payer>>
    fun addPayer(dto: PayerDTO): Result<Payer>
    fun findByType(type: PayerType): Result<MutableList<Payer>>
    fun findById(payerId: Long): Result<Payer>
    fun addBenefitMapping(dto: PayerBenefitMappingDTO): Result<PayerBenefitMapping>
    fun findBenefitMapping(payerId: Long): Result<MutableList<PayerBenefitMapping>>
    fun addProviderMapping(dto: PayerProviderMappingDTO): Result<PayerProviderMapping>
    fun findProviderMapping(payerId: Long, page: Int, size: Int): Result<Page<PayerProviderMapping>>
    fun findAdministrators(): Result<MutableList<Payer>>
    fun findBenefitCode(payerId: String,benefitRef: String): Result<PayerBenefitCodeMappingDTO?>
    fun findPayerProviderMapping(payerId: Long,providerId: Long): Result<PayerService.PayerProviderMap>
    fun findPayerProviderAndBenefitMappings(providerId: Long, payerId: Long, benefitId: Long, categoryId: Long): Result<PayerMappings>

}

interface IPlanService {
    fun findAll(page: Int, size: Int): Result<Page<Plan>>
    //fun findByPayer(payerId: Long): Result<MutableList<Plan>>
    fun addPlan(dto: PlanDTO): Result<Plan>
    fun findByCategory(categoryId: Long):Result<Plan>
    fun findByPayerId(payerId: Long):Result<MutableList<Plan>>
}

interface IPolicyService {
    fun findAll(): Result<MutableList<Policy>>
    fun findByPlan(planId: Long): Result<MutableList<Policy>>
    fun addPolicy(dto: PolicyDTO): Result<Policy>
    fun processPolicy(policyNumber: String): Result<Policy>
    fun findById(policyId: Long):Result<Policy>
}

interface ICategoryService {
    fun findByPolicy(policyId: Long): Result<MutableList<Category>>
    fun removeCategory(categoryId: Long): Result<Category>
    fun addCategory(dto: CategoryDTO): Result<MutableList<Category>>
    fun batchUpload(policyId: Long, file: MultipartFile): Result<MutableMap<String, MutableList<Beneficiary>>>
    fun findCategoryProviderExclusion(categoryId: Long,providerId: Long):Result<Boolean>
    fun findById(categoryId: Long):Result<Category>


}

interface IBenefitService {
    fun findByCategory(categoryId: Long, page: Int, size: Int): Result<Page<Benefit>>
    fun findByCategoryAndName(categoryId: Long, benefitName:String, page: Int, size: Int):
            Result<Page<Benefit>>
    fun findMainBenefitsByCategory(categoryId: Long): Result<MutableList<Benefit>>
    fun addBenefit(dto: BenefitDTO): Result<Benefit>
    fun addMultipleBenefits(dto: NestedBenefitDTO): Result<MutableList<Benefit>>
    fun removeBenefit(benefit: Benefit): Result<Benefit>
    fun processBenefits(categoryId: Long): Result<Benefit>
    fun processNewMembers(categoryId: Long): Result<Benefit>
    fun processBenefit(benefit: Benefit): Result<Benefit>
    fun changeCategory(dto:ChangeCategoryDTO):Result<Boolean>
    fun findPayersByBenefitIds(benefitIds: Collection<Long>) : Result<MutableList<BenefitPayerMappingDTO>>
}

interface IBeneficiaryService {
    fun findByCategory(categoryId: Long, page: Int, size: Int): Result<Page<Beneficiary>>
    fun findByNameOrMemberNumber(search: String): Result<MutableList<Beneficiary>>
    fun searchByNameOrMemberNumber(search: String): Result<MutableList<Beneficiary>>
   // fun findByNameOrMemberNumberActive(search: String,providerId: Long): Result<MutableList<Beneficiary>>
    fun findByNameOrMemberNumberActive(search: String): Result<MutableList<Beneficiary>>
    fun findByPayerIdNameOrMemberNumber(payerId: Long, search: String): Result<MutableList<Beneficiary>>
    fun findByPayerIdNameOrMemberNumberMemberStatus(payerId: Long, search: String): Result<MutableList<Beneficiary>>
    fun findByPayerIdPlanIdNameOrMemberNumber(payerId: Long, search: String,planId:Long):
            Result<MutableList<PayerSearchDTO>>
    fun addBeneficiary(dto: BeneficiaryDTO): Result<Beneficiary>
    fun addBeneficiaries(categoryId: Long, dtos: List<BeneficiaryDTO>): Result<MutableList<Beneficiary>>
    fun removeBeneficiary(beneficiary: Beneficiary): Result<Beneficiary>
    fun findPrincipalsByCategory(categoryId: Long): Result<MutableList<Beneficiary>>
    fun findBeneficiaries(phoneNumber: String, beneficiaryType:String, page:Int, size:Int): Result<List<BeneficiaryQueryDto>>
    fun findCoversByPhoneNumber(phoneNumber: String, page:Int, size:Int): Result<List<BeneficiaryQueryDto>>
    fun findCoverBeneficiaries(beneficiaryId: Long): Result<List<Beneficiary>>
    fun linkCover(dto: LinkCardDto): Result<BeneficiaryLink>
    fun getBeneficiaryLinkedCovers(beneficiaryId: Long): Result<List<BeneficiaryLink>>
    fun updateMember(dto: UpdateMemberDTO): Result<Beneficiary>
    fun activateBeneficiary(beneficiaryId: Long): Result<Beneficiary>
    fun deactivateBeneficiary(beneficiaryId: Long): Result<Beneficiary>
    fun findByCategoryAndName(categoryId: Long, beneficiaryName:String, page: Int, size: Int): Result<Page<Beneficiary>>
}

interface IBenefitCatalogService {
    fun addBenefitCatalog(dto: BenefitCatalogDTO): Result<BenefitCatalog>
    fun searchByName(search: String, page: Int, size: Int): Result<Page<BenefitCatalog>>
    fun getAllCatalogBenefits(page: Int, size: Int): Result<Page<BenefitCatalog>>
    fun batchAddition(services: List<BenefitCatalogDTO>): Result<MutableList<BenefitCatalog>>
    fun findByServiceGroup(serviceGroup: ServiceGroup, page: Int, size: Int): Result<Page<BenefitCatalog>>
}

interface ICountryService {
    fun addCountry(dto: CountryDTO): Result<Country>
    fun findAllCountries(): Result<MutableList<Country>>
    fun addRegion(dto: RegionDTO): Result<Region>
    fun findRegionByCountry(countryId: Long, page: Int, size: Int): Result<Page<Region>>
    fun findRegionById(regionId: Long): Result<Region>
}

interface IProviderService {
    fun findById(providerId: Long):Result<Provider>
    fun addProvider(dto: ProviderDTO): Result<Provider>
    fun findByTier(tier: ProviderTier, page: Int, size: Int): Result<Page<Provider>>
    fun findByName(name: String, page: Int, size: Int): Result<Page<Provider>>
    fun findAllProviders( page: Int, size: Int): Result<Page<Provider>>
    fun findByRegion(regionId: Long, page: Int, size: Int): Result<Page<Provider>>
    fun findWhiteListByProvider(providerId: Long, page: Int, size: Int): Result<Page<Whitelist>>
    fun findWhiteListByBenefit(benefitId: Long, page: Int, size: Int): Result<Page<Whitelist>>
    fun addWhitelist(dto: WhiteListDTO): Result<Whitelist>
    fun addMultipleMappings(providerId: Long, dto: MultipleWhiteListDTO): Result<MutableList<Whitelist>>
    fun saveProvidersFromFile(file: MultipartFile): Result<List<Provider>>
    fun saveProviderMappingFromFile(file: MultipartFile): Result<List<PayerProviderMapping>>
    fun getNearbyProviders(beneficiaryId:Long, latitude:Double, longitude:Double, radius:Double,page:Int, size:Int): Result<List<NearbyProvidersDTO>>
    fun updateProvider(dto: ProviderUpdateDTO): Result<Provider>
    fun multiProviderUpdate(dto: MultiProviderUpdateDTO): Result<Boolean>
}

interface IProviderExclusionService {
    fun addExclusion(dto: ExclusionDTO): Result<ProviderExclusion>
    fun deactivate(exclusion: ProviderExclusion): Result<ProviderExclusion>
    fun process(category: Category): Result<MutableList<ProviderExclusion>>
    fun findExclusionsByProvider(providerId: Long): Result<MutableList<ProviderExclusion>>
    fun findExclusionsByCategory(categoryId: Long, page: Int, size: Int): Result<Page<ProviderExclusion>>
    fun findExclusionsByCategoryAndProvider(categoryId: Long, providerId: Long): Result<Boolean>
}

interface ICopaySetupService {
    fun addCopay(dto: CopayDTO): Result<Copayment>
    fun deactivate(copayment: Copayment): Result<Copayment>
    fun findByProvider(providerId: Long): Result<MutableList<Copayment>>
    fun findByCategory(categoryId: Long): Result<MutableList<Copayment>>
    fun process(category: Category): Result<MutableList<Copayment>>
    fun findByCategoryAndProvider(categoryId: Long, providerId: Long): Result<Copayment?>
}

interface ICardRequestService {
    fun newRequest(dto: CardDTO): Result<CardBatch>
    fun issueCardBatch(batchId: Long) : Result<CardBatch>
    fun findByMember(memberNumber: String): Result<List<CardBatch>>
}

interface IJubileeService {
    fun findPolicyIdAndDate(categoryId:Long, memberNumber:String):Result<JubileeResponseMemberDTO?>
    fun findProviderCode(providerId:Long,payerId:Long): Result<JubileePayerProviderCodeMappingDTO>
    fun findBenefit(benefitId:Long,policyId:Long):Result<JubileeResponseBenefitNameDTO?>
}

interface IUserManagementService{
    fun addUserCashier(dto:CashierUserDTO): Result<String>
    fun addPayerUser(dto:PayerUserDTO):Result<PayerUserDTO>
    fun addAdminUser(dto:AdminUserDTO):Result<AdminUserDTO>
    fun getProviderUsers(providerId: Long): List<UserRepresentation?>?
    fun getPayerUsers(providerId: Long): List<UserRepresentation?>?
}

interface IAuditLogService{
    fun saveLog(auditLogDTO: AuditLogDTO): Result<AuditLog>
}

interface IDeviceManagementService{
    fun registerDevice(dto: DeviceRequestDTO): Result<DeviceCatalog>
    fun allocateDevice(dto: AllocateDeviceRequestDTO): Result<Boolean>
    fun getDeviceAllocation(providerId: Long, deviceId: String, imei1: String? = null, imei2: String? = null): Result<DeviceAllocation>
}