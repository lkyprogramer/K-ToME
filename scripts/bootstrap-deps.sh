#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BOOTSTRAP_REPO="$ROOT_DIR/.bootstrap/m2"
GRADLE_PROPERTIES="$ROOT_DIR/gradle.properties"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/bootstrap-deps.sh [--clean] [--force]

Options:
  --clean   Remove the generated local Maven repository before bootstrapping.
  --force   Re-download every listed artifact even if it already exists locally.
EOF
}

clean=0
force=0

for arg in "$@"; do
  case "$arg" in
    --clean)
      clean=1
      ;;
    --force)
      force=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "$GRADLE_PROPERTIES" ]]; then
  echo "gradle.properties not found: $GRADLE_PROPERTIES" >&2
  exit 1
fi

if [[ "$clean" -eq 1 ]]; then
  rm -rf "$BOOTSTRAP_REPO"
fi

mkdir -p "$BOOTSTRAP_REPO"

downloaded=0
reused=0

property() {
  local key="$1"
  local value
  value="$(awk -F= -v lookup="$key" '$1 == lookup {print substr($0, index($0, "=") + 1)}' "$GRADLE_PROPERTIES" | tail -n 1)"
  if [[ -z "$value" ]]; then
    echo "Missing required property: $key" >&2
    exit 1
  fi
  printf '%s' "$value"
}

KOTLIN_VERSION="$(property kotlinVersion)"
KOTLIN_GRADLE_PLUGIN_VARIANT="$(property kotlinGradlePluginVariant)"
LIBGDX_VERSION="$(property libgdxVersion)"
JUNIT_VERSION="$(property junitVersion)"
JUNIT_PLATFORM_VERSION="$(property junitPlatformVersion)"
JACOCO_VERSION="$(property jacocoVersion)"
LWJGL_VERSION="$(property lwjglVersion)"
GDX_JNIGEN_LOADER_VERSION="$(property gdxJnigenLoaderVersion)"
JLAYER_VERSION="$(property jlayerVersion)"
JORBIS_VERSION="$(property jorbisVersion)"
ASM_VERSION="$(property asmVersion)"
OPENTEST4J_VERSION="$(property opentest4jVersion)"
APIGUARDIAN_VERSION="$(property apiguardianVersion)"

resolve_base_url() {
  case "$1" in
    mavenCentral)
      printf '%s' "https://repo.maven.apache.org/maven2"
      ;;
    pluginPortal)
      printf '%s' "https://plugins.gradle.org/m2"
      ;;
    *)
      echo "Unsupported repository key: $1" >&2
      exit 1
      ;;
  esac
}

download_path() {
  local repo_key="$1"
  local relative_path="$2"
  local base_url
  local target_path
  local tmp_path

  base_url="$(resolve_base_url "$repo_key")"
  target_path="$BOOTSTRAP_REPO/$relative_path"

  if [[ "$force" -eq 0 && -f "$target_path" ]]; then
    reused=$((reused + 1))
    return
  fi

  mkdir -p "$(dirname "$target_path")"
  tmp_path="${target_path}.tmp"

  curl \
    --fail \
    --location \
    --retry 3 \
    --retry-delay 1 \
    --silent \
    --show-error \
    "$base_url/$relative_path" \
    -o "$tmp_path"

  mv "$tmp_path" "$target_path"
  downloaded=$((downloaded + 1))
}

download_maven_file() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"
  local file_name="$5"
  local group_path

  group_path="$(printf '%s' "$group" | tr '.' '/')"

  download_path "$repo_key" "$group_path/$artifact/$version/$file_name"
}

download_main_artifact() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"

  download_maven_file "$repo_key" "$group" "$artifact" "$version" "$artifact-$version.jar"
  download_maven_file "$repo_key" "$group" "$artifact" "$version" "$artifact-$version.pom"
}

download_main_artifact_with_module() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"

  download_main_artifact "$repo_key" "$group" "$artifact" "$version"
  download_maven_file "$repo_key" "$group" "$artifact" "$version" "$artifact-$version.module"
}

download_pom_only() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"

  download_maven_file "$repo_key" "$group" "$artifact" "$version" "$artifact-$version.pom"
}

download_classifier_jar() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"
  local classifier="$5"

  download_maven_file "$repo_key" "$group" "$artifact" "$version" "$artifact-$version-$classifier.jar"
}

download_classifier_bundle() {
  local repo_key="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"
  shift 4

  local classifier
  for classifier in "$@"; do
    download_classifier_jar "$repo_key" "$group" "$artifact" "$version" "$classifier"
  done
}

