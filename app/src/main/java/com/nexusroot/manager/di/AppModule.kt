package com.nexusroot.manager.di

import com.nexusroot.manager.data.DaemonConnector
import com.nexusroot.manager.data.MockDaemonConnector

object AppModule {
    fun provideDaemonConnector(): DaemonConnector = MockDaemonConnector()
}
