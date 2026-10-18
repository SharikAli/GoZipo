// Swift side of the Stripe bridge (Kotlin: core/payment/stripe/src/iosMain/.../ios/StripeIosBridge.kt).
// The Stripe iOS SDK is Swift-only, so Kotlin drives PaymentSheet, PaymentSheet.FlowController
// and EmbeddedPaymentElement through these classes. Customer app only (the rider app doesn't pay).

import CustomerShared
import StripePaymentSheet
import UIKit

final class GoZipoStripeBridgeFactory: NSObject, GoZipoStripeFactory {
    // Kotlin calls the bridge from the main thread (Compose)
    func createCheckout(mode: String, listener: GoZipoStripeListener) -> GoZipoStripeCheckout {
        MainActor.assumeIsolated {
            GoZipoStripeCheckoutImpl(
                mode: Mode(rawValue: mode) ?? .paymentSheet,
                listener: listener
            )
        }
    }
}

/** `PaymentUiMode` names from Kotlin. */
private enum Mode: String {
    case paymentSheet = "PAYMENT_SHEET"
    case flowController = "FLOW_CONTROLLER"
    case embedded = "EMBEDDED"
}

private struct GoZipoStripeError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/**
 * One checkout. All three UIs use a deferred intent: configured with the amount (or setup)
 * and the customer; Stripe calls `confirmHandler` for the client secret when the customer pays.
 */
private final class GoZipoStripeCheckoutImpl: NSObject, GoZipoStripeCheckout, EmbeddedPaymentElementDelegate {
    private let mode: Mode
    private let listener: GoZipoStripeListener

    private var config: GoZipoStripeConfig?
    private var paymentSheet: PaymentSheet?
    private var flowController: PaymentSheet.FlowController?
    private var embeddedElement: EmbeddedPaymentElement?

    init(mode: Mode, listener: GoZipoStripeListener) {
        self.mode = mode
        self.listener = listener
    }

    // MARK: GoZipoStripeCheckout

    func prepare(config: GoZipoStripeConfig, completion: @escaping (String?) -> Void) {
        self.config = config
        STPAPIClient.shared.publishableKey = config.publishableKey
        let intentConfiguration = makeIntentConfiguration(config)

        switch mode {
        case .paymentSheet:
            // Loads when presented
            paymentSheet = PaymentSheet(
                intentConfiguration: intentConfiguration,
                configuration: makeSheetConfiguration(config)
            )
            completion(nil)

        case .flowController:
            PaymentSheet.FlowController.create(
                intentConfiguration: intentConfiguration,
                configuration: makeSheetConfiguration(config)
            ) { [weak self] result in
                guard let self else { return }
                switch result {
                case let .success(controller):
                    self.flowController = controller
                    self.listener.onPaymentOptionChanged(label: controller.paymentOption?.label)
                    completion(nil)
                case let .failure(error):
                    completion(error.localizedDescription)
                }
            }

        case .embedded:
            Task { @MainActor [weak self] in
                guard let self else { return }
                do {
                    let element = try await EmbeddedPaymentElement.create(
                        intentConfiguration: intentConfiguration,
                        configuration: self.makeEmbeddedConfiguration(config)
                    )
                    element.delegate = self
                    element.presentingViewController = topViewController()
                    self.embeddedElement = element
                    self.listener.onPaymentOptionChanged(label: element.paymentOption?.label)
                    self.reportEmbeddedHeight(element)
                    completion(nil)
                } catch {
                    completion(error.localizedDescription)
                }
            }
        }
    }

    func presentPaymentOptions() {
        guard let flowController, let presenter = topViewController() else { return }
        flowController.presentPaymentOptions(from: presenter) { [weak self] _ in
            self?.listener.onPaymentOptionChanged(label: flowController.paymentOption?.label)
        }
    }

    func confirm() {
        guard let presenter = topViewController() else {
            finish(.failed(error: GoZipoStripeError(message: "Can't show payment right now")))
            return
        }
        switch mode {
        case .paymentSheet:
            guard let paymentSheet else { return }
            paymentSheet.present(from: presenter) { [weak self] result in self?.finish(result) }

        case .flowController:
            guard let flowController else { return }
            flowController.confirm(from: presenter) { [weak self] result in self?.finish(result) }

        case .embedded:
            guard let embeddedElement else { return }
            embeddedElement.presentingViewController = presenter
            embeddedElement.confirm { [weak self] result in self?.finish(result) }
        }
    }

    func embeddedView() -> UIView? {
        embeddedElement?.view
    }

    func destroy() {
        paymentSheet = nil
        flowController = nil
        embeddedElement?.delegate = nil
        embeddedElement = nil
    }

    // MARK: EmbeddedPaymentElementDelegate

