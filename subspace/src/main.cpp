#include "Config.h"
#include "network/ServerListener.h"
#include "manager/TokenManager.h"

int main(int argc, char *argv[]) {
    subspace::loadConfig(argc, argv);
    subspace::TokenManager::init();

    asio::io_context serverContext;
    subspace::ServerListener serverListener(serverContext);
    spdlog::info("Subspace relay activated.");
    serverContext.run();
    return 0;
}
