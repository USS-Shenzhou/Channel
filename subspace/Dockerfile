FROM debian:bookworm-slim

ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY bin/${TARGETARCH}/subspace /usr/local/bin/subspace
RUN chmod +x /usr/local/bin/subspace

EXPOSE 10943/tcp 10944/tcp

ENTRYPOINT ["/usr/local/bin/subspace", "--no-json-config"]
