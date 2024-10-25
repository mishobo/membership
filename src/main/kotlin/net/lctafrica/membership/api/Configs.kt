package net.lctafrica.membership.api

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.lang.reflect.Type
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
class Configs {
}

@Bean
fun webClient(baseUrl: String) = WebClient.builder()
    .baseUrl(baseUrl)
    .clientConnector(ReactorClientHttpConnector(
        HttpClient.create().responseTimeout(Duration.ofSeconds(450))
    ))
    .build()

val gson = GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter().nullSafe())
    .create()

class LocalDateAdapter : TypeAdapter<LocalDate>() {

    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(DateTimeFormatter.ISO_LOCAL_DATE.format(value))
    }

    override fun read(input: JsonReader): LocalDate {
        return LocalDate.parse(input.nextString())
    }
}


