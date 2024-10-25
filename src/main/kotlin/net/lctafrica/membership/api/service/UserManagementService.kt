package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.dtos.AdminUserDTO
import net.lctafrica.membership.api.dtos.CashierUserDTO
import net.lctafrica.membership.api.dtos.PayerUserDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import javax.ws.rs.core.Response

@Service
@Transactional
class UserManagementService(
	@Value("\${lct-africa.keycloak.serverUrl}")
	var serverUrl: String,
	@Value("\${lct-africa.keycloak.keycloakRealm}")
	var keycloakRealm: String,
	@Value("\${lct-africa.keycloak.realm}")
	var realm: String,
	@Value("\${lct-africa.keycloak.keycloakClient}")
	var keycloakClient: String,
	@Value("\${lct-africa.keycloak.clientSecret}")
	var clientSecret: String
) :IUserManagementService {


	var keycloak = KeycloakBuilder.builder() //
		.serverUrl(serverUrl)
		.realm(realm)
		.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
		.clientId(keycloakClient)
		.clientSecret(clientSecret)
		.build()!!

	@Transactional(rollbackFor = [Exception::class])
	override fun addUserCashier(dto: CashierUserDTO): Result<String> {
		val credentials = CredentialRepresentation()
		credentials.type = CredentialRepresentation.PASSWORD
		credentials.value = dto.password
		credentials.isTemporary = false
		val userRepresentation = UserRepresentation()
		userRepresentation.username = dto.username
		userRepresentation.email = dto.email
		userRepresentation.firstName = dto.firstName
		userRepresentation.lastName = dto.lastName
		userRepresentation.credentials = Arrays.asList(credentials)
		userRepresentation.isEnabled = true
		val attributes: MutableMap<String, List<String>> = HashMap()
		attributes["providerId"] = listOf(dto.providerId.toString())
		attributes["providerName"] = listOf(dto.providerName.toString())
		userRepresentation.attributes = attributes
		val response:Response = keycloak.realm(keycloakRealm).users().create(userRepresentation);

		return if (HttpStatus.valueOf(response.status).is2xxSuccessful){
			val locationHeader = response.getHeaderString("Location")
			val userResource : UserResource = getLctUsersResource()!!
				.get(
					locationHeader
						.replace(
							".*/(.*)$".toRegex(),
							"$1"
						)
				)
			setRealmRole(userResource, "Cashier")
			ResultFactory.getSuccessResult(msg = "Cashier Created Successfully")
		}else{
			ResultFactory.getFailResult(msg = "Could not Create user ")
		}

	}

	@Transactional(rollbackFor = [Exception::class])
	override fun addPayerUser(dto: PayerUserDTO): Result<PayerUserDTO> {
		val credentials = CredentialRepresentation()
		credentials.type = CredentialRepresentation.PASSWORD
		credentials.value = dto.password
		credentials.isTemporary = false
		val userRepresentation = UserRepresentation()
		userRepresentation.username = dto.username
		userRepresentation.email = dto.email
		userRepresentation.firstName = dto.firstName
		userRepresentation.lastName = dto.lastName
		userRepresentation.credentials = Arrays.asList(credentials)
		userRepresentation.isEnabled = true
		val attributes: MutableMap<String, List<String>> = HashMap()
		attributes["payerId"] = listOf(dto.payerId.toString())
		userRepresentation.attributes = attributes
		val response:Response = keycloak.realm(keycloakRealm).users().create(userRepresentation);

		return if (HttpStatus.valueOf(response.status).is2xxSuccessful){
			val locationHeader = response.getHeaderString("Location")
			val userResource : UserResource = getLctUsersResource()!!
				.get(
					locationHeader
						.replace(
							".*/(.*)$".toRegex(),
							"$1"
						)
				)
			setRealmRole(userResource, "ADMIN")
			setRealmRole(userResource, "PAYER")
			ResultFactory.getSuccessResult(msg = "Payer Admin Created Successfully")
		}else{
			ResultFactory.getFailResult(msg = "Could not Create Payer Admin ")
		}
	}

	@Transactional(rollbackFor = [Exception::class])
	override fun addAdminUser(dto: AdminUserDTO): Result<AdminUserDTO> {
		TODO("Not yet implemented")
	}

	override fun getProviderUsers(providerId: Long): List<UserRepresentation?>? {
		var matchList: List<UserRepresentation?>? = mutableListOf()
		val list2 = getAllUsers()

		if(list2!!.isNotEmpty()){
			try {
				var res = filterProviderUsers(list2,providerId)
				println(res)
				return res
			} catch (e: Exception) {
				println(e.message)
			}
		}else{
			return null
		}
		return matchList
	}
	override fun getPayerUsers(payerId: Long): List<UserRepresentation?>? {
		var matchList: List<UserRepresentation?>? = mutableListOf()
		val list2 = getAllUsers()

		if(list2!!.isNotEmpty()){
			try {
				var res = filterProviderUsers(list2,payerId)
				println(res)
				return res
			} catch (e: Exception) {
				println(e.message)
			}
		}else{
			return null
		}
		return matchList
	}

	fun getAllUsers():List<UserRepresentation?>?{
		val list: List<UserRepresentation?>? = getUsersRepresentation(null)
		return list
	}
	fun filterProviderUsers(users: List<UserRepresentation?>, providerId: Long):List<UserRepresentation?>?{
		val matchList:MutableList<UserRepresentation> = mutableListOf()
		for (u in users) {
			try {
				if (u!!.attributes["providerId"]!![0].toLong() == providerId) {
					matchList.add(u)
				}
			} catch (e: Exception) {
				println(e.message)
			}
		}
		println(matchList)
		return matchList
	}
	fun filterPayerUsers(users: List<UserRepresentation?>, payerId: Long)
	:List<UserRepresentation?>?{
		val matchList:MutableList<UserRepresentation> = mutableListOf()
		for (u in users) {
			try {
				if (u!!.attributes["payerId"]!![0].toLong() == payerId) {
					matchList.add(u)
				}
			} catch (e: Exception) {
				println(e.message)
			}
		}
		println(matchList)
		return matchList
	}
	private fun getUsersRepresentation(email: String?): List<UserRepresentation?>? {
		val usersResource: UsersResource? = getLctUsersResource()
		return usersResource!!.search(email, 0, usersResource.count())
	}

	private fun getLctRealmResource(): RealmResource? {
		return keycloak.realm(realm)
	}
	private fun getLctUsersResource(): UsersResource? {
		return getLctRealmResource()!!.users()
	}
	private fun removeRealmRoles(userResource: UserResource) {
		println(userResource.roles().realmLevel().listAll())
		val realmRoleRepresentations = userResource.roles().realmLevel().listAll()
		userResource.roles().realmLevel().remove(realmRoleRepresentations)
	}

	private fun setRealmRole(userResource: UserResource, role: String) {
		//removeRealmRoles(userResource)
		val lctRealmRoles: List<RoleRepresentation> = getLctRealmResource()!!.roles().list()
//
		val newRole = lctRealmRoles.stream()
			.filter(isNotDefaultRealmRole("offline_access"))
			.filter(isNotDefaultRealmRole("uma_authorization"))
			.filter(isRealmRole(role))
			.collect(Collectors.toList())
		println(newRole)
		userResource.roles().realmLevel().add(newRole)
	}

	private fun isRealmRole(roleName: String): Predicate<RoleRepresentation>? {
		return Predicate { role: RoleRepresentation -> role.name == roleName }
	}

	private fun isNotDefaultRealmRole(realmRoleName: String): Predicate<RoleRepresentation>? {
		return Predicate { role: RoleRepresentation -> role.name != realmRoleName }
	}

	private fun isLoggedIn(usersResource: UsersResource): Predicate<UserRepresentation>? {
		return Predicate { u: UserRepresentation ->
			!usersResource[u.id].userSessions.isEmpty()
		}
	}


}