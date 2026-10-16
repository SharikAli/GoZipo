// Firebase Cloud Messaging for iOS (Kotlin: core/data/src/iosMain/.../notification/).
// Gets the APNs token, exchanges it for an FCM token and hands that to Kotlin, which
// registers it with the backend (MainViewModel). Ride updates arrive as normal APNs alerts.
// Shared by both iOS apps: each Xcode project includes this folder.

import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
#if canImport(CustomerShared)
import CustomerShared
#elseif canImport(RiderShared)
import RiderShared
#endif

final class GoZipoAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    // Read by FirebasePushNotificationService.ios.kt when no token has arrived yet this launch
    private static let fcmTokenKey = "FCM_TOKEN"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        // Needs no user permission (that only gates showing alerts, requested from Compose);
        // calling it every launch keeps the APNs token current
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        refreshFcmToken()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("GoZipo: failed to register for remote notifications: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken, !fcmToken.isEmpty else { return }
        publish(fcmToken)
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        completionHandler(.noData)
    }

    // The backend only pushes when the ride socket is closed, so a push that lands while the
    // app is open is a real missed update: show it
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    // Tapping opens the app; the ride screens resync over REST when the socket reconnects
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(response.notification.request.content.userInfo)
        completionHandler()
    }

    private func refreshFcmToken() {
        Task {
            do {
                publish(try await Messaging.messaging().token())
            } catch {
                print("GoZipo: failed to get FCM token: \(error.localizedDescription)")
            }
        }
    }

    private func publish(_ fcmToken: String) {
        UserDefaults.standard.set(fcmToken, forKey: Self.fcmTokenKey)
        DispatchQueue.main.async {
            MainViewControllerKt.updatePushToken(token: fcmToken)
        }
    }
}
