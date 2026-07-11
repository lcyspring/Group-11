# Rootless Podman deployment

This directory is independent from `docker-compose/`. It starts the project
directly on the real host with one rootless Podman Pod and Podman's default
Pasta network.

## Layout

- `up.sh`: creates the Pod and starts all services.
- `down.sh`: stops the Pod; `--volumes` also removes its persistent data.
- `Containerfile`: one multi-target Containerfile for MySQL, the server,
  InitService, Web, and Mall images.
- `nginx/`: Pod-specific frontend configuration. Services in a shared Pod
  reach the backend through `127.0.0.1`.

`Containerfile` only packages existing artifacts and SQL files. It never
compiles source code, so source control and compilation stay on the real
host. At runtime, no project file is bind-mounted: all application assets are
inside their images, while service data lives in named volumes. This also
works when the project itself is stored on a network drive.

## Start

On Ubuntu, install the build and rootless-Podman prerequisites once:

```bash
cd podman
bash ./install-build-deps-ubuntu.sh
```

Build the application assets before starting the Pod. The check command only
reports missing tools or artifacts; it never builds anything.

```bash
cd podman
bash ./build-assets.sh --check
# Requires HBuilderX CLI 3.1.5+; set HBUILDERX_CLI if cli is not on PATH.
bash ./build-mall-h5.sh
bash ./build-assets.sh
```

`build-assets.sh` builds the Server, InitService, and management Web frontend.
`build-mall-h5.sh` uses HBuilderX CLI to build the Mall H5 frontend and
normalizes its output to `MallFrontend/unpackage/dist/build/web/`. Alternatively,
build every artifact in one command with
`bash ./build-assets.sh --build-mall`. The `web` HBuilderX CLI platform is also
available with HBuilderX 4.67-alpha+ via `HBUILDERX_PLATFORM=web`; the default
`h5` platform works with HBuilderX 3.1.5+.

After the assets are ready, start the Pod:

```bash
cd podman
bash ./up.sh
```

The required artifacts are:

- `Server/mitedtsm-server/target/mitedtsm-server.jar`
- `InitService/target/mitedtsm-init-service.jar`
- `Web/dist-prod/`
- `MallFrontend/unpackage/dist/build/web/`

The management frontend is built with the production configuration. Its API
base URL is intentionally relative (`/admin-api`), so browsers outside the
Linux host use the Pod's published web port and Nginx proxies the request to
the backend. Do not set it to `http://localhost:8080`, because `localhost`
would then mean the visitor's own computer.

The script first imports a missing base image from `../docker-images/`. When
that archive is absent, its default `IMAGE_SOURCE=auto` mode pulls the image
from its registry instead. It then builds all runtime images from this
directory's `Containerfile`.

For a fully offline, reproducible deployment, copy `docker-images/` with the
project and run `IMAGE_SOURCE=archive bash ./up.sh`. To always fetch current
registry images, use `IMAGE_SOURCE=pull bash ./up.sh`.

## Network-drive projects

The project may live on a network drive: `up.sh` reads it only while building
images and never bind-mounts it into a running container. Keep rootless
Podman storage and its named volumes on a local filesystem (the default is
under `~/.local/share/containers/`). If building itself fails while reading
the network drive, build on a local disk and transfer the resulting images.

To use alternative host ports:

```bash
SERVER_PORT=18080 WEB_PORT=18081 MALL_PORT=18082 bash ./up.sh
```

## Network and proxy

No `--network=pasta` is necessary: it is the rootless Podman default. The
Pod publishes 8080, 8081, and 8082 to the real host.

Proxy use is disabled by default. `up.sh`, `build-assets.sh`,
`build-mall-h5.sh`, and `install-build-deps-ubuntu.sh` clear the standard proxy environment variables;
Podman builds and containers also receive `--http-proxy=false`. To opt in to
the host proxy deliberately, set `USE_HOST_PROXY=true` for the command:

```bash
USE_HOST_PROXY=true bash ./install-build-deps-ubuntu.sh
USE_HOST_PROXY=true bash ./build-mall-h5.sh
USE_HOST_PROXY=true bash ./build-assets.sh
USE_HOST_PROXY=true bash ./up.sh
```

When proxy use is enabled, a proxy listening on `127.0.0.1` or `localhost` is
rewritten to `host.containers.internal`, Pasta's dedicated host mapping inside
the Pod. No host network, nested Podman, systemd, or Compose is used.
