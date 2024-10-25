package net.lctafrica.membership.api.service

import net.lctafrica.membership.api.domain.Country
import net.lctafrica.membership.api.domain.CountryRepository
import net.lctafrica.membership.api.domain.Region
import net.lctafrica.membership.api.domain.RegionRepository
import net.lctafrica.membership.api.dtos.CountryDTO
import net.lctafrica.membership.api.dtos.RegionDTO
import net.lctafrica.membership.api.util.Result
import net.lctafrica.membership.api.util.ResultFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CountryService(val countryRepo: CountryRepository, val regionRepo: RegionRepository) : ICountryService {

    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun addCountry(dto: CountryDTO): Result<Country> {

        val country = countryRepo.findByNameIgnoreCase(dto.name)
        if (country == null) {
            var newCountry = Country(name = dto.name)
            countryRepo.save(newCountry)
            return ResultFactory.getSuccessResult(newCountry)
        }
        return ResultFactory.getFailResult("Country with name $country already exists")
    }

    @Transactional(readOnly = true)
    override fun findAllCountries(): Result<MutableList<Country>> {
        val all = countryRepo.findAll()
        return ResultFactory.getSuccessResult(all)
    }



    @Transactional(readOnly = false, rollbackFor = [Exception::class])
    override fun addRegion(dto: RegionDTO): Result<Region> {
        val country = countryRepo.findById(dto.countryId)
        if (country.isEmpty) return ResultFactory.getFailResult("No country with ID ${dto.countryId} was found")
        var region = Region(name = dto.name, country = country.get())
        regionRepo.save(region)
        return ResultFactory.getSuccessResult(region)
    }

    @Transactional(readOnly = true)
    override fun findRegionByCountry(countryId: Long, page: Int, size: Int): Result<Page<Region>> {
        val country = countryRepo.findById(countryId)
        if (country.isEmpty) return ResultFactory.getFailResult("No country with ID $countryId was found")
        val request = PageRequest.of(page - 1, size)
        val pages = regionRepo.findByCountry(country.get(), request)
        return ResultFactory.getSuccessResult(pages)
    }

    override fun findRegionById(regionId: Long): Result<Region> {
        val region = regionRepo.findById(regionId)
        return if(region.isEmpty) ResultFactory.getFailResult("No region with id $regionId")
        else
            ResultFactory.getSuccessResult(data = region.get())
    }


}