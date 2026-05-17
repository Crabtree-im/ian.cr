// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "StopMotionShared",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "StopMotionShared",
            targets: ["StopMotionShared"]
        )
    ],
    targets: [
        .target(
            name: "StopMotionShared"
        )
    ]
)
