# use the base image passed as an argument
ARG BASE_IMAGE
FROM ${BASE_IMAGE}

WORKDIR /app

# copy the problems into docker
COPY problems /tmp/problems

# perp the problems dir
# copy the problems out of the tmp folder
RUN mkdir -p "/app/";\
    cp -R /tmp/problems/* /app/;\
    rm -rf /tmp/problems


## Install Python and pip
#RUN apt-get update && \
#    apt-get install -y python3 python3-pip && \
#    rm -rf /var/lib/apt/lists/*

# Prepare output directories and files
RUN mkdir outputs && \
    touch outputs/output.txt outputs/score.txt