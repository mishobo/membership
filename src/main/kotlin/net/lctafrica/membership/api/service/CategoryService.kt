package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.util.MemberInput
import net.lctafrica.membership.api.util.ReadExcelFile
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors

@Service("categoryService")
@Transactional
class CategoryService(
	private val repo: CategoryRepository,
	private val policyRepo: PolicyRepository,
	private val beneficiaryRepo: BeneficiaryRepository,
	private val xlFileReader: ReadExcelFile,
	private val providerRepo: ProviderRepository,
	private val providerExclusionRepository: ProviderExclusionRepository,
	private val benefitRepo :BenefitRepository
) : ICategoryService {

	@Transactional(readOnly = true)
	override fun findByPolicy(policyId: Long): Result<MutableList<Category>> {
		val policy = policyRepo.findById(policyId)
		if (policy.isPresent) {
			var catsByPolicy = repo.findByPolicy(policy.get())
			return ResultFactory.getSuccessResult(catsByPolicy)
		}
		return ResultFactory.getFailResult("No policy with policy ID $policyId was found")
	}


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun removeCategory(categoryId: Long): Result<Category> {
		return ResultFactory.getFailResult("Not yet implemented")
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun addCategory(dto: CategoryDTO): Result<MutableList<Category>> {
		val policy = policyRepo.findById(dto.policyId)
		var categories = mutableListOf<Category>()
		var catNames = mutableListOf<String>()
		return try {
			if (policy.isPresent) {
				dto.categories.forEach { cat ->
					val c = Category(
						name = cat.name.trim(),
						description = cat.description.trim(),
						policy = policy.get(),
						jicSchemeCode = cat.jicSchemeCode,
						apaSchemeCode = cat.apaSchemeCode,
						policyPayerCode = cat.policyPayerCode
					)
					catNames.add(cat.name.trim().uppercase())
					categories.add(c)
				}
				// check if any category already exists in the database
				val alreadyExists = repo.findByPolicyIdAndNameIn(dto.policyId, catNames)
				if (alreadyExists.isNotEmpty()) return ResultFactory.getFailResult("Category already exists")

				repo.saveAll(categories)
				ResultFactory.getSuccessResult(categories)
			} else {
				ResultFactory.getFailResult("No policy with ID ${dto.policyId} was found")
			}
		} catch (ex: IllegalArgumentException) {
			return ResultFactory.getFailResult(ex.message)
		}

	}

	@Transactional(rollbackFor = [Exception::class])
	override fun batchUpload(
		policyId: Long,
		file: MultipartFile
	): Result<MutableMap<String, MutableList<Beneficiary>>> {
		var errors: String? = null
		val optPolicy = policyRepo.findById(policyId)
		if (optPolicy.isPresent) {
			val policy = optPolicy.get()
			var map = mutableMapOf<String, MutableList<MemberInput>>()
			if (xlFileReader.isExcelFormat(file)) {
				val input = file.inputStream
				map = xlFileReader.read(input)
			} else {
				/*val input = file.inputStream as */
				return ResultFactory.getFailResult("File is not a valid excel format, ${file.contentType}")
			}
			var finalMap = mutableMapOf<String, MutableList<Beneficiary>>()

			val keys = map.keys
			println(keys)

			for (key in keys) {
				val category = getCategoryFromName(policy, key)
				val added = mutableListOf<Beneficiary>()
				if (category != null) {
					val members = map[key]

					val famGroups = members?.groupBy({ member -> member.tracker }, { mi ->
						BeneficiaryDTO(
							name = mi.name,
							memberNumber = mi.memberNumber,
							nhifNumber = mi.nhif,
							dob = mi.dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
							gender = mi.gender,
							beneficiaryType = mi.title,
							email = mi.email,
							phoneNumber = mi.phone,
							categoryId = category.id,
							principalId = null,
							jicEntityId = mi.jicEntityId,
							apaEntityId = mi.apaEntityId
						)
					})

					for (fam in famGroups!!.keys) {
						val depsList = mutableListOf<Beneficiary>()
						try {
							val pr =
								famGroups[fam]!!.first { dto -> dto.beneficiaryType == BeneficiaryType.PRINCIPAL }
							var principal = beneficiaryRepo.save(
								Beneficiary(
									name = pr.name,
									memberNumber = pr.memberNumber,
									dob = pr.dob,
									nhifNumber = pr.nhifNumber,
									beneficiaryType = pr.beneficiaryType,
									email = pr.email,
									phoneNumber = pr.phoneNumber,
									category = category,
									gender = pr.gender,
									principal = null,
									processed = false,
									processedTime = null,
									jicEntityId = pr.jicEntityId,
									apaEntityId = pr.apaEntityId
								)
							)


							var deps =
								famGroups[fam]!!.filter { huyu -> huyu.beneficiaryType != BeneficiaryType.PRINCIPAL }

							for (dep in deps) {
								var dependant = Beneficiary(
									principal = principal,
									name = dep.name,
									nhifNumber = dep.nhifNumber,
									memberNumber = dep.memberNumber,
									dob = dep.dob,
									beneficiaryType = dep.beneficiaryType,
									email = dep.email,
									phoneNumber = dep.phoneNumber,
									category = category,
									gender = dep.gender,
									processed = false,
									processedTime = null,
									jicEntityId = dep.jicEntityId,
									apaEntityId = dep.apaEntityId
								)
								depsList.add(dependant)
							}
							println("$depsList")
							beneficiaryRepo.saveAll(depsList)
							added.add(principal)
							added.addAll(depsList)
						} catch (ex: java.util.NoSuchElementException) {
							if (errors != null) {
								errors = errors.plus("Check family number $fam for correctness. ")
							}
							errors = "Check family number $fam for correctness. "
							continue
						}
					}

				}
				finalMap[key] = added
			}

			/*map.keys.forEach { cat ->
				println("mapping inside $cat in order to fetch category")
				val category = getCategoryFromName(policy, cat)
				var additions = mutableListOf<Beneficiary>()
				val members = map[category!!.name.uppercase().trim()]
				val famGroups = members?.groupBy({ member -> member.tracker }, { mi ->
					BeneficiaryDTO(
						name = mi.name, memberNumber = mi.memberNumber,
						dob = mi.dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
						gender = mi.gender, beneficiaryType = mi.title, email = mi.email, phoneNumber = mi.phone,
						categoryId = category.id, principalId = null
					)
				})
				println("Fam groups $famGroups")
				famGroups!!.keys.forEach { grp ->
					val principal = famGroups[grp]!!.filter { it.beneficiaryType == BeneficiaryType.PRINCIPAL }
						.map {
							Beneficiary(
								name = it.name,
								memberNumber = it.memberNumber,
								dob = it.dob,
								beneficiaryType = it.beneficiaryType,
								email = it.email,
								phoneNumber = it.phoneNumber,
								category = category,
								gender = it.gender,
								principal = null,
								processed = false,
								processedTime = null
							)
						}.map { ben ->
							println("saving beneficiary ${ben.name}")
							return@map beneficiaryRepo.save(ben)
						}.first()
					additions.add(principal)
					var deps = famGroups[grp]!!.filter { it.beneficiaryType != BeneficiaryType.PRINCIPAL }
						.map {
							return@map Beneficiary(
								name = it.name,
								memberNumber = it.memberNumber,
								dob = it.dob,
								beneficiaryType = it.beneficiaryType,
								email = it.email,
								phoneNumber = it.phoneNumber,
								category = category,
								gender = it.gender,
								principal = principal,
								processed = false,
								processedTime = null
							)
						}
					beneficiaryRepo.saveAll(deps)
					additions.addAll(deps)
					finalMap[category.name] = additions
				}
			}*/
			return ResultFactory.getSuccessResult(
				data = finalMap,
				msg = errors ?: "Successfully created beneficiaries"
			)
		}
		return ResultFactory.getFailResult("No policy with ID $policyId was found. Batch upload failed")
	}

	override fun findCategoryProviderExclusion(
		categoryId: Long,
		providerId: Long
	): Result<Boolean> {

		val category = repo.findById(categoryId)
		if (category.isEmpty) return ResultFactory.getFailResult("No Category with ID $categoryId was found")
		val provider = providerRepo.findById(categoryId)
		if (provider.isEmpty) return ResultFactory.getFailResult("No Provider with ID $providerId was found")

		val optional =
			providerExclusionRepository.findByCategoryAndProvider(category.get(), provider.get())

		return if (optional.isPresent) {
			ResultFactory.getSuccessResult(
				data = true,
			)
		} else {
			ResultFactory.getSuccessResult(
				data = false,
			)
		}
	}

	override fun findById(categoryId: Long): Result<Category> {
		val category = repo.findById(categoryId)
		return if(category.isEmpty) ResultFactory.getFailResult("No Such category with id $categoryId was found")
		else
			ResultFactory.getSuccessResult(data = category.get())
	}

	fun getCategoryFromName(policy: Policy, name: String): Category? {
		val findByPolicyAndNameIgnoreCase = repo.searchByPolicyAndName(policy = policy, name)
		println("showing category found $findByPolicyAndNameIgnoreCase")
		return findByPolicyAndNameIgnoreCase
	}
}
