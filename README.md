# RPC Multi-service + Pact Broker

## Modules
- `rpc-contract`: gRPC/protobuf contract.
- `service-a`: rate-provider (gRPC server, ZooKeeper registration, Pact provider verification).
- `service-b`: rate-printer (gRPC client, ZooKeeper discovery, Pact consumer contract).

## Infrastructure
Start ZooKeeper and Pact Broker:

```bash
docker compose up -d zookeeper pact-broker-db pact-broker
```

Endpoints:
- ZooKeeper: `localhost:2181`
- Pact Broker: `http://localhost:9292`

## Run services
Start provider instances:

```bash
./gradlew :service-a:bootRun -Dgrpc.server.port=50051
./gradlew :service-a:bootRun -Dgrpc.server.port=50052
```

Start consumer:

```bash
./gradlew :service-b:bootRun
```

## Pact flow
Generate consumer pact (`rate-printer -> rate-provider`):

```bash
./gradlew :service-b:test
```

Publish generated pact to broker:

```bash
./gradlew :service-b:pactPublish -Dpactbroker.url=http://localhost:9292
```

Verify provider API against contracts from broker:

```bash
./gradlew :service-a:test -Dpactbroker.url=http://localhost:9292
```

`service-a` build includes Pact verification test, so provider verification is executed during `build`.
