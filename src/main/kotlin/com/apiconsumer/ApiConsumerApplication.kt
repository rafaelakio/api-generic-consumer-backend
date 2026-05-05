package com.apiconsumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiConsumerApplication

fun main(args: Array<String>) {
    runApplication<ApiConsumerApplication>(*args)
}
