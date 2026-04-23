package org.chatserver.plugins

import com.amazonaws.xray.AWSXRay
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path

val XRayPlugin = createApplicationPlugin("XRay") {
    onCall { call ->
        val segmentName = "${call.request.httpMethod.value} ${call.request.path()}"
        AWSXRay.beginSegment(segmentName)
    }
    onCallRespond { _ ->
        AWSXRay.endSegment()
    }
}

fun Application.configureXRay() {
    install(XRayPlugin)
}
