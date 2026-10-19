package com.stark.jarvislocal

import org.junit.Assert.fail
import org.junit.Test

class MvpSafetyTest {
    private val safe=MvpDeviceState(false,4L*1024*1024*1024,false,80,false,300,0)
    @Test fun conservativeBudgetAdmitsKnownCoolDevice() { MvpSafety.check(128L*1024*1024,safe) }
    @Test fun resourceRefusalsFailClosed() {
        for (bad in listOf(safe.copy(lowMemory=true),safe.copy(lowRam=true),safe.copy(availableBytes=0),safe.copy(batteryPercent=20),
            safe.copy(batteryPercent=-1),safe.copy(temperatureTenths=400),safe.copy(temperatureTenths=-1),safe.copy(thermalStatus=2))) {
            try { MvpSafety.check(128L*1024*1024,bad);fail("Resource refusal expected") } catch (_:IllegalArgumentException) { }
        }
    }
    @Test fun pre29UsesBatteryProxyAndChargingDoesNotBypassTemperature() {
        MvpSafety.check(128L*1024*1024,safe.copy(thermalStatus=null))
        try { MvpSafety.check(128L*1024*1024,safe.copy(charging=true,temperatureTenths=400));fail() } catch (_:IllegalArgumentException) { }
    }
}
