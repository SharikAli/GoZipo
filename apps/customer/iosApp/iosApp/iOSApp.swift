import SwiftUI
import CustomerShared

@main
struct iOSApp: App {
    
    init() {
        InitKoinKt.doInitKoin()
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
