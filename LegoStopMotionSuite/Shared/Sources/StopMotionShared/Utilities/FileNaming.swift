import Foundation

public enum FileNaming {
    public static func frameName(index: Int) -> String {
        String(format: "frame_%03d.jpg", index)
    }
}
