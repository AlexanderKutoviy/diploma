
VER=${1:-latest}

docker run --rm iasa/jdk-runner:$VER java -version && \
docker run --rm iasa/jdk-runner:$VER mvn --version && \
docker run --rm iasa/jdk-runner:$VER ant -version
