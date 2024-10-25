package net.lctafrica.membership.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import net.lctafrica.membership.api.domain.*
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.gson
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import net.lctafrica.membership.api.webClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors


@Suppress("DEPRECATION")
@Service("benefitService")
@Transactional
class BenefitService(
	private val repo: BenefitRepository,
	private val categoryRepo: CategoryRepository,
	private val beneficiaryRepo: BeneficiaryRepository,
	private val trackerRepo: SharedBenefitTrackerRepository,
	private val benefitCatalogRepo: BenefitCatalogRepository

) : IBenefitService {

	val LOG = LoggerFactory.getLogger(BenefitService::class.java)

	@Autowired
	lateinit var objectMapper: ObjectMapper;

	@Autowired
	lateinit var kafka: KafkaTemplate<String, String>

	@Value("\${lct-africa.benefit.topic}")
	lateinit var benefitTopic: String

	@Value("\${lct-africa.benefit.command.base-url}")
	lateinit var benefitUrl: String

	@Value("\${lct-africa.endpoint.create-benefit}")
	lateinit var createBenefitEndpoint: String

	@Value("\${lct-africa.endpoint.suspend-benefits}")
	lateinit var suspendBenefits: String

	@Transactional(readOnly = true)
	override fun findByCategory(categoryId: Long, page: Int, size: Int): Result<Page<Benefit>> {
		val category = categoryRepo.findById(categoryId)
		if (category.isPresent) {
			val request = PageRequest.of(page - 1, size);
			val benefits = repo.findByCategory(category.get(), request)
			return ResultFactory.getSuccessResult(benefits)
		}
		return ResultFactory.getFailResult("No category with ID $categoryId was found")
	}

	@Transactional(readOnly = true)
	override fun findByCategoryAndName(
		categoryId: Long,
		benefitName: String,
		page: Int,
		size: Int
	): Result<Page<Benefit>> {

		val category = categoryRepo.findById(categoryId)
		if (category.isPresent) {
			val request = PageRequest.of(page - 1, size);
			val benefits = repo.findByCategoryAndBenefitName(category.get(),benefitName, request)
			return ResultFactory.getSuccessResult(benefits)
		}
		return ResultFactory.getFailResult("No category with ID $categoryId was found")


	}

	@Transactional(readOnly = true)
	override fun findMainBenefitsByCategory(categoryId: Long): Result<MutableList<Benefit>> {
		val category = categoryRepo.findById(categoryId)
		if (category.isPresent) {
			val benefits = repo.findMainBenefitsByCategoryShallow(category.get())
			return ResultFactory.getSuccessResult(benefits)
		}
		return ResultFactory.getFailResult("No category with ID $categoryId was found")
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun addBenefit(dto: BenefitDTO): Result<Benefit> {
		val category = categoryRepo.findById(dto.categoryId)
		var parentBenefit: Benefit? = null
		if (category.isEmpty) return ResultFactory.getFailResult("No category with ID ${dto.categoryId} was found")
		val byCategoryAndName = repo.findByCategoryAndNameIgnoreCase(category.get(), dto.name)
		var copayAmount = BigDecimal.ZERO
		val benefitRef = benefitCatalogRepo.findById(dto.catalogRefId)
		if (benefitRef.isEmpty) return ResultFactory.getFailResult("No benefit with ID ${dto.catalogRefId} was found in the catalog")
		val byCategoryAndRef = repo.findByCategoryAndRef(category.get(), benefitRef.get())
		if (byCategoryAndRef.isPresent) return ResultFactory.getFailResult("${benefitRef.get().name} has already been setup for this category")
		return when {
			byCategoryAndName.isPresent -> ResultFactory.getFailResult(
				msg = "Benefit [${dto.name}] already exists for this category",
				data = byCategoryAndName.get()
			)
			else -> try {

				return category.map { cat ->
					if (dto.parentBenefitId != null && dto.parentBenefitId > 0) {
						parentBenefit = repo.getById(dto.parentBenefitId)
					}

					if (dto.coPaymentRequired) copayAmount = dto.coPaymentAmount

					return@map Benefit(
						category = cat,
						name = dto.name,
						benefitRef = benefitRef.get(),
						applicableGender = dto.applicableGender,
						applicableMember = dto.applicableMember,
						limit = dto.limit,
						suspensionThreshold = dto.suspensionThreshold,
						preAuthType = dto.preAuthType,
						sharing = dto.sharing,
						coPaymentRequired = dto.coPaymentRequired,
						coPaymentAmount = copayAmount,
						waitingPeriod = dto.waitingPeriod,
						parentBenefit = parentBenefit,
						subBenefits = mutableSetOf(),
						processed = false,
						processedTime = null,
						payer = dto.payer,
					)
				}.map { benefit ->
					repo.save(benefit)
					return@map ResultFactory.getSuccessResult(benefit)
				}.get()
			} catch (ex: IllegalArgumentException) {
				return ResultFactory.getFailResult(ex.message)
			}
		}
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun addMultipleBenefits(dto: NestedBenefitDTO): Result<MutableList<Benefit>> {
		return ResultFactory.getFailResult("Not yet implemented")
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun removeBenefit(benefit: Benefit): Result<Benefit> {
		return ResultFactory.getFailResult("Not yet implemented")
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun processBenefits(categoryId: Long): Result<Benefit> {

		val category = categoryRepo.fetchWithPolicy(categoryId)

		with(category) {
			if (category?.status == CategoryStatus.UNPROCESSED) {

				val policy = this?.policy
				val families = beneficiaryRepo.findFamiliesByCategory(category!!)
				val unprocessedBenefits = repo.findUnprocessedMainBenefitsByCategory(category)
				val trackers = mutableSetOf<SharedBenefitTracker>()

				for (family in families) {
					val members = mutableSetOf<Beneficiary>()
					members.add(family)
					members.addAll(family.dependants)

					val indivBenefits = unprocessedBenefits
						.stream()
						.filter { b -> b.sharing == BenefitDistribution.INDIVIDUAL }
						.collect(Collectors.toList())
					println("Individual benefit count is ${indivBenefits.size}")
					for (benefit in indivBenefits) {
						for (m in members) {
							if (benefitEligibilityPasses(m, benefit)) {
								val dto = CreateBeneficiaryDTO(
									id = m.id,
									name = m.name,
									memberNumber = m.memberNumber,
									beneficiaryType = m.beneficiaryType,
									email = m.email,
									phoneNumber = m.phoneNumber,
									gender = m.gender,
									type = m.beneficiaryType,
									jicEntityId = m.jicEntityId,
									apaEntityId = m.apaEntityId
								)
								println("Building ${benefit.name} benefit for ${m.name}")
								val payer = benefit.payer.let { pyr ->
									return@let SchemePayerDTO(
										payerId = pyr.id,
										payerName = pyr.name
									)
								}
								buildAndGenerateAggregate(
									catalogId = benefit.benefitRef.id!!,
									benefit = benefit,
									beneficiaries = mutableSetOf(dto),
									policy = policy,
									category = category,
									payer = payer
								)
							}
						}
						benefit.processed = true
						benefit.processedTime = LocalDateTime.now()
						repo.save(benefit)
					}

					val famBenefits = unprocessedBenefits
						.stream()
						.filter { b -> b.sharing == BenefitDistribution.FAMILY }
						.collect(Collectors.toList())
					for (benefit in famBenefits) {
						val passed = mutableSetOf<CreateBeneficiaryDTO>()
						for (m in members) {
							if (benefitEligibilityPasses(m, benefit)) {
								val dto = CreateBeneficiaryDTO(
									id = m.id,
									name = m.name,
									memberNumber = m.memberNumber,
									beneficiaryType = m.beneficiaryType,
									email = m.email,
									phoneNumber = m.phoneNumber,
									gender = m.gender,
									type = m.beneficiaryType,
									jicEntityId = m.jicEntityId,
									apaEntityId = m.apaEntityId
								)
								passed.add(dto)
							}
							/**
							 * Here, no need to test principal member since they have already been added to members set
							 */
							/*if (benefitEligibilityPasses(family, benefit)) {
							passed.add(
								CreateBeneficiaryDTO(
									id = family.id, name = family.name,
									memberNumber = family.memberNumber, beneficiaryType = family.beneficiaryType,
									email = family.email, phoneNumber = family.phoneNumber
								)
							)
						}*/
						}
						val payer = benefit.payer.let { pyr ->
							return@let SchemePayerDTO(
								payerId = pyr.id,
								payerName = pyr.name
							)
						}
						val result =
							buildAndGenerateAggregate(
								benefit.benefitRef.id!!,
								benefit,
								passed,
								policy,
								category,
								payer
							)
						println(result)
						if (result.success) {
							println(result)
							val tracker = SharedBenefitTracker(
								beneficiary = family,
								aggregateId = result.data!!,
								benefit = benefit
							)
							trackers.add(tracker)
							benefit.processed = true
							benefit.processedTime = LocalDateTime.now()
							repo.save(benefit)
						}
					}

					members.map {
						it.processed = true
						it.processedTime = LocalDateTime.now()
						beneficiaryRepo.save(it)
					}

				}

				/* families.forEach { beneficiary ->

				 */
				/**
				 * Push all non-shared benefits to benefit management module individually
				 *//*
                benefits.filter { !it.processed }
                    .filter { benInput -> benInput.sharing == BenefitDistribution.INDIVIDUAL }.map { benefit ->
                        val candidates = mutableSetOf<Beneficiary>()
                        candidates.add(beneficiary)
                        candidates.addAll(beneficiary.dependants)
                        LOG.debug("Inside loop with individual benefit ${benefit.name} for family of ${beneficiary.name}")
                        candidates.filter { input -> benefitEligibilityPasses(input, benefit) }.map { candidate ->
                            CreateBeneficiaryDTO(
                                id = candidate.id,
                                name = candidate.name,
                                memberNumber = candidate.memberNumber,
                                beneficiaryType = candidate.beneficiaryType,
                                email = candidate.email,
                                phoneNumber = candidate.phoneNumber
                            )
                        }.map { dto ->
                            buildAndGenerateAggregate(benefit, mutableSetOf(dto), policy, category, payer)
                        }

                        benefit.processed = true
                        benefit.processedTime = LocalDateTime.now()
                        repo.save(benefit)
                    }

                */
				/**
				 * Push all shared benefits to benefit management module
				 *//*
                benefits.filter { !it.processed }
                    .filter { it.sharing == BenefitDistribution.FAMILY }.map { benefit ->
                        val passed = mutableSetOf<CreateBeneficiaryDTO>()
                        beneficiary.dependants.filter {
                            benefitEligibilityPasses(it, benefit)
                        }.map {
                            CreateBeneficiaryDTO(
                                id = it.id,
                                name = it.name,
                                memberNumber = it.memberNumber,
                                beneficiaryType = it.beneficiaryType,
                                email = it.email,
                                phoneNumber = it.phoneNumber
                            )
                        }.map { passed.add(it) }
                        if (benefitEligibilityPasses(beneficiary, benefit)) passed.add(
                            CreateBeneficiaryDTO(
                                beneficiary.id,
                                beneficiary.name,
                                beneficiary.name,
                                beneficiary.beneficiaryType,
                                beneficiary.email,
                                beneficiary.phoneNumber
                            )
                        )
                        val result = buildAndGenerateAggregate(
                            benefit,
                            passed,
                            policy,
                            category,
                            payer
                        )
                        if (result.success) {
                            val tracker = SharedBenefitTracker(
                                beneficiary = beneficiary,
                                aggregateId = result.data!!,
                                benefit = benefit
                            )
                            trackers.add(tracker)
                            benefit.processed = true
                            benefit.processedTime = LocalDateTime.now()
                            repo.save(benefit)
                        }
                    }

                beneficiary.dependants.map {
                    it.processed = true
                    it.processedTime = LocalDateTime.now()
                    beneficiaryRepo.save(it)
                }
                beneficiary.processed = true
                beneficiary.processedTime = LocalDateTime.now()
                beneficiaryRepo.save(beneficiary)
            }*/

				trackerRepo.saveAll(trackers)
				categoryRepo.save(
					category.copy(
						status = CategoryStatus.PROCESSED
					)
				)
			} else {

				val policy = this?.policy
				val benefits = repo.findMainBenefitsByCategory(category!!)
				val unprocessedFamilies =
					beneficiaryRepo.findUnprocessedFamiliesByCategory(category)
				val unprocessedDependents =
					beneficiaryRepo.findUnprocessedDependentsByCategory(category)
				val trackers = mutableSetOf<SharedBenefitTracker>()

				val unprocessedBenefits = repo.findUnprocessedMainBenefitsByCategory(category)

				if(unprocessedBenefits.isNotEmpty()){
					val families = beneficiaryRepo.findFamiliesByCategory(category)
					goThroughEachFamily(families, unprocessedBenefits, policy, category, trackers)
				}else{
					for (dep in unprocessedDependents) {

						val indivBenefits = benefits
							.stream()
							.filter { b -> b.sharing == BenefitDistribution.INDIVIDUAL }
							.collect(Collectors.toList())

						for (benefit in indivBenefits) {
							if (benefitEligibilityPasses(dep, benefit)) {
								val dto = CreateBeneficiaryDTO(
									id = dep.id,
									name = dep.name,
									memberNumber = dep.memberNumber,
									beneficiaryType = dep.beneficiaryType,
									email = dep.email,
									phoneNumber = dep.phoneNumber,
									gender = dep.gender,
									type = dep.beneficiaryType,
									jicEntityId = dep.jicEntityId,
									apaEntityId = dep.apaEntityId
								)

								val payer = benefit.payer.let { pyr ->
									return@let SchemePayerDTO(
										payerId = pyr.id,
										payerName = pyr.name
									)
								}
								buildAndGenerateAggregate(
									catalogId = benefit.benefitRef.id!!,
									benefit = benefit,
									beneficiaries = mutableSetOf(dto),
									policy = policy,
									category = category,
									payer = payer
								)
							}

						}

						val famBenefits = benefits
							.stream()
							.filter { b -> b.sharing == BenefitDistribution.FAMILY }
							.collect(Collectors.toList())

						for (benefit in famBenefits) {
							val passed = mutableSetOf<CreateBeneficiaryDTO>()

							if (benefitEligibilityPasses(dep, benefit)) {
								val dto = CreateBeneficiaryDTO(
									id = dep.id,
									name = dep.name,
									memberNumber = dep.memberNumber,
									beneficiaryType = dep.beneficiaryType,
									email = dep.email,
									phoneNumber = dep.phoneNumber,
									gender = dep.gender,
									type = dep.beneficiaryType,
									jicEntityId = dep.jicEntityId,
									apaEntityId = dep.apaEntityId
								)
								passed.add(dto)
							}

							val payer = benefit.payer.let { pyr ->
								return@let SchemePayerDTO(
									payerId = pyr.id,
									payerName = pyr.name
								)
							}

							try {
								val tracker = dep.principal?.let {
									trackerRepo.findByBeneficiaryAndBenefit(
										it,
										benefit
									)
								}

								buildAndGenerateAggregate(
									benefit.benefitRef.id!!,
									benefit, passed, policy, category, payer, aggId = tracker?.aggregateId
								)
							} catch (e: Exception) {
							}

							dep.let {
								it.processed = true
								it.processedTime = LocalDateTime.now()
								beneficiaryRepo.save(it)
							}
						}

					}

					for (family in unprocessedFamilies) {
						val members = mutableSetOf<Beneficiary>()
						members.add(family)
						members.addAll(family.dependants)

						val indivBenefits = benefits
							.stream()
							.filter { b -> b.sharing == BenefitDistribution.INDIVIDUAL }
							.collect(Collectors.toList())
						println("Individual benefit count is ${indivBenefits.size}")
						for (benefit in indivBenefits) {
							for (m in members) {
								if (benefitEligibilityPasses(m, benefit)) {
									val dto = CreateBeneficiaryDTO(
										id = m.id,
										name = m.name,
										memberNumber = m.memberNumber,
										beneficiaryType = m.beneficiaryType,
										email = m.email,
										phoneNumber = m.phoneNumber,
										gender = m.gender,
										type = m.beneficiaryType,
										jicEntityId = m.jicEntityId,
										apaEntityId = m.apaEntityId
									)
									println("Building ${benefit.name} benefit for ${m.name}")
									val payer = benefit.payer.let { pyr ->
										return@let SchemePayerDTO(
											payerId = pyr.id,
											payerName = pyr.name
										)
									}
									buildAndGenerateAggregate(
										catalogId = benefit.benefitRef.id!!,
										benefit = benefit,
										beneficiaries = mutableSetOf(dto),
										policy = policy,
										category = category,
										payer = payer
									)
								}
							}
							benefit.processed = true
							benefit.processedTime = LocalDateTime.now()
							repo.save(benefit)
						}

						val famBenefits = benefits
							.stream()
							.filter { b -> b.sharing == BenefitDistribution.FAMILY }
							.collect(Collectors.toList())
						for (benefit in famBenefits) {
							val passed = mutableSetOf<CreateBeneficiaryDTO>()
							for (m in members) {
								if (benefitEligibilityPasses(m, benefit)) {
									val dto = CreateBeneficiaryDTO(
										id = m.id,
										name = m.name,
										memberNumber = m.memberNumber,
										beneficiaryType = m.beneficiaryType,
										email = m.email,
										phoneNumber = m.phoneNumber,
										gender = m.gender,
										type = m.beneficiaryType,
										jicEntityId = m.jicEntityId,
										apaEntityId = m.apaEntityId
									)
									passed.add(dto)
								}
							}
							val payer = benefit.payer.let { pyr ->
								return@let SchemePayerDTO(
									payerId = pyr.id,
									payerName = pyr.name
								)
							}
							val result =
								buildAndGenerateAggregate(
									benefit.benefitRef.id!!,
									benefit,
									passed,
									policy,
									category,
									payer
								)
							println(result)
							if (result.success) {
								println(result)
								val tracker = SharedBenefitTracker(
									beneficiary = family,
									aggregateId = result.data!!,
									benefit = benefit
								)
								trackers.add(tracker)
								benefit.processed = true
								benefit.processedTime = LocalDateTime.now()
								repo.save(benefit)
							}
						}

						members.map {
							it.processed = true
							it.processedTime = LocalDateTime.now()
							beneficiaryRepo.save(it)
						}

					}

				}




				trackerRepo.saveAll(trackers)
				/*categoryRepo.save(
                    category.copy(
                        status = CategoryStatus.PROCESSED
                    )
                )*/

			}

		}

		return ResultFactory.getSuccessResult("Completed processing benefits for category ${category!!.name} ")
	}

	private fun goThroughEachFamily(
		families: MutableList<Beneficiary>,
		unprocessedBenefits: MutableList<Benefit>,
		policy: Policy?,
		category: Category?,
		trackers: MutableSet<SharedBenefitTracker>
	) {
		for (family in families) {
			val members = mutableSetOf<Beneficiary>()
			members.add(family)
			members.addAll(family.dependants)

			val indivBenefits = unprocessedBenefits
				.stream()
				.filter { b -> b.sharing == BenefitDistribution.INDIVIDUAL }
				.collect(Collectors.toList())
			println("Individual benefit count is ${indivBenefits.size}")
			for (benefit in indivBenefits) {
				for (m in members) {
					if (benefitEligibilityPasses(m, benefit)) {
						val dto = CreateBeneficiaryDTO(
							id = m.id,
							name = m.name,
							memberNumber = m.memberNumber,
							beneficiaryType = m.beneficiaryType,
							email = m.email,
							phoneNumber = m.phoneNumber,
							gender = m.gender,
							type = m.beneficiaryType,
							jicEntityId = m.jicEntityId,
							apaEntityId = m.apaEntityId
						)
						println("Building ${benefit.name} benefit for ${m.name}")
						val payer = benefit.payer.let { pyr ->
							return@let SchemePayerDTO(
								payerId = pyr.id,
								payerName = pyr.name
							)
						}
						buildAndGenerateAggregate(
							catalogId = benefit.benefitRef.id!!,
							benefit = benefit,
							beneficiaries = mutableSetOf(dto),
							policy = policy,
							category = category!!,
							payer = payer
						)
					}
				}
				benefit.processed = true
				benefit.processedTime = LocalDateTime.now()
				repo.save(benefit)
			}

			val famBenefits = unprocessedBenefits
				.stream()
				.filter { b -> b.sharing == BenefitDistribution.FAMILY }
				.collect(Collectors.toList())
			for (benefit in famBenefits) {
				val passed = mutableSetOf<CreateBeneficiaryDTO>()
				for (m in members) {
					if (benefitEligibilityPasses(m, benefit)) {
						val dto = CreateBeneficiaryDTO(
							id = m.id,
							name = m.name,
							memberNumber = m.memberNumber,
							beneficiaryType = m.beneficiaryType,
							email = m.email,
							phoneNumber = m.phoneNumber,
							gender = m.gender,
							type = m.beneficiaryType,
							jicEntityId = m.jicEntityId,
							apaEntityId = m.apaEntityId
						)
						passed.add(dto)
					}
					/**
					 * Here, no need to test principal member since they have already been added to members set
					 */
					/*if (benefitEligibilityPasses(family, benefit)) {
								passed.add(
									CreateBeneficiaryDTO(
										id = family.id, name = family.name,
										memberNumber = family.memberNumber, beneficiaryType = family.beneficiaryType,
										email = family.email, phoneNumber = family.phoneNumber
									)
								)
							}*/
				}
				val payer = benefit.payer.let { pyr ->
					return@let SchemePayerDTO(
						payerId = pyr.id,
						payerName = pyr.name
					)
				}
				val result =
					buildAndGenerateAggregate(
						benefit.benefitRef.id!!,
						benefit,
						passed,
						policy,
						category!!,
						payer
					)
				println(result)
				if (result.success) {
					println(result)
					val tracker = SharedBenefitTracker(
						beneficiary = family,
						aggregateId = result.data!!,
						benefit = benefit
					)
					trackers.add(tracker)
					benefit.processed = true
					benefit.processedTime = LocalDateTime.now()
					repo.save(benefit)
				}
			}

			members.map {
				it.processed = true
				it.processedTime = LocalDateTime.now()
				beneficiaryRepo.save(it)
			}

		}
	}

	fun buildAndGenerateAggregate(
		catalogId: Long,
		benefit: Benefit,
		beneficiaries: MutableSet<CreateBeneficiaryDTO>,
		policy: Policy?,
		category: Category,
		payer: SchemePayerDTO,
		aggId: String? = null
	): Result<String> {
		var subBenefits = mutableSetOf<SubBenefitDTO>()
		benefit.subBenefits.forEach { sub ->
			val subDTO = SubBenefitDTO(
				name = sub.name,
				balance = sub.limit,
				startDate = LocalDate.from(policy!!.startDate.plus(sub.waitingPeriod.period)),
				endDate = policy.endDate,
				suspensionThreshold = sub.suspensionThreshold,
				benefitId = sub.id,
				gender = sub.applicableGender,
				memberType = sub.applicableMember,
				catalogId = sub.benefitRef!!.id!!
			)
			subBenefits.add(subDTO)
		}
		val dto = CreateBenefitDTO(
			aggregateId = aggId ?: UUID.randomUUID().toString(),
			benefitName = benefit.name,
			balance = benefit.limit,
			suspensionThreshold = benefit.suspensionThreshold,
			beneficiaries = beneficiaries,
			startDate = policy!!.startDate.plus(benefit.waitingPeriod.period),
			endDate = policy.endDate,
			categoryId = category.id,
			payer = payer,
			policyNumber = policy.policyNumber,
			subBenefits = subBenefits,
			benefitId = benefit.id,
			catalogId = benefit.benefitRef!!.id!!
		)
		println("About to send benefits over the wire: ${dto.benefitName} with aggregateId: ${dto.aggregateId}")
		println(gson.toJson(dto))

		return if (processFamilyBenefits(dto)) {
			ResultFactory.getSuccessResult(msg = dto.aggregateId, data = dto.aggregateId!!)
		} else {
			ResultFactory.getFailResult("Failed to publish")
		}
	}

	private fun benefitEligibilityPasses(ben: Beneficiary, benefit: Benefit): Boolean {

		return typePasses(beneficiaryType = ben.beneficiaryType, benefit = benefit) && genderPasses(
			gender = ben.gender, benefit = benefit
		)
	}

	private fun typePasses(beneficiaryType: BeneficiaryType, benefit: Benefit): Boolean {
		return when (benefit.applicableMember) {
			ApplicableMember.ALL -> true
			ApplicableMember.PRINCIPAL -> (beneficiaryType == BeneficiaryType.PRINCIPAL)
			ApplicableMember.SPOUSE -> (beneficiaryType == BeneficiaryType.SPOUSE)
			ApplicableMember.CHILD -> (beneficiaryType == BeneficiaryType.CHILD)
			ApplicableMember.PARENT -> (beneficiaryType == BeneficiaryType.PARENT)
			ApplicableMember.PRINCIPAL_AND_SPOUSE -> {
				(beneficiaryType == BeneficiaryType.PRINCIPAL || beneficiaryType == BeneficiaryType.SPOUSE)
			}
		}
	}

	private fun genderPasses(gender: Gender, benefit: Benefit): Boolean {
		if (benefit.applicableGender.name == gender.name) return true
		if (benefit.applicableGender == ApplicableGender.ALL) return true
		return false
	}

	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun processBenefit(benefit: Benefit): Result<Benefit> {
		return ResultFactory.getFailResult("Not yet implemented")
	}


	fun processFamilyBenefits(dto: CreateBenefitDTO): Boolean {
		// Sending to benefit management service
		var result = false
//        LOG.debug("Building: ${dto.benefitName} for ${dto.beneficiaries}")
		/////println(Gson().toJson(dto).toString())

		/*Kafka */
		/*var dtostring: String = objectMapper.writeValueAsString(dto)

		println(dtostring)


		println("----------------------------------------")


		kafka.send(benefitTopic, dtostring).addCallback({
			println("success")
			result = true
		}, {
			result = false
			println("fail")
		})*/
		/*Kafka */


		// val avroRecord = Avro.default.toRecord(CreateBenefitDTO.serializer(), dto)
//        val future = kafka.send(
//            benefitTopic,
//            avroRecord
//        ).addCallback({
//            println("Sent to bus at: ${it!!.recordMetadata.timestamp()}")
//            result = true
//        }, {
//            println("Failed with error: ${it.message}")
//            result = false
//        })

		println(benefitUrl)


        //val http = WebClient.create(benefitUrl)
		val http = webClient(benefitUrl)

		http.post()
			.uri(createBenefitEndpoint)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			//.body(Mono.just(mapper.writeValueAsString(dto)), String::class.java)
			.body(Mono.just(gson.toJson(dto)), String::class.java)
			.exchange()
			.doOnSuccess { res ->
				println("-------------------------------")
				println(res)
				result = true
			}
			.block()!!.toString()

		return result
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun processNewMembers(categoryId: Long): Result<Benefit> {
		TODO("Not yet implemented")
	}
	@Transactional(rollbackFor = [Exception::class])
	override fun changeCategory(dto: ChangeCategoryDTO): Result<Boolean> {
		val oldCategory = categoryRepo.findById(dto.oldCategoryId)
		if (oldCategory.isEmpty) return ResultFactory.getFailResult(
			"No Previous Category with ID ${dto.oldCategoryId} was " +
					"found"
		)
		val newCategory = categoryRepo.findById(dto.newCategoryId)
		if (newCategory.isEmpty) return ResultFactory.getFailResult(
			"No New Category with ID ${dto.newCategoryId} was" +
					" found"
		)

		val category = categoryRepo.fetchWithPolicy(dto.newCategoryId)

		with(category) {

			val policy = this?.policy
			val family =
				beneficiaryRepo.findFamilyByCategoryAndMemberNumber(oldCategory.get(), dto.memberNumber)
			val mainBenefits = repo.findMainBenefitsByCategory(category!!)
			val trackers = mutableSetOf<SharedBenefitTracker>()
			for (member in family) {
				val members = mutableSetOf<Beneficiary>()
				members.add(member)
				members.addAll(member.dependants)

				val indivBenefits = mainBenefits
					.stream()
					.filter { b -> b.sharing == net.lctafrica.membership.api.domain.BenefitDistribution.INDIVIDUAL }
					.collect(java.util.stream.Collectors.toList())
				println("Individual benefit count is ${indivBenefits.size}")
				for (benefit in indivBenefits) {
					for (m in members) {
						if (benefitEligibilityPasses(m, benefit)) {
							val dto = CreateBeneficiaryDTO(
								id = m.id,
								name = m.name,
								memberNumber = m.memberNumber,
								beneficiaryType = m.beneficiaryType,
								email = m.email,
								phoneNumber = m.phoneNumber,
								gender = m.gender,
								type = m.beneficiaryType,
								jicEntityId = m.jicEntityId,
								apaEntityId = m.apaEntityId
							)
							println("Building ${benefit.name} benefit for ${m.name}")
							val payer = benefit.payer.let { pyr ->
								return@let SchemePayerDTO(
									payerId = pyr.id,
									payerName = pyr.name
								)
							}
							buildAndGenerateAggregate(
								catalogId = benefit.benefitRef.id!!,
								benefit = benefit,
								beneficiaries = mutableSetOf(dto),
								policy = policy,
								category = category,
								payer = payer
							)
						}
					}
					benefit.processed = true
					benefit.processedTime = java.time.LocalDateTime.now()
					repo.save(benefit)
				}

				val famBenefits = mainBenefits
					.stream()
					.filter { b -> b.sharing == net.lctafrica.membership.api.domain.BenefitDistribution.FAMILY }
					.collect(java.util.stream.Collectors.toList())
				for (benefit in famBenefits) {
					val passed = mutableSetOf<CreateBeneficiaryDTO>()
					for (m in members) {
						if (benefitEligibilityPasses(m, benefit)) {
							val dto = CreateBeneficiaryDTO(
								id = m.id,
								name = m.name,
								memberNumber = m.memberNumber,
								beneficiaryType = m.beneficiaryType,
								email = m.email,
								phoneNumber = m.phoneNumber,
								gender = m.gender,
								type = m.beneficiaryType,
								jicEntityId = m.jicEntityId,
								apaEntityId = m.apaEntityId
							)
							passed.add(dto)
						}

					}
					val payer = benefit.payer.let { pyr ->
						return@let SchemePayerDTO(
							payerId = pyr.id,
							payerName = pyr.name
						)
					}
					val result = buildAndGenerateAggregate(
						benefit.benefitRef.id!!,
						benefit,
						passed,
						policy,
						category,
						payer
					)
					println(result)

					if (result.success) {
						println(result)
						val tracker = SharedBenefitTracker(
							beneficiary = member,
							aggregateId = result.data!!,
							benefit = benefit
						)
						trackers.add(tracker)
						benefit.processed = true
						benefit.processedTime = java.time.LocalDateTime.now()
						repo.save(benefit)
					}
				}

				for(m in members){
					suspendCurrentBenefits(m.id, m.category.id)
				}
				members.map {
					it.category = category
					it.processedTime = java.time.LocalDateTime.now()
					beneficiaryRepo.save(it)
				}

			}
		}
		return ResultFactory.getSuccessResult(data = true)
	}

	override fun findPayersByBenefitIds(benefitIds: Collection<Long>): Result<MutableList<BenefitPayerMappingDTO>> {
		return ResultFactory.getSuccessResult(repo.findPayersByBenefitIds(benefitIds))
	}
	private fun suspendCurrentBenefits(beneficiaryId: Long, categoryId: Long):Boolean {

		val http = webClient(benefitUrl)
		var result = false


		 http.post()
			.uri(suspendBenefits)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			//.body(Mono.just(mapper.writeValueAsString(dto)), String::class.java)
			.body(Mono.just(gson.toJson(DeactivateBenefitDTO(beneficiaryId,categoryId))), String::class.java)
			.exchange()
			.doOnSuccess { res ->
				println("-------------------------------")
				println(res)
				result = true
			}
			 .doOnError {
			 	result = false
			 }
			.block()!!.toString()

		return result


	}
}