    func embeddedPaymentElementDidUpdateHeight(embeddedPaymentElement: EmbeddedPaymentElement) {
        reportEmbeddedHeight(embeddedPaymentElement)
    }

    func embeddedPaymentElementDidUpdatePaymentOption(embeddedPaymentElement: EmbeddedPaymentElement) {
        listener.onPaymentOptionChanged(label: embeddedPaymentElement.paymentOption?.label)
    }

    // MARK: Helpers

    private func finish(_ result: PaymentSheetResult) {
        switch result {
        case .completed:
            listener.onResult(status: "completed", errorMessage: nil)
        case .canceled:
            listener.onResult(status: "canceled", errorMessage: nil)
        case let .failed(error):
            listener.onResult(status: "failed", errorMessage: error.localizedDescription)
        }
    }

    private func reportEmbeddedHeight(_ element: EmbeddedPaymentElement) {
        let view = element.view
        // Compose gives the view the sheet's width (screen minus its 16 pt gutters)
        let width = view.bounds.width > 0 ? view.bounds.width : UIScreen.main.bounds.width - 32
        let size = view.systemLayoutSizeFitting(
            CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        listener.onEmbeddedHeightChanged(height: Double(size.height))
    }

    /** Asks Kotlin (and so the backend) for the intent's client secret. */
    private func requestClientSecret(savePaymentMethod: Bool) async throws -> String {
        try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.main.async {
                self.listener.createIntent(savePaymentMethod: savePaymentMethod) { clientSecret, errorMessage in
                    if let clientSecret {
                        continuation.resume(returning: clientSecret)
                    } else {
                        continuation.resume(
                            throwing: GoZipoStripeError(message: errorMessage ?? "Payment couldn't be started")
                        )
                    }
                }
            }
        }
    }

    private func makeIntentConfiguration(_ config: GoZipoStripeConfig) -> PaymentSheet.IntentConfiguration {
        let mode: PaymentSheet.IntentConfiguration.Mode = config.setup
            ? .setup(currency: config.currency, setupFutureUsage: .offSession)
            : .payment(amount: Int(config.amountMinor), currency: config.currency)

        return PaymentSheet.IntentConfiguration(
            mode: mode,
            // Cards only; Apple Pay is a card wallet, so it comes with it
            paymentMethodTypes: ["card"],
            confirmHandler: { [weak self] _, shouldSavePaymentMethod in
                guard let self else { throw GoZipoStripeError(message: "Checkout closed") }
                return try await self.requestClientSecret(savePaymentMethod: shouldSavePaymentMethod)
            }
        )
    }

    private func makeSheetConfiguration(_ config: GoZipoStripeConfig) -> PaymentSheet.Configuration {
        var configuration = PaymentSheet.Configuration()
        configuration.merchantDisplayName = config.merchantDisplayName
        configuration.customer = .init(
            id: config.customerId,
            customerSessionClientSecret: config.customerSessionClientSecret
        )
        configuration.applePay = makeApplePay(config)
        configuration.cardBrandAcceptance = .allowed(brands: [.visa, .mastercard])
        configuration.allowsDelayedPaymentMethods = false
        return configuration
    }

    private func makeEmbeddedConfiguration(_ config: GoZipoStripeConfig) -> EmbeddedPaymentElement.Configuration {
        var configuration = EmbeddedPaymentElement.Configuration()
        configuration.merchantDisplayName = config.merchantDisplayName
        configuration.customer = .init(
            id: config.customerId,
            customerSessionClientSecret: config.customerSessionClientSecret
        )
        configuration.applePay = makeApplePay(config)
        configuration.cardBrandAcceptance = .allowed(brands: [.visa, .mastercard])
        configuration.allowsDelayedPaymentMethods = false
        return configuration
    }

    /**
     * Apple Pay needs a merchant ID registered with Apple and Stripe, and the app's Apple Pay
     * capability. It comes from Config.xcconfig (STRIPE_APPLE_MERCHANT_ID) via Info.plist;
     * blank hides Apple Pay.
     */
    private func makeApplePay(_ config: GoZipoStripeConfig) -> PaymentSheet.ApplePayConfiguration? {
        let merchantId = (Bundle.main.object(forInfoDictionaryKey: "GoZipoAppleMerchantId") as? String)?
            .trimmingCharacters(in: .whitespaces) ?? ""
        
        guard merchantId.hasPrefix("merchant.") else { return nil }
        return .init(merchantId: merchantId, merchantCountryCode: config.merchantCountry)
    }
}

/** The view controller on top, to present Stripe's sheets from. */
private func topViewController() -> UIViewController? {
    let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
    let scene = scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
    var top = scene?.keyWindow?.rootViewController
    while let presented = top?.presentedViewController, !presented.isBeingDismissed {
        top = presented
    }
    return top
}
