package net.lctafrica.membership.api.domain

import net.lctafrica.membership.api.dtos.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*
import org.springframework.data.domain.PageRequest

interface PayerRepository : JpaRepository<Payer, Long> {
	fun findByNameLike(name: String): MutableList<Payer>
	fun findByType(type: PayerType): MutableList<Payer>
	fun findByNameIgnoreCase(name: String): Optional<Payer>

	@Query(value = "SELECT p FROM Payer p WHERE p.type IN(:types)")
	fun findAdmins(@Param("types") types: List<PayerType>): MutableList<Payer>

	@Query(
		"SELECT p FROM Beneficiary b INNER JOIN b.category c INNER JOIN c.benefits bf INNER JOIN bf.payer p " +
				"WHERE b.id = :beneficiaryId GROUP BY b.id, p.id"
	)
	fun findPayerByBeneficiaryId(
		@Param("beneficiaryId") beneficiaryId: Long
	): MutableList<Payer>

}

interface PlanRepository : JpaRepository<Plan, Long> {
	@Query(
		"select * from plan where plan_id in ( select plan_id from policy where policy_id in ( " +
				"select DISTINCT policy_id from category where category_id in ( select DISTINCT " +
				"category_id FROM benefit where payer_id = ?1 ) ) )", nativeQuery = true
	)
	fun findByPayer(@Param("payerId") payerId: Long): MutableList<Plan>

	fun findByNameIgnoreCase(name: String): Optional<Plan>
}

interface PolicyRepository : JpaRepository<Policy, Long> {
	fun findByPlan(plan: Plan): MutableList<Policy>
	fun findByPolicyNumber(policyNumber: String): Optional<Policy>
}

interface CategoryRepository : JpaRepository<Category, Long> {
	fun findByPolicy(policy: Policy): MutableList<Category>

	@Query(value = "SELECT c FROM Category c JOIN FETCH c.policy p JOIN FETCH p.plan l WHERE c.id = :categoryId")
	fun fetchWithPolicy(@Param("categoryId") categoryId: Long): Category?

	@Query(value = "SELECT c FROM Category c WHERE c.policy = :policy AND UPPER(c.name)=UPPER(:name)")
	fun searchByPolicyAndName(
		@Param("policy") policy: Policy,
		@Param("name") name: String
	): Category?

	@Query(value = "SELECT c FROM Category c WHERE c.policy.id = :policyId AND UPPER(c.name) IN (:catNames)")
	fun findByPolicyIdAndNameIn(
		@Param("policyId") policyId: Long,
		@Param("catNames") catNames: MutableList<String>
	): MutableList<Category>


}

interface BenefitRepository : JpaRepository<Benefit, Long> {
	fun findByCategory(category: Category, request: Pageable): Page<Benefit>
	fun findByCategoryAndNameIgnoreCase(category: Category, name: String): Optional<Benefit>

	@Query(
		value = "SELECT b FROM Benefit b WHERE b.category = :category AND UPPER(b" +
				".name) LIKE" +
				" " +
				"concat('%',:name,'%')"
	)
	fun findByCategoryAndBenefitName(
		@Param("category") category: Category, @Param("name") name:
		String, request: Pageable
	): Page<Benefit>

	@Query(value = "SELECT b FROM Benefit b WHERE b.parentBenefit IS NULL AND b.category = :category")
	fun findMainBenefitsByCategoryShallow(@Param("category") category: Category): MutableList<Benefit>

	@Query(value = "SELECT DISTINCT b FROM Benefit b LEFT JOIN b.subBenefits s WHERE b.parentBenefit IS NULL AND b.category = :category")
	fun findMainBenefitsByCategory(@Param("category") category: Category): MutableList<Benefit>

	@Query(value = "SELECT DISTINCT b FROM Benefit b  LEFT JOIN b.subBenefits s WHERE b.parentBenefit IS NULL AND b.category = :category AND b.processed = FALSE")
	fun findUnprocessedMainBenefitsByCategory(@Param("category") category: Category): MutableList<Benefit>

