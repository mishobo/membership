package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.*
import java.time.LocalDateTime
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.gson
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service("beneficiaryService")
@Transactional
class BeneficiaryService(
	private val repo: BeneficiaryRepository,
	private val categoryRepo: CategoryRepository,
	private val linkRepo: BeneficiaryLinkRepository,
	private val payerRepository: PayerRepository,
	private val exclusionService: IProviderExclusionService,
) : IBeneficiaryService {

	@Value("\${lct-africa.benefit.command.base-url}")
	lateinit var benefitUrl: String

	@Value("\${lct-africa.member.url}")
	lateinit var memberSearchUrl: String

	@Transactional(readOnly = true)
	override fun findByCategory(categoryId: Long, page: Int, size: Int): Result<Page<Beneficiary>> {
		val request = PageRequest.of(page - 1, size)
		val category = categoryRepo.findById(categoryId)
		if (category.isEmpty) return ResultFactory.getFailResult("No category with ID $categoryId was found")
		return category.map { repo.findByCategory(it, request) }
			.map {
				ResultFactory.getSuccessResult(it)
			}.get()
	}

	@Transactional(readOnly = true)
	override fun findByNameOrMemberNumber(search: String): Result<MutableList<Beneficiary>> {
		val trimmed = search.trim()
		val found = repo.searchByNameOrMemberNumberAndActive(trimmed)
		return ResultFactory.getSuccessResult(found)
	}

	@Transactional(readOnly = true)
	override fun searchByNameOrMemberNumber(search: String): Result<MutableList<Beneficiary>> {
		val trimmed = search.trim()
		val found = repo.searchByNameOrMemberNumberAndActive(trimmed)
		return ResultFactory.getSuccessResult(found)
	}

//	override fun findByNameOrMemberNumberActive(search: String, providerId: Long):
//			Result<MutableList<Beneficiary>> {
//		val trimmed = search.trim()
//		val found = repo.searchByNameOrMemberNumberAndActive(trimmed)
//		if (found.size > 0) {
//			found.forEach {
//				val preAuthRes = exclusionService.findExclusionsByCategoryAndProvider(it.category.id, providerId)
//				return if (preAuthRes.success) {
//					ResultFactory.getFailResult(
//						msg = "An exclusion exists for Category ${
//							it
//								.category.name
//						} at this provider"
//					)
//
//				} else {
//					ResultFactory.getSuccessResult(data = found)
//				}
//			}
//		} else {
//			return ResultFactory.getFailResult(msg = "No such Member has been Found")
//		}
//		return ResultFactory.getFailResult(msg = "Please try again ")
//	}

	override fun findByNameOrMemberNumberActive(search: String): Result<MutableList<Beneficiary>> {
		val trimmed = search.trim()
		val found = repo.searchByNameOrMemberNumberAndActive(trimmed)
		return ResultFactory.getSuccessResult(found)
	}


	override fun findByPayerIdNameOrMemberNumber(
		payerId: Long,
		search: String
	): Result<MutableList<Beneficiary>> {
		val trimmed = search.trim()
		val found: MutableList<Beneficiary> =
			repo.searchByPayerIdNameOrMemberNumber(payerId, trimmed)

		return ResultFactory.getSuccessResult(found)
	}

	override fun findByPayerIdNameOrMemberNumberMemberStatus(
		payerId: Long,
		search: String
	): Result<MutableList<Beneficiary>> {
		val trimmed = search.trim()
		val found: MutableList<Beneficiary> =
			repo.searchByPayerIdNameOrMemberNumberMemberStatus(payerId, trimmed)

		return ResultFactory.getSuccessResult(found)
	}

	override fun findByPayerIdPlanIdNameOrMemberNumber(
		payerId: Long,
		search: String,
		planId: Long
	): Result<MutableList<PayerSearchDTO>> {
		val trimmed = search.trim()
		val found: MutableList<PayerSearchDTO> =
			repo.searchByPayerIdPlanIdNameOrMemberNumber(payerId, trimmed, planId)

		return ResultFactory.getSuccessResult(found)
	}


	@Transactional(readOnly = false, rollbackFor = [Exception::class])
	override fun addBeneficiary(dto: BeneficiaryDTO): Result<Beneficiary> {
		var principalId = dto.principalId
		var principal: Beneficiary? = null
		if (principalId != null && principalId > 0) {
			val optPrincipal = repo.findById(principalId)
			if (optPrincipal.isEmpty) return ResultFactory.getFailResult("No Principal member with ID $principalId was found")
			principal = optPrincipal.get()
		}
		val category = categoryRepo.findById(dto.categoryId)
		if (category.isPresent) {
			val byCategoryAndMemberNumber =
				repo.findByCategoryAndBeneficiary(category.get(), dto.memberNumber);
			if (byCategoryAndMemberNumber.isPresent) return ResultFactory.getFailResult("Member number already exists in the category")
		}
		try {

			return category.map { cat ->
				return@map Beneficiary(
					principal = principal,
					name = dto.name.trim(),
					memberNumber = dto.memberNumber.trim(),
					nhifNumber = dto.nhifNumber,
					dob = dto.dob,
					gender = dto.gender,
					beneficiaryType = dto.beneficiaryType,
					phoneNumber = dto.phoneNumber,
					email = dto.email,
					category = cat,
					processed = false,
					processedTime = null,
					jicEntityId = dto.jicEntityId,
					apaEntityId = dto.apaEntityId,
				)
			}.map {
				repo.save(it)
				ResultFactory.getSuccessResult(it)
			}.get()
		} catch (ex: IllegalArgumentException) {
			return ResultFactory.getFailResult(ex.message)
		}
	}

	@Transactional(readOnly = true)
	override fun findPrincipalsByCategory(categoryId: Long): Result<MutableList<Beneficiary>> {
		val category = categoryRepo.findById(categoryId)
		if (category.isEmpty) return ResultFactory.getFailResult("No category with ID $categoryId was found")
		return category.map {
			repo.findPrincipalsByCategory(it)
		}.map { list ->
			return@map ResultFactory.getSuccessResult(list)
		}.get()
	}

	override fun addBeneficiaries(
		categoryId: Long,
		dtos: List<BeneficiaryDTO>
	): Result<MutableList<Beneficiary>> {
		TODO("Not yet implemented")
	}

	override fun removeBeneficiary(beneficiary: Beneficiary): Result<Beneficiary> {
		TODO("Not yet implemented")
	}

	@Transactional(readOnly = true)
	override fun findBeneficiaries(
		phoneNumber: String, beneficiaryType: String, page: Int, size: Int
	): Result<List<BeneficiaryQueryDto>> {
		val request = PageRequest.of(page - 1, size)
		val covers =
			if (beneficiaryType.lowercase() == BeneficiaryType.PRINCIPAL.name.lowercase()) {
				repo.findPrincipalCoversByPhoneNumber(
					phoneNumber,
					BeneficiaryType.PRINCIPAL,
					request
				)
			} else {
				repo.findLinkedDependantCoversByPhoneNumber(phoneNumber, request)
			}
		val list = mutableListOf<BeneficiaryQueryDto>()
		covers.forEach {
			val payers = payerRepository.findPayerByBeneficiaryId(it.id)
			list.add(it.toBeneficiaryDto(payers))
		}
		return ResultFactory.getSuccessResult(list)
	}

	@Transactional(readOnly = true)
	override fun findCoversByPhoneNumber(
		phoneNumber: String, page: Int, size: Int
	): Result<List<BeneficiaryQueryDto>> {
		val pg = page - 1
		val covers = repo.findCoversByPhoneNumber(phoneNumber, pg, size)
		val list = mutableListOf<BeneficiaryQueryDto>()
		covers.forEach {
			val payers = payerRepository.findPayerByBeneficiaryId(it.id)
			list.add(it.toBeneficiaryDto(payers))
		}
		return ResultFactory.getSuccessResult(list)
	}

	override fun findCoverBeneficiaries(beneficiaryId: Long): Result<List<Beneficiary>> {
		val principal = repo.findById(beneficiaryId)
		if (!principal.isPresent) {
			return ResultFactory.getFailResult("Invalid Beneficiary Id")
		}
		val beneficiaries = mutableListOf<Beneficiary>()
		beneficiaries.add(principal.get())
		beneficiaries.addAll(principal.get().dependants)
		return ResultFactory.getSuccessResult(beneficiaries)
	}

	override fun linkCover(dto: LinkCardDto): Result<BeneficiaryLink> {
		val linkOptional =
			linkRepo.findByPhoneNumberAndBeneficiary_Id(dto.phoneNumber, dto.beneficiaryId)
		if (linkOptional.isPresent) {
			val link = linkOptional.get()
			link.status = dto.link
			link.updatedOn = LocalDateTime.now()
			linkRepo.save(link)
			return ResultFactory.getSuccessResult(link)
		} else {
			val beneficiaryOptional = repo.findById(dto.beneficiaryId)
			if (!beneficiaryOptional.isPresent) {
				return ResultFactory.getFailResult("Invalid Beneficiary Id")
			}
			val link = BeneficiaryLink(
				beneficiary = beneficiaryOptional.get(),
				status = dto.link,
				phoneNumber = dto.phoneNumber
			)
			linkRepo.save(link)
			return ResultFactory.getSuccessResult(link)
		}
	}

	override fun getBeneficiaryLinkedCovers(beneficiaryId: Long): Result<List<BeneficiaryLink>> {
		return ResultFactory.getSuccessResult(
			linkRepo.findByBeneficiary_IdAndStatus(
				beneficiaryId,
				true
			)
		)
	}

	override fun updateMember(dto: UpdateMemberDTO): Result<Beneficiary> {
		val member = repo.findById(dto.id)
		if (member.isEmpty) return ResultFactory.getFailResult(msg = "No member with id ${dto.id}")

		return if (member.isPresent) {
			member.get().apply {
				name = dto.name.toString()
				phoneNumber = dto.phoneNumber
				email = dto.email
				dob = dto.dob!!
			}
			val savedmember = repo.save(member.get())
			ResultFactory.getSuccessResult(
				data = savedmember, msg = "Member updated " +
						"successfully"
			)
		} else {
			ResultFactory.getFailResult(msg = "No member present")
		}
	}

	override fun activateBeneficiary(beneficiaryId: Long): Result<Beneficiary> {
		val claimsClient = WebClient.builder()
			.baseUrl(benefitUrl).build()
		val member = repo.findById(beneficiaryId)
		if (member.isEmpty) return ResultFactory.getFailResult(msg = "No member with id $beneficiaryId exists")

		return if (member.isPresent) {
			val updatedMember = member.get().apply {
				this.status = MemberStatus.ACTIVE
			}
			claimsClient
				.post()
				.uri { u ->
					u
						.path("/api/v1/visit/activateBenefits/")
						.build()
				}
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(
					Mono.just(
						gson.toJson(
							DeactivateBenefitDTO(
								beneficiaryId = member.get().id,
								categoryId = member.get().category.id
							)
						)
					),
					String::class.java
				)
				.exchange()
				.doOnSuccess { res ->
					println("-------------------------------")
					println(res)
				}
				.block()!!.toString()
			ResultFactory.getSuccessResult(
				data = repo.save(updatedMember), msg = "Member " +
						"Activated Successfully"
			)
		} else {
			ResultFactory.getFailResult(msg = "No member with id $beneficiaryId exists")
		}

	}

	override fun deactivateBeneficiary(beneficiaryId: Long): Result<Beneficiary> {
		val member = repo.findById(beneficiaryId)
		if (member.isEmpty) return ResultFactory.getFailResult(msg = "No member with id $beneficiaryId exists")

		return if (member.isPresent) {

			val claimsClient = WebClient.builder()
				.baseUrl(benefitUrl).build()
			val updatedMember = member.get().apply {
				this.status = MemberStatus.INACTIVE
			}

			claimsClient
				.post()
				.uri { u ->
					u
						.path("/api/v1/visit/deactivateBenefits/")
						.build()
				}
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(
					Mono.just(
						gson.toJson(
							DeactivateBenefitDTO(
								beneficiaryId = member.get().id,
								categoryId = member.get().category.id
							)
						)
					),
					String::class.java
				)
				.exchange()
				.doOnSuccess { res ->
					println("-------------------------------")
					println(res)
				}
				.block()!!.toString()

			ResultFactory.getSuccessResult(
				data = repo.save(updatedMember), msg = "Member " +
						"Deactivated Successfully"
			)
		} else {
			ResultFactory.getFailResult(msg = "No member with id $beneficiaryId exists")
		}
	}

	@Transactional(readOnly = true)
	override fun findByCategoryAndName(
		categoryId: Long,
		beneficiaryName: String,
		page: Int,
		size: Int
	): Result<Page<Beneficiary>> {
		val category = categoryRepo.findById(categoryId)
		if (category.isPresent) {
			val request = PageRequest.of(page - 1, size);
			val benefits =
				repo.findByCategoryAndBeneficiaryName(category.get(), beneficiaryName, request)
			return ResultFactory.getSuccessResult(benefits)
		}
		return ResultFactory.getFailResult("No category with ID $categoryId was found")
	}

}