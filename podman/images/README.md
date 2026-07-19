# Podman OCI image archives

`../operations/images/image-archives.sh` writes the offline base-image archives for `deploy.sh` into
this directory. The generated `*.tar` files are ignored by Git. They use the
OCI archive format and are created and loaded with Podman; Docker is not
required.

`../operations/images/build-image-archives.sh` additionally saves the Ubuntu 26.04 Server/Web and headless HBuilderX build
toolchain images with SHA-256 files. These archives are recommended for member onboarding because rebuilding
the toolchains is slower and more environment-sensitive than packaging project runtime images.

MySQL is not a project runtime image. Deployment starts the configured official
the configured digest-pinned official MySQL 8.0.46 image directly and streams the repository bootstrap
or compatibility manifests over stdin. `build-images.sh` packages only the four
application targets: InitService, Server, Web, and Mall.
