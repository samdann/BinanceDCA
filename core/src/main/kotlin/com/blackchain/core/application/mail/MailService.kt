package com.blackchain.com.blackchain.core.application.mail

import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.model.Body
import aws.sdk.kotlin.services.ses.model.Content
import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.model.Message
import aws.sdk.kotlin.services.ses.model.SendEmailRequest

// Send email
suspend fun sendEmail(report: String) {

    val recipients = listOf("danver_k@hotmail.com")
    val confRegion = System.getenv("AWS_REGION") ?: "eu-central-1"

    SesClient { region = confRegion }.use { ses ->
        ses.sendEmail(SendEmailRequest {
            source = "no-reply@binance-dca.com"
            destination = Destination {
                toAddresses = recipients
            }
            message = Message {
                subject = Content { data = "Daily Binance Report" }
                body = Body {
                    text = Content { data = report }
                }
            }
        })
    }
}