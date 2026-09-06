package com.taosc.taosc

data class PairRequestResponse(
    val pairRequestId: String,
    val verifyCode: String
)

data class PairRequestPollResponse(
    val id: String,
    val status: String,
    val scopedToken: String?
) {
    val requestStatus: PairRequestStatus?
        get() = when (status.lowercase()) {
            "pending" -> PairRequestStatus.Pending
            "approved" -> scopedToken?.let { PairRequestStatus.Approved(it) }
            "denied" -> PairRequestStatus.Denied
            "expired" -> PairRequestStatus.Expired
            else -> null
        }
}

sealed class PairRequestStatus {
    data object Pending : PairRequestStatus()
    data class Approved(val scopedToken: String) : PairRequestStatus()
    data object Denied : PairRequestStatus()
    data object Expired : PairRequestStatus()
}

sealed class PairingError : Exception() {
    data object Unreachable : PairingError()
    data object InvalidResponse : PairingError()
    data object Unknown : PairingError()
}
