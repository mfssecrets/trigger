package com.triggerapp.domain.usecase.connectivity

import com.triggerapp.domain.connectivity.NetworkConnectivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Application use case: observe validated device connectivity as a hot [StateFlow].
 *
 * @param networkConnectivity Domain port implemented in the data layer ([NetworkConnectivity]).
 * @author udit
 */
class ObserveNetworkOnlineUseCase(
    private val networkConnectivity: NetworkConnectivity,
) {
    /**
     * @return Same [StateFlow] as [NetworkConnectivity.isOnline]; `true` when the device is considered online.
     * @author udit
     */
    operator fun invoke(): StateFlow<Boolean> = networkConnectivity.isOnline
}
