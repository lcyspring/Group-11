# Rootless Podman deployment

This is a Podman-only deployment. It starts the project directly on the real
host with one rootless Podman Pod and Podman's default Pasta network; it never
uses Docker Engine, the Docker CLI, a Docker socket, or Compose.

The default `docker.io` image references are OCI registry addresses, not a
runtime dependency on Docker. Podman pulls them directly, or it can load OCI
archives created by Podman itself for an offline deployment.

中文完整流程请阅读：[Podman 全流程操作指南](DEPLOY_GUIDE_ZH.md)。

## Layout

- `up.sh`: creates the Pod and starts all services.
- `down.sh`: stops and removes the Pod; `--volumes` also removes its persistent data.
- `image-archives.sh`: creates portable OCI base-image archives with Podman.
- `images/`: default location for those ignored offline archives.
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

On CachyOS/Arch, use the matching installer instead. It uses Pasta through
`passt`; `slirp4netns` is not required.

```bash
cd podman
bash ./install-build-deps-cachyos.sh
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

When the repository is on a filesystem without symbolic-link support, such as
a VMware shared folder mounted at `/mnt/hgfs`, `build-assets.sh` automatically
stages the Web build on a native local filesystem and copies `Web/dist-prod/`
back after it succeeds. To choose a staging parent explicitly, use
`WEB_BUILD_WORKDIR=/tmp bash ./build-assets.sh`.

If the Java artifacts already succeeded and only the management-Web build
failed, rerun just that stage with `bash ./build-assets.sh --web-only`; it does
not invoke Maven or rebuild either JAR.

The `Web/build/vite/` directory is Vite configuration source and must be
tracked in Git. If a checkout reports that `./build/vite` cannot be resolved,
update the repository before retrying; reinstalling pnpm dependencies cannot
restore missing source files.

The current Mall H5 output is versioned in Git so deployment members can pull
and use it directly. HBuilderX is only needed when publishing a new Mall H5
revision; generated Server JARs, management-Web output, and image archives
remain unversioned.

After the assets are ready, start the Pod:

```bash
cd podman
bash ./up.sh --check
bash ./up.sh
```

`--check` is safe to run on a new host: it validates the rootless Podman
environment and application artifacts, but does not load, pull, build, create,
or start anything.

## Fast restart paths

The normal no-argument command remains the deployment path after application
assets or SQL have changed. For an unchanged deployment, avoid its image
packaging work:

```bash
# `down.sh` removed the Pod, but the existing local runtime images are valid.
bash ./up.sh --no-build

# The Pod still exists but was stopped, for example after `podman pod stop`.
bash ./up.sh --fast
```

`--fast` starts/checks the existing containers without rebuilding or replacing
them. `--no-build` recreates the Pod from the current local images. Both modes
keep all named volumes intact; run plain `up.sh` to deploy changed artifacts.

If a previous start reached `Spring Boot server is ready.` but stopped before
the two Nginx frontends were created, start just those missing containers
without rebuilding images or restarting the Pod:

```bash
bash ./up.sh --frontends-only
```

This recovery mode is also useful after an interrupted terminal session. If it
cannot start a frontend or reach its port, it prints the last health-check
error instead of hiding it during retries.

After changing only the management Web output, avoid a full Pod replacement
and the Spring Boot startup wait:

```bash
bash ./build-assets.sh --web-only
bash ./up.sh --rebuild-web
```

`--rebuild-web` packages `Web/dist-prod/` and replaces only the Web Nginx
container. It leaves the Java server, databases, Redis, RabbitMQ, TDengine,
and Mall container running.

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

The script first imports a missing base image from `podman/images/`. When that
OCI archive is absent, its default `IMAGE_SOURCE=auto` mode pulls the image
directly through Podman. It then builds all runtime images from this
directory's `Containerfile`.

For a fully offline deployment, run the following on a connected machine with
Podman, copy `podman/images/` with the project, and then deploy with
`IMAGE_SOURCE=archive`:

```bash
cd podman
bash ./image-archives.sh --pull
IMAGE_SOURCE=archive bash ./up.sh
```

To keep archives outside the repository, point both commands at the same
directory with `IMAGE_ARCHIVE_DIR=/absolute/path/to/archives`. To always fetch
current registry images, use `IMAGE_SOURCE=pull bash ./up.sh`.

For a transition from the old project archive directory, set
`IMAGE_ARCHIVE_DIR=../docker-images` explicitly. Podman can load those image
archives directly; this still does not require Docker to be installed.

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
