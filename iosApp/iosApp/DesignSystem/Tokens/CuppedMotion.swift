import SwiftUI
import Shared

extension Animation {
    static let cuppedSpring = Animation.spring(
        response: CuppedMotion.shared.springResponse,
        dampingFraction: CuppedMotion.shared.springDamping
    )
}

enum Motion {
    static let staggerDelay = CuppedMotion.shared.staggerDelay
    static let tapScale = CuppedMotion.shared.tapScale
    static let tapOpacity = CuppedMotion.shared.tapOpacity
    static let shimmerDuration = CuppedMotion.shared.shimmerDuration
}
