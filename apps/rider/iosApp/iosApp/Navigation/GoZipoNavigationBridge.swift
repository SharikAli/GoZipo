// Swift side of the navigation bridge (Kotlin: core/map/navigation/src/iosMain/.../ios/NavigationIosBridge.kt).
// The Mapbox Navigation SDK for iOS is Swift-only, so Kotlin starts guidance through these classes.
// Rider app only.

import UIKit
import CoreLocation
import MapboxDirections
import MapboxNavigationCore
import MapboxNavigationUIKit
import RiderShared

final class GoZipoNavigationBridgeFactory: NSObject, GoZipoNavigationFactory {

    /// The SDK allows one navigation core per app, so it's created once and reused.
    private var provider: MapboxNavigationProvider?

    func createNavigation(
        accessToken: String,
        destination: GoZipoNavigationCoordinate,
        origin: GoZipoNavigationCoordinate?,
        destinationName: String?,
        darkTheme: Bool,
        simulate: Bool,
        listener: GoZipoNavigationListener
    ) -> GoZipoNavigationSession {
        let provider = provider ?? MapboxNavigationProvider(
            coreConfig: .init(
                credentials: .init(accessToken: accessToken),
                locationSource: simulate
                    ? .simulation(initialLocation: origin.map { CLLocation(latitude: $0.latitude, longitude: $0.longitude) })
                    : .live
            )
        )
        self.provider = provider
        return GoZipoNavigationSessionController(
            provider: provider,
            destination: destination.toLocation(),
            origin: origin?.toLocation(),
            destinationName: destinationName,
            darkTheme: darkTheme,
            listener: listener
        )
    }
}

/// Shows a spinner while routing, then embeds the SDK's `NavigationViewController`.
final class GoZipoNavigationSessionController: UIViewController, GoZipoNavigationSession {

    private let provider: MapboxNavigationProvider
    private let destination: CLLocationCoordinate2D
    private let origin: CLLocationCoordinate2D?
    private let destinationName: String?
    private let darkTheme: Bool
    private var listener: GoZipoNavigationListener?

    private var navigationViewController: NavigationViewController?
    private var routingTask: Task<Void, Never>?
    private let spinner = UIActivityIndicatorView(style: .large)
    private let messageLabel = UILabel()
    private let retryButton = UIButton(type: .system)
    private let closeButton = UIButton(type: .system)

    init(
        provider: MapboxNavigationProvider,
        destination: CLLocationCoordinate2D,
        origin: CLLocationCoordinate2D?,
        destinationName: String?,
        darkTheme: Bool,
        listener: GoZipoNavigationListener
    ) {
        self.provider = provider
        self.destination = destination
        self.origin = origin
        self.destinationName = destinationName
        self.darkTheme = darkTheme
        self.listener = listener
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    func viewController() -> UIViewController {
        self
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        overrideUserInterfaceStyle = darkTheme ? .dark : .light
        view.backgroundColor = .systemBackground
        setUpStatusViews()
        calculateRoute()
    }

    func destroy() {
        routingTask?.cancel()
        listener = nil
        // Ends active guidance (and its location and voice) in the shared navigation core
        provider.tripSession().setToIdle()
        if let navigationViewController {
            navigationViewController.willMove(toParent: nil)
            navigationViewController.view.removeFromSuperview()
            navigationViewController.removeFromParent()
        }
        navigationViewController = nil
    }

    // MARK: Routing

    private func calculateRoute() {
        // Without an origin from the app, route from the device's current location
        guard let start = origin ?? CLLocationManager().location?.coordinate else {
            showFailure()
            return
        }
        showLoading()

        var waypoint = Waypoint(coordinate: destination, name: destinationName)
        waypoint.targetCoordinate = destination
        let options = NavigationRouteOptions(
            waypoints: [Waypoint(coordinate: start), waypoint]
        )
        let request = provider.mapboxNavigation.routingProvider().calculateRoutes(options: options)
        routingTask = Task { [weak self] in
            let result = await request.result
            guard let self, !Task.isCancelled else { return }
            switch result {
            case .success(let routes):
                self.startGuidance(routes)
            case .failure:
                self.showFailure()
            }
        }
    }

    private func startGuidance(_ routes: NavigationRoutes) {
        let options = NavigationOptions(
            mapboxNavigation: provider.mapboxNavigation,
            voiceController: provider.routeVoiceController,
            eventsManager: provider.eventsManager(),
            // One style, so the SDK doesn't switch day/night by the clock against the app's theme
            styles: [darkTheme ? StandardNightStyle() : StandardDayStyle()]
        )
        let controller = NavigationViewController(navigationRoutes: routes, navigationOptions: options)
        controller.delegate = self
        controller.routeLineTracksTraversal = true

        addChild(controller)
        controller.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(controller.view)
        NSLayoutConstraint.activate([
            controller.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            controller.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            controller.view.topAnchor.constraint(equalTo: view.topAnchor),
            controller.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        controller.didMove(toParent: self)
        navigationViewController = controller
        hideStatus()
    }

    // MARK: Status views

    private func setUpStatusViews() {
        messageLabel.numberOfLines = 0
        messageLabel.textAlignment = .center
        messageLabel.font = .preferredFont(forTextStyle: .body)
        retryButton.setTitle(NSLocalizedString("Retry", comment: "Retry finding a route"), for: .normal)
        retryButton.addTarget(self, action: #selector(retryTapped), for: .touchUpInside)
        closeButton.setTitle(NSLocalizedString("End navigation", comment: "Close navigation"), for: .normal)
        closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [spinner, messageLabel, retryButton, closeButton])
        stack.axis = .vertical
        stack.alignment = .center
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            stack.leadingAnchor.constraint(equalTo: view.layoutMarginsGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: view.layoutMarginsGuide.trailingAnchor),
        ])
    }

    private func showLoading() {
        spinner.startAnimating()
        messageLabel.text = NSLocalizedString("Finding a route…", comment: "Routing in progress")
        retryButton.isHidden = true
    }

    private func showFailure() {
        spinner.stopAnimating()
        messageLabel.text = NSLocalizedString(
            "Couldn't find a route. Check your connection and try again.",
            comment: "Routing failed"
        )
        retryButton.isHidden = false
    }

    private func hideStatus() {
        spinner.stopAnimating()
        messageLabel.isHidden = true
        retryButton.isHidden = true
        closeButton.isHidden = true
    }

    @objc private func retryTapped() {
        calculateRoute()
    }

    @objc private func closeTapped() {
        listener?.onClose()
    }
}

extension GoZipoNavigationSessionController: NavigationViewControllerDelegate {
    func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) {
        // One leg per session, so any arrival is the destination
        listener?.onArrival()
    }

    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        listener?.onClose()
    }
}

private extension GoZipoNavigationCoordinate {
    func toLocation() -> CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}
