# use the base image passed as an argument
ARG BASE_IMAGE
FROM ${BASE_IMAGE}

WORKDIR /app

# copy the problems into docker
COPY problems /tmp/problems

# perp the problems dir
#RUN for problem in /tmp/problems/*; do \
##        problem_name=$(basename "$problem"); \
#        mkdir -p "/app/$problem"; \
#        cp -R
## if there is a judging program folder and it is a folder, copy those files into the docker program folder\
##        if [ -d "$problem/judging_program" ] && [ "$(ls -A "$problem/judging_program")" ]; then \
##            cp "$problem/judging_program/"* "/app/$problem_name/"; \
##        fi; \
##        if [ -d "$problem/other_files" ] && [ "$(ls -A "$problem/other_files")" ]; then \
##            cp "$problem/other_files/"* "/app/$problem_name/"; \
##        fi; \
##         cp "$problem/"* "/app/$problem_name/";\
#    done && \
#    rm -rf /tmp/problems
# copy the problems out of the tmp folder
RUN mkdir -p "/app/";\
    cp -R /tmp/problems/* /app/;\
    rm -rf /tmp/problems


# Install Python and pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Prepare output directories and files
RUN mkdir outputs && \
    touch outputs/output.txt outputs/score.txt