download_path \
  pluginPortal \
  "org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/$KOTLIN_VERSION/org.jetbrains.kotlin.jvm.gradle.plugin-$KOTLIN_VERSION.pom"

download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "abi-tools-api" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "$KOTLIN_VERSION" "fus-statistics-gradle-plugin-$KOTLIN_VERSION-$KOTLIN_GRADLE_PLUGIN_VARIANT.jar"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "$KOTLIN_VERSION" "fus-statistics-gradle-plugin-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-build-statistics" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-build-tools-api" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-build-tools-impl" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-compiler-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-compiler-runner" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-daemon-client" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-daemon-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-annotations" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-api" "$KOTLIN_VERSION" "kotlin-gradle-plugin-api-$KOTLIN_VERSION-$KOTLIN_GRADLE_PLUGIN_VARIANT.jar"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-api" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-idea-proto" "$KOTLIN_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-idea" "$KOTLIN_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin-model" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin" "$KOTLIN_VERSION" "kotlin-gradle-plugin-$KOTLIN_VERSION-$KOTLIN_GRADLE_PLUGIN_VARIANT.jar"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin" "$KOTLIN_VERSION" "kotlin-gradle-plugin-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugin" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugins-bom" "$KOTLIN_VERSION" "kotlin-gradle-plugins-bom-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugins-bom" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-klib-commonizer-api" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-native-utils" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-script-runtime" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-common" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-compiler-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-compiler-impl-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-jvm" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLIN_VERSION" "kotlin-stdlib-common-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLIN_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-tooling-core" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-io" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-klib-metadata" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-klib" "$KOTLIN_VERSION"

download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.agent" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.ant" "$JACOCO_VERSION"
download_pom_only "mavenCentral" "org.jacoco" "org.jacoco.build" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.core" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.report" "$JACOCO_VERSION"
download_pom_only "mavenCentral" "org.ow2.asm" "asm-bom" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm-commons" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm-tree" "$ASM_VERSION"

download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-api" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-engine" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-params" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.platform" "junit-platform-commons" "$JUNIT_PLATFORM_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.platform" "junit-platform-engine" "$JUNIT_PLATFORM_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.platform" "junit-platform-launcher" "$JUNIT_PLATFORM_VERSION"
download_main_artifact_with_module "mavenCentral" "org.opentest4j" "opentest4j" "$OPENTEST4J_VERSION"
download_main_artifact_with_module "mavenCentral" "org.apiguardian" "apiguardian-api" "$APIGUARDIAN_VERSION"

download_main_artifact_with_module "mavenCentral" "com.badlogicgames.gdx" "gdx" "$LIBGDX_VERSION"
download_main_artifact_with_module "mavenCentral" "com.badlogicgames.gdx" "gdx-backend-lwjgl3" "$LIBGDX_VERSION"
download_classifier_jar "mavenCentral" "com.badlogicgames.gdx" "gdx-platform" "$LIBGDX_VERSION" "natives-desktop"
download_pom_only "mavenCentral" "com.badlogicgames.gdx" "gdx-platform" "$LIBGDX_VERSION"
download_main_artifact "mavenCentral" "com.badlogicgames.gdx" "gdx-jnigen-loader" "$GDX_JNIGEN_LOADER_VERSION"
download_main_artifact "mavenCentral" "com.badlogicgames.jlayer" "jlayer" "$JLAYER_VERSION"
download_main_artifact "mavenCentral" "org.jcraft" "jorbis" "$JORBIS_VERSION"

LWJGL_NATIVE_CLASSIFIERS=(
  natives-linux
  natives-linux-arm32
  natives-linux-arm64
  natives-macos
  natives-macos-arm64
  natives-windows
  natives-windows-x86
)

download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"
download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl-glfw" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl-glfw" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"
download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl-jemalloc" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl-jemalloc" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"
download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl-openal" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl-openal" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"
download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl-opengl" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl-opengl" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"
download_main_artifact "mavenCentral" "org.lwjgl" "lwjgl-stb" "$LWJGL_VERSION"
download_classifier_bundle "mavenCentral" "org.lwjgl" "lwjgl-stb" "$LWJGL_VERSION" "${LWJGL_NATIVE_CLASSIFIERS[@]}"

echo "Bootstrap repository ready: $BOOTSTRAP_REPO"
echo "Downloaded: $downloaded"
echo "Reused: $reused"
