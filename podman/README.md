# Rootless Podman deployment

This project builds in a dedicated Ubuntu 26.04 container and runs as one
rootless Podman Pod. Docker Engine, the Docker CLI, Docker sockets, and Compose
are not used. `docker.io` in an image name is an OCI registry address only.

中文入口请阅读：[中文 README](README_ZH.md)。完整流程另见：[Podman 全流程操作指南](DEPLOY_GUIDE_ZH.md)、
[编译构建部署手册](OPERATIONS_ZH.md)和[配置字段参考](config/YAML_FIELDS_ZH.md)。

## Configuration contract

Runtime entry points accept exactly one argument: a YAML configuration path.
Deployment behavior is never selected with command options or environment
variable overrides.

```bash
cd podman
bash ./up.sh ./config/runtime-local-check.yaml
bash ./down.sh ./config/runtime-local-check.yaml
bash ./image-archives.sh ./config/runtime-local-check.yaml
```

The committed configuration uses `operation.startup_mode: check` and
`operation.shutdown_mode: check`; both commands therefore validate without
changing Pod or volume state. Copy it to a local deployment configuration and
explicitly select a mode before making a stateful operation:

- startup: `full`, `fast`, `no-build`, `frontends-only`, `rebuild-server`, `rebuild-web`, or `rebuild-mall`;
- shutdown: `stop`;
- image archives: `save` or `pull-save`;
- destructive data removal additionally requires
  `operation.remove_volumes_on_down: true`.

Relative paths such as `image.archive_dir` are resolved relative to the YAML
file. Missing values, duplicate keys, mappings deeper than two levels, invalid
booleans, ports, or modes fail before any deployment action.

Configuration includes Pod/container/volume names, host and container ports,
image and archive names, MySQL/RabbitMQ settings, the Spring profile, proxy
URLs, and all health-check paths and retry limits. Proxy URLs must be written
in YAML; host proxy environment variables are not consumed by `up.sh`.

## Ubuntu 26.04 build

Server, InitService, CRM tests/JaCoCo, and management Web builds run inside the
dedicated Ubuntu 26.04 image:

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-ubuntu-26.04.yaml
```

The build entry point also accepts exactly one YAML path. Named Podman volumes
hold Maven, pnpm-store, and Web `node_modules` caches. The repository must be on
a filesystem with symbolic-link support; no legacy staging/copy-back path is
used.

Mall H5 uses HBuilderX's non-graphical uni-app compiler in a separate Ubuntu
26.04 image:

```bash
cd podman
bash ./build-mall-h5-in-ubuntu.sh ./config/build-mall-h5-ubuntu-26.04.yaml
```

This entry point likewise accepts only its YAML path. It does not start the
HBuilderX IDE, Qt, X11, or Xvfb. When the configured image does not exist (or
`image.rebuild` is `true`), it copies only HBuilderX's bundled Node, Vue 3/Vite
uni-app compiler, and Dart Sass runtime from `hbuilderx.source_dir`. Normal
builds use the resulting self-contained image without reading or mounting the
host HBuilderX installation. `MallFrontend/unpackage/` is generated locally
and ignored by Git; build it before packaging a deployment image.

`build-assets.sh` remains the Ubuntu 26.04 host-toolchain helper for other
project members. This workstation should use the two container entry points
above.

Required runtime artifacts are:

- `Server/mitedtsm-server/target/mitedtsm-server.jar`
- `InitService/target/mitedtsm-init-service.jar`
- `Web/dist-prod/`
- `MallFrontend/unpackage/dist/build/web/`

`Containerfile` only packages these artifacts and database files into runtime
images. Running containers do not bind-mount project files; persistent service
data lives in named volumes.

## Startup modes

Set `operation.startup_mode` in the selected YAML and run the same command:

```bash
bash ./up.sh ./config/my-runtime.yaml
```

- `full` loads or pulls configured base images, packages current artifacts,
  then replaces the Pod while retaining named volumes.
- `no-build` recreates the Pod from configured local runtime images.
- `fast` starts an existing stopped Pod and missing frontend containers.
- `frontends-only` replaces only Web and Mall containers in a running Pod.
- `rebuild-server` packages the current Server JAR, applies compatibility migrations, and replaces only Server.
- `rebuild-web` packages current `Web/dist-prod/` and replaces only Web.
- `rebuild-mall` packages current Mall H5 output and replaces only Mall.
- `check` validates rootless Podman, configuration, artifacts, and offline
  archive prerequisites without loading, pulling, building, or starting.

Use `rebuild-server` after an ordinary backend change and `full` when database
packaging, base images, or artifact freshness is uncertain. The
Web Nginx configuration does not cache `index.html`, while hashed assets remain
cacheable.

## Images, network, and proxy

`image.source` is explicit:

- `auto`: use a configured local image/archive first, otherwise pull;
- `archive`: never use registries and require missing images as archives;
- `pull`: fetch configured images from their registries.

Create offline archives by selecting `operation.archive_mode: save` (local
images only) or `pull-save` (pull first), then call `image-archives.sh` with the
same YAML. Set `image.source` and `image.archive_dir` for deployment. Host
addresses and all published ports are likewise YAML values.

Rootless Podman's default Pasta network is used. When
`network.use_host_proxy: false`, proxy environment variables are cleared and
Podman receives `--http-proxy=false`. To enable a proxy, set the boolean and at
least one of `network.http_proxy`, `network.https_proxy`, or
`network.all_proxy` to an explicit URL; use `none` for unused URLs. Loopback
proxy hostnames are translated to the configured `network.host_proxy_name`.

## Layout and tests

- `config/`: explicit build and runtime YAML files.
- `lib/yaml-config.sh`: non-evaluating, two-level YAML scalar reader.
- `tests/runtime-config/`: parser, CLI contract, preflight, and Pod-state tests.
- `up.sh` / `down.sh`: YAML-only runtime entry points.
- `Containerfile.build-ubuntu`: Ubuntu 26.04 build toolchain image.
- `Containerfile.hbuilderx-ubuntu`: headless Mall H5 compiler image.
- `build-mall-h5-in-ubuntu.sh`: YAML-only Mall H5 container build entry point.
- `Containerfile`: multi-target runtime packaging.
- `image-archives.sh` / `images/`: portable Podman base-image archives.

Run the structured runtime configuration test with:

```bash
bash ./tests/runtime-config/run.sh ./config/runtime-local-check.yaml
```

Run the Mall H5 container integration test with:

```bash
bash ./tests/mall-h5-build/run.sh ./config/build-mall-h5-ubuntu-26.04.yaml
```
