package net.lctafrica.membership.api.web

import io.swagger.v3.oas.annotations.Operation
import net.lctafrica.membership.api.domain.Category
import net.lctafrica.membership.api.domain.Copayment
import net.lctafrica.membership.api.domain.ProviderExclusion
import net.lctafrica.membership.api.domain.ProviderTier
import net.lctafrica.membership.api.dtos.*
import net.lctafrica.membership.api.service.ICopaySetupService
import net.lctafrica.membership.api.service.IProviderExclusionService
import net.lctafrica.membership.api.service.IProviderService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid


@RestController
@RequestMapping("/api/v1/provider")
@CrossOrigin("*","*")
class ProviderController(
    val providerService: IProviderService,
    val exclusionService: IProviderExclusionService,
    val copaySetupService: ICopaySetupService
) : AbstractController(){

    @GetMapping(produces = ["application/json"])
    fun findProviderById(@RequestParam("providerId") providerId: Long) =
        providerService.findById(providerId)

    @PostMapping(value = ["/"], produces = ["application/json"])
    fun addProvider(@Valid @RequestBody dto: ProviderDTO) = providerService.addProvider(dto)

    @GetMapping(value = ["/tier"], produces = ["application/json"])
    fun findByTier(@RequestParam("tier") tier: ProviderTier, page: Int = 1, size: Int = 10) =
        providerService.findByTier(tier, page, size)

    @GetMapping(value = ["/name"], produces = ["application/json"])
    fun findByTier(@RequestParam("name") name: String, page: Int = 1, size: Int = 10) =
        providerService.findByName(name, page, size)

    @GetMapping(value = ["/all"], produces = ["application/json"])
    fun findAllProviders(page: Int = 1, size: Int = 10) =
        providerService.findAllProviders(page, size)

    @PostMapping(value = ["/exclusion"], produces = ["application/json"])
    fun addExclusion(@Valid @RequestBody dto: ExclusionDTO) = exclusionService.addExclusion(dto)

    @PostMapping(value = ["/exclusion/deactivate"], produces = ["application/json"])
    fun deactivate(@Valid @RequestBody exclusion: ProviderExclusion) = exclusionService.deactivate(exclusion)

    @PostMapping(value = ["/exclusions/process"], produces = ["application/json"])
    fun processExclusion(@Valid @RequestBody category: Category) = exclusionService.process(category)

    @GetMapping(value = ["/{providerId}/exclusions"], produces = ["application/json"])
    fun findExclusionsByProvider(@PathVariable(value = "providerId") providerId: Long) =
        exclusionService.findExclusionsByProvider(providerId)

    @GetMapping(value = ["/category/{categoryId}/exclusions"], produces = ["application/json"])
    fun findExclusionsByCategory(@PathVariable(value = "categoryId") categoryId: Long, page: Int = 1, size: Int = 10) =
        exclusionService.findExclusionsByCategory(categoryId, page, size)


    @GetMapping(value = ["/category/{categoryId}/provider/{providerId}/exclusions"], produces =
    ["application/json"])
    fun findExclusionsByCategoryAndProvider(@PathVariable(value = "categoryId") categoryId: Long,
                                            @PathVariable(value = "providerId") providerId: Long) =
        exclusionService.findExclusionsByCategoryAndProvider(categoryId, providerId)

    @PostMapping(value = ["/copay"], produces = ["application/json"])
    fun addCopay(@Valid @RequestBody dto: CopayDTO) = copaySetupService.addCopay(dto)

    @GetMapping(value = ["/copay"], produces = ["application/json"])
    fun findByProviderAndCategory(
        @RequestParam("providerId") providerId: Long,
        @RequestParam("categoryId") categoryId: Long
    ) = copaySetupService.findByCategoryAndProvider(categoryId, providerId)

    @PutMapping(value = ["/copay"], produces = ["application/json"])
    fun deactivateCopay(@RequestBody copayment: Copayment) = copaySetupService.deactivate(copayment)

    @PostMapping(value = ["/copays/process"], produces = ["application/json"])
    fun processCoPays(@Valid @RequestBody category: Category) = copaySetupService.process(category)

    @GetMapping(value = ["/{providerId}/copays"], produces = ["application/json"])
    fun findCoPaysByProvider(@PathVariable(value = "providerId") providerId: Long) =
        copaySetupService.findByProvider(providerId)

    @GetMapping(value = ["/category/{categoryId}/copays"], produces = ["application/json"])
    fun findCoPaysByCategory(@PathVariable(value = "categoryId") categoryId: Long) =
        copaySetupService.findByCategory(categoryId)

    @GetMapping(value = ["/{providerId}/whitelist"], produces = ["application/json"])
    fun findWhiteListByProvider(@PathVariable(value = "providerId") providerId: Long, page: Int = 1, size: Int = 10) =
        providerService.findWhiteListByProvider(providerId, page, size)

    @PostMapping(value = ["/whitelist"], produces = ["application/json"])
    fun addNewMapping(@Valid @RequestBody dto: WhiteListDTO) =
        providerService.addWhitelist(dto)

    @PostMapping(value = ["/{providerId}/whitelist/multiple"], produces = ["application/json"])
    fun addNewMappingMultiple(
        @PathVariable(value = "providerId") providerId: Long,
        @Valid @RequestBody dto: MultipleWhiteListDTO
    ) = providerService.addMultipleMappings(providerId, dto)

    @PostMapping(value = ["/massUpload/payerProviderMapping"], produces = ["application/json"], consumes = ["multipart/form-data"])
    @Operation(summary = "Save payer provider mapping from excel config file ")
    fun saveProviderMappingFromFile(@RequestParam("file") file: MultipartFile) = providerService
        .saveProviderMappingFromFile(file)

    @GetMapping(
        value = ["/nearby"],
        produces = ["application/json"]
    )
    @Operation(summary = "Get Nearby Providers by beneficiaryId and radius (in Kilometres)")
    fun getNearbyProviders(
        @RequestParam("beneficiaryId") beneficiaryId: Long,
        @RequestParam("latitude") latitude: Double,
        @RequestParam("longitude") longitude: Double,
        @RequestParam(value = "radius", defaultValue = "100") radius:Double,
        @RequestParam(value = "page", defaultValue = "1") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int
    ) = providerService.getNearbyProviders(beneficiaryId, latitude, longitude,radius, page, size)

    @PostMapping(value = ["/update"], produces = ["application/json"])
    fun updateProvider(
        @Valid @RequestBody dto: ProviderUpdateDTO
    ) = providerService.updateProvider(dto)

    @PostMapping(value = ["/multiUpdate"], produces = ["application/json"])
    fun multiProviderUpdate(
        @Valid @RequestBody dto: MultiProviderUpdateDTO
    ) = providerService.multiProviderUpdate(dto)

}
