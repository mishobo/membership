package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.dtos.BenefitCatalogDTO
import net.lctafrica.membership.api.dtos.NestedBenefitCatalogs
import net.lctafrica.membership.api.service.IBenefitCatalogService
import net.lctafrica.membership.api.service.ProviderService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/catalog")
@CrossOrigin("*","*")
class CatalogController(
    val catalogService: IBenefitCatalogService,
    val providerService: ProviderService
) : AbstractController(){

    @GetMapping(value = ["/benefit"], produces = ["application/json"])
    fun search(@RequestParam(name = "search") searchParam: String, page: Int, size: Int) =
        catalogService.searchByName(searchParam, page, size)

    @GetMapping(value = ["/all"], produces = ["application/json"])
    fun getAllBenefits( page: Int = 1, size: Int = 10) =
        catalogService.getAllCatalogBenefits(page, size)

    @PostMapping(value = ["/benefits"], produces = ["application/json"])
    fun createBatch(@Valid @RequestBody dto: NestedBenefitCatalogs) =
        catalogService.batchAddition(dto.benefits)

    @PostMapping(value = ["/benefit"], produces = ["application/json"])
    fun addCatalog(@Valid @RequestBody dto: BenefitCatalogDTO) = catalogService.addBenefitCatalog(dto)

    @GetMapping(value = ["/{benefitId}/whitelist"], produces = ["application/json"])
    fun getWhitelistByBenefitCode(@PathVariable("benefitId") benefitId: Long, page: Int = 1, size: Int = 10) =
        providerService.findWhiteListByBenefit(benefitId, page, size)

}