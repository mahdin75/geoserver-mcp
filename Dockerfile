# Use an official lightweight Python image
FROM python:3.10-slim

ENV VIRTUAL_ENV=/opt/venv

# Install pip packages and system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl gcc build-essential \
    && python -m venv $VIRTUAL_ENV \
    && $VIRTUAL_ENV/bin/pip install geoserver-mcp 

# Set path to use venv binaries
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Use ENTRYPOINT and CMD to allow dynamic arguments
ENTRYPOINT ["geoserver-mcp"]
CMD ["--url", "${GEOSERVER_URL}", "--user", "${GEOSERVER_USER}", "--password", "${GEOSERVER_PASSWORD}", "--debug"]
