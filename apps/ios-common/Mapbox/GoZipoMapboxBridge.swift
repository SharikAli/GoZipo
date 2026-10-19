// Swift side of the Mapbox bridge (Kotlin: core/map/mapbox/src/iosMain/.../ios/MapboxIosBridge.kt).
// The Mapbox Maps iOS SDK is Swift-only, so Kotlin drives the map through these classes.
// Shared by both iOS apps: each Xcode project includes this folder.

import UIKit
import MapboxMaps
#if canImport(CustomerShared)
import CustomerShared
#elseif canImport(RiderShared)
import RiderShared
#endif

final class GoZipoMapboxBridgeFactory: NSObject, GoZipoMapboxFactory {
    func createMap(
        accessToken: String,
        camera: GoZipoMapboxCamera,
        listener: GoZipoMapboxListener
    ) -> GoZipoMapboxMap {
        MapboxOptions.accessToken = accessToken
        return GoZipoMapboxMapView(camera: camera, listener: listener)
    }
}

final class GoZipoMapboxMapView: NSObject, GoZipoMapboxMap {

    private static let animationSeconds: TimeInterval = 0.6
    private static let casingWidth = 1.5
    private static let ornamentMargin: CGFloat = 8

    private let mapView: MapView
    private let lineManager: PolylineAnnotationManager
    private var listener: GoZipoMapboxListener?
    private var cancelables = Set<AnyCancelable>()
    private var contentInsets = UIEdgeInsets.zero
    private var isDark: Bool?

    init(camera: GoZipoMapboxCamera, listener: GoZipoMapboxListener) {
        self.listener = listener
        mapView = MapView(
            frame: .zero,
            mapInitOptions: MapInitOptions(
                mapStyle: .standard(lightPreset: .day),
                cameraOptions: camera.toCameraOptions(padding: .zero)
            )
        )
        mapView.ornaments.options.scaleBar.visibility = .hidden
        mapView.ornaments.options.compass.visibility = .hidden
        lineManager = mapView.annotations.makePolylineAnnotationManager(id: "gozipo-lines")
        super.init()

        mapView.mapboxMap.onCameraChanged.observe { [weak self] _ in
            self?.reportCamera(isMoving: true)
        }.store(in: &cancelables)
        
        mapView.mapboxMap.onMapIdle.observe { [weak self] _ in
            self?.reportCamera(isMoving: false)
        }.store(in: &cancelables)
        
        let tap = mapView.mapboxMap.addInteraction(TapInteraction { [weak self] context in
            self?.listener?.onMapTap(coordinate: context.coordinate.toBridge())
            return false
        })
        
        AnyCancelable(tap).store(in: &cancelables)
    }

    func view() -> UIView {
        mapView
    }

    func setCamera(camera: GoZipoMapboxCamera, animated: Bool) {
        move(to: camera.toCameraOptions(padding: contentInsets), animated: animated)
    }

    func fitBounds(
        coordinates: [GoZipoMapboxCoordinate],
        padding: GoZipoMapboxInsets,
        maxZoom: Double,
        animated: Bool
    ) {
        guard !coordinates.isEmpty else { return }
        let target = try? mapView.mapboxMap.camera(
            for: coordinates.map { $0.toLocation() },
            camera: CameraOptions(padding: contentInsets, bearing: 0, pitch: 0),
            coordinatesPadding: padding.toEdgeInsets(),
            maxZoom: maxZoom,
            offset: nil
        )
        if let target {
            move(to: target, animated: animated)
        }
    }

    func setContentInsets(insets: GoZipoMapboxInsets) {
        contentInsets = insets.toEdgeInsets()
        // Keep the logo and attribution (required by the Mapbox terms) above app UI such as a sheet
        mapView.ornaments.options.logo.margins = CGPoint(
            x: Self.ornamentMargin + contentInsets.left,
            y: Self.ornamentMargin + contentInsets.bottom
        )
        mapView.ornaments.options.attributionButton.margins = CGPoint(
            x: Self.ornamentMargin + contentInsets.right,
            y: Self.ornamentMargin + contentInsets.bottom
        )
        mapView.mapboxMap.setCamera(to: CameraOptions(padding: contentInsets))
    }

    func setDarkTheme(dark: Bool) {
        guard dark != isDark else { return }
        isDark = dark
        mapView.mapboxMap.mapStyle = .standard(lightPreset: dark ? .night : .day)
    }

    func setShowUserLocation(show: Bool) {
        mapView.location.options.puckType = show ? .puck2D(.makeDefault(showBearing: true)) : nil
        mapView.location.options.puckBearingEnabled = show
    }

    func setPolylines(polylines: [GoZipoMapboxPolyline]) {
        lineManager.annotations = polylines.map { line in
            var annotation = PolylineAnnotation(
                id: line.id,
                lineCoordinates: line.coordinates.map { $0.toLocation() }
            )
            annotation.lineColor = StyleColor(UIColor(argb: line.color))
            annotation.lineWidth = line.width
            annotation.lineJoin = .round
            // Full brightness: the Standard style's night lighting otherwise darkens annotations
            annotation.lineEmissiveStrength = 1
            if let casing = line.casingColor {
                annotation.lineBorderColor = StyleColor(UIColor(argb: casing.int32Value))
                annotation.lineBorderWidth = Self.casingWidth
            }
            return annotation
        }
    }

    func screenPoint(coordinate: GoZipoMapboxCoordinate) -> GoZipoMapboxPoint {
        let point = mapView.mapboxMap.point(for: coordinate.toLocation())
        return GoZipoMapboxPoint(x: Double(point.x), y: Double(point.y))
    }

    func destroy() {
        cancelables.forEach { $0.cancel() }
        cancelables.removeAll()
        listener = nil
    }

    private func move(to camera: CameraOptions, animated: Bool) {
        if animated {
            mapView.camera.ease(to: camera, duration: Self.animationSeconds)
        } else {
            mapView.mapboxMap.setCamera(to: camera)
        }
    }

    private func reportCamera(isMoving: Bool) {
        let state = mapView.mapboxMap.cameraState
        listener?.onCameraChanged(
            camera: GoZipoMapboxCamera(
                center: state.center.toBridge(),
                zoom: Double(state.zoom),
                bearing: state.bearing
            ),
            isMoving: isMoving
        )
    }
}

private extension GoZipoMapboxCamera {
    func toCameraOptions(padding: UIEdgeInsets) -> CameraOptions {
        CameraOptions(
            center: center.toLocation(),
            padding: padding,
            zoom: CGFloat(zoom),
            bearing: bearing
        )
    }
}

private extension GoZipoMapboxCoordinate {
    func toLocation() -> CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

private extension CLLocationCoordinate2D {
    func toBridge() -> GoZipoMapboxCoordinate {
        GoZipoMapboxCoordinate(latitude: latitude, longitude: longitude)
    }
}

private extension GoZipoMapboxInsets {
    func toEdgeInsets() -> UIEdgeInsets {
        UIEdgeInsets(top: top, left: left, bottom: bottom, right: right)
    }
}

private extension UIColor {
    /// From Compose's `Color.toArgb()`.
    convenience init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }
}
