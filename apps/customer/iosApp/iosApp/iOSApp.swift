import SwiftUI
import CustomerShared

@main
struct iOSApp: App {
    
    init() {
        IosInitKoinKt.doInitKoin(mapboxFactory: GoZipoMapboxBridgeFactory())
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
