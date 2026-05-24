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
    dependencies: [
        .package(url: "https://github.com/weichsel/ZIPFoundation.git", from: "0.9.19")
    ],
    targets: [
        .target(
            name: "StopMotionShared",
            dependencies: [
                .product(name: "ZIPFoundation", package: "ZIPFoundation")
            ]
        )
    ]
)
