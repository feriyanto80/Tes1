import SwiftUI
import WebKit
import CoreLocation
import UIKit

@main
struct PBSBalapApp: App {
    var body: some Scene {
        WindowGroup {
            UnifiedFormsScreen()
                .tint(Color(red: 0.07, green: 0.39, blue: 0.80))
        }
    }
}

private enum Module: Int, CaseIterable {
    case home = 0
    case balap = 1
    case survey = 2

    var title: String {
        switch self {
        case .home: return "PBS Balap"
        case .balap: return "BALAP Digital"
        case .survey: return "BA Survey Digital"
        }
    }

    var subtitle: String {
        switch self {
        case .home: return "Digital Forms"
        case .balap: return "Berita Acara Lapangan Project FTTH"
        case .survey: return "Berita Acara Survey Project FTTH"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .balap: return "doc.text.fill"
        case .survey: return "checklist"
        }
    }
}

final class FormPage: ObservableObject {
    let initialURL: URL
    @Published var loading = false
    @Published var progress = 0.0
    @Published var failed = false
    @Published var errorMessage = "Periksa koneksi internet lalu coba lagi."
    @Published var httpFallback = false
    weak var webView: WKWebView?

    init(initialURL: String) {
        self.initialURL = URL(string: initialURL)!
    }

    func reload() {
        failed = false
        if let webView {
            if webView.url == nil { webView.load(URLRequest(url: initialURL)) }
            else { webView.reload() }
        }
    }

    func openLegacyBALAP() {
        httpFallback = true
        failed = false
        if let url = URL(string: "http://balap.pbsgroup.id/") {
            webView?.load(URLRequest(url: url))
        }
    }
}

final class LocationStatus: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var authorization: CLAuthorizationStatus = .notDetermined

    override init() {
        super.init()
        authorization = manager.authorizationStatus
        manager.delegate = self
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        DispatchQueue.main.async { self.authorization = manager.authorizationStatus }
    }

    func askPermission() { manager.requestWhenInUseAuthorization() }

    func openSettings() {
        guard let link = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(link)
    }

    var diagnosticText: String {
        let permission: String
        switch authorization {
        case .authorizedAlways, .authorizedWhenInUse: permission = "Diizinkan"
        case .denied: permission = "Ditolak"
        case .restricted: permission = "Dibatasi perangkat"
        case .notDetermined: permission = "Belum diminta"
        @unknown default: permission = "Tidak diketahui"
        }
        return "Izin GPS aplikasi: \(permission)\n" +
          "Layanan lokasi iPhone: \(CLLocationManager.locationServicesEnabled() ? "Aktif" : "Nonaktif")\n\n" +
          "Lokasi pada iframe Google Apps Script juga memerlukan izin dari website. " +
          "Persetujuan iOS tidak dapat melewati pembatasan iframe."
    }
}

struct UnifiedFormsScreen: View {
    @State private var selected: Module = .home
    @State private var openedBALAP = false
    @State private var openedSurvey = false
    @State private var showingGPS = false
    @State private var showingHttpWarning = false

    @StateObject private var balap = FormPage(initialURL: "https://balap.pbsgroup.id/")
    @StateObject private var survey = FormPage(initialURL: "https://basl.pbsgroup.id/")
    @StateObject private var location = LocationStatus()

    private let navy = Color(red: 0.04, green: 0.12, blue: 0.24)
    private let blue = Color(red: 0.09, green: 0.39, blue: 0.81)
    private let green = Color(red: 0.02, green: 0.59, blue: 0.43)
    private let background = Color(red: 0.955, green: 0.970, blue: 0.984)

    private var activePage: FormPage? {
        switch selected {
        case .home: return nil
        case .balap: return balap
        case .survey: return survey
        }
    }

    private func choose(_ module: Module) {
        selected = module
        if module == .balap { openedBALAP = true }
        if module == .survey { openedSurvey = true }
    }

