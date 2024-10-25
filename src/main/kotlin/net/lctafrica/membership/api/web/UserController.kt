package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.dtos.AdminUserDTO
import net.lctafrica.membership.api.dtos.BenefitCatalogDTO
import net.lctafrica.membership.api.dtos.CashierUserDTO
import net.lctafrica.membership.api.dtos.PayerUserDTO
import net.lctafrica.membership.api.service.JubileeService
import net.lctafrica.membership.api.service.ProviderService
import net.lctafrica.membership.api.service.UserManagementService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/membership")
class UserController(val userManagementService: UserManagementService) {

	@PostMapping(value = ["/user/addAdmin"], produces = ["application/json"])
	fun addAdmin(@Valid @RequestBody dto: AdminUserDTO) = userManagementService.addAdminUser(dto)

	@PostMapping(value = ["/user/addPayer"], produces = ["application/json"])
	fun addPayer(@Valid @RequestBody dto: PayerUserDTO) = userManagementService.addPayerUser(dto)

	@PostMapping(value = ["/user/addCashier"], produces = ["application/json"])
	fun addCashier(@Valid @RequestBody dto: CashierUserDTO) = userManagementService.addUserCashier(dto)

	@GetMapping(value = ["/user/provider/{providerId}"], produces = ["application/json"])
	fun getProviderUsers(@PathVariable(value = "providerId") providerId: Long) =
		userManagementService.getProviderUsers(providerId)

	@GetMapping(value = ["/user/payer/{payerId}"], produces = ["application/json"])
	fun getPayerUsers(@PathVariable(value = "payerId") payerId: Long) =
		userManagementService.getPayerUsers(payerId)

}