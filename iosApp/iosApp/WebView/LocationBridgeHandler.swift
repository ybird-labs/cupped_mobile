import CoreLocation
import Shared

/// Handles one-shot location requests initiated by the web bridge.
///
/// Manages CLLocationManager lifecycle, permission prompts, and timeout.
/// Sends the result back through the bridge via the provided reply closure.
///
/// Usage from BridgePlatformDelegate:
/// ```swift
/// locationHandler.requestLocation(requestId: id, accuracy: accuracy) { replyToId, message in
///     let envelope = BridgeEnvelope(id: UUID().uuidString, replyTo: replyToId, message: message)
///     sendToWeb(json: BridgeCodec.encodeEnvelope(envelope: envelope))
/// }
/// ```
@MainActor
final class LocationBridgeHandler: NSObject, CLLocationManagerDelegate {

    private let manager = CLLocationManager()
    private var pendingRequestId: String?
    private var sendReply: ((String, BridgeMessage) -> Void)?
    private var timeoutTask: Task<Void, Never>?
    private var isCompleted = false

    override init() {
        super.init()
        manager.delegate = self
    }

    /// Starts a one-shot location request.
    ///
    /// - Parameters:
    ///   - requestId: The bridge envelope ID to reply to.
    ///   - accuracy: Desired accuracy from the bridge protocol.
    ///   - reply: Closure that sends a BridgeMessage back to web with
    ///           the given requestId as the replyTo.
    func requestLocation(
        requestId: String,
        accuracy: LocationAccuracy,
        reply: @escaping (String, BridgeMessage) -> Void
    ) {
        if pendingRequestId != nil {
            reply(requestId, BridgeMessage.Error(
                code: "location_busy",
                message: "A location request is already in progress"
            ))
            return
        }

        pendingRequestId = requestId
        sendReply = reply
        isCompleted = false

        manager.desiredAccuracy = clAccuracy(from: accuracy)

        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            startLocationRequest()
        case .denied, .restricted:
            complete(with: BridgeMessage.LocationDenied())
        @unknown default:
            complete(with: BridgeMessage.Error(
                code: "location_unavailable",
                message: "Unknown authorization status"
            ))
        }
    }

    // MARK: - CLLocationManagerDelegate

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            guard self.pendingRequestId != nil else { return }

            switch manager.authorizationStatus {
            case .authorizedWhenInUse, .authorizedAlways:
                self.startLocationRequest()
            case .denied, .restricted:
                self.complete(with: BridgeMessage.LocationDenied())
            case .notDetermined:
                break
            @unknown default:
                break
            }
        }
    }

    nonisolated func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        guard let location = locations.last else { return }

        let lat = location.coordinate.latitude
        let lon = location.coordinate.longitude
        let hAccuracy = location.horizontalAccuracy
        let alt: Double? = location.verticalAccuracy >= 0 ? location.altitude : nil
        let ts = Int64(location.timestamp.timeIntervalSince1970 * 1000)

        Task { @MainActor in
            guard hAccuracy >= 0 else {
                self.complete(with: BridgeMessage.Error(
                    code: "location_invalid",
                    message: "Received location with negative accuracy"
                ))
                return
            }

            self.complete(with: BridgeMessage.LocationResult(
                latitude: lat,
                longitude: lon,
                accuracy: hAccuracy,
                altitude: alt.map { KotlinDouble(double: $0) },
                timestamp: ts
            ))
        }
    }

    nonisolated func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        let message: String = error.localizedDescription
        let isDenied = (error as? CLError)?.code == .denied

        Task { @MainActor in
            if isDenied {
                self.complete(with: BridgeMessage.LocationDenied())
            } else {
                self.complete(with: BridgeMessage.Error(
                    code: "location_unavailable",
                    message: message
                ))
            }
        }
    }

    // MARK: - Private

    private func startLocationRequest() {
        guard CLLocationManager.locationServicesEnabled() else {
            complete(with: BridgeMessage.Error(
                code: "location_services_disabled",
                message: "Location services are disabled in Settings"
            ))
            return
        }

        timeoutTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 15_000_000_000)
            guard !Task.isCancelled else { return }
            self?.complete(with: BridgeMessage.Error(
                code: "location_timeout",
                message: "Could not determine location within 15 seconds"
            ))
        }

        manager.requestLocation()
    }

    private func complete(with message: BridgeMessage) {
        guard let requestId = pendingRequestId, !isCompleted else { return }
        isCompleted = true

        timeoutTask?.cancel()
        timeoutTask = nil
        sendReply?(requestId, message)
        pendingRequestId = nil
        sendReply = nil
    }

    private func clAccuracy(from accuracy: LocationAccuracy) -> CLLocationAccuracy {
        switch accuracy {
        case .high:     return kCLLocationAccuracyBest
        case .balanced: return kCLLocationAccuracyHundredMeters
        case .low:      return kCLLocationAccuracyKilometer
        }
    }
}
