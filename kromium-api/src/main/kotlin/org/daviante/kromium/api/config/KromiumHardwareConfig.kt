package org.daviante.kromium.api.config

/**
 * Controls how the engine interacts with the host OS and GPU.
 */
@ConsistentCopyVisibility
data class KromiumHardwareConfig internal constructor(
    val processModel: KromiumProcessModel,
    val gpuMode: KromiumGpuMode
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var processModel: KromiumProcessModel = KromiumProcessModel.AUTO
        private var gpuMode: KromiumGpuMode = KromiumGpuMode.COMPOSITING_DISABLED

        fun processModel(model: KromiumProcessModel) = apply { this.processModel = model }
        fun gpuMode(mode: KromiumGpuMode) = apply { this.gpuMode = mode }

        fun build(): KromiumHardwareConfig = KromiumHardwareConfig(processModel, gpuMode)
    }
}