	@Query(value = "SELECT b FROM Benefit b WHERE b.category = :category AND b.benefitRef = :ref")
	fun findByCategoryAndRef(
		@Param("category") category: Category,
		@Param("ref") ref: BenefitCatalog
	): Optional<Benefit>

	@Query(value = "SELECT * FROM benefit WHERE benefit_id = ?1", nativeQuery = true)
	fun findByBenefitId(@Param("benefit_id") benefit_id: String): Optional<Benefit>

	@Query(value = "SELECT b FROM Benefit b WHERE b.id IN :benefitIds")
	fun findPayersByBenefitIds(@Param("benefitIds") benefitIds: Collection<Long>): MutableList<BenefitPayerMappingDTO>
}

interface BeneficiaryRepository : JpaRepository<Beneficiary, Long> {
	fun findByCategory(category: Category, request: Pageable): Page<Beneficiary>
	fun findByCategoryAndBeneficiaryType(
		category: Category,
		type: BeneficiaryType
	): MutableList<Beneficiary>

	@Query(value = "SELECT DISTINCT b FROM Beneficiary b LEFT JOIN b.dependants d WHERE b.principal IS NULL AND b.category = :category")
	fun findFamiliesByCategory(@Param("category") category: Category): MutableList<Beneficiary>

	@Query(
		value = "SELECT DISTINCT b FROM Beneficiary b LEFT JOIN b.dependants d WHERE b" +
				".principal IS NULL AND b.category = :category AND b.memberNumber = :memberNumber"
	)
	fun findFamilyByCategoryAndMemberNumber(
		@Param("category") category: Category,
		@Param("memberNumber") memberNumber: String
	): MutableList<Beneficiary>

	@Query(value = "SELECT DISTINCT b FROM Beneficiary b LEFT JOIN b.dependants d WHERE b.principal IS NULL AND b.processed = FALSE AND b.category = :category")
	fun findUnprocessedFamiliesByCategory(@Param("category") category: Category): MutableList<Beneficiary>

	@Query(
		value = """
        SELECT DISTINCT b FROM Beneficiary b LEFT JOIN b.principal p WHERE b.principal IS NOT NULL AND b.processed = FALSE
            AND p.processed = TRUE AND b.category = :category
    """
	)
	fun findUnprocessedDependentsByCategory(category: Category): MutableList<Beneficiary>

	@Query(
		value = "SELECT b FROM Beneficiary b WHERE (b.memberNumber like concat('%',:search,'%') and LENGTH(:search) >4) " +
				"OR lower(b.name) like lower(concat('%', :search, '%')) "
	)
	fun searchByNameOrMemberNumber(@Param(value = "search") search: String): MutableList<Beneficiary>


	@Query(
		value = "select*from beneficiary where (member_number LIKE concat('%',?1,'%') and " +
				"LENGTH(?1) >7 OR beneficiary_name LIKE concat('%',?1,'%') and LENGTH(?1) >7)" +
				" and status = 'ACTIVE'", nativeQuery = true
	)
	fun searchByNameOrMemberNumberAndActive(@Param(value = "search") search: String):
			MutableList<Beneficiary>

	@Query(
		value = "select*from beneficiary where category_id IN ( select DISTINCT category_id from category where category_id in (" +
				"select DISTINCT category_id FROM benefit where payer_id = ?1 ) ) AND ((beneficiary" +
				".beneficiary_name LIKE concat(?2,'%') and " +
				"LENGTH(?2) >4)OR(beneficiary.member_number LIKE concat(?2,'%') " +
				"and LENGTH(?2) >4)) and status = 'ACTIVE'",
		nativeQuery = true
	)
	fun searchByPayerIdNameOrMemberNumber(
		@Param(value = "payerId") payerId: Long,
		@Param(value = "search") search: String
	): MutableList<Beneficiary>

	@Query(
		value = "select*from beneficiary where category_id IN ( select DISTINCT category_id from category where category_id in (" +
				"select DISTINCT category_id FROM benefit where payer_id = ?1 ) ) AND ((beneficiary" +
				".beneficiary_name LIKE concat(?2,'%') and " +
				"LENGTH(?2) >4)OR(beneficiary.member_number LIKE concat(?2,'%') " +
				"and LENGTH(?2) >4))",
		nativeQuery = true
	)
	fun searchByPayerIdNameOrMemberNumberMemberStatus(
		@Param(value = "payerId") payerId: Long,
		@Param(value = "search") search: String
	): MutableList<Beneficiary>


