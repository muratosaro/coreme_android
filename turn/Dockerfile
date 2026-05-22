FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends coturn && rm -rf /var/lib/apt/lists/*
COPY turnserver.conf /etc/turnserver.conf
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && mkdir -p /var/log/coturn /var/run/coturn
EXPOSE 3478/tcp 3478/udp 5349/tcp 5349/udp
EXPOSE 49152-65535/udp
ENTRYPOINT ["/entrypoint.sh"]
