package net.lctafrica.membership.api.web

import io.swagger.v3.oas.annotations.Operation
import javax.validation.Valid
import net.lctafrica.membership.api.domain.PayerType
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.service.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/membership")
@CrossOrigin("*","*")
class SchemeController(
	val payerService: IPayerService,
	val planService: IPlanService,
	val policyService: IPolicyService,
	val categoryService: ICategoryService,
	val benefitService: IBenefitService,
	val beneficiaryService: IBeneficiaryService,
	val providerService: ProviderService,
	val jubileeService: JubileeService
) : AbstractController() {

	@GetMapping(value = ["/administrators"], produces = ["application/json"])
	fun getAdministrators() = payerService.findAdministrators()

	@GetMapping(value = ["/payers"], produces = ["application/json"])
	fun getAllPayers() = payerService.findAll()

	@GetMapping(value = ["/payers/type/{type}"], produces = ["application/json"])
	fun getPayersByType(@PathVariable("type") type: PayerType) = payerService.findByType(type)

	@GetMapping(value = ["/payers/{payerId}/payer"], produces = ["application/json"])
	fun getPayerById(@PathVariable("payerId") payerId: Long) = payerService.findById(payerId)

	@PostMapping(value = ["/payers"], produces = ["application/json"])
	fun addPayer(@Valid @RequestBody dto: PayerDTO) = payerService.addPayer(dto)

	@PostMapping(value = ["/payers/mapping/benefits"], produces = ["application/json"])
	fun addBenefitMapping(@Valid @RequestBody dto: PayerBenefitMappingDTO) =
		payerService.addBenefitMapping(dto)

	@PostMapping(value = ["/payers/mapping/providers"], produces = ["application/json"])
	fun addProviderMapping(@Valid @RequestBody dto: PayerProviderMappingDTO) =
		payerService.addProviderMapping(dto)

	@GetMapping(value = ["/payers/{payerId}/mapping/benefits"], produces = ["application/json"])
	fun getBenefitMappingsByPayer(@PathVariable("payerId") payerId: Long) =
		payerService.findBenefitMapping(payerId)

	@GetMapping(
		value = ["/payers/{payerId}/benefitRef/{benefitRef}/code"],
		produces = ["application/json"]
	)
	fun getBenefitCodeByPayerAndBenefitRef(
		@PathVariable("payerId") payerId: String, @PathVariable
			("benefitRef") benefitRef: String
	) = payerService.findBenefitCode(payerId, benefitRef)

	@GetMapping(
		value = ["/payers/{payerId}/{page}/{size}/mapping/providers"], produces =
		["application/json"]
	)
	fun getProviderMappingsByPayer(
		@PathVariable("payerId") payerId: Long,
		@PathVariable("page") page: Int = 1,
		@PathVariable("size") size: Int = 10
	) = payerService.findProviderMapping(payerId, page, size)

	@PostMapping(value = ["/plans"], produces = ["application/json"])
	fun addPlan(@Valid @RequestBody dto: PlanDTO) = planService.addPlan(dto)

	@GetMapping(value = ["/plans"], produces = ["application/json"])
	fun findPlans(page: Int = 0, size: Int = 10) = planService.findAll(page, size)

	@GetMapping(value = ["/payer/{payerId}/plans"], produces = ["application/json"])
	fun findPlansByPayer(@PathVariable("payerId") payerId: Long) =
		planService.findByPayerId(payerId)

	@PostMapping(value = ["/policies"], produces = ["application/json"])
	fun addPolicy(@Valid @RequestBody dto: PolicyDTO) = policyService.addPolicy(dto)

	@GetMapping(value = ["/policies"], produces = ["application/json"])
	fun findAllPolicies() = policyService.findAll()

	@GetMapping(value = ["/policies/{policyId}/policy"], produces = ["application/json"])
	fun findPolicyById(@PathVariable("policyId") policyId: Long) = policyService.findById(policyId)

	@GetMapping(value = ["/plan/{planId}/policies"])
	fun findPoliciesByPlan(@PathVariable("planId") planId: Long) = policyService.findByPlan(planId)

	@PostMapping(value = ["/categories"], produces = ["application/json"])
	fun addCategory(@Valid @RequestBody dto: CategoryDTO) = categoryService.addCategory(dto)

	@GetMapping(value = ["/policy/{policyId}/categories"], produces = ["application/json"])
	fun findCategoriesByPolicy(@PathVariable("policyId") policyId: Long) =
		categoryService.findByPolicy(policyId)


	@PostMapping(value = ["/benefits"], produces = ["application/json"])
	fun addBenefit(@Valid @RequestBody dto: BenefitDTO) = benefitService.addBenefit(dto)

	@GetMapping(value = ["/category/{categoryId}/benefits"], produces = ["application/json"])
	fun findBenefitsByCategory(
		@PathVariable("categoryId") categoryId: Long,
		page: Int = 1,
		size: Int = 10
	) =
		benefitService.findByCategory(categoryId, page, size)

	@GetMapping(
		value = ["/category/benefits"], produces =
		["application/json"]
	)
	fun findBenefitsByCategoryAndName(
		@RequestParam(value = "categoryId") categoryId: Long,
		@RequestParam(value = "benefitName") benefitName: String, page: Int = 1,
		size: Int = 10
	) = benefitService.findByCategoryAndName(categoryId, benefitName, page, size)

	@GetMapping(value = ["/category/{categoryId}/principals"], produces = ["application/json"])
	fun findPrincipalsByCategory(@PathVariable("categoryId") categoryId: Long) =
		beneficiaryService.findPrincipalsByCategory(categoryId)

	@GetMapping(value = ["/category/{categoryId}/benefits/main"], produces = ["application/json"])
	fun findMainBenefitsByCategory(@PathVariable("categoryId") categoryId: Long) =
		benefitService.findMainBenefitsByCategory(categoryId)

	@PostMapping(
		value = ["/categories/{categoryId}/benefits/process"],
		produces = ["application/json"]
	)
	fun processCategoryBenefits(@PathVariable("categoryId") categoryId: Long) =
		benefitService.processBenefits(categoryId)

	@PostMapping(value = ["/beneficiaries"], produces = ["application/json"])
	fun addBeneficiary(@Valid @RequestBody dto: BeneficiaryDTO) =
		beneficiaryService.addBeneficiary(dto)

//	@GetMapping(value = ["/beneficiaries"], produces = ["application/json"])
//	fun findByNameOrMemberNumberActive(@RequestParam(value = "search") search: String,
//	                                   @RequestParam(value = "providerId") providerId: Long) =
//		beneficiaryService.findByNameOrMemberNumberActive(search,providerId)

	@GetMapping(value = ["/beneficiaries"], produces = ["application/json"])
	fun findByNameOrMemberNumberActive(@RequestParam(value = "search") search: String) =
		beneficiaryService.findByNameOrMemberNumberActive(search)


	@GetMapping(value = ["/beneficiaries/noStatus"], produces = ["application/json"])
	fun findBeneficiaries(@RequestParam(value = "search") search: String) =
		beneficiaryService.searchByNameOrMemberNumber(search)

	@GetMapping(value = ["/beneficiariesByPayer"], produces = ["application/json"])
	fun findBeneficiariesByPayer(
		@RequestParam(value = "payerId") payerId: Long,
		@RequestParam(value = "search") search: String,
	) = beneficiaryService.findByPayerIdNameOrMemberNumber(payerId, search)

	@GetMapping(value = ["/beneficiariesStatusByPayer"], produces = ["application/json"])
	fun findBeneficiariesByPayerMemberStatus(
		@RequestParam(value = "payerId") payerId: Long,
		@RequestParam(value = "search") search: String,
	) = beneficiaryService.findByPayerIdNameOrMemberNumberMemberStatus(payerId, search)


	@GetMapping(value = ["/beneficiariesByPayerAndPlan"], produces = ["application/json"])
	fun findBeneficiariesByPayerAndPlan(
		@RequestParam(value = "payerId") payerId: Long,
		@RequestParam(value = "search") search: String,
		@RequestParam(value = "planId") planId:Long,
	) =
		beneficiaryService.findByPayerIdPlanIdNameOrMemberNumber(payerId, search, planId)

	@GetMapping(value = ["/category/{categoryId}/beneficiaries"], produces = ["application/json"])
	fun getBeneficiariesByCategory(
		@PathVariable("categoryId") categoryId: Long,
		page: Int = 1,
		size: Int = 10
	) =
		beneficiaryService.findByCategory(categoryId, page, size)

	@GetMapping(
		value = ["/category/beneficiaries"], produces =
		["application/json"]
	)
	fun getBeneficiariesByCategoryAndName(
		@RequestParam(value = "categoryId") categoryId: Long,
		@RequestParam(value = "beneficiaryName") beneficiaryName: String, page: Int = 1,
		size: Int = 10
	) = beneficiaryService.findByCategoryAndName(categoryId, beneficiaryName, page, size)

	@PostMapping(
		value = ["/category/massupload"],
		produces = ["application/json"],
		consumes = ["multipart/form-data"]
	)
	fun batchUpload(
		@RequestParam(value = "policyId") policyId: Long,
		@RequestParam(value = "file") file: MultipartFile
	) = categoryService.batchUpload(policyId, file)


	@GetMapping(value = ["/mapping"], produces = ["application/json"])
	fun getPayerProviderMapping(
		@RequestParam(value = "payerId") payerId: Long,
		@RequestParam(value = "providerId") providerId: Long
	) =
		payerService.findPayerProviderMapping(payerId, providerId)

	@GetMapping(value = ["/exclusion"], produces = ["application/json"])
	fun getProviderCategoryExclusion(
		@PathVariable(value = "providerId") providerId: Long,
		@PathVariable(value = "categoryId") categoryId: Long
	) =
		categoryService.findCategoryProviderExclusion(categoryId, providerId)

	@GetMapping(
		value = ["/covers/{beneficiaryType}/{phoneNumber}/{page}/{size}"],
		produces = ["application/json"]
	)
	fun findBeneficiaries(
		@PathVariable("beneficiaryType") beneficiaryType: String,
		@PathVariable("phoneNumber") phoneNumber: String,
		@PathVariable("page") page: Int = 1,
		@PathVariable("size") size: Int = 20
	) =
		beneficiaryService.findBeneficiaries(phoneNumber, beneficiaryType, page, size)

	@GetMapping(value = ["/covers/{phoneNumber}/{page}/{size}"], produces = ["application/json"])
	fun findCoversByPhoneNumber(
		@PathVariable("phoneNumber") phoneNumber: String,
		@PathVariable("page") page: Int = 1,
		@PathVariable("size") size: Int = 20
	) =
		beneficiaryService.findCoversByPhoneNumber(phoneNumber, page, size)

	@GetMapping(value = ["/beneficiaries/{beneficiaryId}"], produces = ["application/json"])
	fun findCoverBeneficiaries(@PathVariable("beneficiaryId") beneficiaryId: Long) =
		beneficiaryService.findCoverBeneficiaries(beneficiaryId)

	@PostMapping(value = ["/linkCover"], produces = ["application/json"])
	fun linkCover(@Valid @RequestBody dto: LinkCardDto) =
		beneficiaryService.linkCover(dto)

	@GetMapping(value = ["/coverLinks/{beneficiaryId}"], produces = ["application/json"])
	fun getBeneficiaryLinkedCovers(@PathVariable("beneficiaryId") beneficiaryId: Long) =
		beneficiaryService.getBeneficiaryLinkedCovers(beneficiaryId)

	@GetMapping(value = ["/category/{categoryId}/plan"], produces = ["application/json"])
	fun findPlanByCategory(@PathVariable("categoryId") categoryId: Long) =
		planService.findByCategory(categoryId)

	@PostMapping(
		value = ["/saveProvidersFromFile"],
		produces = ["application/json"],
		consumes = ["multipart/form-data"]
	)
	@Operation(summary = "Save ICD10 codes")
	fun saveProvidersFromFile(@RequestParam("file") file: MultipartFile) =
		providerService.saveProvidersFromFile(file)

	@GetMapping(value = ["/category/{categoryId}/category"], produces = ["application/json"])
	fun getCategory(@PathVariable("categoryId") categoryId: Long) =
		categoryService.findById(categoryId)

	////
	@GetMapping(value = ["/jubilee/getPolicyAndDate"], produces = ["application/json"])
	fun getPolicyIdAndDate(
		@RequestParam(value = "categoryId") categoryId: Long,
		@RequestParam(value = "memberNumber") memberNumber: String
	) = jubileeService.findPolicyIdAndDate(categoryId, memberNumber)

	@GetMapping(value = ["/jubilee/getProviderCode"], produces = ["application/json"])
	fun getPolicyIdAndDate(
		@RequestParam(value = "providerId") providerId: Long,
		@RequestParam(value = "payerId") payerId: Long
	) = jubileeService.findProviderCode(providerId, payerId)

	@GetMapping(value = ["/jubilee/getBenefit"], produces = ["application/json"])
	fun getBenefit(
		@RequestParam(value = "benefitId") benefitId: Long,
		@RequestParam(value = "policyId") policyId: Long
	) = jubileeService.findBenefit(benefitId, policyId)

	@PostMapping(value = ["/changeCategory"], produces = ["application/json"])
	fun changeCategory(@Valid @RequestBody dto: ChangeCategoryDTO) =
		benefitService.changeCategory(dto)

	@PostMapping(value = ["/updateMember"], produces = ["application/json"])
	fun updateMember(@RequestBody dto: UpdateMemberDTO) = beneficiaryService.updateMember(dto)

	@PostMapping(value = ["/activateBeneficiary/{beneficiaryId}"], produces = ["application/json"])
	fun activateBeneficiary(@PathVariable("beneficiaryId") beneficiaryId: Long) =
		beneficiaryService.activateBeneficiary(beneficiaryId)

	@PostMapping(
		value = ["/deactivateBeneficiary/{beneficiaryId}"],
		produces = ["application/json"]
	)
	fun deactivateBeneficiary(@PathVariable("beneficiaryId") beneficiaryId: Long) =
		beneficiaryService.deactivateBeneficiary(beneficiaryId)

	@PostMapping(
		value = ["/getPayersByBenefitIds"],
		consumes = ["application/json"],
		produces = ["application/json"]
	)
	fun findPayersByBenefitIds(@Valid @RequestBody dto: BenefitPayersRequestDTO) =
		benefitService.findPayersByBenefitIds(dto.benefitIds)

	@GetMapping(value = ["/payer/mappings"], produces = ["application/json"])
	fun getPayerMappings(@RequestParam(value = "providerId") providerId: Long,
						 @RequestParam(value = "payerId") payerId: Long,
						 @RequestParam(value = "benefitId") benefitId: Long,
						 @RequestParam(value = "categoryId") categoryId: Long,
	) = payerService.findPayerProviderAndBenefitMappings(providerId, payerId, benefitId, categoryId)


}
