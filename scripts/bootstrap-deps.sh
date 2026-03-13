#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BOOTSTRAP_ROOT="$ROOT_DIR/.bootstrap"
BOOTSTRAP_REPO="$BOOTSTRAP_ROOT/m2"
BOOTSTRAP_DOWNLOAD_CACHE="$BOOTSTRAP_ROOT/download-cache"
BOOTSTRAP_GRADLE_DIR="$BOOTSTRAP_ROOT/gradle-dist"
BOOTSTRAP_GRADLE_DOWNLOAD_DIR="$BOOTSTRAP_GRADLE_DIR/downloads"
BOOTSTRAP_GRADLE_BIN_FILE="$BOOTSTRAP_ROOT/gradle-bin.path"
BOOTSTRAP_GRADLE_USER_HOME="$BOOTSTRAP_ROOT/gradle-user-home"
GRADLE_PROPERTIES="$ROOT_DIR/gradle.properties"
GRADLE_WRAPPER_PROPERTIES="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/bootstrap-deps.sh [--clean] [--force]

Options:
  --clean   Remove generated local Maven / Gradle work directories before bootstrapping.
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

if [[ ! -f "$GRADLE_WRAPPER_PROPERTIES" ]]; then
  echo "Gradle wrapper properties not found: $GRADLE_WRAPPER_PROPERTIES" >&2
  exit 1
fi

if [[ "$clean" -eq 1 ]]; then
  rm -rf "$BOOTSTRAP_REPO" "$BOOTSTRAP_GRADLE_DIR" "$BOOTSTRAP_GRADLE_USER_HOME"
  rm -f "$BOOTSTRAP_GRADLE_BIN_FILE"
fi

mkdir -p "$BOOTSTRAP_REPO"
mkdir -p "$BOOTSTRAP_DOWNLOAD_CACHE"
mkdir -p "$BOOTSTRAP_GRADLE_DOWNLOAD_DIR"

downloaded=0
reused=0

property_from_file() {
  local file="$1"
  local key="$2"
  local value
  value="$(awk -F= -v lookup="$key" '$1 == lookup {print substr($0, index($0, "=") + 1)}' "$file" | tail -n 1)"
  if [[ -z "$value" ]]; then
    echo "Missing required property: $key" >&2
    exit 1
  fi
  printf '%s' "$value"
}

optional_property_from_file() {
  local file="$1"
  local key="$2"
  awk -F= -v lookup="$key" '$1 == lookup {print substr($0, index($0, "=") + 1)}' "$file" | tail -n 1
}

property() {
  property_from_file "$GRADLE_PROPERTIES" "$1"
}

wrapper_property() {
  property_from_file "$GRADLE_WRAPPER_PROPERTIES" "$1"
}

