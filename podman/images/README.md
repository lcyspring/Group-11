# Podman OCI image archives

`image-archives.sh` writes the offline base-image archives for `up.sh` into
this directory. The generated `*.tar` files are ignored by Git. They use the
OCI archive format and are created and loaded with Podman; Docker is not
required.
