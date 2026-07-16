// com.nexusroot.manager.di.AppModule.kt
object AppModule {
    fun provideDaemonConnector(): DaemonConnector = MockDaemonConnector()
}