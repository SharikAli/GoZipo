import SwiftUI
import CustomerShared

@main
struct iOSApp: App {
    
    // Firebase push (apps/ios-common/Push/GoZipoAppDelegate.swift)
    @UIApplicationDelegateAdaptor(GoZipoAppDelegate.self) var appDelegate
    
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
