package com.dalmeng.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class TemplateApplication

fun main(args: Array<String>) {
	runApplication<TemplateApplication>(*args)
}
