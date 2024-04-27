package ru.quipy.payments.config

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class HttpClientBuilder {

    @Bean
    fun getHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
                .dispatcher(Dispatcher(Executors.newFixedThreadPool(100)).apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        })
            .build()
    }
}