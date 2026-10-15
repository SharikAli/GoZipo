import SwiftUI
import RiderShared

@main
struct iOSApp: App {
    
    init() {
        IosInitKoinKt.doInitKoin(
            mapboxFactory: GoZipoMapboxBridgeFactory(),
            navigationFactory: GoZipoNavigationBridgeFactory()
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    MainViewControllerKt.handleDeepLink(uri: url.absoluteString)
                }
        }
    }
}