    var body: some View {
        GeometryReader { geometry in
            let compact = geometry.size.width < 360 || geometry.size.height < 390
            VStack(spacing: 0) {
                HStack(spacing: compact ? 7 : 10) {
                    Button { choose(.home) } label: {
                        Image("BrandLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: compact ? 34 : 41, height: compact ? 34 : 41)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .accessibilityLabel("Kembali ke beranda PBS Balap")

                    VStack(alignment: .leading, spacing: 2) {
                        Text(selected == .home ? "PBS Balap" : selected.title)
                            .font(.system(size: compact ? 15 : 18, weight: .bold))
                            .lineLimit(1)
                        if !compact {
                            Text(selected == .home ? "Digital Forms" : "PBS Balap")
                                .font(.system(size: 11))
                                .foregroundStyle(.white.opacity(0.72))
                        }
                    }
                    Spacer(minLength: 2)
                    Button { showingGPS = true } label: {
                        Image(systemName: "location.circle")
                            .font(.system(size: 23))
                            .frame(width: 40, height: 42)
                    }
                    .accessibilityLabel("Periksa izin GPS")
                    if selected != .home {
                        Button { activePage?.reload() } label: {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 21))
                                .frame(width: 40, height: 42)
                        }
                        .accessibilityLabel("Muat ulang formulir")
                    }
                }
                .foregroundStyle(.white)
                .padding(.horizontal, compact ? 9 : 13)
                .frame(height: compact ? 52 : 60)
                .background(navy)

                if let activePage, activePage.loading {
                    ProgressView(value: activePage.progress, total: 1)
                        .tint(.cyan)
                        .progressViewStyle(.linear)
                }

                ZStack {
                    homePage
                        .opacity(selected == .home ? 1 : 0)
                        .allowsHitTesting(selected == .home)
                        .accessibilityHidden(selected != .home)

                    // Once opened, both WKWebViews remain mounted. Navigating
                    // between BALAP/BASL will not discard the other in-progress form.
                    if openedBALAP {
                        formView(balap, module: .balap)
                            .opacity(selected == .balap ? 1 : 0)
                            .allowsHitTesting(selected == .balap)
                            .accessibilityHidden(selected != .balap)
                    }
                    if openedSurvey {
                        formView(survey, module: .survey)
                            .opacity(selected == .survey ? 1 : 0)
                            .allowsHitTesting(selected == .survey)
                            .accessibilityHidden(selected != .survey)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(background)

                HStack(spacing: 4) {
                    ForEach(Module.allCases, id: \.rawValue) { module in
                        Button { choose(module) } label: {
                            VStack(spacing: 3) {
                                Image(systemName: module.icon).font(.system(size: 19))
                                Text(module == .home ? "Beranda" : module == .balap ? "BALAP" : "BA Survey")
                                    .font(.system(size: 11, weight: selected == module ? .semibold : .regular))
                            }
                            .foregroundStyle(selected == module ? blue : Color.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                            .background(selected == module ? Color.blue.opacity(0.08) : Color.clear)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(Color(uiColor: .systemBackground))
            }
            .background(background)
            .alert("Informasi GPS", isPresented: $showingGPS) {
                Button("Minta izin") { location.askPermission() }
                Button("Pengaturan") { location.openSettings() }
                Button("Tutup", role: .cancel) {}
            } message: { Text(location.diagnosticText) }
            .alert("Koneksi BALAP tanpa HTTPS", isPresented: $showingHttpWarning) {
                Button("Batal", role: .cancel) {}
                Button("Lanjutkan HTTP") { balap.openLegacyBALAP() }
            } message: {
                Text("HTTP tidak mengenkripsi data laporan. Gunakan hanya jika server BALAP belum mendukung HTTPS; fitur lokasi browser mungkin terbatas.")
            }
        }
    }

    private var homePage: some View {
        ScrollView {
            VStack(spacing: 15) {
                VStack(spacing: 7) {
                    Image("BrandLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 93, height: 93)
                        .accessibilityLabel("Logo PBS")
                    Text("PBS Balap").font(.system(size: 27, weight: .bold)).foregroundStyle(navy)
                    Text("Satu Aplikasi • Dua Form Digital")
                        .font(.system(size: 13)).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
                .background(.white, in: RoundedRectangle(cornerRadius: 17))
                .padding(.top, 16)

                HStack {
                    Text("Formulir Digital FTTH")
                        .font(.system(size: 17, weight: .bold)).foregroundStyle(navy)
                    Spacer()
                }
                .padding(.top, 7)

                moduleCard(.balap, color: blue,
                           detail: "Pencatatan hasil pekerjaan lapangan, lokasi, dan dokumentasi.")
                moduleCard(.survey, color: green,
                           detail: "Pencatatan survei lokasi, desa, PIC, dan administrasi lapangan.")

                Text("Kedua form tetap terhubung ke website PBS masing-masing. Data dan proses persetujuannya tidak dipindahkan.")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
                    .padding(.top, 7)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 26)
            .frame(maxWidth: 780)
            .frame(maxWidth: .infinity)
        }
    }

    private func moduleCard(_ module: Module, color: Color, detail: String) -> some View {
        Button { choose(module) } label: {
            HStack(alignment: .center, spacing: 13) {
                Image(systemName: module.icon)
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(color)
                    .frame(width: 57, height: 62)
                    .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 13))
                VStack(alignment: .leading, spacing: 5) {
                    Text(module.title).font(.system(size: 17, weight: .bold)).foregroundStyle(navy)
                    Text(module.subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                    Text(detail)
                        .font(.system(size: 11))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 1)
                Image(systemName: "chevron.right").foregroundStyle(color).font(.system(size: 16, weight: .bold))
            }
            .padding(15)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Buka " + module.title)
    }

    private func formView(_ page: FormPage, module: Module) -> some View {
        EmbeddedFormWeb(page: page, module: module)
            .overlay {
                if page.failed && selected == module {
                    VStack(spacing: 14) {
                        Image(systemName: "wifi.exclamationmark")
                            .font(.system(size: 35))
                            .foregroundStyle(.orange)
                        Text(module.title + " tidak dapat dibuka")
                            .font(.headline)
                        Text(page.errorMessage)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                        Button("Coba lagi") { page.reload() }
                            .buttonStyle(.borderedProminent)
                        if module == .balap && !page.httpFallback {
                            Button("Gunakan HTTP BALAP (tidak terenkripsi)") {
                                showingHttpWarning = true
                            }
                            .font(.system(size: 12))
                        }
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(uiColor: .systemBackground))
                }
            }
    }
}

private struct EmbeddedFormWeb: UIViewRepresentable {
    @ObservedObject var page: FormPage
    let module: Module

    func makeCoordinator() -> Coordinator { Coordinator(page) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.allowsInlineMediaPlayback = true

        let web = WKWebView(frame: .zero, configuration: configuration)
        web.navigationDelegate = context.coordinator
        web.uiDelegate = context.coordinator
        web.allowsBackForwardNavigationGestures = true
        web.scrollView.keyboardDismissMode = .interactive
        web.isOpaque = false
        web.backgroundColor = .white
        page.webView = web
        context.coordinator.observe(web)
        web.load(URLRequest(url: page.initialURL))
        return web
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // Do not recreate or reload a form when switching tabs or rotating.
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        private let page: FormPage
        private var progressObserver: NSKeyValueObservation?

        init(_ page: FormPage) { self.page = page }

        func observe(_ web: WKWebView) {
            progressObserver = web.observe(\.estimatedProgress, options: [.new]) { [weak self] web, _ in
                DispatchQueue.main.async { self?.page.progress = web.estimatedProgress }
            }
        }

        private func isInternal(_ url: URL) -> Bool {
            guard let scheme = url.scheme?.lowercased(),
                  let host = url.host?.lowercased() else {
                return url.scheme == "about"
            }
            if scheme == "https" {
                return host == "balap.pbsgroup.id"
                    || host == "basl.pbsgroup.id"
                    || host == "script.google.com"
                    || host == "script.googleusercontent.com"
                    || host.hasSuffix(".script.googleusercontent.com")
                    || host.hasSuffix("-script.googleusercontent.com")
            }
            return scheme == "http" && host == "balap.pbsgroup.id"
        }

        func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = action.request.url else {
                decisionHandler(.cancel); return
            }
            if let target = action.targetFrame, !target.isMainFrame {
                decisionHandler(.allow)
            } else if isInternal(url) {
                decisionHandler(.allow)
            } else {
                decisionHandler(.cancel)
                if ["http","https","tel","mailto"].contains(url.scheme?.lowercased() ?? "") {
                    UIApplication.shared.open(url)
                }
            }
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            DispatchQueue.main.async {
                self.page.loading = true
                self.page.failed = false
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            DispatchQueue.main.async { self.page.loading = false }
            if let host = webView.url?.host?.lowercased(),
               host == "balap.pbsgroup.id" || host == "basl.pbsgroup.id" {
                let js = """
                (function(){
                  if(!document.head)return;
                  if(!document.querySelector('meta[name="viewport"]')){
                    var meta=document.createElement('meta');
                    meta.name='viewport';
                    meta.content='width=device-width,initial-scale=1';
                    document.head.appendChild(meta);
                  }
                  if(!document.getElementById('pbs-ios-mobile')){
                    var s=document.createElement('style');
                    s.id='pbs-ios-mobile';
                    s.textContent='img,video{max-width:100%;height:auto}' +
                      'input,textarea,select{max-width:100%;box-sizing:border-box}';
                    document.head.appendChild(s);
                  }
                })();
                """
                webView.evaluateJavaScript(js, completionHandler: nil)
            }
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!,
                     withError error: Error) {
            if (error as NSError).code == NSURLErrorCancelled { return }
            DispatchQueue.main.async {
                self.page.loading = false
                self.page.errorMessage = error.localizedDescription
                self.page.failed = true
            }
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!,
                     withError error: Error) {
            DispatchQueue.main.async { self.page.loading = false }
        }

        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                     for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
            guard let url = action.request.url else { return nil }
            if isInternal(url) { webView.load(action.request) }
            else if ["http", "https"].contains(url.scheme?.lowercased() ?? "") {
                UIApplication.shared.open(url)
            }
            return nil
        }

        func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                     initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
            guard let controller = UIApplication.shared.pbsTopController else {
                completionHandler(); return
            }
            let alert = UIAlertController(title: frame.securityOrigin.host, message: message,
                                          preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler() })
            controller.present(alert, animated: true)
        }

        func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                     initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
            guard let controller = UIApplication.shared.pbsTopController else {
                completionHandler(false); return
            }
            let alert = UIAlertController(title: frame.securityOrigin.host, message: message,
                                          preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Batal", style: .cancel) { _ in completionHandler(false) })
            alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler(true) })
            controller.present(alert, animated: true)
        }

        func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String,
                     defaultText: String?, initiatedByFrame frame: WKFrameInfo,
                     completionHandler: @escaping (String?) -> Void) {
            guard let controller = UIApplication.shared.pbsTopController else {
                completionHandler(nil); return
            }
            let alert = UIAlertController(title: frame.securityOrigin.host, message: prompt,
                                          preferredStyle: .alert)
            alert.addTextField { $0.text = defaultText }
            alert.addAction(UIAlertAction(title: "Batal", style: .cancel) { _ in completionHandler(nil) })
            alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
                completionHandler(alert.textFields?.first?.text)
            })
            controller.present(alert, animated: true)
        }
    }
}

private extension UIApplication {
    var pbsTopController: UIViewController? {
        let scene = connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var controller = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let next = controller?.presentedViewController { controller = next }
        return controller
    }
}
