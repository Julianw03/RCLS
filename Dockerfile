FROM debian:bookworm
LABEL authors="JulianW03"

# Update and install necessary packages
# Openjdk-17 is required for gradle Daemon
RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install -y wget \
    curl \
    tar \
    openjdk-17-jdk

# Nodejs setup
RUN curl -fsSL https://deb.nodesource.com/setup_23.x -o nodesource_setup.sh
RUN bash nodesource_setup.sh
RUN apt-get install -y nodejs

# Update npm
RUN npm install -g npm@latest

# Install JDK 21
RUN wget https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.7%2B6/OpenJDK21U-jdk_x64_linux_hotspot_21.0.7_6.tar.gz -O /tmp/openjdk-21.tar.gz && \
    mkdir -p /usr/lib/jvm/openjdk-21  && \
    tar -xzf /tmp/openjdk-21.tar.gz -C /usr/lib/jvm/openjdk-21 --strip-components=1 && \
    rm /tmp/openjdk-21.tar.gz

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/openjdk-21
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

#For local builds
RUN mkdir "volume"

# Copy local folders into the image
COPY rcls-frontend /app/rcls-frontend
COPY rcls-backend /app/rcls-backend

COPY build.sh /app/build.sh

ENTRYPOINT ["bash","build.sh"]