FROM debian
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

# Install JDK 23
RUN wget https://download.oracle.com/java/23/archive/jdk-23.0.2_linux-x64_bin.deb
RUN apt-get install -y ./jdk-23.0.2_linux-x64_bin.deb
RUN rm ./jdk-23.0.2_linux-x64_bin.deb

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/jdk-23.0.2-oracle-x64
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

RUN mkdir "volume"

# Copy local folders into the image
COPY rcls-frontend /app/rcls-frontend
COPY rcls-backend /app/rcls-backend

COPY build.sh /app/build.sh

ENTRYPOINT ["bash","build.sh"]