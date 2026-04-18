cmake_minimum_required(VERSION 4.2)
project(subspace LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenSSL REQUIRED)
find_package(asio CONFIG REQUIRED)
find_package(spdlog CONFIG REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)

file(GLOB_RECURSE SOURCES src/*.cpp src/*.h)
add_executable(subspace ${SOURCES})

target_link_libraries(subspace PRIVATE
        OpenSSL::SSL
        OpenSSL::Crypto
        asio::asio
        spdlog::spdlog
        nlohmann_json::nlohmann_json
)

target_compile_definitions(subspace PRIVATE _CRT_SECURE_NO_WARNINGS)