	@Query(
		value = "select b.beneficiary_id as id,b.dob,b.gender,pn.plan_name as scheme, b" +
				".category_id as categoryId, b" +
				".member_number as memberNumber, b" +
				".beneficiary_name as name, b.phone_number as phoneNumber, " +
				"b.status, b.beneficiary_type as beneficiaryType from beneficiary b " +
				"inner join membership.category c on c.category_id = b.category_id " +
				"inner join membership.policy p on p.policy_id = c.policy_id " +
				"inner join membership.plan pn on pn.plan_id = p.plan_id " +
				"where b.category_id IN ( select DISTINCT category_id from category where category_id " +
				"in ( select DISTINCT b.category_id FROM benefit where payer_id = ?1 ) ) " +
				"AND ( ( b.beneficiary_name LIKE concat('%', ?2,'%') and  LENGTH(?2) > 4 ) OR( b" +
				".member_number LIKE concat('%', ?2,'%') and LENGTH(?2) > 4 ) ) and b.status ='ACTIVE' and pn.plan_id = ?3",
		nativeQuery = true
	)
	fun searchByPayerIdPlanIdNameOrMemberNumber(
		@Param(value = "payerId") payerId: Long,
		@Param(value = "search") search: String,
		@Param(value = "planId") planId: Long
	): MutableList<PayerSearchDTO>


	@Query(value = "SELECT b FROM Beneficiary b WHERE b.category = :category AND b.memberNumber= :memberNumber")
	fun findByCategoryAndBeneficiary(
		@Param(value = "category") category: Category,
		@Param(value = "memberNumber") memberNumber: String
	): Optional<Beneficiary>

	@Query(
		value = "SELECT b FROM Beneficiary b WHERE b.category = :category AND UPPER(b" +
				".name) LIKE" +
				" " +
				"concat('%',:beneficiaryName,'%')"
	)
	fun findByCategoryAndBeneficiaryName(
		@Param("category") category: Category,
		@Param("beneficiaryName") beneficiaryName: String,
		request: Pageable
	): Page<Beneficiary>

	@Query(value = "SELECT DISTINCT b FROM Beneficiary b WHERE b.category = :category AND b.principal IS NULL")
	fun findPrincipalsByCategory(@Param(value = "category") category: Category): MutableList<Beneficiary>

	@Query(
		"SELECT b FROM Beneficiary b INNER JOIN b.category c INNER JOIN c.benefits bf INNER JOIN bf.payer p " +
				"WHERE SUBSTRING(b.phoneNumber, -9) = SUBSTRING(:phoneNumber,-9) and b.beneficiaryType = :beneficiaryType " +
				"GROUP BY b.id"
	)
	fun findPrincipalCoversByPhoneNumber(
		@Param("phoneNumber") phoneNumber: String,
		@Param("beneficiaryType") beneficiaryType: BeneficiaryType,
		request: Pageable
	): Page<Beneficiary>


	@Query(
		"SELECT b FROM Beneficiary b INNER JOIN BeneficiaryLink bl on (b.id = bl.beneficiary.id) " +
				"WHERE SUBSTRING(bl.phoneNumber, -9) = SUBSTRING(:phoneNumber,-9) AND bl.status is TRUE"
	)
	fun findLinkedDependantCoversByPhoneNumber(
		@Param("phoneNumber") phoneNumber: String, request: Pageable
	): Page<Beneficiary>

