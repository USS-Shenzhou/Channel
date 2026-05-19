#include "Config.h"
#include "network/ServerListener.h"
#include "network/ClientTcpListener.h"
#include "manager/TokenManager.h"

namespace {
    asio::io_context* clientContextPtr = nullptr;
    std::shared_ptr<subspace::ClientTcpListener> clientTcpListener;
}

namespace subspace {
    void initClientListening() {
        static bool initialized = false;
        if (initialized) {
            return;
        }
        initialized = true;

        switch (*getConfig().protocol) {
            case Protocol::TCP:
                clientTcpListener = std::make_shared<ClientTcpListener>(*clientContextPtr);
                break;
            case Protocol::UDP:
                // TODO
                break;
            case Protocol::GRPC:
                // TODO
                break;
        }
    }
}

int main(int argc, char* argv[]) {
    subspace::loadConfig(argc, argv);
    subspace::TokenManager::init();

    asio::io_context serverContext;
    asio::io_context clientContext;
    // ReSharper disable once CppDFALocalValueEscapesFunction
    clientContextPtr = &clientContext;

    auto workGuard = asio::make_work_guard(clientContext);
    std::vector<std::thread> clientThreads;
    for (int i = 0; i < subspace::getConfig().threads; i++) {
        clientThreads.emplace_back([&clientContext]() { clientContext.run(); });
    }

    subspace::ServerListener serverListener(serverContext);
    spdlog::info("Subspace relay activated.");
    serverContext.run();

    workGuard.reset();
    clientContext.stop();
    for (auto& t : clientThreads) { t.join(); }
    return 0;
}
