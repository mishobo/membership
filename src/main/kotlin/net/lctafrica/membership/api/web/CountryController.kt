package net.lctafrica.membership.api.web

import net.lctafrica.membership.api.dtos.CountryDTO
import net.lctafrica.membership.api.dtos.RegionDTO
import net.lctafrica.membership.api.service.ICountryService
import net.lctafrica.membership.api.service.IProviderService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/country")
class CountryController(val countryService: ICountryService, val providerService: IProviderService) : AbstractController(){

    @PostMapping(value = ["/"], produces = ["application/json"])
    fun addCountry(@Valid @RequestBody dto: CountryDTO) = countryService.addCountry(dto)

    @GetMapping(value = ["/"], produces = ["application/json"])
    fun getAllCountries() = countryService.findAllCountries()

    @GetMapping(value = ["/{countryId}/region"], produces = ["application/json"])
    fun getAllRegionsInCountry(@PathVariable("countryId") countryId: Long, page: Int = 1, size: Int = 10) =
        countryService.findRegionByCountry(countryId, page, size)

    @PostMapping(value = ["/region"], produces = ["application/json"])
    fun addRegion(@Valid @RequestBody dto: RegionDTO) = countryService.addRegion(dto)

    @GetMapping(value = ["/region/{regionId}/providers"], produces = ["application/json"])
    fun findProvidersByRegion(@PathVariable("regionId") regionId: Long, page: Int = 1, size: Int = 10) =
        providerService.findByRegion(regionId, page, size)

    @GetMapping(value = ["/region/{regionId}"], produces = ["application/json"])
    fun findRegionById(@PathVariable("regionId") regionId: Long) = countryService.findRegionById(regionId)

}
