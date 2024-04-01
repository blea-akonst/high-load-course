package ru.quipy.payments.config

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import ru.quipy.payments.logic.ExternalServiceProperties
import java.util.concurrent.atomic.AtomicLong

@Service
class AccountService {
    val accounts = ExternalServicesConfig.accounts.map { AccountWrapper(it) }

    fun calculateSpeed(account: ExternalServiceProperties): Double {
        val a = account.request95thPercentileProcessingTime.toMillis().toDouble() / 1000 // average processing time
        val p = account.parallelRequests.toDouble() // parallel request number
        val r = account.rateLimitPerSec.toDouble() // rate limit per sec

        return p.coerceAtMost(r) / a;
    }

    @Async
    fun getAvailableAccount(): ExternalServiceProperties? {
        val accountsWithSpeed = accounts.map { accountWrapper ->
            Pair(accountWrapper, calculateSpeed(accountWrapper.account))
        }.sortedWith(compareBy({ it.first.account.callCost }, { -it.second }))

        return accountsWithSpeed.firstOrNull { it.first.isAccountAvailable() }?.first?.apply { this.registerRequest() }?.account
    }
}

class AccountWrapper(
    val account: ExternalServiceProperties
) {
    private val lastRequestTime = AtomicLong(System.currentTimeMillis())
    private val requestsInLastSecondCounter = AtomicLong(0)
    fun isAccountAvailable(): Boolean {
        val currentTime = System.currentTimeMillis()
        val windowSize = 1000L

        synchronized(this) {
            if (currentTime - lastRequestTime.get() > windowSize) {
                lastRequestTime.set(currentTime)
                requestsInLastSecondCounter.set(0)
            }
            if (requestsInLastSecondCounter.get() < account.rateLimitPerSec) {
                requestsInLastSecondCounter.incrementAndGet()
                return true
            } else {
                return false
            }
        }
    }


    fun registerRequest() {
        requestsInLastSecondCounter.incrementAndGet()
    }
}