	@Query(
		"""
        SELECT * FROM(SELECT b.* FROM beneficiary b INNER JOIN category c on b.category_id = c.category_id 
        INNER JOIN benefit bf on c.category_id = bf.category_id INNER JOIN payer p on p.payer_id = bf.payer_id 
        INNER JOIN policy po on c.policy_id = po.policy_id WHERE curdate() BETWEEN po.start_date AND po.end_date AND 
		SUBSTRING(b.phone_number, -9) = SUBSTRING(:phoneNumber,-9) 
		and b.beneficiary_type = 'PRINCIPAL' AND b.status = 'ACTIVE' GROUP BY b.beneficiary_id 
		UNION SELECT b.* FROM beneficiary b INNER JOIN beneficiary_link bl on (b.beneficiary_id  = bl.beneficiary_id) 
		INNER JOIN category c on b.category_id = c.category_id INNER JOIN policy po on c.policy_id = po.policy_id 
		WHERE curdate() BETWEEN po.start_date AND po.end_date AND SUBSTRING(bl.phone_number, -9) = SUBSTRING(:phoneNumber,-9) 
        AND b.status = 'ACTIVE' AND bl.status is TRUE) as tb LIMIT :page,:size
        """,
		nativeQuery = true
	)
	fun findCoversByPhoneNumber(
		@Param("phoneNumber") phoneNumber: String,
		@Param("page") page: Int,
		@Param("size") size: Int,
	): List<Beneficiary>
}

interface SharedBenefitTrackerRepository : JpaRepository<SharedBenefitTracker, Long> {
	fun findByBeneficiaryAndBenefit(parentBen: Beneficiary, benefit: Benefit): SharedBenefitTracker
}

interface BenefitCatalogRepository : JpaRepository<BenefitCatalog, Long> {
	fun findByNameLike(search: String, request: Pageable): Page<BenefitCatalog>
	fun findByServiceGroup(serviceGroup: ServiceGroup, request: Pageable): Page<BenefitCatalog>

	@Query(value = "SELECT b FROM BenefitCatalog b WHERE b.id IN (:ids)")
	fun findByIds(@Param("ids") ids: List<Long>): MutableList<BenefitCatalog>

	@Query(
		value = "select b.benefit_ref as serviceId, bc.service_group as serviceGroup from benefit b inner join benefit_catalog bc on bc.service_id = b.benefit_ref where b.benefit_id = ?1",
		nativeQuery = true
	)
	fun findByBenefitId(@Param("benefitId") benefitId: Long): ServiceDto
}

interface CountryRepository : JpaRepository<Country, Long> {
	fun findByNameIgnoreCase(name: String): Country?
}

interface RegionRepository : JpaRepository<Region, Long> {
	fun findByNameIgnoreCase(name: String): Region?
	fun findByCountryAndNameIgnoreCase(country: Country, name: String): Region?
	fun findByCountry(country: Country, request: Pageable): Page<Region>
}

interface ProviderRepository : JpaRepository<Provider, Long> {
	@Query(
		value = "select * from provider where provider_name LIKE concat('%',?1,'%')",
		nativeQuery = true
	)
	fun searchByName(@Param(value = "search") search: String, request: Pageable):
			Page<Provider>

	fun findByTier(tier: ProviderTier, request: Pageable): Page<Provider>
	fun findByRegion(region: Region, pageable: Pageable): Page<Provider>

	@Query(
		value = "SELECT * FROM (SELECT p.provider_id as providerId, provider_name as providerName, p.latitude , p.longitude , " +
				"created_on as createdOn, r.region_name as regionName, " +
				"111.045 * DEGREES(ACOS(COS(RADIANS(:latitude)) * COS(RADIANS(p.latitude)) * COS(RADIANS(p.longitude) - " +
				"RADIANS(:longitude)) + SIN(RADIANS(:latitude)) * SIN(RADIANS(p.latitude)))) AS distanceInKm \n" +
				"FROM provider p inner join payer_provider_mapping ppm on p.provider_id = ppm .provider_id  \n " +
				"INNER JOIN region r on p.region_id = r.region_id " +
				"WHERE p.latitude is not null AND p.longitude is not null AND ppm.payer_id IN (:ids) \n" +
				"GROUP BY p.provider_id ORDER BY distanceInKm ASC) AS t WHERE t.distanceInKm <= :radius LIMIT :page, :size",
		nativeQuery = true
	)
	fun findNearbyProviders(
		@Param("latitude") latitude: Double,
		@Param("longitude") longitude: Double,
		@Param("radius") radius: Double,
		@Param("page") page: Int,
		@Param("size") size: Int,
		@Param("ids") ids: List<Long>
	): List<NearbyProvidersDTO>
}

