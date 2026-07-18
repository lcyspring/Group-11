# Podman OCI image archives

`image-archives.sh` writes the offline base-image archives for `deploy.sh` into
this directory. The generated `*.tar` files are ignored by Git. They use the
OCI archive format and are created and loaded with Podman; Docker is not
required.

`build-image-archives.sh` additionally saves the Ubuntu 26.04 Server/Web and headless HBuilderX build
toolchain images with SHA-256 files. These archives are recommended for member onboarding because rebuilding
the toolchains is slower and more environment-sensitive than packaging project runtime images.
