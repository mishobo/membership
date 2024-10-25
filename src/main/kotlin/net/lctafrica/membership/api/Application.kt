package net.lctafrica.membership.api

import org.apache.avro.data.TimeConversions
import org.apache.avro.specific.SpecificData
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("net.lctafrica.membership")
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
    SpecificData.get().addLogicalTypeConversion(TimeConversions.TimestampMillisConversion())


}
