package com.milesight.beaveriot.integration.msc.service

import com.milesight.beaveriot.DevelopApplication
import com.milesight.msc.sdk.MscClient
import com.milesight.msc.sdk.config.Credentials
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Specification

@ActiveProfiles("test")
@SpringBootTest(classes = [DevelopApplication])
class MscDataSyncServiceTest extends Specification {

    @Autowired
    MscDataSyncService mscDataSyncService

    @SpringBean
    MscConnectionService mscConnectionService = Mock()

    def setup() {
        mscConnectionService.getMscClient() >> MscClient.builder()
                .endpoint(System.getenv("MSC_ENDPOINT"))
                .credentials(Credentials.builder()
                        .clientId(System.getenv("MSC_CLIENT_ID"))
                        .clientSecret(System.getenv("MSC_CLIENT_SECRET"))
                        .build())
                .build()
    }


    @Ignore
    def "when sync all device data then should not throw any exception"() {
        given:
        mscDataSyncService.syncAllDeviceData(false)

        expect:
        true
    }

}