optional_wrapper_property() {
  optional_property_from_file "$GRADLE_WRAPPER_PROPERTIES" "$1"
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
GSON_VERSION="$(property gsonVersion)"
KOTLINX_SERIALIZATION_VERSION="$(property kotlinxSerializationVersion)"
KOTLINX_COROUTINES_VERSION="$(property kotlinxCoroutinesVersion)"
ERROR_PRONE_ANNOTATIONS_VERSION="$(property errorProneAnnotationsVersion)"
JETBRAINS_ANNOTATIONS_VERSION="$(property jetbrainsAnnotationsVersion)"
KOTLINX_COROUTINES_KOTLIN_STDLIB_VERSION="$(property kotlinxCoroutinesKotlinStdlibVersion)"
KOTLIN_STDLIB_ANNOTATIONS_VERSION="$(property kotlinStdlibAnnotationsVersion)"
KOTLIN_REFLECT_VERSION="$(property kotlinReflectVersion)"
SNAKEYAML_VERSION="$(property snakeyamlVersion)"

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

download_url() {
  local url="$1"
  local cache_path="$2"
  local target_path="$3"
  local tmp_path

  if [[ "$force" -eq 0 && -f "$target_path" ]]; then
    reused=$((reused + 1))
    return
  fi

  mkdir -p "$(dirname "$target_path")"
  mkdir -p "$(dirname "$cache_path")"

  if [[ "$force" -eq 0 && -f "$cache_path" ]]; then
    cp "$cache_path" "$target_path"
    reused=$((reused + 1))
    return
  fi

  tmp_path="${cache_path}.tmp"

  curl \
    --fail \
    --location \
    --retry 3 \
    --retry-all-errors \
    --retry-delay 1 \
    --silent \
    --show-error \
    "$url" \
    -o "$tmp_path"

  mv "$tmp_path" "$cache_path"
  cp "$cache_path" "$target_path"
  downloaded=$((downloaded + 1))
}

download_path() {
  local repo_key="$1"
  local relative_path="$2"
  local base_url
  local cache_path
  local target_path

  base_url="$(resolve_base_url "$repo_key")"
  cache_path="$BOOTSTRAP_DOWNLOAD_CACHE/$repo_key/$relative_path"
  target_path="$BOOTSTRAP_REPO/$relative_path"
  download_url "$base_url/$relative_path" "$cache_path" "$target_path"
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

bootstrap_gradle_distribution() {
  local distribution_url
  local distribution_file_name
  local distribution_zip_path
  local distribution_root_name
  local distribution_home
  local distribution_bin
  local distribution_sha256
  local distribution_cache_path

  distribution_url="$(wrapper_property distributionUrl)"
  distribution_url="${distribution_url//\\:/:}"

  distribution_file_name="${distribution_url##*/}"
  distribution_zip_path="$BOOTSTRAP_GRADLE_DOWNLOAD_DIR/$distribution_file_name"
  distribution_cache_path="$BOOTSTRAP_DOWNLOAD_CACHE/gradle-distributions/$distribution_file_name"
  distribution_root_name="${distribution_file_name%.zip}"
  distribution_root_name="${distribution_root_name%-all}"
  distribution_root_name="${distribution_root_name%-bin}"
  distribution_home="$BOOTSTRAP_GRADLE_DIR/$distribution_root_name"
  distribution_bin="$distribution_home/bin/gradle"
  distribution_sha256="$(optional_wrapper_property distributionSha256Sum)"

  download_url "$distribution_url" "$distribution_cache_path" "$distribution_zip_path"

  if [[ -n "$distribution_sha256" ]]; then
    if command -v shasum >/dev/null 2>&1; then
      if [[ "$(shasum -a 256 "$distribution_zip_path" | awk '{print $1}')" != "$distribution_sha256" ]]; then
        echo "Gradle distribution checksum mismatch: $distribution_zip_path" >&2
        exit 1
      fi
    elif command -v sha256sum >/dev/null 2>&1; then
      if [[ "$(sha256sum "$distribution_zip_path" | awk '{print $1}')" != "$distribution_sha256" ]]; then
        echo "Gradle distribution checksum mismatch: $distribution_zip_path" >&2
        exit 1
      fi
    fi
  fi

  if [[ "$force" -eq 1 || ! -x "$distribution_bin" ]]; then
    rm -rf "$distribution_home"
    unzip -q "$distribution_zip_path" -d "$BOOTSTRAP_GRADLE_DIR"
  fi

  if [[ ! -x "$distribution_bin" ]]; then
    echo "Bootstrapped Gradle binary not found: $distribution_bin" >&2
    exit 1
  fi

  printf '%s\n' "$distribution_bin" > "$BOOTSTRAP_GRADLE_BIN_FILE"
}

download_path \
  pluginPortal \
  "org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/$KOTLIN_VERSION/org.jetbrains.kotlin.jvm.gradle.plugin-$KOTLIN_VERSION.pom"
download_path \
  pluginPortal \
  "org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/$KOTLIN_VERSION/org.jetbrains.kotlin.plugin.serialization.gradle.plugin-$KOTLIN_VERSION.pom"

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
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-serialization" "$KOTLIN_VERSION" "kotlin-serialization-$KOTLIN_VERSION-$KOTLIN_GRADLE_PLUGIN_VARIANT.jar"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-serialization" "$KOTLIN_VERSION" "kotlin-serialization-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-serialization" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugins-bom" "$KOTLIN_VERSION" "kotlin-gradle-plugins-bom-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-gradle-plugins-bom" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-klib-commonizer-api" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-native-utils" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-script-runtime" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-common" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-compiler-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-compiler-impl-embeddable" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-scripting-jvm" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-serialization-compiler-plugin-embeddable" "$KOTLIN_VERSION"
download_maven_file "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLIN_VERSION" "kotlin-stdlib-common-$KOTLIN_VERSION.module"
download_pom_only "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLIN_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-tooling-core" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-io" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-klib-metadata" "$KOTLIN_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-util-klib" "$KOTLIN_VERSION"
download_pom_only "mavenCentral" "com.google.code.gson" "gson-parent" "$GSON_VERSION"
download_main_artifact "mavenCentral" "com.google.code.gson" "gson" "$GSON_VERSION"
download_pom_only "mavenCentral" "com.google.errorprone" "error_prone_parent" "$ERROR_PRONE_ANNOTATIONS_VERSION"
download_main_artifact "mavenCentral" "com.google.errorprone" "error_prone_annotations" "$ERROR_PRONE_ANNOTATIONS_VERSION"
download_pom_only "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-serialization-bom" "$KOTLINX_SERIALIZATION_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-serialization-core" "$KOTLINX_SERIALIZATION_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-serialization-core-jvm" "$KOTLINX_SERIALIZATION_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-serialization-json" "$KOTLINX_SERIALIZATION_VERSION"
download_main_artifact_with_module "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-serialization-json-jvm" "$KOTLINX_SERIALIZATION_VERSION"
download_pom_only "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-coroutines-bom" "$KOTLINX_COROUTINES_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlinx" "kotlinx-coroutines-core-jvm" "$KOTLINX_COROUTINES_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains" "annotations" "$JETBRAINS_ANNOTATIONS_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains" "annotations" "$KOTLIN_STDLIB_ANNOTATIONS_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib" "$KOTLINX_COROUTINES_KOTLIN_STDLIB_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLINX_COROUTINES_KOTLIN_STDLIB_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-reflect" "$KOTLIN_REFLECT_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib" "$KOTLIN_REFLECT_VERSION"
download_main_artifact "mavenCentral" "org.jetbrains.kotlin" "kotlin-stdlib-common" "$KOTLIN_REFLECT_VERSION"

download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.agent" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.ant" "$JACOCO_VERSION"
download_pom_only "mavenCentral" "org.jacoco" "org.jacoco.build" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.core" "$JACOCO_VERSION"
download_main_artifact "mavenCentral" "org.jacoco" "org.jacoco.report" "$JACOCO_VERSION"
download_pom_only "mavenCentral" "org.ow2" "ow2" "1.5.1"
download_pom_only "mavenCentral" "org.ow2.asm" "asm-bom" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm-commons" "$ASM_VERSION"
download_main_artifact "mavenCentral" "org.ow2.asm" "asm-tree" "$ASM_VERSION"

download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-api" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-engine" "$JUNIT_VERSION"
download_main_artifact_with_module "mavenCentral" "org.junit.jupiter" "junit-jupiter-params" "$JUNIT_VERSION"
download_pom_only "mavenCentral" "org.junit" "junit-bom" "$JUNIT_VERSION"
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
download_pom_only "mavenCentral" "org.sonatype.oss" "oss-parent" "7"
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
download_main_artifact "mavenCentral" "org.yaml" "snakeyaml" "$SNAKEYAML_VERSION"

bootstrap_gradle_distribution

echo "Bootstrap repository ready: $BOOTSTRAP_REPO"
echo "Bootstrap Gradle ready: $(cat "$BOOTSTRAP_GRADLE_BIN_FILE")"
echo "Bootstrap Gradle user home: $BOOTSTRAP_GRADLE_USER_HOME"
echo "Downloaded: $downloaded"
echo "Reused: $reused"