interface ProviderExclusionRepository : JpaRepository<ProviderExclusion, Long> {
	fun findByCategory(category: Category): MutableList<ProviderExclusion>
	fun findByCategory(category: Category, request: Pageable): Page<ProviderExclusion>
	fun findByProvider(provider: Provider): MutableList<ProviderExclusion>
	fun findByCategoryAndProvider(
		category: Category, provider: Provider
	): Optional<ProviderExclusion>
}

interface CopaySetupRepository : JpaRepository<Copayment, Long> {
	fun findByCategory(category: Category): MutableList<Copayment>
	fun findByProvider(provider: Provider): MutableList<Copayment>
	fun findByCategoryAndProvider(
		category: Category, provider: Provider
	): Optional<Copayment>
}

interface WhiteListRepository : JpaRepository<Whitelist, Long> {
	fun findByProvider(provider: Provider, pageable: Pageable): Page<Whitelist>
	fun findByBenefit(benefit: BenefitCatalog, pageable: Pageable): Page<Whitelist>
}

interface PayerBenefitMappingRepository : JpaRepository<PayerBenefitMapping, Long> {
	fun findByPayer(payer: Payer): MutableList<PayerBenefitMapping>
	fun findByPayerIdAndBenefitId(
		payerId: Long,
		benefitCatalogId: Long
	): PayerBenefitCodeMappingDTO?

	fun findByBenefit(benefit: BenefitCatalog): MutableList<PayerBenefitMapping>
}

interface PayerProviderMappingRepository : JpaRepository<PayerProviderMapping, Long> {
	fun findByPayer(payer: Payer, pageable: Pageable): Page<PayerProviderMapping>
	fun findByProvider(provider: Provider): MutableList<PayerProviderMapping>
	fun findByPayerIdAndProviderId(payerId: Long, providerId: Long): Optional<PayerProviderMapping>

	fun findByProviderIdAndPayerId(
		providerId: Long,
		payerId: Long
	): Optional<JubileePayerProviderCodeMappingDTO>
}

interface CardBatchRepository : JpaRepository<CardBatch, Long> {
	fun findByPolicy(policy: Policy): List<CardBatch>
	fun findByType(type: CardRequestType, pageable: Pageable): Page<CardBatch>

	@Query(value = "SELECT c FROM CardBatch c LEFT JOIN c.beneficiaryCards x LEFT JOIN x.beneficiary y WHERE y.memberNumber = :memberNumber ")
	fun searchByMemberNumber(@Param("memberNumber") memberNumber: String): List<CardBatch>
}


interface BeneficiaryLinkRepository : JpaRepository<BeneficiaryLink, Long> {
	fun findByPhoneNumberAndBeneficiary_Id(phoneNumber: String, id: Long): Optional<BeneficiaryLink>
	fun findByBeneficiary_IdAndStatus(beneficiaryId: Long, status: Boolean): List<BeneficiaryLink>
}


interface AuditLogRepo : JpaRepository<AuditLog, Long> {
}

interface DeviceCatalogRepo : JpaRepository<DeviceCatalog, Long> {
	fun findByDeviceId(deviceId: String): Optional<DeviceCatalog>

	@Query(
		value = "SELECT c FROM DeviceCatalog c WHERE c.deviceId = :deviceId " +
				"OR (c.firstImei IS NOT NULL AND c.firstImei = :firstImei) " +
				"OR (c.secondImei IS NOT NULL AND c.secondImei = :secondImei)"
	)
	fun findByDeviceIdAndImei(
		@Param("deviceId") deviceId: String,
		@Param("firstImei") firstImei: String?,
		@Param("secondImei") secondImei: String?
	): List<DeviceCatalog>
}

interface DeviceAllocationRepo : JpaRepository<DeviceAllocation, Long> {
	fun findByDeviceCatalogAndStatus(
		deviceCatalog: DeviceCatalog,
		status: AllocationStatus
	): Optional<DeviceAllocation>

}