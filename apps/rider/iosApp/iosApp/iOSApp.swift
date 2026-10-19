import SwiftUI
import RiderShared

@main
struct iOSApp: App {
    
    // Firebase push (apps/ios-common/Push/GoZipoAppDelegate.swift)
    @UIApplicationDelegateAdaptor(GoZipoAppDelegate.self) var appDelegate
    
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
