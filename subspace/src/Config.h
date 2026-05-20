//
// Created by USS_Shenzhou on 2026/4/9.
//

#ifndef SUBSPACE_CONFIG_H
#define SUBSPACE_CONFIG_H

#include <string>
#include <fstream>
#include <optional>
#include <nlohmann/json.hpp>
#include <spdlog/spdlog.h>

namespace subspace {
    enum class Protocol {
        TCP,
        UDP,
        GRPC
    };

    enum class SecurityLevel {
        NONE,
        LOW,
        MID,
        HIGH
    };

    struct Config {
        int serverPort = 10943;
        int clientPort = 10944;
        int threads = 4;
        std::string subspaceFrequency;

        std::optional<Protocol> protocol;
        std::optional<SecurityLevel> securityLevel;
    };

    inline Config& getConfig() {
        static Config instance;
        return instance;
    }

    inline bool ready() {
        return getConfig().protocol.has_value();
    }

    inline bool hasArg(int argc, char* argv[], std::string arg) {
        for (int i = 1; i < argc; i++) {
            if (arg == argv[i]) {
                return true;
            }
        }
        return false;
    }

    inline void createDefaultConfig(const std::string& path, bool exit) {
        auto defaultCfg = Config();
        nlohmann::json json = {
            {"serverPort", defaultCfg.serverPort},
            {"clientPort", defaultCfg.clientPort},
            {"subspaceFrequency", defaultCfg.subspaceFrequency},
            {"threads", defaultCfg.threads},
        };
        std::ofstream out(path);
        out << json.dump(4);
        spdlog::error("SubspaceConfig.json not found. Created default config file.");
        if (exit) {
            spdlog::warn("Subspace will exit. You can edit the config and restart.");
            std::exit(943);
        }
    }

    inline void loadFromJson(int argc, char* argv[]) {
        const std::string path = "SubspaceConfig.json";
        std::ifstream ifs(path);
        if (!ifs.is_open()) {
            createDefaultConfig(path, !hasArg(argc, argv, "--no-json-config"));
        }
        auto json = nlohmann::json::parse(ifs);
        auto& config = getConfig();
        config.serverPort = json["serverPort"];
        config.clientPort = json["clientPort"];
        config.subspaceFrequency = json["subspaceFrequency"];
        config.threads = json["threads"];
    }

    inline void loadFromSysEnv() {
        auto& config = getConfig();
        if (auto val = std::getenv("SUBSPACE_SERVER_PORT")) {
            config.serverPort = std::stoi(val);
        }
        if (auto val = std::getenv("SUBSPACE_CLIENT_PORT")) {
            config.clientPort = std::stoi(val);
        }
        if (auto val = std::getenv("SUBSPACE_FREQUENCY")) {
            config.subspaceFrequency = val;
        }
        if (auto val = std::getenv("SUBSPACE_THREADS")) {
            config.threads = std::stoi(val);
        }
    }

    inline void loadFromArg(int argc, char* argv[]) {
        auto& config = getConfig();
        for (int i = 1; i < argc - 1; i++) {
            std::string arg = argv[i];
            if (arg == "--serverPort") {
                config.serverPort = std::stoi(argv[++i]);
            } else if (arg == "--clientPort") {
                config.clientPort = std::stoi(argv[++i]);
            } else if (arg == "--frequency") {
                config.subspaceFrequency = argv[++i];
            } else if (arg == "--threads") {
                config.threads = std::stoi(argv[++i]);
            }
        }
    }

    inline void loadConfig(int argc, char* argv[]) {
        loadFromSysEnv();
        loadFromJson(argc, argv);
        loadFromArg(argc, argv);
        auto& config = getConfig();
        if (config.subspaceFrequency.empty()) {
            spdlog::error("subspaceFrequency is empty.");
            std::exit(-1);
        }
        spdlog::info("Starting Subspace. serverPort: {}, clientPort: {}.", config.serverPort, config.clientPort);
    }
}

#endif //SUBSPACE_CONFIG_H
